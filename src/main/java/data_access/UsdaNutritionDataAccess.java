
package data_access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.view_nutrition.IngredientNutritionData;
import use_case.view_nutrition.NutritionDataAccessInterface;

/**
 * Retrieves per-100-gram nutrition values from USDA FoodData Central.
 * The first relevance-ranked search result containing a supported nutrient is used.
 */
public class UsdaNutritionDataAccess implements NutritionDataAccessInterface {

    static final String SEARCH_URL =
            "https://api.nal.usda.gov/fdc/v1/foods/search";

    private static final int ENERGY = 1008;
    private static final int ENERGY_ATWATER_GENERAL = 2047;
    private static final int ENERGY_ATWATER_SPECIFIC = 2048;
    private static final int PROTEIN = 1003;
    private static final int CARBOHYDRATES = 1005;
    private static final int FAT = 1004;

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 8000;
    private static final int READ_TIMEOUT_MILLISECONDS = 8000;
    private static final int SEARCH_RESULT_LIMIT = 10;

    private final String apiKey;
    private final HttpGateway httpGateway;
    private final Map<String, Optional<IngredientNutritionData>> cache =
            new HashMap<>();

    /** Creates a USDA data access object configured by the USDA_API_KEY variable. */
    public UsdaNutritionDataAccess() {
        this(System.getenv("USDA_API_KEY"), UsdaNutritionDataAccess::get);
    }

    /** Constructor used by tests to supply deterministic API responses. */
    UsdaNutritionDataAccess(String apiKey, HttpGateway httpGateway) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        this.httpGateway = httpGateway;
    }

    @Override
    public Optional<IngredientNutritionData> findNutrition(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) {
            return Optional.empty();
        }
        if (apiKey == null) {
            throw new IllegalStateException(
                    "USDA_API_KEY is not set. Configure it before requesting nutrition data.");
        }

        final String normalizedName = ingredientName.trim().toLowerCase(Locale.ROOT);
        if (cache.containsKey(normalizedName)) {
            return cache.get(normalizedName);
        }

        try {
            final String response = httpGateway.get(createSearchUrl(ingredientName.trim()));
            final Optional<IngredientNutritionData> result = parseResponse(response);
            cache.put(normalizedName, result);
            return result;
        }
        catch (IOException exception) {
            throw new UncheckedIOException(
                    "USDA FoodData Central could not be reached.", exception);
        }
        catch (JSONException exception) {
            throw new IllegalStateException(
                    "USDA FoodData Central returned an invalid response.", exception);
        }
    }

    private String createSearchUrl(String ingredientName) {
        return SEARCH_URL
                + "?api_key=" + encode(apiKey)
                + "&query=" + encode(ingredientName)
                + "&pageSize=" + SEARCH_RESULT_LIMIT;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Optional<IngredientNutritionData> parseResponse(String response) {
        final JSONArray foods = new JSONObject(response).getJSONArray("foods");
        for (int index = 0; index < foods.length(); index++) {
            final JSONObject food = foods.optJSONObject(index);
            if (food == null) {
                continue;
            }

            final Optional<IngredientNutritionData> nutrition = parseFood(food);
            if (nutrition.isPresent()) {
                return nutrition;
            }
        }
        return Optional.empty();
    }

    private Optional<IngredientNutritionData> parseFood(JSONObject food) {
        final JSONArray nutrients = food.optJSONArray("foodNutrients");
        if (nutrients == null) {
            return Optional.empty();
        }

        Double calories = null;
        int energyPriority = Integer.MAX_VALUE;
        Double protein = null;
        Double carbohydrates = null;
        Double fat = null;

        for (int index = 0; index < nutrients.length(); index++) {
            final JSONObject nutrient = nutrients.optJSONObject(index);
            if (nutrient == null || nutrient.isNull("value")) {
                continue;
            }

            final double value = nutrient.optDouble("value", Double.NaN);
            if (!Double.isFinite(value) || value < 0.0) {
                continue;
            }

            final int nutrientId = nutrient.optInt("nutrientId", -1);
            final String unit = nutrient.optString("unitName", "");
            final int candidatePriority = energyPriority(nutrientId);
            if (candidatePriority < energyPriority && isUnit(unit, "KCAL")) {
                calories = value;
                energyPriority = candidatePriority;
            }
            else if (nutrientId == PROTEIN && isUnit(unit, "G")) {
                protein = value;
            }
            else if (nutrientId == CARBOHYDRATES && isUnit(unit, "G")) {
                carbohydrates = value;
            }
            else if (nutrientId == FAT && isUnit(unit, "G")) {
                fat = value;
            }
        }

        if (calories == null && protein == null && carbohydrates == null && fat == null) {
            return Optional.empty();
        }

        return Optional.of(new IngredientNutritionData(
                calories, protein, carbohydrates, fat, false, createSourceNote(food)));
    }

    private int energyPriority(int nutrientId) {
        if (nutrientId == ENERGY_ATWATER_GENERAL) {
            return 0;
        }
        if (nutrientId == ENERGY_ATWATER_SPECIFIC) {
            return 1;
        }
        if (nutrientId == ENERGY) {
            return 2;
        }
        return Integer.MAX_VALUE;
    }

    private boolean isUnit(String actual, String expected) {
        return expected.equalsIgnoreCase(actual);
    }

    private String createSourceNote(JSONObject food) {
        final String description = food.optString("description", "selected food").trim();
        final long fdcId = food.optLong("fdcId", -1L);
        final String identifier = fdcId < 0 ? "" : " (FDC ID " + fdcId + ")";
        return "USDA FoodData Central match: " + description + identifier + ".";
    }

    private static String get(String url) throws IOException {
        final HttpURLConnection connection =
                (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("Accept", "application/json");

        try {
            final int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "USDA FoodData Central returned status " + status + ".");
            }
            return readBody(connection.getInputStream());
        }
        finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
            return body.toString();
        }
    }

    /** Small HTTP seam that keeps ordinary unit tests offline. */
    @FunctionalInterface
    interface HttpGateway {
        String get(String url) throws IOException;
    }
}
