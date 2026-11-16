package use_case.weekly_meal_plan;

/** User input for assigning a recipe to a meal. */
public class WeeklyMealPlanInputData {
    private final String date;
    private final String mealType;
    private final String recipeId;

    public WeeklyMealPlanInputData(
            String date, String mealType, String recipeId) {
        this.date = date;
        this.mealType = mealType;
        this.recipeId = recipeId;
    }

    public String getDate() {
        return date;
    }

    public String getMealType() {
        return mealType;
    }

    public String getRecipeId() {
        return recipeId;
    }
}
