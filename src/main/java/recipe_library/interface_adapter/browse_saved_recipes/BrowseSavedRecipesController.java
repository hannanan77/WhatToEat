package recipe_library.interface_adapter.browse_saved_recipes;

import javax.swing.SwingWorker;

import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.use_case.browse_saved_recipes.BrowseSavedRecipesInputBoundary;

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
