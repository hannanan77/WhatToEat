package interface_adapter.add_recipe;

import use_case.add_recipe.AddRecipeOutputBoundary;
import use_case.add_recipe.AddRecipeOutputData;

import java.util.Objects;

/**
 * Converts use case results into information for the view.
 */
public class AddRecipePresenter implements AddRecipeOutputBoundary {

    private final AddRecipeViewModel viewModel;

    public AddRecipePresenter(AddRecipeViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    @Override
    public void prepareSuccessView(AddRecipeOutputData outputData) {
        String message =
                "Recipe saved: " + outputData.getRecipeName();

        viewModel.showResult(message, true);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showResult(errorMessage, false);
    }
}