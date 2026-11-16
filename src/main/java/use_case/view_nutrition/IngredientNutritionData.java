package use_case.view_nutrition;

/**
 * Nutrition values for 100 grams of an ingredient.
 * A null value means that the individual nutrient is unavailable.
 */
public class IngredientNutritionData {

    private final Double caloriesKcalPer100g;
    private final Double proteinGPer100g;
    private final Double carbohydratesGPer100g;
    private final Double fatGPer100g;
    private final boolean estimatedNutritionData;
    private final String note;

    public IngredientNutritionData(Double caloriesKcalPer100g,
                                   Double proteinGPer100g,
                                   Double carbohydratesGPer100g,
                                   Double fatGPer100g,
                                   boolean estimatedNutritionData,
                                   String note) {
        validateNutrient(caloriesKcalPer100g);
        validateNutrient(proteinGPer100g);
        validateNutrient(carbohydratesGPer100g);
        validateNutrient(fatGPer100g);

        this.caloriesKcalPer100g = caloriesKcalPer100g;
        this.proteinGPer100g = proteinGPer100g;
        this.carbohydratesGPer100g = carbohydratesGPer100g;
        this.fatGPer100g = fatGPer100g;
        this.estimatedNutritionData = estimatedNutritionData;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }

    private void validateNutrient(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0.0)) {
            throw new IllegalArgumentException(
                    "Known nutrient values must be non-negative and finite.");
        }
    }

    public Double getCaloriesKcalPer100g() {
        return caloriesKcalPer100g;
    }

    public Double getProteinGPer100g() {
        return proteinGPer100g;
    }

    public Double getCarbohydratesGPer100g() {
        return carbohydratesGPer100g;
    }

    public Double getFatGPer100g() {
        return fatGPer100g;
    }

    public boolean isEstimatedNutritionData() {
        return estimatedNutritionData;
    }

    public String getNote() {
        return note;
    }
}
