package interface_adapter.weekly_meal_plan;

import use_case.weekly_meal_plan.WeeklyMealPlanInputBoundary;
import use_case.weekly_meal_plan.WeeklyMealPlanInputData;

/** Passes meal assignment input from the view to the use case. */
public class WeeklyMealPlanController {
    private final WeeklyMealPlanInputBoundary interactor;

    public WeeklyMealPlanController(
            WeeklyMealPlanInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void loadPlan() {
        interactor.loadPlan();
    }

    public void clearPlan() {
        interactor.clearPlan();
    }

    public void savePlan() {
        interactor.savePlan();
    }

    public void addEntry(
            String date, String mealType, String recipeId) {
        WeeklyMealPlanInputData inputData =
                new WeeklyMealPlanInputData(date, mealType, recipeId);

        interactor.addEntry(inputData);
    }
}
