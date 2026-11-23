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
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import entity.Recipe;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionViewModel;

/**
 * Reusable nutrition content panel intended for a modal dialog.
 */
public class ViewNutritionView extends JPanel implements PropertyChangeListener {

    private static final Color SAGE_GREEN = new Color(110, 155, 118);
    private static final Color FOREST_GREEN = new Color(53, 94, 67);
    private static final Color DEEP_FOREST = new Color(43, 76, 54);
    private static final Color WARM_WHITE = new Color(248, 250, 246);
    private static final Color SOFT_MINT = new Color(230, 241, 232);
    private static final Color PALE_MINT = new Color(242, 249, 243);
    private static final Color SOFT_BEIGE = new Color(243, 235, 221);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(38, 51, 43);
    private static final Color SECONDARY_TEXT = new Color(91, 105, 96);
    private static final Color OUTLINE = new Color(225, 233, 226);
    private static final Color WARNING_TEXT = new Color(138, 75, 20);
    private static final Color WARNING_BACKGROUND = new Color(253, 243, 231);
    private static final Color ERROR_TEXT = new Color(139, 43, 43);
    private static final Color ERROR_BACKGROUND = new Color(253, 236, 236);
    private static final Color ROW_ALTERNATE = new Color(248, 252, 248);

    private static final String[] TABLE_COLUMNS = {
        "INGREDIENT", "QUANTITY", "UNIT", "CALORIES", "PROTEIN",
        "CARBS", "FAT", "DATA STATUS"
    };

    private static final String FONT_FAMILY = findFontFamily();

