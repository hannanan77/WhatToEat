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
import interface_adapter.recipe_library.ResolveSelectedRecipeController;
import interface_adapter.recipe_library.SearchOnlineRecipesController;
import interface_adapter.recipe_library.SearchOnlineRecipesPresenter;
import use_case.recipe_library.BrowseSavedRecipesInteractor;
import use_case.recipe_library.ImportRecipeInteractor;
import use_case.recipe_library.OnlineRecipeGateway;
import use_case.recipe_library.RecipeImageCache;
import use_case.recipe_library.ResolveSelectedRecipeInteractor;
import use_case.recipe_library.SearchOnlineRecipesInteractor;
import view.AppNavigation;
import view.recipe_library.RecipesPanel;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showApplication);
    }

    private static void showApplication() {
        final JFrame mainFrame = new JFrame("WhatToEat");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final FileRecipeLibraryDataAccess library =
                new FileRecipeLibraryDataAccess(FileRecipeLibraryDataAccess.defaultFile());
        final RecipeImageCache images =
                new LocalRecipeImageCache(LocalRecipeImageCache.defaultDirectory());
        final OnlineRecipeGateway gateway = new TheMealDbGateway();
        final RecipeBrowserViewModel viewModel = new RecipeBrowserViewModel();

        final BrowseSavedRecipesController browseController =
                new BrowseSavedRecipesController(
                        new BrowseSavedRecipesInteractor(
                                library, new BrowseSavedRecipesPresenter(viewModel)),
                        viewModel);
        final SearchOnlineRecipesPresenter searchPresenter =
                new SearchOnlineRecipesPresenter(viewModel);
        final SearchOnlineRecipesController searchController =
                new SearchOnlineRecipesController(
                        new SearchOnlineRecipesInteractor(
                                gateway, library, searchPresenter),
                        searchPresenter,
                        viewModel);
        final ImportRecipeController importController =
                new ImportRecipeController(
                        new ImportRecipeInteractor(
                                gateway,
                                library,
                                images,
                                new ImportRecipePresenter(
                                        viewModel, browseController::execute)),
                        viewModel);
        final ResolveSelectedRecipeController selectionController =
                new ResolveSelectedRecipeController(
                        new ResolveSelectedRecipeInteractor(library, gateway));

        final RecipesPanel recipesPanel = new RecipesPanel();
        recipesPanel.bind(viewModel);
        recipesPanel.setOnlineSearchAction(
                () -> searchController.execute(recipesPanel.getSearchText()));
        recipesPanel.setShowCollectionAction(browseController::execute);
        recipesPanel.setImportAction(importController::execute);
        recipesPanel.setRecipeResolver(selectionController);

        final NutritionAssembly nutritionAssembly = new NutritionAssembly(mainFrame);
        recipesPanel.setViewNutritionAction(nutritionAssembly::showForRecipe);

        final boolean mac = System.getProperty("os.name").startsWith("Mac");
        if (mac) {
            mainFrame.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            mainFrame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            mainFrame.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        final AppNavigation navigation = new AppNavigation(mac);
        final String recipesPage = "Recipes";
        navigation.addPage(recipesPage, () -> recipesPanel);
        navigation.addPage("Add Recipe", () -> AddRecipePreview.createView(library, () -> {
            browseController.execute();
            navigation.showPage(recipesPage);
        }));
        navigation.addPage("Weekly Meal Plan", () ->
                WeeklyMealPlanPreview.createPlanner(recipeId -> {
                    navigation.showPage(recipesPage);
                    browseController.execute(() -> recipesPanel.showSavedRecipe(recipeId));
                }));
        navigation.showPage(recipesPage);
        mainFrame.setContentPane(navigation);
        mainFrame.setSize(1100, 720);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        browseController.execute();
    }
}
