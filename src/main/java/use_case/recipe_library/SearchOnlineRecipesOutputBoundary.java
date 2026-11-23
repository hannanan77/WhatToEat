package use_case.recipe_library;

public interface SearchOnlineRecipesOutputBoundary {

    void prepareSuccessView(SearchOnlineRecipesOutputData outputData);

    void prepareFailView(String errorMessage);
}