    private final ViewNutritionViewModel viewModel;
    private final JLabel recipeNameLabel = new JLabel("No recipe selected");
    private final JLabel servingsLabel = new JLabel("Servings unavailable");
    private final JLabel calculationNoteLabel = new JLabel(
            "Nutrition values are estimates based on ingredient quantities.");
    private final JLabel[] totalValueLabels = createValueLabels();
    private final JLabel[] perServingValueLabels = createValueLabels();
    private final RoundedPanel statusPanel = new RoundedPanel(SOFT_MINT, OUTLINE, 14);
    private final JLabel statusTitleLabel = new JLabel("Nutrition details");
    private final JTextArea statusArea = new JTextArea();
    private final JProgressBar loadingIndicator = new JProgressBar();
    private final RoundedButton retryButton = new RoundedButton("Retry", ButtonStyle.SECONDARY);
    private final RoundedButton headerCloseButton = new RoundedButton("X", ButtonStyle.GHOST);
    private final RoundedButton footerCloseButton = new RoundedButton("Close", ButtonStyle.PRIMARY);
    private final JLabel ingredientCountLabel = new JLabel("0 ingredients");
    private final Component statusGap = Box.createVerticalStrut(8);
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
        setBackground(WARM_WHITE);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(1040, 740));

        final JPanel upperContent = verticalPanel();
        upperContent.add(createHeader());
        upperContent.add(Box.createVerticalStrut(8));
        upperContent.add(createStatusBanner());
        upperContent.add(statusGap);
        upperContent.add(createNutritionSummary());

        add(upperContent, BorderLayout.NORTH);
        add(createIngredientBreakdown(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        retryButton.addActionListener(event -> requestNutrition());
        headerCloseButton.addActionListener(event -> closeAction.run());
        footerCloseButton.addActionListener(event -> closeAction.run());
        headerCloseButton.getAccessibleContext().setAccessibleName("Close nutrition dialog");
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

    public void requestInitialFocus() {
        headerCloseButton.requestFocusInWindow();
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
        final RoundedPanel header = new RoundedPanel(SOFT_MINT, OUTLINE, 18);
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 10));
        header.setPreferredSize(new Dimension(1000, 68));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        header.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel textPanel = verticalPanel();
        textPanel.setOpaque(false);

        final JLabel title = new JLabel("Recipe Nutrition");
        title.setFont(appFont(Font.BOLD, 20f));
        title.setForeground(FOREST_GREEN);
        title.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel details = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        details.setOpaque(false);
        recipeNameLabel.setFont(appFont(Font.BOLD, 13f));
        recipeNameLabel.setForeground(TEXT);
        servingsLabel.setFont(appFont(Font.PLAIN, 12f));
        servingsLabel.setForeground(SECONDARY_TEXT);
        calculationNoteLabel.setFont(appFont(Font.PLAIN, 12f));
        calculationNoteLabel.setForeground(SECONDARY_TEXT);
        details.add(recipeNameLabel);
        details.add(separatorLabel());
        details.add(servingsLabel);
        details.add(separatorLabel());
        details.add(calculationNoteLabel);
        details.setAlignmentX(LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(details);

        headerCloseButton.setPreferredSize(new Dimension(36, 36));
        headerCloseButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        headerCloseButton.setFont(appFont(Font.BOLD, 16f));
        headerCloseButton.setToolTipText("Close (Esc)");
        header.add(textPanel, BorderLayout.CENTER);
        header.add(headerCloseButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatusBanner() {
        statusPanel.setLayout(new BorderLayout(10, 3));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 12));
        statusPanel.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);
        statusTitleLabel.setFont(appFont(Font.BOLD, 13f));
        statusTitleLabel.setForeground(FOREST_GREEN);
        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        statusArea.setFont(appFont(Font.PLAIN, 12f));
        statusArea.setForeground(TEXT);
        statusArea.setRows(1);
        textPanel.add(statusTitleLabel, BorderLayout.NORTH);
        textPanel.add(statusArea, BorderLayout.CENTER);

        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setBorderPainted(false);
        loadingIndicator.setBackground(PALE_MINT);
        loadingIndicator.setForeground(SAGE_GREEN);
        loadingIndicator.setPreferredSize(new Dimension(120, 4));
        loadingIndicator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

        retryButton.setPreferredSize(new Dimension(80, 34));
        statusPanel.add(textPanel, BorderLayout.CENTER);
        statusPanel.add(retryButton, BorderLayout.EAST);
        statusPanel.add(loadingIndicator, BorderLayout.SOUTH);
        return statusPanel;
    }

    private JPanel createNutritionSummary() {
        final JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
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
        final RoundedPanel card = new RoundedPanel(SOFT_MINT, OUTLINE, 18);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(appFont(Font.BOLD, 15f));
        titleLabel.setForeground(FOREST_GREEN);
        final JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(appFont(Font.PLAIN, 11f));
        subtitleLabel.setForeground(SECONDARY_TEXT);
        heading.add(titleLabel, BorderLayout.WEST);
        heading.add(subtitleLabel, BorderLayout.EAST);

        final JPanel metrics = new JPanel(new GridLayout(2, 2, 8, 8));
        metrics.setOpaque(false);
        metrics.add(createMetricPanel("Calories", valueLabels[0]));
        metrics.add(createMetricPanel("Protein", valueLabels[1]));
        metrics.add(createMetricPanel("Carbohydrates", valueLabels[2]));
        metrics.add(createMetricPanel("Fat", valueLabels[3]));

        card.add(heading, BorderLayout.NORTH);
        card.add(metrics, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricPanel(String name, JLabel valueLabel) {
        final RoundedPanel panel = new RoundedPanel(WHITE, OUTLINE, 14);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 5, 10));

        final JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(appFont(Font.PLAIN, 11f));
        nameLabel.setForeground(SECONDARY_TEXT);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setFont(appFont(Font.BOLD, 17f));
        valueLabel.setForeground(FOREST_GREEN);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(valueLabel);
        return panel;
    }

    private JPanel createIngredientBreakdown() {
        final RoundedPanel panel = new RoundedPanel(WHITE, OUTLINE, 18);
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        final JLabel title = new JLabel("Ingredient Breakdown");
        title.setFont(appFont(Font.BOLD, 16f));
        title.setForeground(TEXT);
        ingredientCountLabel.setFont(appFont(Font.PLAIN, 12f));
        ingredientCountLabel.setForeground(SECONDARY_TEXT);
        heading.add(title, BorderLayout.WEST);
        heading.add(ingredientCountLabel, BorderLayout.EAST);

        configureTable();
        tableScrollPane.setColumnHeaderView(ingredientTable.getTableHeader());
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(WHITE);
        tableScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setPreferredSize(new Dimension(984, 272));
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        tableScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tableScrollPane.getVerticalScrollBar().setUI(new MinimalScrollBarUI());
        panel.add(heading, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooter() {
        final JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);
        final JLabel note = new JLabel("~ marks estimates; * marks incomplete totals.");
        note.setFont(appFont(Font.PLAIN, 11f));
        note.setForeground(SECONDARY_TEXT);
        footerCloseButton.setPreferredSize(new Dimension(100, 36));
        footer.add(note, BorderLayout.WEST);
        footer.add(footerCloseButton, BorderLayout.EAST);
        return footer;
    }

    private void configureTable() {
        ingredientTable.setFont(appFont(Font.PLAIN, 13f));
        ingredientTable.setForeground(TEXT);
        ingredientTable.setBackground(WHITE);
        ingredientTable.setRowHeight(34);
        ingredientTable.setShowHorizontalLines(false);
        ingredientTable.setShowVerticalLines(false);
        ingredientTable.setIntercellSpacing(new Dimension(0, 0));
        ingredientTable.setSelectionBackground(SOFT_MINT);
        ingredientTable.setSelectionForeground(TEXT);
        ingredientTable.setFillsViewportHeight(true);
        ingredientTable.setAutoCreateRowSorter(false);

        final JTableHeader header = ingredientTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setDefaultRenderer(new TableHeaderRenderer());

        ingredientTable.setDefaultRenderer(Object.class,
                new NutritionTableRenderer(SwingConstants.LEFT, false));
        ingredientTable.getColumnModel().getColumn(0).setCellRenderer(
                new NutritionTableRenderer(SwingConstants.LEFT, true));
        for (int column : new int[] {1, 3, 4, 5, 6}) {
            ingredientTable.getColumnModel().getColumn(column).setCellRenderer(
                    new NutritionTableRenderer(SwingConstants.RIGHT, false));
        }
        ingredientTable.getColumnModel().getColumn(7).setCellRenderer(
                new StatusRenderer());

        final int[] columnWidths = {230, 78, 68, 104, 92, 88, 70, 146};
        for (int index = 0; index < columnWidths.length; index++) {
            ingredientTable.getColumnModel().getColumn(index)
                    .setPreferredWidth(columnWidths[index]);
        }
    }

    private void applyViewModelState() {
        if (viewModel.isLoading()) {
            recipeNameLabel.setText(viewModel.getRecipeName());
            servingsLabel.setText(viewModel.getServings());
            clearNutritionValues();
            showLoadingState();
            return;
        }

        if (viewModel.isError()) {
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

        if (!viewModel.isDataAvailable()) {
            showBanner(BannerType.WARNING, "Nutrition unavailable",
                    viewModel.getStatusMessage());
        }
        else if (!viewModel.isTotalComplete()) {
            showBanner(BannerType.WARNING, "Partial nutritional estimate",
                    viewModel.getStatusMessage());
        }
        else if (viewModel.containsEstimates()) {
            showBanner(BannerType.WARNING, "Estimated nutrition",
                    viewModel.getStatusMessage());
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

        Color background = SOFT_MINT;
        Color foreground = FOREST_GREEN;
        if (type == BannerType.WARNING) {
            background = WARNING_BACKGROUND;
            foreground = WARNING_TEXT;
        }
        else if (type == BannerType.ERROR) {
            background = ERROR_BACKGROUND;
            foreground = ERROR_TEXT;
        }
        statusPanel.setPanelColor(background);
        statusTitleLabel.setForeground(foreground);
        statusArea.setForeground(type == BannerType.INFO || type == BannerType.LOADING
                ? TEXT : foreground);
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
        retryButton.setVisible(false);
        loadingIndicator.setVisible(false);
        clearNutritionValues();
        showBanner(BannerType.INFO, "Nutrition details",
                "Select a recipe to view its nutrition.");
    }

    private void clearDisplayedNutrition() {
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
        separator.setFont(appFont(Font.PLAIN, 13f));
        separator.setForeground(SECONDARY_TEXT);
        return separator;
    }

    private static String servingText(int servings) {
        return servings + (servings == 1 ? " serving" : " servings");
    }

    private static Font appFont(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size)).deriveFont(size);
    }

    private static String findFontFamily() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            if ("Plus Jakarta Sans".equalsIgnoreCase(family)) {
                return family;
            }
        }
        final Font fallback = UIManager.getFont("Label.font");
        return fallback == null ? Font.SANS_SERIF : fallback.getFamily();
    }

    private enum BannerType {
        INFO,
        LOADING,
        WARNING,
        ERROR
    }

    private enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        GHOST
    }

    /** Paints the soft rounded surfaces used by the approved design. */
    private static class RoundedPanel extends JPanel {
        private Color panelColor;
        private final Color outlineColor;
        private final int radius;

        RoundedPanel(Color panelColor, Color outlineColor, int radius) {
            this.panelColor = panelColor;
            this.outlineColor = outlineColor;
            this.radius = radius;
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
                    radius, radius);
            graphics2D.setColor(outlineColor);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    radius, radius);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Paints stable hover, pressed, and keyboard-focus states. */
    private static class RoundedButton extends JButton {
        private static final int RADIUS = 12;
        private final ButtonStyle style;

        RoundedButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setFont(appFont(Font.BOLD, 13f));
            setForeground(style == ButtonStyle.PRIMARY ? WHITE : TEXT);
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(buttonColor());
            graphics2D.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2,
                    RADIUS, RADIUS);

            if (style != ButtonStyle.PRIMARY) {
                graphics2D.setColor(getModel().isRollover() ? SAGE_GREEN : OUTLINE);
                graphics2D.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3,
                        RADIUS, RADIUS);
            }
            if (hasFocus()) {
                graphics2D.setStroke(new BasicStroke(2f));
                graphics2D.setColor(SAGE_GREEN);
                graphics2D.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7,
                        RADIUS - 2, RADIUS - 2);
            }
            graphics2D.dispose();
            super.paintComponent(graphics);
        }

        private Color buttonColor() {
            if (style == ButtonStyle.PRIMARY) {
                return getModel().isPressed() ? DEEP_FOREST
                        : getModel().isRollover() ? SAGE_GREEN : FOREST_GREEN;
            }
            if (style == ButtonStyle.SECONDARY) {
                return getModel().isPressed() ? new Color(217, 235, 220)
                        : getModel().isRollover() ? WHITE : SOFT_MINT;
            }
            return getModel().isRollover() || getModel().isPressed()
                    ? WHITE : new Color(238, 245, 239);
        }
    }

    private static class TableHeaderRenderer extends DefaultTableCellRenderer {
        TableHeaderRenderer() {
            setOpaque(true);
            setBackground(SOFT_MINT);
            setForeground(FOREST_GREEN);
            setFont(appFont(Font.BOLD, 12f));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused,
                    row, column);
            setBorder(BorderFactory.createEmptyBorder(0,
                    column == 7 ? 24 : 10, 0, 10));
            setHorizontalAlignment(column >= 1 && column <= 6
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    private static class NutritionTableRenderer extends DefaultTableCellRenderer {
        private final boolean emphasize;

        NutritionTableRenderer(int alignment, boolean emphasize) {
            this.emphasize = emphasize;
            setHorizontalAlignment(alignment);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused,
                    row, column);
            setFont(appFont(emphasize ? Font.BOLD : Font.PLAIN, 13f));
            setForeground(TEXT);
            if (!selected) {
                setBackground(row % 2 == 0 ? WHITE : ROW_ALTERNATE);
            }
            return this;
        }
    }

    private static class StatusRenderer extends JLabel
            implements javax.swing.table.TableCellRenderer {
        private Color rowBackground = WHITE;
        private Color badgeBackground = new Color(232, 238, 233);

        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(appFont(Font.BOLD, 12f));
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            final String fullStatus = value == null ? "" : value.toString();
            if (fullStatus.startsWith("Available")) {
                setText("Available");
                badgeBackground = new Color(208, 242, 217);
                setForeground(FOREST_GREEN);
            }
            else if (fullStatus.startsWith("Estimated")) {
                setText("Estimated");
                badgeBackground = SOFT_BEIGE;
                setForeground(WARNING_TEXT);
            }
            else {
                setText("Not Available");
                badgeBackground = new Color(232, 238, 233);
                setForeground(TEXT);
            }
            rowBackground = selected ? SOFT_MINT
                    : row % 2 == 0 ? WHITE : ROW_ALTERNATE;
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

    /** Keeps overflow scrolling available without a bulky default scrollbar. */
    private static class MinimalScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(184, 198, 187);
            trackColor = WHITE;
        }

        @Override
        protected void paintThumb(Graphics graphics, javax.swing.JComponent component,
                                  Rectangle bounds) {
            if (!scrollbar.isEnabled() || bounds.isEmpty()) {
                return;
            }
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(thumbColor);
            graphics2D.fillRoundRect(bounds.x + 2, bounds.y, 4, bounds.height,
                    4, 4);
            graphics2D.dispose();
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroSizeButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroSizeButton();
        }

        private JButton zeroSizeButton() {
            final JButton button = new JButton();
            final Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }
    }
}
