package use_case.view_nutrition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import entity.Ingredient;
import entity.Recipe;
import org.junit.jupiter.api.Test;
import use_case.view_nutrition.ViewNutritionOutputData.DataStatus;
import use_case.view_nutrition.ViewNutritionOutputData.IngredientBreakdown;
import use_case.view_nutrition.ViewNutritionOutputData.NutritionValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewNutritionInteractorTest {

    private static final double TOLERANCE = 0.000_001;

    @Test
    void testCalculateTotalNutrition() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("rice", nutrition(100.0, 10.0, 20.0, 5.0));
        Recipe recipe = recipe(List.of(ingredient("rice", 200.0, "g")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertNutrition(output.getTotalNutrition(), 200.0, 20.0, 40.0, 10.0);
        assertTrue(output.isTotalComplete());
        assertEquals(DataStatus.AVAILABLE,
                output.getIngredientBreakdown().get(0).getStatus());
    }

    @Test
    void testCalculateNutritionPerServing() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("pasta", nutrition(100.0, 8.0, 20.0, 4.0));
        Recipe recipe = recipe(List.of(ingredient("pasta", 800.0, "g")), 4);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertNutrition(output.getTotalNutrition(), 800.0, 64.0, 160.0, 32.0);
        assertNutrition(output.getPerServingNutrition(), 200.0, 16.0, 40.0, 8.0);
        assertTrue(output.isPerServingAvailable());
    }

    @Test
    void testConvertSupportedWeightUnits() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("grams", nutrition(100.0, 10.0, 20.0, 5.0));
        dataAccess.addNutrition("kilograms", nutrition(100.0, 10.0, 20.0, 5.0));
        dataAccess.addNutrition("milligrams", nutrition(100.0, 10.0, 20.0, 5.0));
        Recipe recipe = recipe(List.of(
                ingredient("grams", 200.0, "g"),
                ingredient("kilograms", 0.2, "kg"),
                ingredient("milligrams", 200_000.0, "mg")
        ), 1);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertNutrition(output.getTotalNutrition(), 600.0, 60.0, 120.0, 30.0);
        for (IngredientBreakdown breakdown : output.getIngredientBreakdown()) {
            assertEquals(200.0,
                    breakdown.getContribution().getCaloriesKcal(), TOLERANCE);
            assertFalse(breakdown.isEstimatedWeight());
        }
    }

    @Test
    void testUnknownIngredientQuantity() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("salt", nutrition(0.0, 0.0, 0.0, 0.0));
        Recipe recipe = recipe(List.of(ingredient("salt", null, "g")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(DataStatus.NOT_AVAILABLE, breakdown.getStatus());
        assertUnavailable(breakdown.getContribution());
        assertTrue(breakdown.getNote().contains("unknown quantity"));
    }

    @Test
    void testUnsupportedUnitWithoutWeightEstimate() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("flour", nutrition(100.0, 10.0, 20.0, 5.0));
        Recipe recipe = recipe(List.of(ingredient("flour", 1.0, "cup")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(DataStatus.NOT_AVAILABLE, breakdown.getStatus());
        assertUnavailable(breakdown.getContribution());
        assertTrue(breakdown.getNote().contains("unsupported weight unit"));
    }

    @Test
    void testExplicitEstimatedIngredientWeight() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addEstimatedWeight("onion", 100.0);
        dataAccess.addNutrition("onion", nutrition(40.0, 1.0, 9.0, 0.1));
        Recipe recipe = recipe(List.of(ingredient("onion", 1.0, "item")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(DataStatus.ESTIMATED, breakdown.getStatus());
        assertTrue(breakdown.isEstimatedWeight());
        assertFalse(breakdown.isEstimatedNutritionData());
        assertTrue(output.containsEstimates());
        assertTrue(breakdown.getNote().contains("explicitly supplied estimated weight"));
    }

    @Test
    void testEstimatedNutritionSourceIsSeparateFromEstimatedWeight() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("beans",
                new IngredientNutritionData(120.0, 8.0, 20.0, 1.0,
                        true, "Reference value."));
        Recipe recipe = recipe(List.of(ingredient("beans", 100.0, "g")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(DataStatus.ESTIMATED, breakdown.getStatus());
        assertFalse(breakdown.isEstimatedWeight());
        assertTrue(breakdown.isEstimatedNutritionData());
        assertTrue(output.containsEstimates());
        assertTrue(breakdown.getNote().contains("estimated nutrition data"));
    }

    @Test
    void testMissingNutritionData() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        Recipe recipe = recipe(List.of(ingredient("saffron", 10.0, "g")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(DataStatus.NOT_AVAILABLE, breakdown.getStatus());
        assertUnavailable(breakdown.getContribution());
        assertTrue(breakdown.getNote().contains("Nutrition data is unavailable"));
    }

    @Test
    void testMissingNutrientFieldsRemainUnavailable() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("peas",
                new IngredientNutritionData(100.0, null, 20.0, null,
                        false, null));
        Recipe recipe = recipe(List.of(ingredient("peas", 100.0, "g")), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);
        IngredientBreakdown breakdown = output.getIngredientBreakdown().get(0);

        assertEquals(100.0, output.getTotalNutrition().getCaloriesKcal(), TOLERANCE);
        assertNull(output.getTotalNutrition().getProteinG());
        assertEquals(20.0, output.getTotalNutrition().getCarbohydratesG(), TOLERANCE);
        assertNull(output.getTotalNutrition().getFatG());
        assertEquals(DataStatus.NOT_AVAILABLE, breakdown.getStatus());
        assertFalse(output.isTotalComplete());
    }

    @Test
    void testPartiallyAvailableNutritionIsIncomplete() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("rice", nutrition(100.0, 10.0, 20.0, 5.0));
        Recipe recipe = recipe(List.of(
                ingredient("rice", 100.0, "g"),
                ingredient("mystery spice", 10.0, "g")
        ), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertNutrition(output.getTotalNutrition(), 100.0, 10.0, 20.0, 5.0);
        assertFalse(output.isTotalComplete());
        assertTrue(output.getTotalNutrition().hasAnyValue());
        assertTrue(output.getNotes().contains(
                "Totals include only available ingredient nutrition and are incomplete."));
    }

    @Test
    void testNoUsableNutritionData() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        Recipe recipe = recipe(List.of(
                ingredient("unknown amount", null, "g"),
                ingredient("unmatched food", 100.0, "g")
        ), 2);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertUnavailable(output.getTotalNutrition());
        assertFalse(output.getTotalNutrition().hasAnyValue());
        assertFalse(output.isTotalComplete());
        assertTrue(output.getNotes().contains(
                "No usable nutrition data is available for this recipe."));
    }

    @Test
    void testNullServingsKeepsTotalButNotPerServing() {
        FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("tofu", nutrition(100.0, 10.0, 5.0, 4.0));
        Recipe recipe = recipe(List.of(ingredient("tofu", 200.0, "g")), null);

        ViewNutritionOutputData output = execute(recipe, dataAccess);

        assertNutrition(output.getTotalNutrition(), 200.0, 20.0, 10.0, 8.0);
        assertTrue(output.isTotalComplete());
        assertFalse(output.isPerServingAvailable());
        assertUnavailable(output.getPerServingNutrition());
        assertTrue(output.getNotes().contains(
                "Per-serving nutrition is unavailable because the serving count is unknown."));
    }

    private static ViewNutritionOutputData execute(
            Recipe recipe, FakeNutritionDataAccess dataAccess) {
        CapturingOutputBoundary outputBoundary = new CapturingOutputBoundary();
        ViewNutritionInteractor interactor =
                new ViewNutritionInteractor(dataAccess, outputBoundary);

        interactor.execute(new ViewNutritionInputData(recipe));

        assertNull(outputBoundary.errorMessage);
        assertNotNull(outputBoundary.outputData);
        return outputBoundary.outputData;
    }

    private static Recipe recipe(List<Ingredient> ingredients, Integer servings) {
        return new Recipe(
                "recipe-1",
                "Test Recipe",
                ingredients,
                List.of("Cook"),
                null,
                servings
        );
    }

    private static Ingredient ingredient(String name, Double quantity, String unit) {
        return new Ingredient(name, quantity, unit);
    }

    private static IngredientNutritionData nutrition(
            Double calories, Double protein, Double carbohydrates, Double fat) {
        return new IngredientNutritionData(
                calories, protein, carbohydrates, fat, false, null);
    }

    private static void assertNutrition(NutritionValues values, double calories,
                                        double protein, double carbohydrates, double fat) {
        assertEquals(calories, values.getCaloriesKcal(), TOLERANCE);
        assertEquals(protein, values.getProteinG(), TOLERANCE);
        assertEquals(carbohydrates, values.getCarbohydratesG(), TOLERANCE);
        assertEquals(fat, values.getFatG(), TOLERANCE);
    }

    private static void assertUnavailable(NutritionValues values) {
        assertNull(values.getCaloriesKcal());
        assertNull(values.getProteinG());
        assertNull(values.getCarbohydratesG());
        assertNull(values.getFatG());
    }

    private static class FakeNutritionDataAccess
            implements NutritionDataAccessInterface {
        private final Map<String, IngredientNutritionData> nutritionByName =
                new HashMap<>();
        private final Map<String, Double> estimatedWeightByName = new HashMap<>();

        void addNutrition(String ingredientName, IngredientNutritionData nutritionData) {
            nutritionByName.put(ingredientName, nutritionData);
        }

        void addEstimatedWeight(String ingredientName, double weightInGrams) {
            estimatedWeightByName.put(ingredientName, weightInGrams);
        }

        @Override
        public Optional<IngredientNutritionData> findNutrition(String ingredientName) {
            return Optional.ofNullable(nutritionByName.get(ingredientName));
        }

        @Override
        public Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
            return Optional.ofNullable(estimatedWeightByName.get(ingredient.getName()));
        }
    }

    private static class CapturingOutputBoundary
            implements ViewNutritionOutputBoundary {
        private ViewNutritionOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(ViewNutritionOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
