package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import entity.Ingredient;
import org.junit.jupiter.api.Test;
import use_case.view_nutrition.IngredientNutritionData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsdaNutritionDataAccessTest {

    private static final double TOLERANCE = 0.000_001;

    @Test
    void testReadsSupportedNutrientsFromFirstUsableMatch() {
        String response = "{\"foods\":["
                + "{\"fdcId\":1,\"description\":\"Unsupported food\","
                + "\"foodNutrients\":[{\"nutrientId\":1087,"
                + "\"unitName\":\"MG\",\"value\":10}]},"
                + "{\"fdcId\":123,\"description\":\"Rice, brown, cooked\","
                + "\"foodNutrients\":["
                + nutrient(1008, "KCAL", 129.0) + ","
                + nutrient(2047, "KCAL", 130.0) + ","
                + nutrient(1003, "G", 2.7) + ","
                + nutrient(1005, "G", 28.2) + ","
                + nutrient(1004, "G", 0.3) + "]}]}";
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        UsdaNutritionDataAccess dataAccess = new UsdaNutritionDataAccess(
                "test-key", url -> {
            requestedUrl.set(url);
            return response;
        });

        Optional<IngredientNutritionData> result =
                dataAccess.findNutrition("brown rice");

        assertTrue(result.isPresent());
        IngredientNutritionData nutrition = result.get();
        assertEquals(130.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertEquals(2.7, nutrition.getProteinGPer100g(), TOLERANCE);
        assertEquals(28.2, nutrition.getCarbohydratesGPer100g(), TOLERANCE);
        assertEquals(0.3, nutrition.getFatGPer100g(), TOLERANCE);
        assertFalse(nutrition.isEstimatedNutritionData());
        assertTrue(nutrition.getNote().contains("Rice, brown, cooked"));
        assertTrue(nutrition.getNote().contains("FDC ID 123"));
        assertTrue(requestedUrl.get().contains("api_key=test-key"));
        assertTrue(requestedUrl.get().contains("query=brown+rice"));
        assertTrue(requestedUrl.get().contains("pageSize=25"));
        assertFalse(requestedUrl.get().contains("dataType="));
    }

    @Test
    void testMissingNutrientsRemainUnavailable() {
        String response = "{\"foods\":[{\"description\":\"Peas\","
                + "\"foodNutrients\":["
                + nutrient(1003, "G", 5.4) + ","
                + nutrient(1008, "KJ", 340.0) + "]}]}";
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("peas").orElseThrow();

        assertNull(nutrition.getCaloriesKcalPer100g());
        assertEquals(5.4, nutrition.getProteinGPer100g(), TOLERANCE);
        assertNull(nutrition.getCarbohydratesGPer100g());
        assertNull(nutrition.getFatGPer100g());
    }

    @Test
    void testRawOnionIsPreferredOverDriedAndPowderedOnion() {
        String response = foods(
                food(2438059, "ONION", "Branded",
                        completeNutrients(289.0, 10.9, 59.4, 0.78)),
                food(1, "Onions, dehydrated flakes", "SR Legacy",
                        completeNutrients(349.0, 8.9, 83.3, 0.5)),
                food(2, "Spices, onion powder", "SR Legacy",
                        completeNutrients(341.0, 10.4, 79.1, 1.0)),
                food(170000, "Onions, raw", "SR Legacy",
                        completeNutrients(40.0, 1.1, 9.34, 0.1))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Onion").orElseThrow();

        assertEquals(40.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertEquals(1.1, nutrition.getProteinGPer100g(), TOLERANCE);
        assertEquals(9.34, nutrition.getCarbohydratesGPer100g(), TOLERANCE);
        assertEquals(0.1, nutrition.getFatGPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("Onions, raw"));
        assertTrue(nutrition.getNote().contains("FDC ID 170000"));
    }

    @Test
    void testGenericButterAcceptsDairyButterAndRejectsPeanutButter() {
        String response = foods(
                food(10, "Peanut butter, smooth", "Foundation",
                        completeNutrients(598.0, 22.2, 22.3, 51.4)),
                food(1932883, "BUTTER", "Branded",
                        completeNutrients(714.0, 0.0, 0.0, 78.6))
        );
        AtomicInteger requestCount = new AtomicInteger();
        UsdaNutritionDataAccess dataAccess = new UsdaNutritionDataAccess(
                "key", url -> {
            requestCount.incrementAndGet();
            return response;
        });

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Butter").orElseThrow();

        assertEquals(714.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("FDC ID 1932883"));
        assertFalse(nutrition.getNote().contains("Peanut butter"));
        assertEquals(1, requestCount.get());
    }

    @Test
    void testGenericOliveOilPrefersSurveyFoodOverBrandedResults() {
        String response = foods(
                food(2073857, "OLIVE OIL", "Branded",
                        completeNutrients(429.0, 0.0, 7.14, 42.9)),
                food(2710186, "Olive oil", "Survey (FNDDS)",
                        completeNutrients(900.0, 0.0, 0.0, 100.0)),
                food(20, "Olive oil dressing, creamy", "Branded",
                        completeNutrients(250.0, 1.0, 12.0, 22.0))
        );
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        UsdaNutritionDataAccess dataAccess = new UsdaNutritionDataAccess(
                "key", url -> {
            requestCount.incrementAndGet();
            requestedUrl.set(url);
            return response;
        });

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Olive oil").orElseThrow();

        assertEquals(900.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("FDC ID 2710186"));
        assertEquals(1, requestCount.get());
        assertTrue(requestedUrl.get().contains("query=olive+oil"));
    }

    @Test
    void testBasicChickenBreastAcceptsUsdaNamingVariation() {
        String response = foods(
                food(29, "Lunchmeat, chicken breast, sliced", "SR Legacy",
                        completeNutrients(124.0, 19.0, 2.0, 4.0)),
                food(30, "Chicken breast patty, breaded and fried", "Survey (FNDDS)",
                        completeNutrients(260.0, 15.0, 18.0, 14.0)),
                food(31, "Chicken, broilers or fryers, breast, meat only, raw",
                        "Foundation", completeNutrients(120.0, 22.5, 0.0, 2.6))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Chicken breast").orElseThrow();

        assertEquals(120.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("meat only, raw"));
        assertTrue(nutrition.getNote().contains("FDC ID 31"));
    }

    @Test
    void testExplicitCookedChickenBreastPrefersCookedPreparation() {
        String response = foods(
                food(60, "Chicken, broilers or fryers, breast, meat only, raw",
                        "Foundation", completeNutrients(120.0, 22.5, 0.0, 2.6)),
                food(61, "Chicken, broilers or fryers, breast, meat only, cooked, roasted",
                        "SR Legacy", completeNutrients(165.0, 31.0, 0.0, 3.6))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Cooked chicken breast").orElseThrow();

        assertEquals(165.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("cooked, roasted"));
        assertTrue(nutrition.getNote().contains("FDC ID 61"));
    }

    @Test
    void testExactBrandedChickenBreastIsAcceptedWhenItIsTheOnlyBasicMatch() {
        String response = foods(
                food(2187885, "CHICKEN BREAST", "Branded",
                        completeNutrients(165.0, 20.4, 1.06, 8.1)),
                food(32, "Chicken breast patty, breaded and fried", "Survey (FNDDS)",
                        completeNutrients(260.0, 15.0, 18.0, 14.0))
        );
        AtomicInteger requestCount = new AtomicInteger();
        UsdaNutritionDataAccess dataAccess = new UsdaNutritionDataAccess(
                "key", url -> {
            requestCount.incrementAndGet();
            return response;
        });

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Chicken breast").orElseThrow();

        assertEquals(165.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("FDC ID 2187885"));
        assertEquals(1, requestCount.get());
    }

    @Test
    void testLegitimateReorderedOliveOilDescriptionIsAccepted() {
        String response = foods(
                food(171413, "Oil, olive, salad or cooking", "SR Legacy",
                        completeNutrients(884.0, 0.0, 0.0, 100.0))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Olive oil").orElseThrow();

        assertEquals(884.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("FDC ID 171413"));
    }

    @Test
    void testExplicitProcessedPreparationIsRetained() {
        String response = foods(
                food(40, "Onions, raw", "Foundation",
                        completeNutrients(40.0, 1.1, 9.34, 0.1)),
                food(41, "Spices, onion powder", "SR Legacy",
                        completeNutrients(341.0, 10.4, 79.1, 1.0))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Onion powder").orElseThrow();

        assertEquals(341.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertTrue(nutrition.getNote().contains("Spices, onion powder"));
        assertTrue(nutrition.getNote().contains("FDC ID 41"));
    }

    @Test
    void testNoSuitableBasicMatchReturnsEmpty() {
        String response = foods(
                food(50, "Spices, onion powder", "SR Legacy",
                        completeNutrients(341.0, 10.4, 79.1, 1.0)),
                food(51, "Onion rings, breaded, fried", "Survey (FNDDS)",
                        completeNutrients(332.0, 4.5, 31.0, 22.0)),
                food(52, "Bread, onion", "Survey (FNDDS)",
                        completeNutrients(238.0, 8.81, 44.32, 2.95))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        assertTrue(dataAccess.findNutrition("Onion").isEmpty());
    }

    @Test
    void testCompleteUnrelatedProductDoesNotOutrankCorrectIncompleteFood() {
        String response = foods(
                food(70, "Bread, onion", "Survey (FNDDS)",
                        completeNutrients(238.0, 8.81, 44.32, 2.95)),
                food(71, "Onions, raw", "SR Legacy",
                        nutrient(1008, "KCAL", 40.0) + ","
                                + nutrient(1003, "G", 1.1))
        );
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> response);

        IngredientNutritionData nutrition =
                dataAccess.findNutrition("Onion").orElseThrow();

        assertEquals(40.0, nutrition.getCaloriesKcalPer100g(), TOLERANCE);
        assertEquals(1.1, nutrition.getProteinGPer100g(), TOLERANCE);
        assertNull(nutrition.getCarbohydratesGPer100g());
        assertNull(nutrition.getFatGPer100g());
        assertTrue(nutrition.getNote().contains("FDC ID 71"));
    }

    @Test
    void testNoMatchingFoodReturnsEmpty() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> "{\"foods\":[]}");

        assertTrue(dataAccess.findNutrition("unmatched ingredient").isEmpty());
    }

    @Test
    void testMalformedResponseFailsClearly() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> "{\"unexpected\":[]}");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dataAccess.findNutrition("rice"));
        assertTrue(exception.getMessage().contains("invalid response"));
    }

    @Test
    void testNetworkFailureFailsClearly() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> {
                    throw new IOException("offline");
                });

        UncheckedIOException exception = assertThrows(
                UncheckedIOException.class,
                () -> dataAccess.findNutrition("rice"));
        assertEquals("offline", exception.getCause().getMessage());
    }

    @Test
    void testSearchHttpFailurePropagatesClearly() {
        AtomicInteger requestCount = new AtomicInteger();
        UsdaNutritionDataAccess dataAccess = new UsdaNutritionDataAccess(
                "key", url -> {
            requestCount.incrementAndGet();
            throw new IOException("search unavailable");
        });

        UncheckedIOException exception = assertThrows(
                UncheckedIOException.class,
                () -> dataAccess.findNutrition("Chicken breast"));

        assertEquals("search unavailable", exception.getCause().getMessage());
        assertEquals(1, requestCount.get());
    }

    @Test
    void testFailedLookupCanBeRetried() {
        AtomicInteger requestCount = new AtomicInteger();
        String response = "{\"foods\":[{\"description\":\"Onions, raw\","
                + "\"foodNutrients\":[" + nutrient(1008, "KCAL", 40.0) + "]}]}";
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> {
                    if (requestCount.incrementAndGet() == 1) {
                        throw new IOException("temporary failure");
                    }
                    return response;
                });

        assertThrows(UncheckedIOException.class,
                () -> dataAccess.findNutrition("onion"));
        assertTrue(dataAccess.findNutrition("onion").isPresent());
        assertEquals(2, requestCount.get());
    }

    @Test
    void testHttpFailureFailsClearly() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> {
                    throw new IOException("USDA FoodData Central returned status 503.");
                });

        UncheckedIOException exception = assertThrows(
                UncheckedIOException.class,
                () -> dataAccess.findNutrition("rice"));
        assertTrue(exception.getCause().getMessage().contains("status 503"));
    }

    @Test
    void testMissingApiKeyFailsClearly() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess(" ", url -> "{\"foods\":[]}");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dataAccess.findNutrition("rice"));
        assertTrue(exception.getMessage().contains("USDA_API_KEY"));
    }

    @Test
    void testRepeatedIngredientUsesCachedResult() {
        AtomicInteger requestCount = new AtomicInteger();
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> {
                    requestCount.incrementAndGet();
                    return "{\"foods\":[]}";
                });

        dataAccess.findNutrition("Rice");
        dataAccess.findNutrition(" rice ");

        assertEquals(1, requestCount.get());
    }

    @Test
    void testConcurrentIngredientRequestsShareOneCachedLookup() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseRequest = new CountDownLatch(1);
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> {
                    requestCount.incrementAndGet();
                    requestStarted.countDown();
                    try {
                        releaseRequest.await(2, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException("interrupted", exception);
                    }
                    return "{\"foods\":[]}";
                });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Optional<IngredientNutritionData>> first =
                    executor.submit(() -> dataAccess.findNutrition("Rice"));
            assertTrue(requestStarted.await(2, TimeUnit.SECONDS));
            Future<Optional<IngredientNutritionData>> second =
                    executor.submit(() -> dataAccess.findNutrition(" rice "));
            releaseRequest.countDown();

            assertTrue(first.get(2, TimeUnit.SECONDS).isEmpty());
            assertTrue(second.get(2, TimeUnit.SECONDS).isEmpty());
            assertEquals(1, requestCount.get());
        }
        finally {
            releaseRequest.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void testIngredientSpecificReferenceWeights() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> "{\"foods\":[]}");

        assertEstimatedWeight(dataAccess,
                new Ingredient("Yellow Onion", 1.0, "piece"), 110.0);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Onion", 1.0, "medium"), 110.0);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Olive Oil", 1.0, "tbsp"), 13.5);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Extra-Virgin Olive Oil", 2.0, "tbsp"), 27.0);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Olive Oil", 2.0, "teaspoons"), 9.0);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Butter", 1.0, "tablespoon"), 14.2);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Unsalted Butter", 0.5, "cup"), 113.6);
    }

    @Test
    void testUnsupportedReferenceWeightsRemainUnavailable() {
        UsdaNutritionDataAccess dataAccess =
                new UsdaNutritionDataAccess("key", url -> "{\"foods\":[]}");

        assertTrue(dataAccess.estimateWeightInGrams(
                new Ingredient("Flour", 1.0, "cup")).isEmpty());
        assertTrue(dataAccess.estimateWeightInGrams(
                new Ingredient("Garlic", 1.0, "piece")).isEmpty());
        assertTrue(dataAccess.estimateWeightInGrams(
                new Ingredient("Vegetable Oil", 1.0, "tbsp")).isEmpty());
        assertTrue(dataAccess.estimateWeightInGrams(
                new Ingredient("Olive Oil", 1.0, "piece")).isEmpty());
    }

    private static void assertEstimatedWeight(UsdaNutritionDataAccess dataAccess,
                                              Ingredient ingredient,
                                              double expectedGrams) {
        assertEquals(expectedGrams,
                dataAccess.estimateWeightInGrams(ingredient).orElseThrow(), TOLERANCE);
    }

    private static String nutrient(int nutrientId, String unit, double value) {
        return "{\"nutrientId\":" + nutrientId
                + ",\"unitName\":\"" + unit
                + "\",\"value\":" + value + "}";
    }

    private static String completeNutrients(double calories, double protein,
                                            double carbohydrates, double fat) {
        return nutrient(1008, "KCAL", calories) + ","
                + nutrient(1003, "G", protein) + ","
                + nutrient(1005, "G", carbohydrates) + ","
                + nutrient(1004, "G", fat);
    }

    private static String food(long fdcId, String description, String dataType,
                               String nutrients) {
        return "{\"fdcId\":" + fdcId
                + ",\"description\":\"" + description
                + "\",\"dataType\":\"" + dataType
                + "\",\"foodNutrients\":[" + nutrients + "]}";
    }

    private static String foods(String... foods) {
        return "{\"foods\":[" + String.join(",", foods) + "]}";
    }

}
