package interface_adapter.recipe_library;

import javax.swing.SwingWorker;

import use_case.recipe_library.ImportRecipeInputBoundary;
import use_case.recipe_library.ImportRecipeInputData;

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
