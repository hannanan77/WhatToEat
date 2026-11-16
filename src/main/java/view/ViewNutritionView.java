package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import entity.Recipe;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionViewModel;

/**
 * Reusable Swing panel for the View Recipe Nutrition use case.
 */
public class ViewNutritionView extends JPanel implements PropertyChangeListener {

    private static final Color SAGE_GREEN = new Color(110, 155, 118);
    private static final Color FOREST_GREEN = new Color(53, 94, 67);
    private static final Color WARM_WHITE = new Color(248, 250, 246);
    private static final Color SOFT_MINT = new Color(230, 241, 232);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(38, 51, 43);
    private static final Color OUTLINE = new Color(225, 233, 226);
    private static final Color WARNING = new Color(176, 91, 43);

    private static final String[] TABLE_COLUMNS = {
        "Ingredient", "Quantity", "Calories", "Protein", "Carbohydrates",
        "Fat", "Data Status"
    };

    private final ViewNutritionViewModel viewModel;
    private final JLabel recipeNameLabel = new JLabel("No recipe selected");
    private final JLabel servingsLabel = new JLabel("Servings: Not Available");
    private final JLabel[] totalValueLabels = createValueLabels();
    private final JLabel[] perServingValueLabels = createValueLabels();
    private final JTextArea statusArea = new JTextArea();
    private final DefaultTableModel tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ingredientTable = new JTable(tableModel);
    private final JButton viewNutritionButton = new JButton("View Nutrition");

    private ViewNutritionController controller;
    private Recipe selectedRecipe;

