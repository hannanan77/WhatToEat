package interface_adapter.weekly_meal_plan;

import use_case.weekly_meal_plan.WeeklyMealPlanOutputBoundary;
import use_case.weekly_meal_plan.WeeklyMealPlanOutputData;

/** Updates the view model with use case results. */
public class WeeklyMealPlanPresenter
        implements WeeklyMealPlanOutputBoundary {

    private final WeeklyMealPlanViewModel viewModel;

    public WeeklyMealPlanPresenter(
            WeeklyMealPlanViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(
            WeeklyMealPlanOutputData outputData) {
        viewModel.showSuccess(outputData.getEntries(), outputData.getAssignments(),
                outputData.getMessage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
