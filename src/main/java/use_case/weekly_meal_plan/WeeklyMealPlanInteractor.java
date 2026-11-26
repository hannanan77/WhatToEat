package use_case.weekly_meal_plan;

import entity.MealPlan;
import entity.MealPlanEntry;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Manages the current weekly meal plan and its saved copy. */
public class WeeklyMealPlanInteractor
        implements WeeklyMealPlanInputBoundary {

    private MealPlan mealPlan;
    private final WeeklyMealPlanDataAccessInterface dataAccess;
    private final WeeklyMealPlanOutputBoundary presenter;

    public WeeklyMealPlanInteractor(
            MealPlan mealPlan,
            WeeklyMealPlanOutputBoundary presenter) {
        this(mealPlan, presenter, null);
    }

    public WeeklyMealPlanInteractor(
            MealPlan mealPlan,
            WeeklyMealPlanOutputBoundary presenter,
            WeeklyMealPlanDataAccessInterface dataAccess) {
        this.mealPlan = mealPlan;
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void addEntry(WeeklyMealPlanInputData inputData) {
        if (inputData == null || inputData.getDate() == null) {
            presenter.prepareFailView("Please enter a meal date.");
            return;
        }

        if (inputData.getRecipeId() == null || inputData.getRecipeId().isBlank()) {
            presenter.prepareFailView("Please select a recipe.");
            return;
        }

        String message;
        try {
            LocalDate date = LocalDate.parse(
                    inputData.getDate().trim());

            MealPlanEntry entry = new MealPlanEntry(
                    date,
                    inputData.getMealType(),
                    inputData.getRecipeId());

            if (mealPlan.getEntry(date, entry.getMealType()) == null) {
                mealPlan.addEntry(entry);
                message = "Meal assignment added.";
            } else {
                mealPlan.replaceEntry(entry);
                message = "Meal assignment replaced.";
            }
        } catch (DateTimeParseException exception) {
            presenter.prepareFailView(
                    "Please enter a valid date in YYYY-MM-DD format.");
            return;
        } catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        if (dataAccess == null) {
            presentPlan(message);
        } else {
            persistPlan(message + " Saved.", message + " Could not save. Try again.");
        }
    }

    @Override
    public void clearPlan() {
        mealPlan = new MealPlan(mealPlan.getWeekStartDate());
        if (dataAccess == null) {
            presentPlan("Weekly plan cleared.");
        } else {
            persistPlan("Weekly plan cleared and saved.", "Could not save the cleared week. Try again.");
        }
    }

    @Override
    public void loadPlan() {
        if (dataAccess == null) {
            presentPlan("");
            return;
        }
        try {
            mealPlan = dataAccess.load(mealPlan.getWeekStartDate());
            presentPlan("");
        } catch (IOException exception) {
            presenter.prepareFailView("Could not load the weekly meal plan.");
        }
    }

    @Override
    public void savePlan() {
        if (dataAccess == null) {
            presenter.prepareFailView("Meal plan storage is unavailable.");
            return;
        }
        persistPlan("Weekly meal plan saved.", "Could not save the weekly meal plan.");
    }

    private void persistPlan(String successMessage, String failureMessage) {
        try {
            dataAccess.save(mealPlan);
            presentPlan(successMessage);
        } catch (IOException exception) {
            presentPlan(failureMessage);
        }
    }

    private void presentPlan(String message) {
        List<MealPlanEntry> assignments = new ArrayList<>(mealPlan.getEntries());
        assignments.sort(Comparator.comparing(MealPlanEntry::getDate)
                .thenComparingInt(entry -> List.of("Breakfast", "Lunch", "Dinner")
                        .indexOf(entry.getMealType())));
        List<String> entries = new ArrayList<>();
        for (MealPlanEntry entry : assignments) {
            entries.add(
                    entry.getDate() + " | "
                            + entry.getMealType() + " | "
                            + entry.getRecipeId());
        }

        presenter.prepareSuccessView(
                new WeeklyMealPlanOutputData(entries, assignments, message));
    }
}