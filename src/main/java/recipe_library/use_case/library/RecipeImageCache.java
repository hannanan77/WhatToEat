package recipe_library.use_case.library;

import java.io.IOException;

/**
 * Stores a remote recipe image where the application can read it offline.
 */
public interface RecipeImageCache {

    /**
     * Copies the image and returns a local filesystem path.
     *
     * @return the local path, or null when there is no image to cache
     */
    String cache(String recipeId, String remoteUrl) throws IOException;
}
