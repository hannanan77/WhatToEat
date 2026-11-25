package app;

import data_access.FileRecipeLibraryDataAccess;
import data_access.FileWeeklyMealPlanDataAccess;
import data_access.SavedRecipeChoices;
import entity.MealPlan;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanPresenter;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;
import use_case.weekly_meal_plan.WeeklyMealPlanInteractor;
import view.WeeklyMealPlanView;
import view.WeekSidebar;
import view.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.function.Consumer;

/** Opens the weekly calendar with the local recipe collection. */
public class WeeklyMealPlanPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JPanel view = createPlanner(null);

            JFrame window = new JFrame("WhatToEat - Weekly Meal Plan");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(1280, 800);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    static JPanel createPlanner(Consumer<String> openRecipe) {
        return new Planner(openRecipe);
    }

    private static final class Planner extends JPanel {
        private static final String VISIBLE = "visible";
        private static final String HIDDEN = "hidden";
        private static final String REMOVED = "removed";

        private final Path recipeFile = FileRecipeLibraryDataAccess.defaultFile();
        private final FileWeeklyMealPlanDataAccess storage =
                new FileWeeklyMealPlanDataAccess(recipeFile.resolveSibling("meal-plans"));
        private final SavedRecipeChoices collection = new SavedRecipeChoices(recipeFile);
        private final Preferences preferences =
                Preferences.userNodeForPackage(WeeklyMealPlanPreview.class).node("weeks");
        private final NavigableSet<LocalDate> weeks = new TreeSet<>();
        private final Map<LocalDate, WeeklyMealPlanView> views = new HashMap<>();
        private final Consumer<String> openRecipe;
        private LocalDate selected;
        private boolean showHidden;

        Planner(Consumer<String> openRecipe) {
            super(new BorderLayout());
            this.openRecipe = openRecipe;
            LocalDate currentWeek = LocalDate.now().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int offset = -2; offset <= 2; offset++) {
                weeks.add(currentWeek.plusWeeks(offset));
            }
            try {
                weeks.addAll(storage.savedWeeks());
                for (String key : preferences.keys()) {
                    weeks.add(LocalDate.parse(key));
                }
            } catch (IOException | BackingStoreException exception) {
                JOptionPane.showMessageDialog(this, "Some saved weeks could not be loaded.",
                        "Weekly Meal Planner", JOptionPane.ERROR_MESSAGE);
            }
            showWeek(state(currentWeek).equals(VISIBLE) ? currentWeek : null);
        }

        private String state(LocalDate week) {
            return preferences.get(week.toString(), VISIBLE);
        }

        private void showWeek(LocalDate monday) {
            List<LocalDate> visible = new ArrayList<>();
            List<LocalDate> hidden = new ArrayList<>();
            for (LocalDate week : weeks) {
                String state = state(week);
                if (state.equals(VISIBLE)) {
                    visible.add(week);
                } else if (state.equals(HIDDEN)) {
                    hidden.add(week);
                }
            }
            selected = monday == null && !visible.isEmpty() ? nearestWeek(visible) : monday;
            WeekSidebar sidebar = new WeekSidebar(visible, hidden, selected, showHidden, value -> {
                showHidden = value;
                showWeek(selected);
            }, this::showWeek, this::addWeek, this::hideWeek, this::deleteWeek);

            removeAll();
            if (selected != null) {
                WeeklyMealPlanView view = views.computeIfAbsent(selected,
                        date -> createView(date, collection, storage, openRecipe));
                view.setWeekSidebar(sidebar);
                add(view, BorderLayout.CENTER);
            } else {
                JPanel empty = new JPanel(new BorderLayout(UiTheme.SECTION_GAP, UiTheme.SECTION_GAP));
                empty.setBackground(UiTheme.BACKGROUND);
                empty.setBorder(BorderFactory.createEmptyBorder(UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING,
                        UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING));
                JLabel title = new JLabel("Weekly Meal Planner");
                title.setFont(UiTheme.HEADING_FONT);
                empty.add(title, BorderLayout.NORTH);
                JPanel left = new JPanel(new BorderLayout());
                left.setOpaque(false);
                left.add(sidebar, BorderLayout.NORTH);
                empty.add(left, BorderLayout.WEST);
                empty.add(new JLabel("Add a week or restore a hidden week to see its meal plan."),
                        BorderLayout.CENTER);
                add(empty, BorderLayout.CENTER);
            }
            revalidate();
            repaint();
        }

        private LocalDate nearestWeek(List<LocalDate> visible) {
            LocalDate anchor = selected == null ? LocalDate.now() : selected;
            return visible.stream().min(java.util.Comparator.comparingLong(
                    date -> Math.abs(date.toEpochDay() - anchor.toEpochDay()))).orElseThrow();
        }

        private void addWeek(LocalDate monday) {
            preferences.put(monday.toString(), VISIBLE);
            weeks.add(monday);
            showWeek(monday);
        }

        private void hideWeek(LocalDate monday) {
            preferences.put(monday.toString(), HIDDEN);
            showWeek(monday.equals(selected) ? null : selected);
        }

        private void deleteWeek(LocalDate monday) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Delete the week starting " + monday + " and its saved meal plan?",
                    "Delete Week", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                storage.delete(monday);
                preferences.put(monday.toString(), REMOVED);
                views.remove(monday);
                showWeek(monday.equals(selected) ? null : selected);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(this, "Could not delete the weekly meal plan.",
                        "Delete Week", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static WeeklyMealPlanView createView(LocalDate weekStartDate,
                                                SavedRecipeChoices collection,
                                                FileWeeklyMealPlanDataAccess storage,
                                                Consumer<String> openRecipe) {
        MealPlan mealPlan = new MealPlan(weekStartDate);

        WeeklyMealPlanViewModel viewModel =
                new WeeklyMealPlanViewModel();

        WeeklyMealPlanPresenter presenter =
                new WeeklyMealPlanPresenter(viewModel);

        WeeklyMealPlanInteractor interactor =
                new WeeklyMealPlanInteractor(mealPlan, presenter, storage);

        WeeklyMealPlanController controller =
                new WeeklyMealPlanController(interactor);

        WeeklyMealPlanView view = new WeeklyMealPlanView(
                controller, viewModel, weekStartDate, collection::load, openRecipe, collection::loadImages);
        controller.loadPlan();
        return view;
    }
}
