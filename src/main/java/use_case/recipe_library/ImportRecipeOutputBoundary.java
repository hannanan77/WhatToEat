package use_case.recipe_library;

public interface ImportRecipeOutputBoundary {

    void prepareSuccessView(ImportRecipeOutputData outputData);

    void prepareFailView(String errorMessage);
}
