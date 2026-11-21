package use_case.recipe_library;

import java.io.IOException;


/**
 * Loads the local library. It does not contact TheMealDB.
 */
public class BrowseSavedRecipesInteractor implements BrowseSavedRecipesInputBoundary {

    private final RecipeLibraryDataAccessInterface library;
    private final BrowseSavedRecipesOutputBoundary presenter;

    public BrowseSavedRecipesInteractor(RecipeLibraryDataAccessInterface library,
                                        BrowseSavedRecipesOutputBoundary presenter) {
        this.library = library;
        this.presenter = presenter;
    }

    @Override
    public void execute() {
        try {
            presenter.prepareSuccessView(new BrowseSavedRecipesOutputData(library.findAll()));
        }
        catch (IOException exception) {
            presenter.prepareFailView("Saved recipes could not be read.");
        }
    }
}
