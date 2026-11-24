package use_case.view_nutrition;

import entity.Recipe;

/**
 * Input data for viewing the nutrition of a selected recipe.
 */
public class ViewNutritionInputData {

    private final long requestId;
    private final Recipe recipe;

    public ViewNutritionInputData(Recipe recipe) {
        this(0L, recipe);
    }

    public ViewNutritionInputData(long requestId, Recipe recipe) {
        this.requestId = requestId;
        this.recipe = recipe;
    }

    public long getRequestId() {
        return requestId;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
