package recipe_library.interface_adapter.import_recipe;

import javax.swing.SwingWorker;

import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.use_case.import_recipe.ImportRecipeInputBoundary;
import recipe_library.use_case.import_recipe.ImportRecipeInputData;

public class ImportRecipeController {

    private final ImportRecipeInputBoundary interactor;
    private final RecipeBrowserViewModel viewModel;

    public ImportRecipeController(ImportRecipeInputBoundary interactor,
                                  RecipeBrowserViewModel viewModel) {
        this.interactor = interactor;
        this.viewModel = viewModel;
    }

    public void execute(String mealId) {
        viewModel.setStatus("Importing recipe...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                interactor.execute(new ImportRecipeInputData(mealId));
                return null;
            }
        }.execute();
    }
}
