package interface_adapter.search_online_recipes;

import javax.swing.SwingWorker;

import interface_adapter.recipe_browser.RecipeBrowserViewModel;
import use_case.search_online_recipes.SearchOnlineRecipesInputBoundary;
import use_case.search_online_recipes.SearchOnlineRecipesInputData;

public class SearchOnlineRecipesController {

    private final SearchOnlineRecipesInputBoundary interactor;
    private final RecipeBrowserViewModel viewModel;

    public SearchOnlineRecipesController(SearchOnlineRecipesInputBoundary interactor,
                                         RecipeBrowserViewModel viewModel) {
        this.interactor = interactor;
        this.viewModel = viewModel;
    }

    public void execute(String query) {
        viewModel.setStatus("Searching TheMealDB...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                interactor.execute(new SearchOnlineRecipesInputData(query));
                return null;
            }
        }.execute();
    }
}
