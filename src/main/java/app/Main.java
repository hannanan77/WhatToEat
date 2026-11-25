package app;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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

        final JTabbedPane navigation = new JTabbedPane();
        navigation.addTab("Recipes", recipesPanel);
        navigation.addTab("Add Recipe", AddRecipePreview.createView(library, () -> {
            browseController.execute();
            navigation.setSelectedComponent(recipesPanel);
        }));
        final JPanel mealPlanTab = new JPanel(new BorderLayout());
        navigation.addTab("Weekly Meal Plan", mealPlanTab);
        navigation.addChangeListener(event -> {
            if (navigation.getSelectedComponent() == mealPlanTab
                    && mealPlanTab.getComponentCount() == 0) {
                mealPlanTab.add(WeeklyMealPlanPreview.createPlanner(null), BorderLayout.CENTER);
                mealPlanTab.revalidate();
                mealPlanTab.repaint();
            }
        });
        mainFrame.setContentPane(navigation);
        mainFrame.setSize(1100, 720);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        browseController.execute();
    }
}
