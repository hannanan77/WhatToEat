package use_case.view_nutrition;

import entity.Recipe;

/**
 * Input data for viewing the nutrition of a selected recipe.
 */
public class ViewNutritionInputData {

    private final Recipe recipe;

    public ViewNutritionInputData(Recipe recipe) {
        this.recipe = recipe;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
