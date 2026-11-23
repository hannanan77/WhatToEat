package view;

import entity.MealPlanEntry;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;

/** Weekly calendar that uses recipe IDs from the recipe collection supplied by the host. */
public class WeeklyMealPlanView extends JPanel {
    private static final Color CANVAS = Color.decode("#EEFBF0");
    private static final Color MINT = Color.decode("#E6F1E8");
    private static final Color ACTIVE = Color.decode("#BFECCA");
    private static final Color FOREST = Color.decode("#355E43");
    private static final Color INK = Color.decode("#26332B");
    private static final Color OUTLINE = Color.decode("#E1E9E2");
    private static final List<String> MEALS = List.of("Breakfast", "Lunch", "Dinner");
    private static final int DAYS = 7;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final Font PLAIN_TEXT = new Font("Plus Jakarta Sans", Font.PLAIN, 14);
    private static final Font BOLD_TEXT = new Font("Plus Jakarta Sans", Font.BOLD, 14);

    private final JPanel calendar = new JPanel(new GridLayout(4, DAYS + 1, 8, 8));
    private final JLabel planned = label("", BOLD_TEXT, 13);
    private final JLabel status = label(" ", PLAIN_TEXT, 13);
    private final WeeklyMealPlanController controller;
    private final WeeklyMealPlanViewModel viewModel;
    private final LocalDate weekStart;
    private final RecipeCollection collection;
    private Map<String, String> recipes = Map.of();

    @FunctionalInterface
    public interface RecipeCollection {
        Map<String, String> load() throws IOException;
    }

