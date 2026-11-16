package data_access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Ingredient;
import entity.Recipe;
import use_case.add_recipe.AddRecipeDataAccessInterface;
import recipe_library.use_case.online_recipe.OnlineRecipe;
import recipe_library.use_case.library.DuplicateRecipeException;
import recipe_library.use_case.library.RecipeLibraryDataAccessInterface;
import recipe_library.use_case.library.RecipeLibraryRecord;

/**
 * Stores manually created recipes and imported recipes in one JSON file.
 */
public class FileRecipeLibraryDataAccess implements
        AddRecipeDataAccessInterface, RecipeLibraryDataAccessInterface {

    private final Path file;

    public FileRecipeLibraryDataAccess(Path file) {
        this.file = file;
    }

    /**
     * The library file used by the running application, outside the project tree.
     */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".whattoeat", "recipes.json");
    }

    @Override
    public synchronized void save(Recipe recipe) throws IOException {
        List<JSONObject> recipes = readObjects();
        recipes.add(toJson(recipe, null, null, null));
        write(recipes);
    }

    @Override
    public synchronized void saveImported(Recipe recipe,
                                          String sourceId,
                                          String category,
                                          String attribution)
            throws IOException, DuplicateRecipeException {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IOException("An imported recipe needs a TheMealDB id.");
        }
        if (existsBySourceId(sourceId)) {
            throw new DuplicateRecipeException(
                    "This recipe is already in your collection.");
        }
        List<JSONObject> recipes = readObjects();
        recipes.add(toJson(recipe, sourceId.trim(), category, attribution));
        write(recipes);
    }

    @Override
    public synchronized List<RecipeLibraryRecord> findAll() throws IOException {
        List<RecipeLibraryRecord> records = new ArrayList<>();
        for (JSONObject recipe : readObjects()) {
            records.add(toRecord(recipe));
        }
        return records;
    }

    @Override
    public synchronized Optional<RecipeLibraryRecord> findById(String id) throws IOException {
        for (RecipeLibraryRecord record : findAll()) {
            if (record.getRecipe().getId().equals(id)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized boolean existsBySourceId(String sourceId) throws IOException {
        if (sourceId == null || sourceId.isBlank()) {
            return false;
        }
        String expected = sourceId.trim();
        for (JSONObject recipe : readObjects()) {
            if (expected.equals(recipe.optString("sourceId", ""))) {
                return true;
            }
        }
        return false;
    }

    private List<JSONObject> readObjects() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JSONArray recipes = new JSONObject(json).optJSONArray("recipes");
            List<JSONObject> objects = new ArrayList<>();
            if (recipes == null) {
                return objects;
            }
            for (int index = 0; index < recipes.length(); index++) {
                JSONObject recipe = recipes.optJSONObject(index);
                if (recipe != null) {
                    objects.add(recipe);
                }
            }
            return objects;
        }
        catch (JSONException exception) {
            throw new IOException("Saved recipes could not be read.", exception);
        }
    }

    private void write(List<JSONObject> recipes) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        JSONArray array = new JSONArray();
        for (JSONObject recipe : recipes) {
            array.put(recipe);
        }
        JSONObject root = new JSONObject();
        root.put("recipes", array);
        Files.writeString(file, root.toString(2), StandardCharsets.UTF_8);
    }

    private JSONObject toJson(Recipe recipe,
                              String sourceId,
                              String category,
                              String attribution) {
        JSONObject json = new JSONObject();
        json.put("id", recipe.getId());
        json.put("name", recipe.getName());
        json.put("category", category == null ? JSONObject.NULL : category);
        json.put("servings", recipe.getServings() == null
                ? JSONObject.NULL : recipe.getServings());
        json.put("imageUrl", recipe.getImageUrl() == null
                ? JSONObject.NULL : recipe.getImageUrl());
        json.put("sourceId", sourceId == null ? JSONObject.NULL : sourceId);
        json.put("sourceName", sourceId == null
                ? JSONObject.NULL : OnlineRecipe.SOURCE_NAME);
        json.put("attribution", attribution == null ? JSONObject.NULL : attribution);

        JSONArray ingredients = new JSONArray();
        for (Ingredient ingredient : recipe.getIngredients()) {
            JSONObject item = new JSONObject();
            item.put("name", ingredient.getName());
            item.put("quantity", ingredient.getQuantity() == null
                    ? JSONObject.NULL : ingredient.getQuantity());
            item.put("unit", ingredient.getUnit());
            ingredients.put(item);
        }
        json.put("ingredients", ingredients);

        JSONArray instructions = new JSONArray();
        for (String instruction : recipe.getInstructions()) {
            instructions.put(instruction);
        }
        json.put("instructions", instructions);
        return json;
    }

    private RecipeLibraryRecord toRecord(JSONObject json) throws IOException {
        try {
            List<Ingredient> ingredients = new ArrayList<>();
            JSONArray ingredientArray = json.getJSONArray("ingredients");
            for (int index = 0; index < ingredientArray.length(); index++) {
                JSONObject item = ingredientArray.getJSONObject(index);
                ingredients.add(new Ingredient(
                        item.getString("name"),
                        nullableDouble(item, "quantity"),
                        item.getString("unit")));
            }

            List<String> instructions = new ArrayList<>();
            JSONArray instructionArray = json.getJSONArray("instructions");
            for (int index = 0; index < instructionArray.length(); index++) {
                instructions.add(instructionArray.getString(index));
            }

            Recipe recipe = new Recipe(
                    json.getString("id"),
                    json.getString("name"),
                    ingredients,
                    instructions,
                    nullableText(json, "imageUrl"),
                    nullableInteger(json, "servings"));
            return new RecipeLibraryRecord(
                    recipe,
                    nullableText(json, "category"),
                    nullableText(json, "sourceId"),
                    nullableText(json, "sourceName"),
                    nullableText(json, "attribution"));
        }
        catch (RuntimeException exception) {
            throw new IOException("Saved recipes could not be read.", exception);
        }
    }

    private static Double nullableDouble(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.getDouble(key);
    }

    private static Integer nullableInteger(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.getInt(key);
    }

    private static String nullableText(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, "").trim();
        return value.isEmpty() ? null : value;
    }
}
