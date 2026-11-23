package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import entity.Recipe;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionViewModel;
import view.components.RoundedBorder;
import view.components.ThemeButton;
import view.components.UiComponents;

/**
 * Reusable nutrition content panel intended for a modal dialog.
 */
public class ViewNutritionView extends JPanel implements PropertyChangeListener {

    private static final String PARTIAL_ESTIMATE_MESSAGE =
            "Totals include available data and reasonable estimates based on typical "
                    + "weights and reference nutrition. Values that cannot be estimated "
                    + "remain N/A and are excluded.";
    private static final String PARTIAL_AVAILABLE_MESSAGE =
            "Totals include available nutrition data. Values that cannot be determined "
                    + "remain N/A and are excluded.";
    private static final String ESTIMATE_MESSAGE =
            "Some values use reasonable estimates based on typical ingredient weights "
                    + "and reference nutrition.";
    private static final String UNAVAILABLE_MESSAGE =
            "Nutrition could not be calculated reliably. Missing values remain N/A and "
                    + "are excluded from totals.";

    private static final String[] TABLE_COLUMNS = {
        "Ingredient", "Quantity", "Unit", "Calories", "Protein",
        "Carbs", "Fat", "Status"
    };

    private final ViewNutritionViewModel viewModel;
    private final JLabel recipeNameLabel = new JLabel("No recipe selected");
    private final JLabel servingsLabel = new JLabel("Servings unavailable");
    private final JLabel calculationNoteSeparator = separatorLabel();
    private final JLabel calculationNoteLabel = new JLabel();
    private final JLabel[] totalValueLabels = createValueLabels();
    private final JLabel[] perServingValueLabels = createValueLabels();
    private final RoundedPanel statusPanel = new RoundedPanel(
            UiTheme.MINT, UiTheme.BORDER, UiTheme.CONTROL_RADIUS);
    private final JLabel statusTitleLabel = new JLabel("Nutrition details");
    private final JTextArea statusArea = new JTextArea();
    private final JProgressBar loadingIndicator = new JProgressBar();
    private final ThemeButton retryButton =
            new ThemeButton("Retry", ThemeButton.Variant.SECONDARY);
    private final ThemeButton footerCloseButton =
            new KeyboardFocusThemeButton("Close", ThemeButton.Variant.PRIMARY);
    private final JLabel ingredientCountLabel = new JLabel("0 ingredients");
    private final Component statusGap = Box.createVerticalStrut(UiTheme.SPACE_SM);
    private final DefaultTableModel tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ingredientTable = new JTable(tableModel) {
        @Override
        public String getToolTipText(MouseEvent event) {
            final int row = rowAtPoint(event.getPoint());
            final int column = columnAtPoint(event.getPoint());
            if (row >= 0 && column == 7) {
                final Object value = getValueAt(row, column);
                return value == null ? null : value.toString();
            }
            return super.getToolTipText(event);
        }
    };
    private final JScrollPane tableScrollPane = new JScrollPane(ingredientTable);

    private ViewNutritionController controller;
    private Recipe selectedRecipe;
    private Runnable closeAction = () -> { };

