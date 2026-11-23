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
                + "{\"fdcId\":123,\"description\":\"Rice, white, cooked\","
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
        assertTrue(nutrition.getNote().contains("Rice, white, cooked"));
        assertTrue(nutrition.getNote().contains("FDC ID 123"));
        assertTrue(requestedUrl.get().contains("api_key=test-key"));
        assertTrue(requestedUrl.get().contains("query=brown+rice"));
        assertTrue(requestedUrl.get().contains("pageSize=10"));
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
                new Ingredient("Extra-Virgin Olive Oil", 2.0, "tbsp"), 27.0);
        assertEstimatedWeight(dataAccess,
                new Ingredient("Olive Oil", 2.0, "teaspoons"), 9.0);
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
}
