package entity;

import java.time.LocalDate;

/**
 * Represents a recipe assigned to a particular date and meal.
 */
public class MealPlanEntry {

    private final LocalDate date;
    private final String mealType;
    private final String recipeId;

    /**
     * Creates a single meal assignment.
     *
     * @param date the date of the meal
     * @param mealType Breakfast, Lunch, or Dinner
     * @param recipeId the ID of the associated recipe
     * @throws IllegalArgumentException if any argument is invalid
     */
    public MealPlanEntry(LocalDate date, String mealType, String recipeId) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null.");
        }

        if (mealType == null) {
            throw new IllegalArgumentException("Meal type must not be null.");
        }

        String normalizedMealType = mealType.trim();

        if (!normalizedMealType.equals("Breakfast")
                && !normalizedMealType.equals("Lunch")
                && !normalizedMealType.equals("Dinner")) {
            throw new IllegalArgumentException(
                    "Meal type must be Breakfast, Lunch, or Dinner."
            );
        }

        if (recipeId == null || recipeId.isBlank()) {
            throw new IllegalArgumentException("Recipe ID must not be blank.");
        }

        this.date = date;
        this.mealType = normalizedMealType;
        this.recipeId = recipeId.trim();
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMealType() {
        return mealType;
    }

    public String getRecipeId() {
        return recipeId;
    }
}