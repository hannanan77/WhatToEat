package recipe_library.interface_adapter.resolve_selected_recipe;

import java.util.function.Consumer;

import entity.Recipe;

/**
 * Loads one selected recipe. Callers must be able to ignore a late response.
 * Both callbacks run on the Swing event dispatch thread.
 */
@FunctionalInterface
public interface RecipeSelectionResolver {

    /**
     * @param recipeId display id of a saved recipe; unused when a meal id is present
     * @param externalMealId TheMealDB meal id, or null for a collection recipe
     * @param onSuccess receives the complete recipe
     * @param onFailure receives a short message and no recipe
     */
    void resolve(String recipeId,
                 String externalMealId,
                 Consumer<Recipe> onSuccess,
                 Consumer<String> onFailure);
}
