package use_case.search_online_recipes;

public class SearchOnlineRecipesInputData {

    private final String query;

    public SearchOnlineRecipesInputData(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
