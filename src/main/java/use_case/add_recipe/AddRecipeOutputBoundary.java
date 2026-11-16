package use_case.add_recipe;

/**
 * Defines how the add recipe use case reports its result.
 */
public interface AddRecipeOutputBoundary {

    void prepareSuccessView(AddRecipeOutputData outputData);

    void prepareFailView(String errorMessage);
}