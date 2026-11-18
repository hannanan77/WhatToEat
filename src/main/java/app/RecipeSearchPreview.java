package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import data_access.FileRecipeLibraryDataAccess;
import recipe_library.data_access.LocalRecipeImageCache;
import recipe_library.data_access.TheMealDbGateway;
import recipe_library.interface_adapter.browse_saved_recipes.BrowseSavedRecipesController;
import recipe_library.interface_adapter.browse_saved_recipes.BrowseSavedRecipesPresenter;
import recipe_library.interface_adapter.import_recipe.ImportRecipeController;
import recipe_library.interface_adapter.import_recipe.ImportRecipePresenter;
import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.interface_adapter.search_online_recipes.SearchOnlineRecipesController;
import recipe_library.interface_adapter.search_online_recipes.SearchOnlineRecipesPresenter;
import recipe_library.use_case.browse_saved_recipes.BrowseSavedRecipesInteractor;
import recipe_library.use_case.import_recipe.ImportRecipeInteractor;
import recipe_library.use_case.online_recipe.OnlineRecipeGateway;
import recipe_library.use_case.library.RecipeImageCache;
import recipe_library.use_case.library.RecipeLibraryDataAccessInterface;
import recipe_library.use_case.search_online_recipes.SearchOnlineRecipesInteractor;
import recipe_library.view.RecipesPanel;

/**
 * Development window for searching TheMealDB and importing into the local library.
 * This is not the team's shared application frame.
 */
public class RecipeSearchPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RecipeSearchPreview::show);
    }

    private static void show() {
        RecipeLibraryDataAccessInterface library =
                new FileRecipeLibraryDataAccess(FileRecipeLibraryDataAccess.defaultFile());
        RecipeImageCache images = new LocalRecipeImageCache(LocalRecipeImageCache.defaultDirectory());
        OnlineRecipeGateway gateway = new TheMealDbGateway();
        RecipeBrowserViewModel viewModel = new RecipeBrowserViewModel();

        BrowseSavedRecipesController browseController = new BrowseSavedRecipesController(
                new BrowseSavedRecipesInteractor(
                        library, new BrowseSavedRecipesPresenter(viewModel)),
                viewModel);
        SearchOnlineRecipesPresenter searchPresenter = new SearchOnlineRecipesPresenter(viewModel);
        SearchOnlineRecipesController searchController = new SearchOnlineRecipesController(
                new SearchOnlineRecipesInteractor(gateway, library, searchPresenter),
                searchPresenter,
                viewModel);
        ImportRecipeController importController = new ImportRecipeController(
                new ImportRecipeInteractor(
                        gateway,
                        library,
                        images,
                        new ImportRecipePresenter(viewModel, browseController::execute)),
                viewModel);

        RecipesPanel panel = new RecipesPanel();
        panel.bind(viewModel);
        panel.setOnlineSearchAction(() -> searchController.execute(panel.getSearchText()));
        panel.setShowCollectionAction(browseController::execute);
        panel.setImportAction(importController::execute);

        JFrame frame = new JFrame("WhatToEat - Search and Import");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        browseController.execute();
    }
}
