package app;

import entity.MealPlan;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanPresenter;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;
import use_case.weekly_meal_plan.WeeklyMealPlanInteractor;
import view.WeeklyMealPlanView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Opens the initial weekly meal planner for manual testing. */
public class WeeklyMealPlanPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LocalDate weekStartDate = LocalDate.now().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            MealPlan mealPlan = new MealPlan(weekStartDate);

            WeeklyMealPlanViewModel viewModel =
                    new WeeklyMealPlanViewModel();

            WeeklyMealPlanPresenter presenter =
                    new WeeklyMealPlanPresenter(viewModel);

            WeeklyMealPlanInteractor interactor =
                    new WeeklyMealPlanInteractor(mealPlan, presenter);

            WeeklyMealPlanController controller =
                    new WeeklyMealPlanController(interactor);

            WeeklyMealPlanView view = new WeeklyMealPlanView(
                    controller, viewModel, weekStartDate);

            JFrame window = new JFrame("WhatToEat - Weekly Meal Plan");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(700, 450);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
