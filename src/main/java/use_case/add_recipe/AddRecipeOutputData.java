package use_case.add_recipe;

/**
 * Contains information about a successfully saved recipe.
 */
public class AddRecipeOutputData {

    private final String recipeId;
    private final String recipeName;

    public AddRecipeOutputData(String recipeId, String recipeName) {
        this.recipeId = recipeId;
        this.recipeName = recipeName;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }
}