package interface_adapter.import_recipe;

import javax.swing.SwingUtilities;

import interface_adapter.recipe_browser.RecipeBrowserViewModel;
import use_case.import_recipe.ImportRecipeOutputBoundary;
import use_case.import_recipe.ImportRecipeOutputData;

public class ImportRecipePresenter implements ImportRecipeOutputBoundary {

    private final RecipeBrowserViewModel viewModel;
    private final Runnable refreshCollection;

    public ImportRecipePresenter(RecipeBrowserViewModel viewModel, Runnable refreshCollection) {
        this.viewModel = viewModel;
        this.refreshCollection = refreshCollection;
    }

    @Override
    public void prepareSuccessView(ImportRecipeOutputData outputData) {
        Runnable update = () -> {
            viewModel.setStatus("Imported " + outputData.getRecipeName() + ".");
            refreshCollection.run();
        };
        runOnSwingThread(update);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        runOnSwingThread(() -> viewModel.setStatus(errorMessage));
    }

    private void runOnSwingThread(Runnable update) {
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }
}
