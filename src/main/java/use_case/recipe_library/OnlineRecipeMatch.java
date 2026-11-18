package use_case.recipe_library;


public class OnlineRecipeMatch {

    private final OnlineRecipe recipe;
    private final boolean alreadyImported;

    public OnlineRecipeMatch(OnlineRecipe recipe, boolean alreadyImported) {
        this.recipe = recipe;
        this.alreadyImported = alreadyImported;
    }

    public OnlineRecipe getRecipe() {
        return recipe;
    }

    public boolean isAlreadyImported() {
        return alreadyImported;
    }
}
