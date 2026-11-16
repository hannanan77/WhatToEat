package data_access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import use_case.online_recipe.OnlineRecipe;
import use_case.online_recipe.OnlineRecipeGateway;

/**
 * TheMealDB V1 developer API. The test key is the public key {@code 1}.
 */
public class TheMealDbGateway implements OnlineRecipeGateway {

    static final String SEARCH_URL =
            "https://www.themealdb.com/api/json/v1/1/search.php?s=";
    static final String LOOKUP_URL =
            "https://www.themealdb.com/api/json/v1/1/lookup.php?i=";

    @Override
    public List<OnlineRecipe> searchByName(String name) throws IOException {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return TheMealDbRecipeMapper.mapSearchResponse(get(SEARCH_URL + encoded));
    }

    @Override
    public Optional<OnlineRecipe> findByMealId(String mealId) throws IOException {
        if (mealId == null || !mealId.matches("\\d+")) {
            return Optional.empty();
        }
        return TheMealDbRecipeMapper.mapLookupResponse(get(LOOKUP_URL + mealId));
    }

    private String get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");

        int status = connection.getResponseCode();
        InputStream stream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (status != HttpURLConnection.HTTP_OK || stream == null) {
            throw new IOException("TheMealDB could not be reached (status " + status + ").");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
            return body.toString();
        }
    }
}
