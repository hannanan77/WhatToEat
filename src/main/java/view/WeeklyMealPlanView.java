package view;

import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

/** Initial UI for adding and displaying weekly meal assignments. */
public class WeeklyMealPlanView extends JPanel {
    private final JTextField dateField = new JTextField(10);
    private final JComboBox<String> mealTypeBox =
            new JComboBox<>(new String[]{
                    "Breakfast", "Lunch", "Dinner"
            });
    private final JTextField recipeIdField = new JTextField(15);

    private final DefaultListModel<String> entriesModel =
            new DefaultListModel<>();
    private final JLabel messageLabel = new JLabel(" ");

    public WeeklyMealPlanView(
            WeeklyMealPlanController controller,
            WeeklyMealPlanViewModel viewModel,
            LocalDate weekStartDate) {

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel(
                "Weekly Meal Plan: " + weekStartDate
                        + " to " + weekStartDate.plusDays(6));

        dateField.setText(weekStartDate.toString());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Meal date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Meal type:"));
        form.add(mealTypeBox);
        form.add(new JLabel("Recipe ID:"));
        form.add(recipeIdField);

        JButton addButton = new JButton("Add meal");
        form.add(new JLabel(""));
        form.add(addButton);

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.add(title, BorderLayout.NORTH);
        header.add(form, BorderLayout.CENTER);

        JList<String> entriesList = new JList<>(entriesModel);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(entriesList), BorderLayout.CENTER);
        add(messageLabel, BorderLayout.SOUTH);

        addButton.addActionListener(event ->
                controller.addEntry(
                        dateField.getText(),
                        (String) mealTypeBox.getSelectedItem(),
                        recipeIdField.getText()));

        viewModel.addPropertyChangeListener(event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh(viewModel);
            } else {
                SwingUtilities.invokeLater(() -> refresh(viewModel));
            }
        });

        refresh(viewModel);
    }

    private void refresh(WeeklyMealPlanViewModel viewModel) {
        entriesModel.clear();

        for (String entry : viewModel.getEntries()) {
            entriesModel.addElement(entry);
        }

        String message = viewModel.getMessage();
        messageLabel.setText(message.isEmpty() ? " " : message);
    }
}
