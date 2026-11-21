package interface_adapter.recipe_library;

import javax.swing.SwingUtilities;

import use_case.recipe_library.ImportRecipeOutputBoundary;
import use_case.recipe_library.ImportRecipeOutputData;

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
