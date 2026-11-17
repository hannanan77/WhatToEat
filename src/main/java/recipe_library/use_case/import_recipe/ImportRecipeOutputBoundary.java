package recipe_library.use_case.import_recipe;

public interface ImportRecipeOutputBoundary {

    void prepareSuccessView(ImportRecipeOutputData outputData);

    void prepareFailView(String errorMessage);
}
