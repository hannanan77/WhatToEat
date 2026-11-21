package use_case.recipe_library;

public class SearchOnlineRecipesInputData {

    private final String query;

    public SearchOnlineRecipesInputData(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
