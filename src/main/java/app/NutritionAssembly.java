package app;

import java.awt.Component;
import java.awt.Window;

import entity.Recipe;
import data_access.UsdaNutritionDataAccess;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionPresenter;
import interface_adapter.view_nutrition.ViewNutritionViewModel;
import use_case.view_nutrition.NutritionDataAccessInterface;
import use_case.view_nutrition.ViewNutritionInputBoundary;
import use_case.view_nutrition.ViewNutritionInteractor;
import use_case.view_nutrition.ViewNutritionOutputBoundary;
import view.ViewNutritionDialog;
import view.ViewNutritionView;

/** Constructs and retains the dependencies for the View Nutrition use case. */
public final class NutritionAssembly {

    private final ViewNutritionDialog dialog;

    /** Creates one reusable Nutrition dependency graph for the supplied owner. */
    public NutritionAssembly(Window owner) {
        final NutritionDataAccessInterface nutritionDataAccess =
                new UsdaNutritionDataAccess();
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionOutputBoundary presenter =
                new ViewNutritionPresenter(viewModel);
        final ViewNutritionInputBoundary interactor =
                new ViewNutritionInteractor(nutritionDataAccess, presenter);
        final ViewNutritionController controller =
                new ViewNutritionController(interactor, viewModel);

        final ViewNutritionView view = new ViewNutritionView(viewModel);
        view.setViewNutritionController(controller);
        dialog = new ViewNutritionDialog(owner, view);
    }

    /** Opens the reusable modal for the selected recipe. */
    public void showForRecipe(Recipe recipe, Component opener) {
        dialog.showForRecipe(recipe, opener);
    }
}
