package use_case.recipe_library;

public class ImportRecipeOutputData {

    private final String recipeId;
    private final String recipeName;

    public ImportRecipeOutputData(String recipeId, String recipeName) {
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
