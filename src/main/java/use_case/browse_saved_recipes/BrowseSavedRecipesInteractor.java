package use_case.browse_saved_recipes;

import java.io.IOException;

import use_case.recipe_library.RecipeLibraryDataAccessInterface;

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
