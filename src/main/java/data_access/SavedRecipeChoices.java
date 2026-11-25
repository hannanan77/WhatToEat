package data_access;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the names and image locations from the shared recipe collection. */
public final class SavedRecipeChoices {
    private final Path collectionFile;

    public SavedRecipeChoices(Path collectionFile) {
        this.collectionFile = collectionFile;
    }

    public Map<String, String> load() throws IOException {
        return loadField("name");
    }

    public Map<String, String> loadImages() throws IOException {
        return loadField("imageUrl");
    }

    private Map<String, String> loadField(String field) throws IOException {
        String contents;
        try {
            contents = Files.readString(collectionFile);
        } catch (NoSuchFileException exception) {
            return Map.of();
        }

        if (contents.isBlank()) {
            return Map.of();
        }

        Map<String, String> choices = new LinkedHashMap<>();
        try {
            JSONArray recipes = new JSONObject(contents).getJSONArray("recipes");
            for (int i = 0; i < recipes.length(); i++) {
                JSONObject recipe = recipes.getJSONObject(i);
                String id = recipe.getString("id");
                String name = recipe.getString("name");
                if (id.isBlank() || name.isBlank()) {
                    throw new IOException("A saved recipe is missing its ID or name.");
                }
                String value = recipe.optString(field, "");
                if (!value.isBlank()) {
                    choices.putIfAbsent(id, value);
                }
            }
        } catch (JSONException exception) {
            throw new IOException("Saved recipes could not be read.", exception);
        }
        return choices;
    }
}
