package use_case.recipe_library;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import entity.Recipe;

/**
 * Shared local recipe library used by online import and by Build My Own.
 * Build My Own continues to call {@code AddRecipeDataAccessInterface.save}.
 * The file implementation of that interface is this same library.
 */
public interface RecipeLibraryDataAccessInterface {

    List<RecipeLibraryRecord> findAll() throws IOException;

    Optional<RecipeLibraryRecord> findById(String id) throws IOException;

    boolean existsBySourceId(String sourceId) throws IOException;

    /**
     * Saves an imported recipe. The same source id cannot be stored twice.
     */
    void saveImported(Recipe recipe,
                      String sourceId,
                      String category,
                      String attribution) throws IOException, DuplicateRecipeException;
}
