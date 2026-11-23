package use_case.weekly_meal_plan;

import entity.MealPlanEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Meal assignments returned for display after a successful operation. */
public class WeeklyMealPlanOutputData {
    private final List<String> entries;

    private final List<MealPlanEntry> assignments;
    private final String message;

    public WeeklyMealPlanOutputData(List<String> entries, List<MealPlanEntry> assignments,
                                    String message) {
        this.entries = new ArrayList<>(entries);
        this.assignments = List.copyOf(assignments);
        this.message = message;
    }

    public List<MealPlanEntry> getAssignments() {
        return assignments;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
