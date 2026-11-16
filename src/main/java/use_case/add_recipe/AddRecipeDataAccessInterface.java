package use_case.add_recipe;

import entity.Recipe;
import java.io.IOException;

/**
 * Defines the storage operation needed by the add recipe use case.
 */
public interface AddRecipeDataAccessInterface {

    /**
     * Saves a recipe.
     *
     * @throws IOException if the recipe cannot be saved
     */
    void save(Recipe recipe) throws IOException;
}