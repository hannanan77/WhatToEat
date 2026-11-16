package recipe_library.use_case.search_online_recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchOnlineRecipesOutputData {

    private final String query;
    private final List<OnlineRecipeMatch> matches;

    public SearchOnlineRecipesOutputData(String query, List<OnlineRecipeMatch> matches) {
        this.query = query;
        this.matches = Collections.unmodifiableList(new ArrayList<>(matches));
    }

    public String getQuery() {
        return query;
    }

    public List<OnlineRecipeMatch> getMatches() {
        return matches;
    }
}
