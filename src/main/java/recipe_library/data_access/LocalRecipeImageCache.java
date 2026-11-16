package recipe_library.data_access;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import recipe_library.use_case.library.RecipeImageCache;

/**
 * Saves imported recipe photos under the user's WhatToEat data directory.
 */
public class LocalRecipeImageCache implements RecipeImageCache {

    private final Path directory;

    public LocalRecipeImageCache(Path directory) {
        this.directory = directory;
    }

    public static Path defaultDirectory() {
        return Path.of(System.getProperty("user.home"), ".whattoeat", "images");
    }

    @Override
    public String cache(String recipeId, String remoteUrl) throws IOException {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return null;
        }

        BufferedImage image = read(remoteUrl.trim());
        if (image == null) {
            throw new IOException("The recipe image could not be read.");
        }

        Files.createDirectories(directory);
        String filename = recipeId.replaceAll("[^A-Za-z0-9._-]", "_") + ".jpg";
        Path target = directory.resolve(filename);
        if (!ImageIO.write(image, "jpg", target.toFile())) {
            throw new IOException("The recipe image could not be saved.");
        }
        return target.toAbsolutePath().toString();
    }

    private BufferedImage read(String remoteUrl) throws IOException {
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(remoteUrl).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("The recipe image could not be downloaded.");
            }
            try (InputStream stream = connection.getInputStream()) {
                return ImageIO.read(stream);
            }
        }
        return ImageIO.read(Path.of(remoteUrl).toFile());
    }
}
