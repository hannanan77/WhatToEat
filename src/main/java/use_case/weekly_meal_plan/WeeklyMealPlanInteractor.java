package use_case.weekly_meal_plan;

import entity.MealPlan;
import entity.MealPlanEntry;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Adds meal assignments to the current weekly plan. */
public class WeeklyMealPlanInteractor
        implements WeeklyMealPlanInputBoundary {

    private final MealPlan mealPlan;
    private final WeeklyMealPlanOutputBoundary presenter;

    public WeeklyMealPlanInteractor(
            MealPlan mealPlan,
            WeeklyMealPlanOutputBoundary presenter) {
        this.mealPlan = mealPlan;
        this.presenter = presenter;
    }

    @Override
    public void addEntry(WeeklyMealPlanInputData inputData) {
        if (inputData == null || inputData.getDate() == null) {
            presenter.prepareFailView("Please enter a meal date.");
            return;
        }

        try {
            LocalDate date = LocalDate.parse(
                    inputData.getDate().trim());

            MealPlanEntry entry = new MealPlanEntry(
                    date,
                    inputData.getMealType(),
                    inputData.getRecipeId());

            mealPlan.addEntry(entry);
        } catch (DateTimeParseException exception) {
            presenter.prepareFailView(
                    "Please enter a valid date in YYYY-MM-DD format.");
            return;
        } catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        List<String> entries = new ArrayList<>();
        for (MealPlanEntry entry : mealPlan.getEntries()) {
            entries.add(
                    entry.getDate() + " | "
                            + entry.getMealType() + " | "
                            + entry.getRecipeId());
        }

        presenter.prepareSuccessView(
                new WeeklyMealPlanOutputData(entries));
    }
}