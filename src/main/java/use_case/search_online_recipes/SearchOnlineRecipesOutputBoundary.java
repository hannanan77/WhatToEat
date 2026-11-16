package use_case.search_online_recipes;

public interface SearchOnlineRecipesOutputBoundary {

    void prepareSuccessView(SearchOnlineRecipesOutputData outputData);

    void prepareFailView(String errorMessage);
}