    public ViewNutritionView(ViewNutritionViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addPropertyChangeListener(this);
        setLayout(new BorderLayout());
        setBackground(WARM_WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(WARM_WHITE);
        content.add(createRecipeSummary());
        content.add(Box.createVerticalStrut(16));
        content.add(createNutritionSummary());
        content.add(Box.createVerticalStrut(16));
        content.add(createIngredientBreakdown());
        content.add(Box.createVerticalStrut(12));
        content.add(createStatusPanel());

        final JScrollPane pageScrollPane = new JScrollPane(content);
        pageScrollPane.setBorder(BorderFactory.createEmptyBorder());
        pageScrollPane.getViewport().setBackground(WARM_WHITE);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(pageScrollPane, BorderLayout.CENTER);

        viewNutritionButton.setEnabled(false);
        viewNutritionButton.addActionListener(event -> requestNutrition());
    }

    /**
     * Provisional team integration contract for supplying the selected recipe.
     * The team still needs to confirm this method as its shared selection interface.
     */
    public void setSelectedRecipe(Recipe recipe) {
        selectedRecipe = recipe;
        if (recipe == null) {
            recipeNameLabel.setText("No recipe selected");
            servingsLabel.setText("Servings: Not Available");
            viewNutritionButton.setEnabled(false);
        }
        else {
            recipeNameLabel.setText(recipe.getName());
            servingsLabel.setText(recipe.getServings() == null
                    ? "Servings: Not Available"
                    : "Servings: " + recipe.getServings());
            viewNutritionButton.setEnabled(controller != null);
        }
        clearDisplayedNutrition();
    }

    public void setViewNutritionController(ViewNutritionController controller) {
        this.controller = controller;
        viewNutritionButton.setEnabled(controller != null && selectedRecipe != null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!viewModel.isError()) {
            recipeNameLabel.setText(viewModel.getRecipeName());
            servingsLabel.setText(viewModel.getServings());
            updateValueLabels(totalValueLabels, viewModel.getTotalValues());
            updateValueLabels(perServingValueLabels, viewModel.getPerServingValues());
            updateIngredientRows(viewModel.getIngredientRows());
        }
        statusArea.setText(viewModel.getStatusMessage());

        if (viewModel.isError() || !viewModel.isDataAvailable()
                || !viewModel.isTotalComplete()) {
            statusArea.setForeground(WARNING);
        }
        else {
            statusArea.setForeground(FOREST_GREEN);
        }
    }

    private JPanel createRecipeSummary() {
        final JPanel panel = createCardPanel(new BorderLayout(16, 0), WHITE);

        final JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        final JLabel title = new JLabel("Recipe Nutrition");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(FOREST_GREEN);
        recipeNameLabel.setFont(recipeNameLabel.getFont().deriveFont(Font.BOLD, 17f));
        recipeNameLabel.setForeground(TEXT);
        servingsLabel.setForeground(FOREST_GREEN);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(recipeNameLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(servingsLabel);

        stylePrimaryButton(viewNutritionButton);
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(viewNutritionButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel createNutritionSummary() {
        final JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);
        panel.add(createNutritionCard("Total Nutrition", totalValueLabels));
        panel.add(createNutritionCard("Per Serving Nutrition", perServingValueLabels));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        panel.setAlignmentX(CENTER_ALIGNMENT);
        return panel;
    }

    private JPanel createNutritionCard(String title, JLabel[] valueLabels) {
        final JPanel card = createCardPanel(new BorderLayout(0, 12), SOFT_MINT);
        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        titleLabel.setForeground(FOREST_GREEN);
        card.add(titleLabel, BorderLayout.NORTH);

        final JPanel metrics = new JPanel(new GridLayout(2, 2, 10, 10));
        metrics.setOpaque(false);
        metrics.add(createMetricPanel("Calories", valueLabels[0]));
        metrics.add(createMetricPanel("Protein", valueLabels[1]));
        metrics.add(createMetricPanel("Carbohydrates", valueLabels[2]));
        metrics.add(createMetricPanel("Fat", valueLabels[3]));
        card.add(metrics, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricPanel(String name, JLabel valueLabel) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(OUTLINE, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        final JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT);
        valueLabel.setForeground(FOREST_GREEN);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(valueLabel);
        return panel;
    }

    private JPanel createIngredientBreakdown() {
        final JPanel panel = createCardPanel(new BorderLayout(0, 12), WHITE);
        final JLabel title = new JLabel("Ingredient Breakdown");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(FOREST_GREEN);
        panel.add(title, BorderLayout.NORTH);

        ingredientTable.setRowHeight(30);
        ingredientTable.setFillsViewportHeight(true);
        ingredientTable.setGridColor(OUTLINE);
        ingredientTable.setSelectionBackground(SOFT_MINT);
        ingredientTable.getTableHeader().setBackground(SOFT_MINT);
        ingredientTable.getTableHeader().setForeground(TEXT);
        ingredientTable.getTableHeader().setFont(
                ingredientTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        final int[] columnWidths = {180, 120, 120, 110, 140, 100, 260};
        for (int index = 0; index < columnWidths.length; index++) {
            ingredientTable.getColumnModel().getColumn(index)
                    .setPreferredWidth(columnWidths[index]);
        }

        final JScrollPane tableScrollPane = new JScrollPane(ingredientTable);
        tableScrollPane.setPreferredSize(new Dimension(900, 250));
        tableScrollPane.setBorder(new LineBorder(OUTLINE, 1, true));
        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusPanel() {
        final JPanel panel = createCardPanel(new BorderLayout(), SOFT_MINT);
        final JLabel title = new JLabel("Data Status / Notes");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setForeground(FOREST_GREEN);
        panel.add(title, BorderLayout.NORTH);

        statusArea.setText("Select a recipe to view its nutrition.");
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        statusArea.setForeground(FOREST_GREEN);
        statusArea.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(statusArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCardPanel(java.awt.LayoutManager layout, Color background) {
        final JPanel panel = new JPanel(layout);
        panel.setBackground(background);
        final Border roundedLine = new LineBorder(OUTLINE, 1, true);
        panel.setBorder(BorderFactory.createCompoundBorder(
                roundedLine, BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        panel.setAlignmentX(CENTER_ALIGNMENT);
        return panel;
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(SAGE_GREEN);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(FOREST_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
    }

    private void requestNutrition() {
        if (controller != null && selectedRecipe != null) {
            controller.execute(selectedRecipe);
        }
    }

    private void updateValueLabels(JLabel[] labels, String[] values) {
        for (int index = 0; index < labels.length; index++) {
            labels[index].setText(values[index]);
        }
    }

    private void updateIngredientRows(List<String[]> rows) {
        tableModel.setRowCount(0);
        for (String[] row : rows) {
            tableModel.addRow(row);
        }
    }

    private void clearDisplayedNutrition() {
        final String[] unavailable = {
            "Not Available", "Not Available", "Not Available", "Not Available"
        };
        updateValueLabels(totalValueLabels, unavailable);
        updateValueLabels(perServingValueLabels, unavailable);
        tableModel.setRowCount(0);
        statusArea.setText(selectedRecipe == null
                ? "Select a recipe to view its nutrition."
                : "Press View Nutrition to calculate available values.");
        statusArea.setForeground(FOREST_GREEN);
    }

    private static JLabel[] createValueLabels() {
        return new JLabel[] {
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT)
        };
    }
}
