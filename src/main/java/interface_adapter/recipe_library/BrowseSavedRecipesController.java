package interface_adapter.recipe_library;

import javax.swing.SwingWorker;

import use_case.recipe_library.BrowseSavedRecipesInputBoundary;

public class BrowseSavedRecipesController {

    private final BrowseSavedRecipesInputBoundary interactor;
    private final RecipeBrowserViewModel viewModel;

    public BrowseSavedRecipesController(BrowseSavedRecipesInputBoundary interactor,
                                        RecipeBrowserViewModel viewModel) {
        this.interactor = interactor;
        this.viewModel = viewModel;
    }

    public void execute() {
        viewModel.setStatus("Loading your collection...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                interactor.execute();
                return null;
            }
        }.execute();
    }
}
