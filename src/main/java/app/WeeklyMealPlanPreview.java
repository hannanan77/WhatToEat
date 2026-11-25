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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Opens the weekly calendar with the local recipe collection. */
public class WeeklyMealPlanPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JPanel view = createPlanner(null);

            JFrame window = new JFrame("WhatToEat - Weekly Meal Plan");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(1280, 800);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    static JPanel createPlanner(Consumer<String> openRecipe) {
        LocalDate currentWeek = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new JPanel(new BorderLayout()) {
            private final Map<LocalDate, WeeklyMealPlanView> weeks = new HashMap<>();

            {
                showWeek(currentWeek);
            }

            private void showWeek(LocalDate monday) {
                WeeklyMealPlanView view = weeks.computeIfAbsent(monday,
                        date -> createView(date, this::showWeek, openRecipe));
                view.refreshRecipeNames();
                removeAll();
                add(view, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        };
    }

    private static WeeklyMealPlanView createView(LocalDate weekStartDate,
                                                   Consumer<LocalDate> openWeek,
                                                   Consumer<String> openRecipe) {
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
                controller, viewModel, weekStartDate, collection::load, openWeek, openRecipe);
        controller.loadPlan();
        return view;
    }
}
