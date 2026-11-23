package use_case.recipe_library;

public interface BrowseSavedRecipesOutputBoundary {

    void prepareSuccessView(BrowseSavedRecipesOutputData outputData);

    void prepareFailView(String errorMessage);
}
