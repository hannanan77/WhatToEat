package use_case.browse_saved_recipes;

public interface BrowseSavedRecipesOutputBoundary {

    void prepareSuccessView(BrowseSavedRecipesOutputData outputData);

    void prepareFailView(String errorMessage);
}