    public ViewNutritionView(ViewNutritionViewModel viewModel) {
        this.viewModel = viewModel;
        viewModel.addPropertyChangeListener(this);

        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG,
                UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        setPreferredSize(new Dimension(1040, 740));

        final JPanel upperContent = verticalPanel();
        upperContent.add(createHeader());
        upperContent.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        upperContent.add(createStatusBanner());
        upperContent.add(statusGap);
        upperContent.add(createNutritionSummary());

        add(upperContent, BorderLayout.NORTH);
        add(createIngredientBreakdown(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        retryButton.addActionListener(event -> requestNutrition());
        footerCloseButton.addActionListener(event -> closeAction.run());
        footerCloseButton.getAccessibleContext().setAccessibleName("Close nutrition dialog");
        footerCloseButton.setToolTipText("Close dialog (Esc)");
        calculationNoteLabel.setName("nutrition-calculation-note");
        statusPanel.setName("nutrition-status-panel");
        statusTitleLabel.setName("nutrition-status-title");
        statusArea.setName("nutrition-status-message");
        statusGap.setName("nutrition-status-gap");
        ingredientTable.setName("nutrition-ingredient-table");
        tableScrollPane.setName("nutrition-table-scroll");
        statusArea.getAccessibleContext().setAccessibleName("Nutrition status");
        ingredientTable.getAccessibleContext().setAccessibleName("Ingredient nutrition breakdown");
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateStatusBannerHeight();
            }
        });
        configureInitialState();
    }

    /** Supplies the selected recipe without starting the calculation. */
    public void setSelectedRecipe(Recipe recipe) {
        selectedRecipe = recipe;
        if (recipe == null) {
            recipeNameLabel.setText("No recipe selected");
            servingsLabel.setText("Servings unavailable");
        }
        else {
            recipeNameLabel.setText(recipe.getName());
            servingsLabel.setText(recipe.getServings() == null
                    ? "Servings unavailable"
                    : servingText(recipe.getServings()));
        }
        setCalculationNote(null);
        clearDisplayedNutrition();
    }

    public void setViewNutritionController(ViewNutritionController controller) {
        this.controller = controller;
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction == null ? () -> { } : closeAction;
    }

    /** Starts or retries the nutrition request for the current recipe. */
    public void requestNutrition() {
        if (selectedRecipe == null) {
            showBanner(BannerType.ERROR, "Select a recipe",
                    "Select a recipe before viewing nutrition.");
            return;
        }
        if (controller == null) {
            showBanner(BannerType.ERROR, "Nutrition is not connected",
                    "The Nutrition controller has not been configured.");
            return;
        }
        showLoadingState();
        controller.execute(selectedRecipe);
    }

    /** Invalidates a nutrition request that may still be running. */
    void cancelNutritionRequest() {
        if (controller != null) {
            controller.cancelCurrentRequest();
        }
    }

    public void requestInitialFocus() {
        footerCloseButton.requestFocusInWindow();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            applyViewModelState();
        }
        else {
            SwingUtilities.invokeLater(this::applyViewModelState);
        }
    }

    private JPanel createHeader() {
        final RoundedPanel header = new RoundedPanel(
                UiTheme.MINT, UiTheme.BORDER, UiTheme.CARD_RADIUS);
        header.setLayout(new BorderLayout(UiTheme.SPACE_MD, 0));
        header.setBorder(BorderFactory.createEmptyBorder(
                9, UiTheme.SPACE_LG, 9, 10));
        header.setPreferredSize(new Dimension(1000, 68));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        header.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel textPanel = verticalPanel();
        textPanel.setOpaque(false);

        final JLabel title = new JLabel("Recipe Nutrition");
        title.setFont(UiTheme.HEADING_FONT.deriveFont(20f));
        title.setForeground(UiTheme.SECONDARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel details = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        details.setOpaque(false);
        recipeNameLabel.setFont(UiTheme.LABEL_FONT);
        recipeNameLabel.setForeground(UiTheme.TEXT_PRIMARY);
        servingsLabel.setFont(UiTheme.LABEL_FONT.deriveFont(Font.PLAIN, 12f));
        servingsLabel.setForeground(UiTheme.TEXT_SECONDARY);
        calculationNoteLabel.setFont(
                UiTheme.LABEL_FONT.deriveFont(Font.PLAIN, 12f));
        calculationNoteLabel.setForeground(UiTheme.TEXT_SECONDARY);
        details.add(recipeNameLabel);
        details.add(separatorLabel());
        details.add(servingsLabel);
        details.add(calculationNoteSeparator);
        details.add(calculationNoteLabel);
        details.setAlignmentX(LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(details);

        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel createStatusBanner() {
        statusPanel.setLayout(new BorderLayout(10, 3));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_SM, 14, UiTheme.SPACE_SM, UiTheme.SPACE_MD));
        statusPanel.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);
        statusTitleLabel.setFont(UiTheme.LABEL_FONT);
        statusTitleLabel.setForeground(UiTheme.SECONDARY);
        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        statusArea.setFont(UiTheme.LABEL_FONT.deriveFont(Font.PLAIN, 12f));
        statusArea.setForeground(UiTheme.TEXT_PRIMARY);
        statusArea.setRows(1);
        textPanel.add(statusTitleLabel, BorderLayout.NORTH);
        textPanel.add(statusArea, BorderLayout.CENTER);

        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setBorderPainted(false);
        loadingIndicator.setBackground(UiTheme.BACKGROUND);
        loadingIndicator.setForeground(UiTheme.PRIMARY);
        loadingIndicator.setPreferredSize(new Dimension(120, 4));
        loadingIndicator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

        retryButton.setPreferredSize(
                new Dimension(80, UiTheme.BUTTON_MIN_HEIGHT));
        statusPanel.add(textPanel, BorderLayout.CENTER);
        statusPanel.add(retryButton, BorderLayout.EAST);
        statusPanel.add(loadingIndicator, BorderLayout.SOUTH);
        return statusPanel;
    }

    private JPanel createNutritionSummary() {
        final JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setName("nutrition-summary");
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 164));
        panel.setPreferredSize(new Dimension(1008, 164));
        panel.add(createNutritionCard("Total Recipe Nutrition",
                "Sum of all servings", totalValueLabels));
        panel.add(createNutritionCard("Per Serving Nutrition",
                "One serving", perServingValueLabels));
        return panel;
    }

    private JPanel createNutritionCard(String title, String subtitle, JLabel[] valueLabels) {
        final RoundedPanel card = new RoundedPanel(
                UiTheme.MINT, UiTheme.BORDER, UiTheme.CARD_RADIUS);
        card.setLayout(new BorderLayout(0, UiTheme.SPACE_SM));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.BODY_LARGE_FONT.deriveFont(Font.BOLD));
        titleLabel.setForeground(UiTheme.SECONDARY);
        final JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(UiTheme.SMALL_LABEL_FONT.deriveFont(Font.PLAIN));
        subtitleLabel.setForeground(UiTheme.TEXT_SECONDARY);
        heading.add(titleLabel, BorderLayout.WEST);
        heading.add(subtitleLabel, BorderLayout.EAST);

        final JPanel metrics = new JPanel(new GridLayout(
                2, 2, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        metrics.setOpaque(false);
        metrics.add(createMetricPanel("Calories", valueLabels[0],
                NutrientIconType.CALORIES));
        metrics.add(createMetricPanel("Protein", valueLabels[1],
                NutrientIconType.PROTEIN));
        metrics.add(createMetricPanel("Carbohydrates", valueLabels[2],
                NutrientIconType.CARBOHYDRATES));
        metrics.add(createMetricPanel("Fat", valueLabels[3],
                NutrientIconType.FAT));

        card.add(heading, BorderLayout.NORTH);
        card.add(metrics, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricPanel(String name, JLabel valueLabel,
                                     NutrientIconType iconType) {
        final RoundedPanel panel = new RoundedPanel(
                UiTheme.SURFACE, UiTheme.BORDER, UiTheme.CONTROL_RADIUS);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 5, 10));

        final JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UiTheme.SMALL_LABEL_FONT.deriveFont(Font.PLAIN));
        nameLabel.setForeground(UiTheme.TEXT_SECONDARY);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        nameLabel.setIcon(new NutrientIcon(iconType));
        nameLabel.setIconTextGap(UiTheme.SPACE_XS);
        if (iconType == NutrientIconType.CALORIES) {
            valueLabel.setFont(UiTheme.HEADING_FONT.deriveFont(20f));
        }
        else {
            valueLabel.setFont(UiTheme.SECTION_HEADING_FONT.deriveFont(17f));
        }
        valueLabel.setForeground(UiTheme.SECONDARY);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(valueLabel);
        return panel;
    }

    private JPanel createIngredientBreakdown() {
        final RoundedPanel panel = new RoundedPanel(
                UiTheme.SURFACE, UiTheme.BORDER, UiTheme.CARD_RADIUS);
        panel.setLayout(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setBorder(BorderFactory.createEmptyBorder(
                10, 0, 0, 0));

        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(
                0, UiTheme.SPACE_MD, 0, UiTheme.SPACE_MD));
        final JLabel title = new JLabel("Ingredient Breakdown");
        title.setFont(UiTheme.SECTION_HEADING_FONT.deriveFont(17f));
        title.setForeground(UiTheme.TEXT_PRIMARY);
        ingredientCountLabel.setFont(
                UiTheme.LABEL_FONT.deriveFont(Font.PLAIN, 12f));
        ingredientCountLabel.setForeground(UiTheme.TEXT_SECONDARY);
        heading.add(title, BorderLayout.WEST);
        heading.add(ingredientCountLabel, BorderLayout.EAST);

        configureTable();
        tableScrollPane.setColumnHeaderView(ingredientTable.getTableHeader());
        tableScrollPane.setOpaque(true);
        tableScrollPane.setBackground(UiTheme.SURFACE);
        tableScrollPane.setBorder(new RoundedBorder(1, 1));
        tableScrollPane.getViewport().setOpaque(true);
        tableScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        tableScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        tableScrollPane.getColumnHeader().setOpaque(true);
        tableScrollPane.getColumnHeader().setBackground(UiTheme.MINT);
        tableScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setPreferredSize(new Dimension(984, 272));
        UiComponents.styleScrollPane(tableScrollPane);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooter() {
        final JPanel footer = new JPanel(new BorderLayout(UiTheme.SPACE_LG, 0));
        footer.setOpaque(false);
        final JLabel note = new JLabel("~ marks estimates; * marks incomplete totals.");
        note.setFont(UiTheme.SMALL_LABEL_FONT.deriveFont(Font.PLAIN));
        note.setForeground(UiTheme.TEXT_SECONDARY);
        footerCloseButton.setPreferredSize(
                new Dimension(100, UiTheme.BUTTON_MIN_HEIGHT));
        footer.add(note, BorderLayout.WEST);
        footer.add(footerCloseButton, BorderLayout.EAST);
        return footer;
    }

    private void configureTable() {
        ingredientTable.setFont(UiTheme.BODY_FONT);
        ingredientTable.setForeground(UiTheme.TEXT_PRIMARY);
        ingredientTable.setBackground(UiTheme.SURFACE);
        ingredientTable.setOpaque(true);
        ingredientTable.setRowHeight(36);
        ingredientTable.setShowHorizontalLines(false);
        ingredientTable.setShowVerticalLines(false);
        ingredientTable.setIntercellSpacing(new Dimension(0, 0));
        ingredientTable.setRowSelectionAllowed(false);
        ingredientTable.setColumnSelectionAllowed(false);
        ingredientTable.setCellSelectionEnabled(false);
        ingredientTable.setRequestFocusEnabled(false);
        ingredientTable.clearSelection();
        ingredientTable.setSelectionBackground(UiTheme.MINT);
        ingredientTable.setSelectionForeground(UiTheme.TEXT_PRIMARY);
        ingredientTable.setFillsViewportHeight(true);
        ingredientTable.setAutoCreateRowSorter(false);

        final JTableHeader header = ingredientTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setDefaultRenderer(new TableHeaderRenderer());

        ingredientTable.setDefaultRenderer(Object.class,
                new NutritionTableRenderer(SwingConstants.LEFT, false));
        ingredientTable.getColumnModel().getColumn(0).setCellRenderer(
                new NutritionTableRenderer(SwingConstants.LEFT, true,
                        UiTheme.SPACE_MD, UiTheme.SPACE_SM));
        for (int column : new int[] {1, 3, 4, 5, 6}) {
            ingredientTable.getColumnModel().getColumn(column).setCellRenderer(
                    new NutritionTableRenderer(SwingConstants.RIGHT, false));
        }
        ingredientTable.getColumnModel().getColumn(2).setCellRenderer(
                new NutritionTableRenderer(SwingConstants.LEFT, false,
                        UiTheme.SPACE_LG, UiTheme.SPACE_SM));
        ingredientTable.getColumnModel().getColumn(7).setCellRenderer(
                new StatusRenderer());

        final int[] columnWidths = {224, 88, 76, 104, 92, 88, 70, 146};
        for (int index = 0; index < columnWidths.length; index++) {
            ingredientTable.getColumnModel().getColumn(index)
                    .setPreferredWidth(columnWidths[index]);
        }
    }

    private void applyViewModelState() {
        if (viewModel.isLoading()) {
            setCalculationNote(null);
            recipeNameLabel.setText(viewModel.getRecipeName());
            servingsLabel.setText(viewModel.getServings());
            clearNutritionValues();
            showLoadingState();
            return;
        }

        if (viewModel.isError()) {
            setCalculationNote(null);
            clearNutritionValues();
            showBanner(BannerType.ERROR, "Nutrition could not be loaded",
                    viewModel.getStatusMessage());
            return;
        }

        recipeNameLabel.setText(viewModel.getRecipeName());
        servingsLabel.setText(viewModel.getServings());
        updateValueLabels(totalValueLabels, viewModel.getTotalValues());
        updateValueLabels(perServingValueLabels, viewModel.getPerServingValues());
        updateIngredientRows(viewModel.getIngredientRows());
        updateCalculationNote();

        if (!viewModel.isDataAvailable()) {
            showBanner(BannerType.WARNING, "Nutrition unavailable",
                    UNAVAILABLE_MESSAGE);
        }
        else if (!viewModel.isTotalComplete()) {
            final boolean containsEstimates = viewModel.containsEstimates();
            showBanner(BannerType.WARNING,
                    containsEstimates
                            ? "Partial Nutritional Estimate" : "Partial Nutrition Data",
                    containsEstimates
                            ? PARTIAL_ESTIMATE_MESSAGE : PARTIAL_AVAILABLE_MESSAGE);
        }
        else if (viewModel.containsEstimates()) {
            showBanner(BannerType.WARNING, "Estimated Nutrition",
                    ESTIMATE_MESSAGE);
        }
        else {
            hideStatusBanner();
        }
    }

    private void showLoadingState() {
        clearNutritionValues();
        showBanner(BannerType.LOADING, "Calculating nutrition",
                "Retrieving ingredient nutrition and calculating recipe totals.");
    }

    private void showBanner(BannerType type, String title, String message) {
        statusPanel.setVisible(true);
        statusGap.setVisible(true);
        statusTitleLabel.setText(title);
        statusArea.setText(message == null ? "" : message);
        statusArea.setCaretPosition(0);
        loadingIndicator.setVisible(type == BannerType.LOADING);
        retryButton.setVisible(type == BannerType.ERROR);

        Color background = UiTheme.MINT;
        Color foreground = UiTheme.SECONDARY;
        if (type == BannerType.WARNING) {
            background = UiTheme.WARNING_BACKGROUND;
            foreground = UiTheme.WARNING_TEXT;
        }
        else if (type == BannerType.ERROR) {
            background = UiTheme.ERROR_BACKGROUND;
            foreground = UiTheme.ERROR;
        }
        statusPanel.setPanelColor(background);
        statusTitleLabel.setForeground(foreground);
        statusArea.setForeground(type == BannerType.INFO || type == BannerType.LOADING
                ? UiTheme.TEXT_PRIMARY : foreground);
        statusArea.getAccessibleContext().setAccessibleDescription(title + ". " + message);
        statusPanel.revalidate();
        statusPanel.repaint();
        SwingUtilities.invokeLater(this::updateStatusBannerHeight);
    }

    private void hideStatusBanner() {
        statusPanel.setVisible(false);
        statusGap.setVisible(false);
        revalidate();
        repaint();
    }

    /** Sizes the wrapped warning to its complete message at the current width. */
    private void updateStatusBannerHeight() {
        if (!statusPanel.isVisible()) {
            return;
        }

        final int panelWidth = statusPanel.getWidth() > 0
                ? statusPanel.getWidth() : getPreferredSize().width - 32;
        final int actionWidth = retryButton.isVisible()
                ? retryButton.getPreferredSize().width + 12 : 0;
        final int textWidth = Math.max(280, panelWidth - 28 - actionWidth);
        final FontMetrics metrics = statusArea.getFontMetrics(statusArea.getFont());
        final int lineCount = countWrappedLines(statusArea.getText(), metrics, textWidth);
        final int messageHeight = Math.max(metrics.getHeight(),
                lineCount * metrics.getHeight());
        statusArea.setRows(Math.max(1, lineCount));
        statusArea.setPreferredSize(new Dimension(textWidth, messageHeight + 2));
        statusArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, messageHeight + 2));

        final int progressHeight = loadingIndicator.isVisible() ? 7 : 0;
        final int bannerHeight = 18 + statusTitleLabel.getPreferredSize().height
                + messageHeight + progressHeight;
        statusPanel.setPreferredSize(new Dimension(panelWidth, bannerHeight));
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bannerHeight));
        statusPanel.revalidate();
        revalidate();
    }

    private static int countWrappedLines(String text, FontMetrics metrics, int width) {
        if (text == null || text.isBlank()) {
            return 1;
        }

        int lineCount = 0;
        for (String paragraph : text.split("\\R", -1)) {
            int currentWidth = 0;
            int paragraphLines = 1;
            for (String word : paragraph.trim().split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                final int wordWidth = metrics.stringWidth(word);
                final int separatorWidth = currentWidth == 0 ? 0 : metrics.charWidth(' ');
                if (currentWidth > 0 && currentWidth + separatorWidth + wordWidth > width) {
                    paragraphLines++;
                    currentWidth = wordWidth;
                }
                else {
                    currentWidth += separatorWidth + wordWidth;
                }
            }
            lineCount += paragraphLines;
        }
        return Math.max(1, lineCount);
    }

    private void configureInitialState() {
        setCalculationNote(null);
        retryButton.setVisible(false);
        loadingIndicator.setVisible(false);
        clearNutritionValues();
        showBanner(BannerType.INFO, "Nutrition details",
                "Select a recipe to view its nutrition.");
    }

    private void clearDisplayedNutrition() {
        setCalculationNote(null);
        clearNutritionValues();
        if (selectedRecipe == null) {
            showBanner(BannerType.INFO, "Nutrition details",
                    "Select a recipe to view its nutrition.");
        }
        else {
            showBanner(BannerType.INFO, "Ready to calculate",
                    "Nutrition will be calculated from the selected recipe's ingredients.");
        }
    }

    private void clearNutritionValues() {
        final String[] unavailable = {
            "Not Available", "Not Available", "Not Available", "Not Available"
        };
        updateValueLabels(totalValueLabels, unavailable);
        updateValueLabels(perServingValueLabels, unavailable);
        tableModel.setRowCount(0);
        ingredientCountLabel.setText("0 ingredients");
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
        final int count = rows.size();
        ingredientCountLabel.setText(count + (count == 1 ? " ingredient" : " ingredients"));
    }

    private void updateCalculationNote() {
        final String message;
        if (viewModel.containsEstimates() && !viewModel.isTotalComplete()) {
            message = "Includes estimated and unavailable nutrition values.";
        }
        else if (viewModel.containsEstimates()) {
            message = "Some nutrition values are estimated.";
        }
        else if (!viewModel.isTotalComplete()) {
            message = "Some nutrition values are unavailable.";
        }
        else {
            message = null;
        }
        setCalculationNote(message);
    }

    private void setCalculationNote(String message) {
        final boolean visible = message != null && !message.isBlank();
        calculationNoteLabel.setText(visible ? message : "");
        calculationNoteLabel.setVisible(visible);
        calculationNoteSeparator.setVisible(visible);
    }

    private static JLabel[] createValueLabels() {
        return new JLabel[] {
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT),
            new JLabel("Not Available", SwingConstants.LEFT)
        };
    }

    private static JPanel verticalPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel separatorLabel() {
        final JLabel separator = new JLabel("|");
        separator.setFont(UiTheme.LABEL_FONT.deriveFont(Font.PLAIN));
        separator.setForeground(UiTheme.TEXT_SECONDARY);
        return separator;
    }

    private static String servingText(int servings) {
        return servings + (servings == 1 ? " serving" : " servings");
    }

    private enum BannerType {
        INFO,
        LOADING,
        WARNING,
        ERROR
    }

    private enum NutrientIconType {
        CALORIES,
        PROTEIN,
        CARBOHYDRATES,
        FAT
    }

    /** Small outline icons that support the visible nutrient labels. */
    private static class NutrientIcon implements Icon {
        private static final int SIZE = 14;
        private static final float STROKE_WIDTH = 1.4f;

        private final NutrientIconType type;

        NutrientIcon(NutrientIconType type) {
            this.type = type;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.translate(x, y);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            graphics2D.setColor(UiTheme.SECONDARY);
            graphics2D.setStroke(new BasicStroke(STROKE_WIDTH,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case CALORIES:
                    drawFlame(graphics2D);
                    break;
                case PROTEIN:
                    drawBean(graphics2D);
                    break;
                case CARBOHYDRATES:
                    drawWheat(graphics2D);
                    break;
                case FAT:
                    drawDroplet(graphics2D);
                    break;
                default:
                    break;
            }
            graphics2D.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        private void drawFlame(Graphics2D graphics) {
            final Path2D flame = new Path2D.Double();
            flame.moveTo(7, 1);
            flame.curveTo(8, 4, 12, 6, 12, 9);
            flame.curveTo(12, 12, 10, 13, 7, 13);
            flame.curveTo(4, 13, 2, 12, 2, 9);
            flame.curveTo(2, 6, 4, 4, 6, 2);
            flame.curveTo(6, 5, 8, 6, 8, 8);
            flame.curveTo(10, 5, 8, 3, 7, 1);
            flame.closePath();
            graphics.draw(flame);
        }

        private void drawBean(Graphics2D graphics) {
            final Path2D bean = new Path2D.Double();
            bean.moveTo(2, 8);
            bean.curveTo(2, 4, 5, 2, 9, 2);
            bean.curveTo(12, 2, 13, 4, 12, 7);
            bean.curveTo(11, 11, 8, 13, 5, 12);
            bean.curveTo(2, 12, 1, 10, 2, 8);
            bean.closePath();
            graphics.draw(bean);

            final Path2D detail = new Path2D.Double();
            detail.moveTo(4, 8);
            detail.curveTo(6, 10, 9, 8, 10, 5);
            graphics.draw(detail);
        }

        private void drawWheat(Graphics2D graphics) {
            final Path2D grain = new Path2D.Double();
            grain.moveTo(7, 1);
            grain.curveTo(4, 3, 5, 5, 7, 6);
            grain.curveTo(9, 5, 10, 3, 7, 1);
            grain.closePath();
            graphics.draw(grain);
            graphics.drawLine(7, 5, 7, 13);
            graphics.drawLine(7, 8, 4, 6);
            graphics.drawLine(7, 10, 10, 8);
        }

        private void drawDroplet(Graphics2D graphics) {
            final Path2D droplet = new Path2D.Double();
            droplet.moveTo(7, 1);
            droplet.curveTo(6, 3, 3, 6, 3, 9);
            droplet.curveTo(3, 12, 5, 13, 7, 13);
            droplet.curveTo(9, 13, 11, 12, 11, 9);
            droplet.curveTo(11, 6, 8, 3, 7, 1);
            droplet.closePath();
            graphics.draw(droplet);
        }
    }

    /** Paints the soft rounded surfaces used by the approved design. */
    private static class RoundedPanel extends JPanel {
        private Color panelColor;
        private final Color outlineColor;
        private final int arc;

        RoundedPanel(Color panelColor, Color outlineColor, int cornerRadius) {
            this.panelColor = panelColor;
            this.outlineColor = outlineColor;
            this.arc = cornerRadius * 2;
            setOpaque(false);
        }

        void setPanelColor(Color panelColor) {
            this.panelColor = panelColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(panelColor);
            graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    arc, arc);
            graphics2D.setColor(outlineColor);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    arc, arc);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Shows the shared focus ring only when focus arrived by keyboard traversal. */
    private static class KeyboardFocusThemeButton extends ThemeButton {
        private boolean keyboardFocusVisible;
        private boolean suppressFocusDuringPaint;

        KeyboardFocusThemeButton(String text, Variant variant) {
            super(text, variant);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    keyboardFocusVisible = isKeyboardTraversal(event.getCause());
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent event) {
                    keyboardFocusVisible = false;
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hideKeyboardFocusRing();
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    hideKeyboardFocusRing();
                }
            });
        }

        @Override
        public boolean isFocusOwner() {
            return super.isFocusOwner()
                    && (!suppressFocusDuringPaint || keyboardFocusVisible);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            suppressFocusDuringPaint = !keyboardFocusVisible;
            try {
                super.paintComponent(graphics);
            }
            finally {
                suppressFocusDuringPaint = false;
            }
        }

        private void hideKeyboardFocusRing() {
            keyboardFocusVisible = false;
            repaint();
        }

        private static boolean isKeyboardTraversal(FocusEvent.Cause cause) {
            return cause == FocusEvent.Cause.TRAVERSAL
                    || cause == FocusEvent.Cause.TRAVERSAL_FORWARD
                    || cause == FocusEvent.Cause.TRAVERSAL_BACKWARD
                    || cause == FocusEvent.Cause.TRAVERSAL_UP
                    || cause == FocusEvent.Cause.TRAVERSAL_DOWN;
        }
    }

    private static class TableHeaderRenderer extends DefaultTableCellRenderer {
        TableHeaderRenderer() {
            setOpaque(true);
            setBackground(UiTheme.MINT);
            setForeground(UiTheme.NEUTRAL_TEXT);
            setFont(UiTheme.LABEL_FONT);
            setBorder(BorderFactory.createEmptyBorder(
                    0, UiTheme.SPACE_SM, 0, UiTheme.SPACE_SM));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused,
                    row, column);
            final int leftInset;
            if (column == 0) {
                leftInset = UiTheme.SPACE_MD;
            }
            else if (column == 2) {
                leftInset = UiTheme.SPACE_LG;
            }
            else {
                leftInset = UiTheme.SPACE_SM;
            }
            setBorder(BorderFactory.createEmptyBorder(
                    0, leftInset, 0, UiTheme.SPACE_SM));
            if (column == 7) {
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else if (column == 1 || column >= 3 && column <= 6) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return this;
        }
    }

    private static class NutritionTableRenderer extends DefaultTableCellRenderer {
        private final boolean emphasize;
        private final int leftInset;
        private final int rightInset;

        NutritionTableRenderer(int alignment, boolean emphasize) {
            this(alignment, emphasize, UiTheme.SPACE_SM, UiTheme.SPACE_SM);
        }

        NutritionTableRenderer(int alignment, boolean emphasize,
                               int leftInset, int rightInset) {
            this.emphasize = emphasize;
            this.leftInset = leftInset;
            this.rightInset = rightInset;
            setHorizontalAlignment(alignment);
            setBorder(BorderFactory.createEmptyBorder(
                    0, leftInset, 0, rightInset));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused,
                    row, column);
            setBorder(BorderFactory.createEmptyBorder(
                    0, leftInset, 0, rightInset));
            setFont(UiTheme.BODY_FONT.deriveFont(
                    emphasize ? Font.BOLD : Font.PLAIN));
            setForeground(UiTheme.TEXT_PRIMARY);
            if (!selected) {
                setBackground(row % 2 == 0
                        ? UiTheme.SURFACE : UiTheme.BACKGROUND);
            }
            return this;
        }
    }

    private static class StatusRenderer extends JLabel
            implements javax.swing.table.TableCellRenderer {
        private Color rowBackground = UiTheme.SURFACE;
        private Color badgeBackground = UiTheme.BORDER;

        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(UiTheme.LABEL_FONT.deriveFont(Font.BOLD, 12f));
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            final String fullStatus = value == null ? "" : value.toString();
            if (fullStatus.startsWith("Available")) {
                setText("Available");
                badgeBackground = UiTheme.SUCCESS_BACKGROUND;
                setForeground(UiTheme.SECONDARY);
            }
            else if (fullStatus.startsWith("Estimated")) {
                setText("Estimated");
                badgeBackground = UiTheme.WARNING_BACKGROUND;
                setForeground(UiTheme.WARNING_TEXT);
            }
            else {
                setText("Not Available");
                badgeBackground = UiTheme.NEUTRAL_BACKGROUND;
                setForeground(UiTheme.NEUTRAL_TEXT);
            }
            rowBackground = selected ? UiTheme.MINT
                    : row % 2 == 0 ? UiTheme.SURFACE : UiTheme.BACKGROUND;
            setToolTipText(fullStatus);
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(rowBackground);
            graphics2D.fillRect(0, 0, getWidth(), getHeight());
            final int badgeWidth = Math.min(getWidth() - 12,
                    getFontMetrics(getFont()).stringWidth(getText()) + 22);
            final int badgeHeight = Math.min(24, getHeight() - 8);
            final int badgeX = (getWidth() - badgeWidth) / 2;
            final int badgeY = (getHeight() - badgeHeight) / 2;
            graphics2D.setColor(badgeBackground);
            graphics2D.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight,
                    badgeHeight, badgeHeight);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

}