    public WeeklyMealPlanView(WeeklyMealPlanController controller,
                              WeeklyMealPlanViewModel viewModel, LocalDate weekStart,
                              RecipeCollection collection) {
        this.controller = controller;
        this.viewModel = viewModel;
        this.weekStart = weekStart;
        this.collection = collection;
        try {
            recipes = Map.copyOf(collection.load());
        } catch (IOException exception) {
            // The picker retries when opened and displays the read error there.
        }
        setLayout(new BorderLayout());
        setBackground(CANVAS);
        add(createSidebar(), BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.setBackground(CANVAS);
        content.add(createHeader());
        content.add(Box.createVerticalStrut(16));
        content.add(createWeekBar());
        content.add(Box.createVerticalStrut(12));

        calendar.setBackground(Color.WHITE);
        calendar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        calendar.setPreferredSize(new Dimension(840, 420));
        calendar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        calendar.setAlignmentX(LEFT_ALIGNMENT);
        content.add(calendar);
        content.add(Box.createVerticalStrut(12));
        status.setAlignmentX(LEFT_ALIGNMENT);
        content.add(status);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        viewModel.addPropertyChangeListener(event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh();
            } else {
                SwingUtilities.invokeLater(this::refresh);
            }
        });
        refresh();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));
        JLabel brand = label("WhatToEat", BOLD_TEXT, 23);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(24));
        JLabel section = label("Weekly Meal Planner", BOLD_TEXT, 14);
        section.setOpaque(true);
        section.setBackground(ACTIVE);
        section.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sidebar.add(section);
        return sidebar;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.add(label("Weekly Meal Planner", BOLD_TEXT, 30), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton clear = button("Clear Week", MINT, FOREST);
        clear.addActionListener(event -> {
            if (JOptionPane.showConfirmDialog(this, "Clear all meals for this week?",
                    "Clear Week", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                controller.clearPlan();
            }
        });
        JButton save = button("Save Meal Plan", FOREST, Color.WHITE);
        save.addActionListener(event -> controller.savePlan());
        JButton export = button("Export PDF", MINT, FOREST);
        export.addActionListener(event -> exportPdf());
        actions.add(clear);
        actions.add(export);
        actions.add(save);
        header.add(actions, BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        return header;
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Weekly Meal Plan");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF documents", "pdf"));
        chooser.setSelectedFile(new java.io.File("meal-plan-" + weekStart + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = target.resolveSibling(target.getFileName() + ".pdf");
        }
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                "Replace " + target.getFileName() + "?", "Export PDF",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            WeeklyMealPlanPdf.write(target, weekStart, MEALS, viewModel.getAssignments(), recipes);
            status.setText("PDF exported to " + target.getFileName());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Could not export PDF: " + exception.getMessage(),
                    "Export PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createWeekBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setBackground(MINT);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        bar.add(label(DATE.format(weekStart) + " – " + DATE.format(weekStart.plusDays(6))
                + ", " + weekStart.plusDays(6).getYear(), BOLD_TEXT, 15), BorderLayout.WEST);
        bar.add(planned, BorderLayout.EAST);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, bar.getPreferredSize().height));
        return bar;
    }

    private void refresh() {
        List<MealPlanEntry> assignments = viewModel.getAssignments();
        calendar.removeAll();
        calendar.add(heading("Meal", MINT, FOREST));
        for (int day = 0; day < DAYS; day++) {
            LocalDate date = weekStart.plusDays(day);
            boolean today = date.equals(LocalDate.now());
            calendar.add(heading(DAY.format(date) + " " + date.getDayOfMonth(),
                    today ? FOREST : MINT, today ? Color.WHITE : FOREST));
        }

        for (String meal : MEALS) {
            Map<LocalDate, String> recipeByDay = new LinkedHashMap<>();
            for (MealPlanEntry entry : assignments) {
                if (entry.getMealType().equals(meal)) {
                    recipeByDay.put(entry.getDate(), entry.getRecipeId());
                }
            }
            calendar.add(heading(meal, MINT, FOREST));
            for (int day = 0; day < DAYS; day++) {
                LocalDate date = weekStart.plusDays(day);
                calendar.add(createSlot(date, meal, recipeByDay.get(date)));
            }
        }
        planned.setText(assignments.size() + " / " + (DAYS * MEALS.size()) + " planned");
        String message = viewModel.getMessage();
        status.setText(message.isEmpty() ? " " : message);
        calendar.revalidate();
        calendar.repaint();
    }

    private JComponent createSlot(LocalDate date, String meal, String recipeId) {
        JPanel slot = new JPanel(new BorderLayout());
        slot.setBackground(Color.WHITE);
        slot.setBorder(BorderFactory.createLineBorder(OUTLINE));
        boolean assigned = recipeId != null;
        if (assigned) {
            String name = recipes.getOrDefault(recipeId, recipeId);
            JLabel title = label(name, BOLD_TEXT, 13);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setToolTipText(name);
            slot.add(title, BorderLayout.CENTER);
        }
        JButton choose = button(assigned ? "Replace" : "+ Add Recipe", MINT, FOREST);
        choose.getAccessibleContext().setAccessibleName(
                choose.getText() + " for " + DAY.format(date) + " " + meal);
        choose.addActionListener(event -> chooseRecipe(date, meal, recipeId));
        slot.add(choose, assigned ? BorderLayout.SOUTH : BorderLayout.CENTER);
        return slot;
    }

    private void chooseRecipe(LocalDate date, String meal, String currentRecipeId) {
        String message;
        try {
            recipes = Map.copyOf(collection.load());
            refresh();
            message = recipes.isEmpty()
                    ? "Your collection is empty. Save or import a recipe first."
                    : "Choose a recipe from your collection.";
        } catch (IOException exception) {
            showRecipePicker(date, meal, currentRecipeId,
                    "Could not read your recipe collection.", List.of());
            return;
        }

        List<String> recipeIds = new ArrayList<>(recipes.keySet());
        recipeIds.sort(Comparator.comparing((String id) -> recipes.get(id),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Comparator.naturalOrder()));
        showRecipePicker(date, meal, currentRecipeId, message, recipeIds);
    }

    private void showRecipePicker(LocalDate date, String meal, String currentRecipeId,
                                  String message, List<String> recipeIds) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Choose a Recipe",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(CANVAS);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(label(DAY.format(date) + ", " + DATE.format(date) + " • " + meal,
                BOLD_TEXT, 22), BorderLayout.NORTH);
        header.add(label(message, PLAIN_TEXT, 14), BorderLayout.SOUTH);
        body.add(header, BorderLayout.NORTH);

        JButton cancel = button(recipeIds.isEmpty() ? "Close" : "Cancel", MINT, FOREST);
        cancel.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        body.add(actions, BorderLayout.SOUTH);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (!recipeIds.isEmpty()) {
            JList<String> choices = new JList<>(recipeIds.toArray(new String[0]));
            choices.setFixedCellHeight(40);
            choices.setVisibleRowCount(5);
            choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            choices.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean selected, boolean focused) {
                    String name = value instanceof String ? recipes.get((String) value) : "";
                    super.getListCellRendererComponent(list, name, index, selected, focused);
                    setBackground(selected ? ACTIVE : Color.WHITE);
                    setForeground(selected ? FOREST : INK);
                    setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                    setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 15));
                    setToolTipText(getText());
                    return this;
                }
            });
            if (currentRecipeId != null) {
                choices.setSelectedValue(currentRecipeId, true);
            }

            JScrollPane scroll = new JScrollPane(choices);
            scroll.setPreferredSize(new Dimension(420, 202));
            scroll.setBorder(BorderFactory.createLineBorder(OUTLINE));
            body.add(scroll, BorderLayout.CENTER);

            JButton confirm = button(currentRecipeId == null ? "Add Recipe" : "Replace Recipe",
                    FOREST, Color.WHITE);
            confirm.setEnabled(choices.getSelectedIndex() >= 0);
            choices.addListSelectionListener(event ->
                    confirm.setEnabled(choices.getSelectedIndex() >= 0));
            confirm.addActionListener(event -> {
                controller.addEntry(date.toString(), meal, choices.getSelectedValue());
                dialog.dispose();
            });
            actions.add(confirm);
            dialog.getRootPane().setDefaultButton(confirm);
        }

        dialog.setContentPane(body);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static JLabel heading(String text, Color background, Color foreground) {
        JLabel label = label(text, BOLD_TEXT, 13);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        return label;
    }

    private static JLabel label(String text, Font font, int size) {
        JLabel label = new JLabel(text);
        label.setFont(font.deriveFont((float) size));
        label.setForeground(INK);
        return label;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 13));
        button.setOpaque(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        return button;
    }
}
