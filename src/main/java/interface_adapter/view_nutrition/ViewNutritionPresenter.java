package interface_adapter.view_nutrition;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import use_case.view_nutrition.ViewNutritionOutputBoundary;
import use_case.view_nutrition.ViewNutritionOutputData;
import use_case.view_nutrition.ViewNutritionOutputData.IngredientBreakdown;
import use_case.view_nutrition.ViewNutritionOutputData.NutritionValues;

/**
 * Formats nutrition results for the View Nutrition Swing panel.
 */
public class ViewNutritionPresenter implements ViewNutritionOutputBoundary {

    private final ViewNutritionViewModel viewModel;
    private final DecimalFormat numberFormat = new DecimalFormat("0.#");

    public ViewNutritionPresenter(ViewNutritionViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewNutritionOutputData outputData) {
        final boolean dataAvailable = outputData.getTotalNutrition().hasAnyValue();
        final boolean markIncomplete = dataAvailable && !outputData.isTotalComplete();
        final String[] totalValues = formatNutrition(
                outputData.getTotalNutrition(), outputData.containsEstimates(),
                markIncomplete);
        final String[] perServingValues = outputData.isPerServingAvailable()
                ? formatNutrition(outputData.getPerServingNutrition(),
                        outputData.containsEstimates(), markIncomplete)
                : unavailableValues();

        final List<String[]> rows = new ArrayList<>();
        for (IngredientBreakdown ingredient : outputData.getIngredientBreakdown()) {
            rows.add(formatIngredientRow(ingredient));
        }

        final String servings = outputData.getServings() == null
                ? "Servings unavailable"
                : outputData.getServings()
                        + (outputData.getServings() == 1 ? " serving" : " servings");
        final String statusMessage = createStatusMessage(outputData, dataAvailable);

        viewModel.setNutrition(outputData.getRecipeName(), servings,
                totalValues, perServingValues, rows, statusMessage,
                outputData.isTotalComplete(), dataAvailable,
                outputData.containsEstimates());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }

    private String[] formatIngredientRow(IngredientBreakdown ingredient) {
        final NutritionValues values = ingredient.getContribution();
        final boolean estimated = ingredient.isEstimatedWeight()
                || ingredient.isEstimatedNutritionData();
        return new String[] {
            ingredient.getIngredientName(),
            formatQuantity(ingredient.getQuantity()),
            formatUnit(ingredient.getUnit()),
            formatTableValue(values.getCaloriesKcal(), " kcal", estimated),
            formatTableValue(values.getProteinG(), " g", estimated),
            formatTableValue(values.getCarbohydratesG(), " g", estimated),
            formatTableValue(values.getFatG(), " g", estimated),
            formatStatus(ingredient)
        };
    }

    private String[] formatNutrition(NutritionValues values,
                                     boolean estimated, boolean incomplete) {
        return new String[] {
            formatValue(values.getCaloriesKcal(), " kcal", estimated, incomplete),
            formatValue(values.getProteinG(), " g", estimated, incomplete),
            formatValue(values.getCarbohydratesG(), " g", estimated, incomplete),
            formatValue(values.getFatG(), " g", estimated, incomplete)
        };
    }

    private String formatQuantity(Double quantity) {
        if (quantity == null) {
            return "Unknown";
        }
        return numberFormat.format(quantity);
    }

    private String formatUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "Not Available";
        }
        return unit;
    }

    private String formatValue(Double value, String unit,
                               boolean estimated, boolean incomplete) {
        if (value == null) {
            return "Not Available";
        }
        final String prefix = estimated ? "~" : "";
        final String suffix = incomplete ? "*" : "";
        return prefix + numberFormat.format(value) + unit + suffix;
    }

    private String formatTableValue(Double value, String unit, boolean estimated) {
        return value == null ? "N/A" : formatValue(value, unit, estimated, false);
    }

    private String formatStatus(IngredientBreakdown ingredient) {
        switch (ingredient.getStatus()) {
            case AVAILABLE:
                return "Available";
            case ESTIMATED:
                if (ingredient.isEstimatedWeight()
                        && ingredient.isEstimatedNutritionData()) {
                    return "Estimated (weight and nutrition data)";
                }
                if (ingredient.isEstimatedWeight()) {
                    return "Estimated (weight)";
                }
                return "Estimated (nutrition data)";
            default:
                if (ingredient.isEstimatedWeight()
                        && ingredient.isEstimatedNutritionData()) {
                    return "Not Available (estimated weight and nutrition data)";
                }
                if (ingredient.isEstimatedWeight()) {
                    return "Not Available (estimated weight)";
                }
                if (ingredient.isEstimatedNutritionData()) {
                    return "Not Available (estimated nutrition data)";
                }
                return "Not Available";
        }
    }

    private String createStatusMessage(ViewNutritionOutputData outputData,
                                       boolean dataAvailable) {
        final List<String> parts = new ArrayList<>();
        if (!dataAvailable) {
            parts.add("Nutrition is unavailable for this recipe; unavailable values "
                    + "are not treated as zero.");
        }
        else if (!outputData.isTotalComplete()) {
            parts.add("Totals include only available nutrient values; unavailable values "
                    + "are not treated as zero.");
        }
        else {
            parts.add("Nutrition totals include all listed ingredients.");
        }

        final List<String> estimatedIngredients = new ArrayList<>();
        final List<String> unavailableIngredients = new ArrayList<>();
        for (IngredientBreakdown ingredient : outputData.getIngredientBreakdown()) {
            switch (ingredient.getStatus()) {
                case ESTIMATED:
                    estimatedIngredients.add(ingredient.getIngredientName());
                    break;
                case NOT_AVAILABLE:
                    unavailableIngredients.add(ingredient.getIngredientName());
                    break;
                default:
                    break;
            }
        }
        if (!estimatedIngredients.isEmpty()) {
            parts.add("Estimated: " + String.join(", ", estimatedIngredients) + ".");
        }
        if (!unavailableIngredients.isEmpty()) {
            parts.add("Unavailable: " + String.join(", ", unavailableIngredients) + ".");
        }
        if (!outputData.isPerServingAvailable()) {
            parts.add("Per-serving values require a known serving count.");
        }
        return String.join(" ", parts);
    }

    private String[] unavailableValues() {
        return new String[] {
            "Not Available", "Not Available", "Not Available", "Not Available"
        };
    }
}
