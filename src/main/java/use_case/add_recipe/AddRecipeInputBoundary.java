package use_case.add_recipe;

/**
 * Defines how to start the add recipe use case.
 */
public interface AddRecipeInputBoundary {

    void execute(AddRecipeInputData inputData);
}