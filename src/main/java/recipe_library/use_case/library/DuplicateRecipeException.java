package recipe_library.use_case.library;

/**
 * Raised when an online recipe has already been imported.
 */
public class DuplicateRecipeException extends Exception {

    public DuplicateRecipeException(String message) {
        super(message);
    }
}
