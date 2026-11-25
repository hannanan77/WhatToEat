package app;

import data_access.FileRecipeLibraryDataAccess;
import data_access.FileWeeklyMealPlanDataAccess;
import data_access.SavedRecipeChoices;
import entity.MealPlan;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanPresenter;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;
import use_case.weekly_meal_plan.WeeklyMealPlanInteractor;
import view.WeeklyMealPlanView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Opens the weekly calendar with the local recipe collection. */
public class WeeklyMealPlanPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeeklyMealPlanView view = createView();

            JFrame window = new JFrame("WhatToEat - Weekly Meal Plan");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(1280, 800);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    static WeeklyMealPlanView createView() {
        LocalDate weekStartDate = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Path recipeFile = FileRecipeLibraryDataAccess.defaultFile();
        SavedRecipeChoices collection = new SavedRecipeChoices(recipeFile);
        MealPlan mealPlan = new MealPlan(weekStartDate);

        WeeklyMealPlanViewModel viewModel =
                new WeeklyMealPlanViewModel();

        WeeklyMealPlanPresenter presenter =
                new WeeklyMealPlanPresenter(viewModel);

        WeeklyMealPlanInteractor interactor =
                new WeeklyMealPlanInteractor(mealPlan, presenter,
                        new FileWeeklyMealPlanDataAccess(
                                recipeFile.resolveSibling("meal-plans")));

        WeeklyMealPlanController controller =
                new WeeklyMealPlanController(interactor);

        WeeklyMealPlanView view = new WeeklyMealPlanView(
                controller, viewModel, weekStartDate, collection::load);
        controller.loadPlan();
        return view;
    }
}
