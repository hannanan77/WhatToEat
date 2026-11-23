package use_case.weekly_meal_plan;

import entity.MealPlan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WeeklyMealPlanInteractorTest {
    private final MealPlan mealPlan = new MealPlan(LocalDate.of(2026, 11, 23));

    private boolean presenterCalled;

    @Test
    void addsDinnerAndPresentsUpdatedPlan() {
        WeeklyMealPlanInteractor interactor = new WeeklyMealPlanInteractor(
                mealPlan, new WeeklyMealPlanOutputBoundary() {
                    @Override
                    public void prepareSuccessView(WeeklyMealPlanOutputData outputData) {
                        presenterCalled = true;
                        assertEquals(List.of("2026-11-25 | Dinner | recipe-1"),
                                outputData.getEntries());
                    }

                    @Override
                    public void prepareFailView(String errorMessage) {
                        fail(errorMessage);
                    }
                });

        interactor.addEntry(new WeeklyMealPlanInputData("2026-11-25", "Dinner", "recipe-1"));

        assertTrue(presenterCalled);
        assertEquals(1, mealPlan.getEntries().size());
        assertEquals("recipe-1",
                mealPlan.getEntry(LocalDate.of(2026, 11, 25), "Dinner").getRecipeId());
    }

    @Test
    void rejectsInvalidDateWithoutChangingPlan() {
        WeeklyMealPlanInteractor interactor = new WeeklyMealPlanInteractor(
                mealPlan, new WeeklyMealPlanOutputBoundary() {
                    @Override
                    public void prepareSuccessView(WeeklyMealPlanOutputData outputData) {
                        fail("An invalid date should not produce a success response.");
                    }

                    @Override
                    public void prepareFailView(String errorMessage) {
                        presenterCalled = true;
                        assertEquals("Please enter a valid date in YYYY-MM-DD format.",
                                errorMessage);
                    }
                });

        interactor.addEntry(new WeeklyMealPlanInputData("11/25/2026", "Dinner", "recipe-1"));

        assertTrue(presenterCalled);
        assertTrue(mealPlan.getEntries().isEmpty());
    }
}
