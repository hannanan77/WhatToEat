package use_case.view_nutrition;

/**
 * Output boundary for presenting recipe nutrition results.
 */
public interface ViewNutritionOutputBoundary {

    void prepareSuccessView(ViewNutritionOutputData outputData);

    void prepareFailView(String errorMessage);
}
