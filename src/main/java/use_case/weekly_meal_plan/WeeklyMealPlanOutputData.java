package use_case.weekly_meal_plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Meal assignments returned for display after a successful operation. */
public class WeeklyMealPlanOutputData {
    private final List<String> entries;

    public WeeklyMealPlanOutputData(List<String> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
