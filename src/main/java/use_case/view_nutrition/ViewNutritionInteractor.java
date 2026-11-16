package use_case.view_nutrition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import entity.Ingredient;
import entity.Recipe;
import use_case.view_nutrition.ViewNutritionOutputData.DataStatus;
import use_case.view_nutrition.ViewNutritionOutputData.IngredientBreakdown;
import use_case.view_nutrition.ViewNutritionOutputData.NutritionValues;

/**
 * Calculates available nutrition for a selected recipe.
 */
public class ViewNutritionInteractor implements ViewNutritionInputBoundary {

    private static final int CALORIES = 0;
    private static final int PROTEIN = 1;
    private static final int CARBOHYDRATES = 2;
    private static final int FAT = 3;
    private static final int NUTRIENT_COUNT = 4;

    private final NutritionDataAccessInterface nutritionDataAccess;
    private final ViewNutritionOutputBoundary presenter;

    public ViewNutritionInteractor(NutritionDataAccessInterface nutritionDataAccess,
                                   ViewNutritionOutputBoundary presenter) {
        this.nutritionDataAccess = nutritionDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ViewNutritionInputData inputData) {
        if (inputData == null || inputData.getRecipe() == null) {
            presenter.prepareFailView("Select a recipe before viewing nutrition.");
            return;
        }

        try {
            calculateNutrition(inputData.getRecipe());
        }
        catch (RuntimeException exception) {
            presenter.prepareFailView("Nutrition information could not be retrieved.");
        }
    }

    private void calculateNutrition(Recipe recipe) {
        final double[] totals = new double[NUTRIENT_COUNT];
        final boolean[] hasValue = new boolean[NUTRIENT_COUNT];
        final List<IngredientBreakdown> breakdown = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        boolean complete = true;
        boolean containsEstimates = false;

        for (Ingredient ingredient : recipe.getIngredients()) {
            final IngredientResult result = calculateIngredient(ingredient);
            breakdown.add(result.breakdown);
            containsEstimates = containsEstimates || result.containsEstimate;

            if (!result.complete) {
                complete = false;
                notes.add(result.breakdown.getNote());
            }
            else if (result.breakdown.getNote() != null) {
                notes.add(result.breakdown.getNote());
            }

            addKnownValue(totals, hasValue, CALORIES,
                    result.breakdown.getContribution().getCaloriesKcal());
            addKnownValue(totals, hasValue, PROTEIN,
                    result.breakdown.getContribution().getProteinG());
            addKnownValue(totals, hasValue, CARBOHYDRATES,
                    result.breakdown.getContribution().getCarbohydratesG());
            addKnownValue(totals, hasValue, FAT,
                    result.breakdown.getContribution().getFatG());
        }

        final NutritionValues totalNutrition = createNutritionValues(totals, hasValue);
        final boolean hasUsableData = totalNutrition.hasAnyValue();
        complete = complete && hasUsableData;

        final Integer servings = recipe.getServings();
        final boolean validServings = servings != null && servings > 0;
        final boolean perServingAvailable = validServings && hasUsableData;
        final NutritionValues perServingNutrition = perServingAvailable
                ? divideNutrition(totalNutrition, servings)
                : unavailableNutrition();

        if (!validServings) {
            notes.add("Per-serving nutrition is unavailable because the serving count is unknown.");
        }
        if (!hasUsableData) {
            notes.add("No usable nutrition data is available for this recipe.");
        }
        else if (!complete) {
            notes.add("Totals include only available ingredient nutrition and are incomplete.");
        }

        final ViewNutritionOutputData outputData = new ViewNutritionOutputData(
                recipe.getId(), recipe.getName(), servings, totalNutrition,
                perServingNutrition, breakdown, notes, complete,
                perServingAvailable, containsEstimates);
        presenter.prepareSuccessView(outputData);
    }

