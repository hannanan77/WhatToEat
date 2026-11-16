package use_case.view_nutrition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculation results for the View Recipe Nutrition use case.
 */
public class ViewNutritionOutputData {

    /** Availability shown for one ingredient row. */
    public enum DataStatus {
        AVAILABLE,
        ESTIMATED,
        NOT_AVAILABLE
    }

    /** Four nutrition values. Null fields remain explicitly unavailable. */
    public static class NutritionValues {
        private final Double caloriesKcal;
        private final Double proteinG;
        private final Double carbohydratesG;
        private final Double fatG;

        public NutritionValues(Double caloriesKcal, Double proteinG,
                               Double carbohydratesG, Double fatG) {
            this.caloriesKcal = caloriesKcal;
            this.proteinG = proteinG;
            this.carbohydratesG = carbohydratesG;
            this.fatG = fatG;
        }

        public Double getCaloriesKcal() {
            return caloriesKcal;
        }

        public Double getProteinG() {
            return proteinG;
        }

        public Double getCarbohydratesG() {
            return carbohydratesG;
        }

        public Double getFatG() {
            return fatG;
        }

        public boolean hasAnyValue() {
            return caloriesKcal != null || proteinG != null
                    || carbohydratesG != null || fatG != null;
        }
    }

    /** Nutrition contribution and availability information for one ingredient. */
    public static class IngredientBreakdown {
        private final String ingredientName;
        private final Double quantity;
        private final String unit;
        private final NutritionValues contribution;
        private final DataStatus status;
        private final boolean estimatedWeight;
        private final boolean estimatedNutritionData;
        private final String note;

        public IngredientBreakdown(String ingredientName, Double quantity, String unit,
                                   NutritionValues contribution, DataStatus status,
                                   boolean estimatedWeight, boolean estimatedNutritionData,
                                   String note) {
            this.ingredientName = ingredientName;
            this.quantity = quantity;
            this.unit = unit;
            this.contribution = contribution;
            this.status = status;
            this.estimatedWeight = estimatedWeight;
            this.estimatedNutritionData = estimatedNutritionData;
            this.note = note;
        }

        public String getIngredientName() {
            return ingredientName;
        }

        public Double getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }

        public NutritionValues getContribution() {
            return contribution;
        }

        public DataStatus getStatus() {
            return status;
        }

        public boolean isEstimatedWeight() {
            return estimatedWeight;
        }

        public boolean isEstimatedNutritionData() {
            return estimatedNutritionData;
        }

        public String getNote() {
            return note;
        }
    }

    private final String recipeId;
    private final String recipeName;
    private final Integer servings;
    private final NutritionValues totalNutrition;
    private final NutritionValues perServingNutrition;
    private final List<IngredientBreakdown> ingredientBreakdown;
    private final List<String> notes;
    private final boolean totalComplete;
    private final boolean perServingAvailable;
    private final boolean containsEstimates;

    public ViewNutritionOutputData(String recipeId, String recipeName, Integer servings,
                                   NutritionValues totalNutrition,
                                   NutritionValues perServingNutrition,
                                   List<IngredientBreakdown> ingredientBreakdown,
                                   List<String> notes, boolean totalComplete,
                                   boolean perServingAvailable, boolean containsEstimates) {
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.servings = servings;
        this.totalNutrition = totalNutrition;
        this.perServingNutrition = perServingNutrition;
        this.ingredientBreakdown = Collections.unmodifiableList(
                new ArrayList<>(ingredientBreakdown));
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.totalComplete = totalComplete;
        this.perServingAvailable = perServingAvailable;
        this.containsEstimates = containsEstimates;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public Integer getServings() {
        return servings;
    }

    public NutritionValues getTotalNutrition() {
        return totalNutrition;
    }

    public NutritionValues getPerServingNutrition() {
        return perServingNutrition;
    }

    public List<IngredientBreakdown> getIngredientBreakdown() {
        return ingredientBreakdown;
    }

    public List<String> getNotes() {
        return notes;
    }

    public boolean isTotalComplete() {
        return totalComplete;
    }

    public boolean isPerServingAvailable() {
        return perServingAvailable;
    }

    public boolean containsEstimates() {
        return containsEstimates;
    }
}
