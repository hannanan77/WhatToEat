package interface_adapter.weekly_meal_plan;

import entity.MealPlanEntry;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stores display data and notifies the view when it changes. */
public class WeeklyMealPlanViewModel {
    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private List<String> entries = new ArrayList<>();
    private List<MealPlanEntry> assignments = List.of();
    private String message = "";

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public String getMessage() {
        return message;
    }

    public List<MealPlanEntry> getAssignments() {
        return assignments;
    }

    public void showSuccess(List<String> updatedEntries, List<MealPlanEntry> updatedAssignments,
                            String successMessage) {
        entries = new ArrayList<>(updatedEntries);
        assignments = List.copyOf(updatedAssignments);
        message = successMessage;
        support.firePropertyChange("state", null, null);
    }

    public void showError(String errorMessage) {
        message = errorMessage;
        support.firePropertyChange("state", null, null);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
