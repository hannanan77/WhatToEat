package data_access;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Ingredient;
import use_case.online_recipe.OnlineRecipe;

/**
 * Converts a TheMealDB JSON document into online recipes.
 * Meals that lack a name, an ingredient, or a cooking step are skipped.
 */
public final class TheMealDbRecipeMapper {

    private TheMealDbRecipeMapper() {
    }

    public static List<OnlineRecipe> mapSearchResponse(String json) throws IOException {
        return mapMeals(readMeals(json));
    }

    public static Optional<OnlineRecipe> mapLookupResponse(String json) throws IOException {
        List<OnlineRecipe> recipes = mapMeals(readMeals(json));
        if (recipes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(0));
    }

    private static JSONArray readMeals(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("TheMealDB returned data that could not be read.");
        }
        try {
            JSONObject root = new JSONObject(json);
            if (!root.has("meals") || root.isNull("meals")) {
                return new JSONArray();
            }
            return root.getJSONArray("meals");
        }
        catch (JSONException exception) {
            throw new IOException("TheMealDB returned data that could not be read.", exception);
        }
    }

    private static List<OnlineRecipe> mapMeals(JSONArray meals) {
        List<OnlineRecipe> recipes = new ArrayList<>();
        for (int index = 0; index < meals.length(); index++) {
            JSONObject meal = meals.optJSONObject(index);
            if (meal == null) {
                continue;
            }
            OnlineRecipe recipe = mapMeal(meal);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private static OnlineRecipe mapMeal(JSONObject meal) {
        String mealId = text(meal, "idMeal");
        String name = text(meal, "strMeal");
        if (mealId == null || name == null) {
            return null;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        for (int number = 1; number <= 20; number++) {
            String ingredientName = text(meal, "strIngredient" + number);
            if (ingredientName == null) {
                continue;
            }
            IngredientMeasureParser.ParsedMeasure measure =
                    IngredientMeasureParser.parse(meal.optString("strMeasure" + number, ""));
            ingredients.add(new Ingredient(
                    ingredientName, measure.getQuantity(), measure.getUnit()));
        }
        if (ingredients.isEmpty()) {
            return null;
        }

        List<String> instructions = instructions(text(meal, "strInstructions"));
        if (instructions.isEmpty()) {
            return null;
        }

        return new OnlineRecipe(
                mealId,
                name,
                text(meal, "strCategory"),
                text(meal, "strMealThumb"),
                ingredients,
                instructions);
    }

    private static List<String> instructions(String raw) {
        List<String> steps = new ArrayList<>();
        if (raw == null) {
            return steps;
        }
        for (String step : raw.split("\\r?\\n")) {
            if (!step.isBlank()) {
                steps.add(step.trim());
            }
        }
        return steps;
    }

    private static String text(JSONObject meal, String key) {
        if (!meal.has(key) || meal.isNull(key)) {
            return null;
        }
        String value = meal.optString(key, "").trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }
}
