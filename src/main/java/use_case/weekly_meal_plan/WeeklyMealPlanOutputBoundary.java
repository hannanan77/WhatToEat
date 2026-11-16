package use_case.weekly_meal_plan;

/** Allows the interactor to report results to the presenter. */
public interface WeeklyMealPlanOutputBoundary {
    void prepareSuccessView(WeeklyMealPlanOutputData outputData);

    void prepareFailView(String errorMessage);
}
