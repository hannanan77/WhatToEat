package use_case.weekly_meal_plan;

import entity.MealPlan;

import java.io.IOException;
import java.time.LocalDate;

/** Stores meal plans by their start date. */
public interface WeeklyMealPlanDataAccessInterface {
    MealPlan load(LocalDate weekStartDate) throws IOException;

    void save(MealPlan mealPlan) throws IOException;
}
