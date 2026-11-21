package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import data_access.FileRecipeLibraryDataAccess;
import data_access.recipe_library.LocalRecipeImageCache;
import data_access.recipe_library.TheMealDbGateway;
import interface_adapter.recipe_library.BrowseSavedRecipesController;
import interface_adapter.recipe_library.BrowseSavedRecipesPresenter;
import interface_adapter.recipe_library.ImportRecipeController;
import interface_adapter.recipe_library.ImportRecipePresenter;
import interface_adapter.recipe_library.RecipeBrowserViewModel;
import interface_adapter.recipe_library.SearchOnlineRecipesController;
import interface_adapter.recipe_library.SearchOnlineRecipesPresenter;
import use_case.recipe_library.BrowseSavedRecipesInteractor;
import use_case.recipe_library.ImportRecipeInteractor;
import use_case.recipe_library.OnlineRecipeGateway;
import use_case.recipe_library.RecipeImageCache;
import use_case.recipe_library.RecipeLibraryDataAccessInterface;
import use_case.recipe_library.SearchOnlineRecipesInteractor;
import view.recipe_library.RecipesPanel;

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
