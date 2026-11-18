package interface_adapter.recipe_library;

import javax.swing.SwingWorker;

import use_case.recipe_library.SearchOnlineRecipesInputBoundary;
import use_case.recipe_library.SearchOnlineRecipesInputData;

public class SearchOnlineRecipesController {

    private final SearchOnlineRecipesInputBoundary interactor;
    private final SearchOnlineRecipesPresenter presenter;
    private final RecipeBrowserViewModel viewModel;

    public SearchOnlineRecipesController(SearchOnlineRecipesInputBoundary interactor,
                                         SearchOnlineRecipesPresenter presenter,
                                         RecipeBrowserViewModel viewModel) {
        this.interactor = interactor;
        this.presenter = presenter;
        this.viewModel = viewModel;
    }

    public void execute(String query) {
        int request = presenter.startRequest();
        viewModel.setStatus("Searching TheMealDB...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                presenter.runRequest(request, () ->
                        interactor.execute(new SearchOnlineRecipesInputData(query)));
                return null;
            }
        }.execute();
    }
}