    private IngredientResult calculateIngredient(Ingredient ingredient) {
        Double weightInGrams = ingredient.getWeightInGrams();
        boolean estimatedWeight = false;

        if (weightInGrams == null) {
            final Optional<Double> estimatedWeightResult =
                    nutritionDataAccess.estimateWeightInGrams(ingredient);
            if (estimatedWeightResult.isPresent()
                    && isValidWeight(estimatedWeightResult.get())) {
                weightInGrams = estimatedWeightResult.get();
                estimatedWeight = true;
            }
        }

        if (weightInGrams == null) {
            final String note = ingredient.hasKnownQuantity()
                    ? ingredient.getName() + " uses an unsupported weight unit."
                    : ingredient.getName() + " has an unknown quantity.";
            return unavailableIngredient(ingredient, note, false);
        }

        final Optional<IngredientNutritionData> nutritionResult =
                nutritionDataAccess.findNutrition(ingredient.getName());
        if (nutritionResult.isEmpty()) {
            return unavailableIngredient(ingredient,
                    "Nutrition data is unavailable for " + ingredient.getName() + ".",
                    estimatedWeight);
        }

        final IngredientNutritionData nutrition = nutritionResult.get();
        final double scale = weightInGrams / 100.0;
        final NutritionValues contribution = new NutritionValues(
                scale(nutrition.getCaloriesKcalPer100g(), scale),
                scale(nutrition.getProteinGPer100g(), scale),
                scale(nutrition.getCarbohydratesGPer100g(), scale),
                scale(nutrition.getFatGPer100g(), scale));
        final boolean allNutrientsKnown = allNutrientsKnown(contribution);
        final boolean estimatedNutrition = nutrition.isEstimatedNutritionData();
        final boolean containsEstimate = estimatedWeight || estimatedNutrition;

        final DataStatus status;
        if (!allNutrientsKnown) {
            status = DataStatus.NOT_AVAILABLE;
        }
        else if (containsEstimate) {
            status = DataStatus.ESTIMATED;
        }
        else {
            status = DataStatus.AVAILABLE;
        }

        String note = createEstimateNote(ingredient.getName(), estimatedWeight,
                estimatedNutrition, nutrition.getNote());
        if (!allNutrientsKnown) {
            note = "Some nutrient values are unavailable for " + ingredient.getName() + ".";
        }

        final IngredientBreakdown row = new IngredientBreakdown(
                ingredient.getName(), ingredient.getQuantity(), ingredient.getUnit(),
                contribution, status, estimatedWeight, estimatedNutrition, note);
        return new IngredientResult(row, allNutrientsKnown,
                containsEstimate && contribution.hasAnyValue());
    }

    private IngredientResult unavailableIngredient(Ingredient ingredient, String note,
                                                   boolean estimatedWeight) {
        final IngredientBreakdown row = new IngredientBreakdown(
                ingredient.getName(), ingredient.getQuantity(), ingredient.getUnit(),
                unavailableNutrition(), DataStatus.NOT_AVAILABLE,
                estimatedWeight, false, note);
        return new IngredientResult(row, false, false);
    }

    private String createEstimateNote(String ingredientName, boolean estimatedWeight,
                                      boolean estimatedNutrition, String dataNote) {
        String estimateNote = null;
        if (estimatedWeight && estimatedNutrition) {
            estimateNote = ingredientName
                    + " uses an estimated weight and estimated nutrition data.";
        }
        else if (estimatedWeight) {
            estimateNote = ingredientName + " uses an explicitly supplied estimated weight.";
        }
        else if (estimatedNutrition) {
            estimateNote = ingredientName + " uses estimated nutrition data.";
        }

        if (estimateNote == null) {
            return dataNote;
        }
        if (dataNote == null) {
            return estimateNote;
        }
        return estimateNote + " " + dataNote;
    }

    private boolean isValidWeight(Double weight) {
        return weight != null && Double.isFinite(weight) && weight > 0.0;
    }

    private Double scale(Double nutrientPer100g, double factor) {
        return nutrientPer100g == null ? null : nutrientPer100g * factor;
    }

    private boolean allNutrientsKnown(NutritionValues values) {
        return values.getCaloriesKcal() != null && values.getProteinG() != null
                && values.getCarbohydratesG() != null && values.getFatG() != null;
    }

    private void addKnownValue(double[] totals, boolean[] hasValue,
                               int nutrientIndex, Double value) {
        if (value != null) {
            totals[nutrientIndex] += value;
            hasValue[nutrientIndex] = true;
        }
    }

    private NutritionValues createNutritionValues(double[] totals, boolean[] hasValue) {
        return new NutritionValues(
                hasValue[CALORIES] ? totals[CALORIES] : null,
                hasValue[PROTEIN] ? totals[PROTEIN] : null,
                hasValue[CARBOHYDRATES] ? totals[CARBOHYDRATES] : null,
                hasValue[FAT] ? totals[FAT] : null);
    }

    private NutritionValues divideNutrition(NutritionValues values, int servings) {
        return new NutritionValues(
                divide(values.getCaloriesKcal(), servings),
                divide(values.getProteinG(), servings),
                divide(values.getCarbohydratesG(), servings),
                divide(values.getFatG(), servings));
    }

    private Double divide(Double value, int servings) {
        return value == null ? null : value / servings;
    }

    private NutritionValues unavailableNutrition() {
        return new NutritionValues(null, null, null, null);
    }

    /** Keeps the calculation result for one pass through the ingredient loop. */
    private static class IngredientResult {
        private final IngredientBreakdown breakdown;
        private final boolean complete;
        private final boolean containsEstimate;

        IngredientResult(IngredientBreakdown breakdown, boolean complete,
                         boolean containsEstimate) {
            this.breakdown = breakdown;
            this.complete = complete;
            this.containsEstimate = containsEstimate;
        }
    }
}
