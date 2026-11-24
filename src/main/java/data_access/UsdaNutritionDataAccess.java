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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import entity.Ingredient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.view_nutrition.IngredientNutritionData;
import use_case.view_nutrition.NutritionDataAccessInterface;

/**
 * Retrieves per-100-gram nutrition values from USDA FoodData Central.
 * Selects the most suitable basic food match with supported nutrients.
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
    private static final int SEARCH_RESULT_LIMIT = 25;

    private static final Set<String> IGNORED_MATCH_WORDS = Set.of(
            "and", "or", "with", "without", "style");
    private static final Set<String> PREPARATION_WORDS = Set.of(
            "raw", "cooked", "boiled", "roasted", "grilled", "baked",
            "dried", "dehydrated", "powder", "fried", "breaded",
            "battered", "canned", "frozen", "pickled");
    private static final Set<String> UNREQUESTED_PROCESSED_WORDS = Set.of(
            "dried", "dehydrated", "powder", "fried", "breaded", "battered",
            "ring", "soup", "sauce", "dressing", "dip", "spread", "snack",
            "chip", "bread", "lunchmeat", "luncheon", "nugget", "patty",
            "sausage", "pickled", "relish", "juice", "seasoning");
    private static final Set<String> BUTTER_SUBSTITUTE_WORDS = Set.of(
            "peanut", "almond", "cashew", "sunflower", "apple", "cocoa");

    private final String apiKey;
    private final HttpGateway httpGateway;
    private final Map<String, Optional<IngredientNutritionData>> cache =
            new ConcurrentHashMap<>();

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
        return cache.computeIfAbsent(normalizedName,
                ignored -> loadNutrition(ingredientName.trim()));
    }

    private Optional<IngredientNutritionData> loadNutrition(String ingredientName) {
        try {
            final String response = httpGateway.get(createSearchUrl(ingredientName));
            return parseResponse(response, ingredientName);
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

    @Override
    public Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
        if (ingredient == null || !ingredient.hasKnownQuantity()) {
            return Optional.empty();
        }

        final Double gramsPerUnit = referenceGramsPerUnit(
                normalizeMeasure(ingredient.getName()),
                normalizeMeasure(ingredient.getUnit()));
        if (gramsPerUnit == null) {
            return Optional.empty();
        }

        final double totalGrams = ingredient.getQuantity() * gramsPerUnit;
        return Double.isFinite(totalGrams) && totalGrams > 0.0
                ? Optional.of(totalGrams) : Optional.empty();
    }

    /**
     * Uses food-specific USDA household portions instead of a universal volume guess.
     * References: raw onion FDC 170000, olive oil FDC 171413, and butter NDB 01001.
     */
    private Double referenceGramsPerUnit(String ingredientName, String unit) {
        if (isOnion(ingredientName) && isPiece(unit)) {
            return 110.0;
        }
        if (isOliveOil(ingredientName)) {
            if (isTablespoon(unit)) {
                return 13.5;
            }
            if (isTeaspoon(unit)) {
                return 4.5;
            }
            if (isCup(unit)) {
                return 216.0;
            }
        }
        if (isButter(ingredientName)) {
            if (isTablespoon(unit)) {
                return 14.2;
            }
            if (isTeaspoon(unit)) {
                return 14.2 / 3.0;
            }
            if (isCup(unit)) {
                return 227.2;
            }
        }
        return null;
    }

    private boolean isOnion(String name) {
        return "onion".equals(name) || "yellow onion".equals(name)
                || "white onion".equals(name) || "red onion".equals(name)
                || "sweet onion".equals(name);
    }

    private boolean isOliveOil(String name) {
        return "olive oil".equals(name) || "virgin olive oil".equals(name)
                || "extra virgin olive oil".equals(name);
    }

    private boolean isButter(String name) {
        return "butter".equals(name) || "salted butter".equals(name)
                || "unsalted butter".equals(name);
    }

    private boolean isPiece(String unit) {
        return "piece".equals(unit) || "pieces".equals(unit)
                || "item".equals(unit) || "items".equals(unit)
                || "medium".equals(unit);
    }

    private boolean isTablespoon(String unit) {
        return "tbsp".equals(unit) || "tablespoon".equals(unit)
                || "tablespoons".equals(unit);
    }

    private boolean isTeaspoon(String unit) {
        return "tsp".equals(unit) || "teaspoon".equals(unit)
                || "teaspoons".equals(unit);
    }

    private boolean isCup(String unit) {
        return "cup".equals(unit) || "cups".equals(unit);
    }

    private String normalizeMeasure(String value) {
        return value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String createSearchUrl(String ingredientName) {
        final String normalizedQuery = ingredientName.toLowerCase(Locale.ROOT);
        return SEARCH_URL
                + "?api_key=" + encode(apiKey)
                + "&query=" + encode(normalizedQuery)
                + "&pageSize=" + SEARCH_RESULT_LIMIT;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Optional<IngredientNutritionData> parseResponse(String response,
                                                             String ingredientName) {
        final JSONArray foods = new JSONObject(response).getJSONArray("foods");
        IngredientNutritionData bestNutrition = null;
        int bestScore = Integer.MIN_VALUE;
        for (int index = 0; index < foods.length(); index++) {
            final JSONObject food = foods.optJSONObject(index);
            if (food == null) {
                continue;
            }

            final Optional<IngredientNutritionData> nutrition = parseFood(food);
            if (nutrition.isPresent()) {
                final int score = matchScore(ingredientName, food, nutrition.get());
                if (score > bestScore) {
                    bestScore = score;
                    bestNutrition = nutrition.get();
                }
            }
        }
        return Optional.ofNullable(bestNutrition);
    }

    private int matchScore(String ingredientName, JSONObject food,
                           IngredientNutritionData nutrition) {
        final Set<String> requestedWords = matchWords(ingredientName);
        final Set<String> descriptionWords = matchWords(
                food.optString("description", ""));
        if (requestedWords.isEmpty() || !descriptionWords.containsAll(requestedWords)) {
            return Integer.MIN_VALUE;
        }

        for (String processedWord : UNREQUESTED_PROCESSED_WORDS) {
            if (descriptionWords.contains(processedWord)
                    && !requestedWords.contains(processedWord)) {
                return Integer.MIN_VALUE;
            }
        }
        if (requestedWords.size() == 1 && requestedWords.contains("butter")) {
            for (String substitute : BUTTER_SUBSTITUTE_WORDS) {
                if (descriptionWords.contains(substitute)) {
                    return Integer.MIN_VALUE;
                }
            }
        }

        final String dataType = food.optString("dataType", "");
        int score = dataTypeScore(dataType) + nutrientCompleteness(nutrition) * 20;
        final Set<String> requestedPreparation = new HashSet<>(requestedWords);
        requestedPreparation.retainAll(PREPARATION_WORDS);
        if (!requestedPreparation.isEmpty()) {
            score += 60;
        }
        // Prefer raw food when the ingredient does not specify preparation.
        else if (descriptionWords.contains("raw")) {
            score += 50;
        }

        final String normalizedIngredient = normalizeForMatch(ingredientName);
        final String normalizedDescription = normalizeForMatch(
                food.optString("description", ""));
        if (normalizedDescription.equals(normalizedIngredient)) {
            score += 100;
        }
        else if (normalizedDescription.startsWith(normalizedIngredient + " ")) {
            score += 40;
        }
        score -= Math.min(30, Math.max(0,
                descriptionWords.size() - requestedWords.size()) * 2);
        return score;
    }

    private int dataTypeScore(String dataType) {
        if ("Foundation".equalsIgnoreCase(dataType)) {
            return 50;
        }
        if ("SR Legacy".equalsIgnoreCase(dataType)) {
            return 40;
        }
        if (dataType.toLowerCase(Locale.ROOT).contains("survey")) {
            return 20;
        }
        if ("Branded".equalsIgnoreCase(dataType)) {
            return -60;
        }
        return 10;
    }

    private int nutrientCompleteness(IngredientNutritionData nutrition) {
        int count = 0;
        count += nutrition.getCaloriesKcalPer100g() == null ? 0 : 1;
        count += nutrition.getProteinGPer100g() == null ? 0 : 1;
        count += nutrition.getCarbohydratesGPer100g() == null ? 0 : 1;
        count += nutrition.getFatGPer100g() == null ? 0 : 1;
        return count;
    }

    private Set<String> matchWords(String value) {
        final Set<String> words = new HashSet<>();
        for (String word : normalizeForMatch(value).split("\\s+")) {
            final String canonicalWord = canonicalMatchWord(word);
            if (!canonicalWord.isEmpty()
                    && !IGNORED_MATCH_WORDS.contains(canonicalWord)) {
                words.add(canonicalWord);
            }
        }
        return words;
    }

    private String normalizeForMatch(String value) {
        return value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String canonicalMatchWord(String word) {
        if ("powdered".equals(word)) {
            return "powder";
        }
        if ("flakes".equals(word) || "flaked".equals(word)) {
            return "flake";
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
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
