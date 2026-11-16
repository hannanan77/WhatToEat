package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A weekly meal plan with at most one assignment per date and meal type.
 * The week includes weekStartDate and the following six days.
 */
public class MealPlan {
    private final LocalDate weekStartDate;
    private final List<MealPlanEntry> entries;

    /** Creates an empty plan. The caller supplies the first day of the week. */
    public MealPlan(LocalDate weekStartDate) {
        if (weekStartDate == null) {
            throw new IllegalArgumentException("Week start date must not be null.");
        }
        this.weekStartDate = weekStartDate;
        this.entries = new ArrayList<>();
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    /** Returns an unmodifiable snapshot of the entries. */
    public List<MealPlanEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Adds an entry to an unassigned meal.
     * @throws IllegalArgumentException if the entry is null, outside the week,
     *         or its date and meal type already have an assignment
     */
    public void addEntry(MealPlanEntry entry) {
        validateEntry(entry);
        if (getEntry(entry.getDate(), entry.getMealType()) != null) {
            throw new IllegalArgumentException("This meal already has an assignment.");
        }
        entries.add(entry);
    }

    /**
     * Removes the assignment for a date and meal type.
     * Returns false if there was no assignment.
     */
    public boolean removeEntry(LocalDate date, String mealType) {
        MealPlanEntry existing = getEntry(date, mealType);
        return existing != null && entries.remove(existing);
    }

    /**
     * Replaces the assignment matching the supplied entry's date and meal type.
     * @throws IllegalArgumentException if the entry is invalid or no assignment exists
     */
    public void replaceEntry(MealPlanEntry entry) {
        validateEntry(entry);
        MealPlanEntry existing = getEntry(entry.getDate(), entry.getMealType());
        if (existing == null) {
            throw new IllegalArgumentException("There is no assignment to replace.");
        }
        entries.set(entries.indexOf(existing), entry);
    }

    /**
     * Returns the matching entry, or null if the meal is unassigned.
     * @throws IllegalArgumentException if the date is outside the week or the
     *         meal type is not Breakfast, Lunch, or Dinner (after trimming)
     */
    public MealPlanEntry getEntry(LocalDate date, String mealType) {
        validateDate(date);
        String normalized = validateMealType(mealType);
        for (MealPlanEntry entry : entries) {
            if (entry.getDate().equals(date) && entry.getMealType().equals(normalized)) {
                return entry;
            }
        }
        return null;
    }

    private void validateEntry(MealPlanEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry must not be null.");
        }
        validateDate(entry.getDate());
        validateMealType(entry.getMealType());
    }

    private void validateDate(LocalDate date) {
        if (date == null || date.isBefore(weekStartDate)
                || date.toEpochDay() - weekStartDate.toEpochDay() >= 7) {
            throw new IllegalArgumentException("Date must be within this plan's seven days.");
        }
    }

    private String validateMealType(String mealType) {
        if (mealType == null) {
            throw new IllegalArgumentException("Meal type must not be null.");
        }
        String normalized = mealType.trim();
        if (!normalized.equals("Breakfast") && !normalized.equals("Lunch")
                && !normalized.equals("Dinner")) {
            throw new IllegalArgumentException(
                    "Meal type must be Breakfast, Lunch, or Dinner.");
        }
        return normalized;
    }
}

