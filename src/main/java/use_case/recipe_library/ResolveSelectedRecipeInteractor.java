package use_case.recipe_library;

import java.io.IOException;
import java.util.Optional;

import entity.Recipe;

/**
 * Loads the complete recipe for the current selection.
 * An imported TheMealDB meal is read from the library. An unsaved online meal
 * is read from the gateway and is not stored.
 */
public class ResolveSelectedRecipeInteractor {

    static final String SAVED_MISSING = "That saved recipe could not be loaded.";
    static final String SAVED_UNREADABLE = "Saved recipes could not be read.";
    static final String ONLINE_MISSING = "That recipe is no longer available.";
    static final String MISSING_SELECTION = "Choose a recipe before continuing.";

    private final RecipeLibraryDataAccessInterface library;
    private final OnlineRecipeGateway gateway;

    public ResolveSelectedRecipeInteractor(RecipeLibraryDataAccessInterface library,
                                           OnlineRecipeGateway gateway) {
        this.library = library;
        this.gateway = gateway;
    }

    /**
     * Resolves a collection recipe when {@code externalMealId} is blank.
     * Otherwise looks up that TheMealDB meal, preferring the saved copy.
     */
    public Result resolve(String recipeId, String externalMealId) {
        if (externalMealId != null && !externalMealId.isBlank()) {
            return resolveOnline(externalMealId.trim());
        }
        if (recipeId == null || recipeId.isBlank()) {
            return Result.failure(MISSING_SELECTION);
        }
        return resolveSaved(recipeId.trim());
    }

    private Result resolveSaved(String recipeId) {
        try {
            return recipeFrom(library.findById(recipeId), SAVED_MISSING);
        }
        catch (IOException exception) {
            return Result.failure(SAVED_UNREADABLE);
        }
    }

    private Result resolveOnline(String mealId) {
        try {
            Optional<RecipeLibraryRecord> saved = library.findById(OnlineRecipe.idForMeal(mealId));
            if (saved.isPresent() && saved.get().getRecipe() != null) {
                return Result.success(saved.get().getRecipe());
            }
        }
        catch (IOException exception) {
            return Result.failure(SAVED_UNREADABLE);
        }

        try {
            Optional<OnlineRecipe> found = gateway.findByMealId(mealId);
            if (found.isEmpty()) {
                return Result.failure(ONLINE_MISSING);
            }
            OnlineRecipe online = found.get();
            return Result.success(online.toRecipe(online.getRemoteImageUrl()));
        }
        catch (IOException exception) {
            return Result.failure(SearchOnlineRecipesInteractor.failureMessage(exception));
        }
    }

    private static Result recipeFrom(Optional<RecipeLibraryRecord> found, String missing) {
        if (found.isEmpty() || found.get().getRecipe() == null) {
            return Result.failure(missing);
        }
        return Result.success(found.get().getRecipe());
    }

    /**
     * A resolved recipe, or a message when it could not be loaded.
     */
    public static final class Result {

        private final Recipe recipe;
        private final String error;

        private Result(Recipe recipe, String error) {
            this.recipe = recipe;
            this.error = error;
        }

        public static Result success(Recipe recipe) {
            return new Result(recipe, null);
        }

        public static Result failure(String error) {
            return new Result(null, error);
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public String getError() {
            return error;
        }
    }
}
