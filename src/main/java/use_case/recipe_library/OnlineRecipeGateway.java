package use_case.recipe_library;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads recipes from an online catalogue. Implementations perform network I/O.
 */
public interface OnlineRecipeGateway {

    List<OnlineRecipe> searchByName(String name) throws IOException;

    Optional<OnlineRecipe> findByMealId(String mealId) throws IOException;
}
