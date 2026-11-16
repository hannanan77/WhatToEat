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
                ? "Servings: Not Available"
                : "Servings: " + outputData.getServings();
        final String statusMessage = createStatusMessage(outputData, dataAvailable);

        viewModel.setNutrition(outputData.getRecipeName(), servings,
                totalValues, perServingValues, rows, statusMessage,
                outputData.isTotalComplete(), dataAvailable);
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
            formatQuantity(ingredient.getQuantity(), ingredient.getUnit()),
            formatValue(values.getCaloriesKcal(), " kcal", estimated, false),
            formatValue(values.getProteinG(), " g", estimated, false),
            formatValue(values.getCarbohydratesG(), " g", estimated, false),
            formatValue(values.getFatG(), " g", estimated, false),
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

    private String formatQuantity(Double quantity, String unit) {
        if (quantity == null) {
            return "Unknown (" + unit + ")";
        }
        return numberFormat.format(quantity) + " " + unit;
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
            parts.add("Nutrition is not available for this recipe.");
        }
        else if (!outputData.isTotalComplete()) {
            parts.add("Totals are incomplete and include only available values. * marks partial totals.");
        }
        else {
            parts.add("Nutrition totals include all listed ingredients.");
        }

        if (outputData.containsEstimates()) {
            parts.add("~ marks values that include estimated weight or nutrition data.");
        }
        parts.addAll(outputData.getNotes());
        return String.join("  ", parts);
    }

    private String[] unavailableValues() {
        return new String[] {
            "Not Available", "Not Available", "Not Available", "Not Available"
        };
    }
}
