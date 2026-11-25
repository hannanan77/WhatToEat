package view;

import entity.MealPlanEntry;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;
import view.components.ThemeButton;
import view.components.ThemeCard;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
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
import javax.swing.SwingWorker;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

/** Weekly calendar that uses recipe IDs from the recipe collection supplied by the host. */
public class WeeklyMealPlanView extends JPanel {
    private static final Color CANVAS = UiTheme.BACKGROUND;
    private static final Color MINT = UiTheme.MINT;
    private static final Color ACTIVE = UiTheme.SUCCESS_BACKGROUND;
    private static final Color FOREST = UiTheme.SECONDARY;
    private static final Color INK = UiTheme.TEXT_PRIMARY;
    private static final Color OUTLINE = UiTheme.BORDER;
    private static final List<String> MEALS = List.of("Breakfast", "Lunch", "Dinner");
    private static final int DAYS = 7;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final Font PLAIN_TEXT = UiTheme.BODY_FONT;
    private static final Font BOLD_TEXT = UiTheme.LABEL_FONT;

    private LocalDate selectedDay;
    private final Map<LocalDate, JButton> dayHeaders = new LinkedHashMap<>();
    private final JPanel calendar = new JPanel(new GridLayout(4, DAYS + 1, 8, 8));
    private final JLabel planned = label("", BOLD_TEXT, 13);
    private final JLabel status = label(" ", PLAIN_TEXT, 13);
    private final WeeklyMealPlanController controller;
    private final WeeklyMealPlanViewModel viewModel;
    private final LocalDate weekStart;
    private final RecipeCollection collection;
    private final RecipeCollection imageCollection;
    private Map<String, String> imageLocations = Map.of();
    private final Map<String, Optional<BufferedImage>> photos = new HashMap<>();
    private final JPanel columns = new JPanel(new BorderLayout(UiTheme.SECTION_GAP, 0));
    private final JPanel weekColumn = new JPanel(new BorderLayout());
    private final Consumer<String> openRecipe;
    private Map<String, String> recipes = Map.of();

    @FunctionalInterface
    public interface RecipeCollection {
        Map<String, String> load() throws IOException;
    }

    public WeeklyMealPlanView(WeeklyMealPlanController controller,
                              WeeklyMealPlanViewModel viewModel, LocalDate weekStart,
                              RecipeCollection collection) {
        this(controller, viewModel, weekStart, collection, null, Map::of);
    }

    public WeeklyMealPlanView(WeeklyMealPlanController controller,
                              WeeklyMealPlanViewModel viewModel, LocalDate weekStart,
                              RecipeCollection collection, Consumer<String> openRecipe,
                              RecipeCollection imageCollection) {
        this.imageCollection = imageCollection;
        this.openRecipe = openRecipe;
        this.controller = controller;
        this.viewModel = viewModel;
        this.weekStart = weekStart;
        this.collection = collection;
        try {
            loadCollection();
        } catch (IOException exception) {
            // The picker retries when opened and displays the read error there.
        }
        setLayout(new BorderLayout());
        setBackground(CANVAS);

        JPanel page = new JPanel(new BorderLayout(0, UiTheme.SECTION_GAP));
        page.setBackground(CANVAS);
        page.setBorder(BorderFactory.createEmptyBorder(UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING,
                UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING));
        JLabel title = new JLabel("Weekly Meal Planner");
        title.setFont(UiTheme.HEADING_FONT);
        title.setForeground(INK);
        page.add(title, BorderLayout.NORTH);

        columns.setOpaque(false);
        weekColumn.setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createActions());
        content.add(Box.createVerticalStrut(UiTheme.SPACE_MD));
        content.add(createWeekBar());
        content.add(Box.createVerticalStrut(UiTheme.SPACE_MD));

        calendar.setBackground(UiTheme.SURFACE);
        calendar.setPreferredSize(new Dimension(750, 420));
        calendar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        calendar.setAlignmentX(LEFT_ALIGNMENT);
        content.add(calendar);
        content.add(Box.createVerticalStrut(UiTheme.SPACE_MD));
        status.setAlignmentX(LEFT_ALIGNMENT);
        content.add(status);

        JPanel calendarColumn = new JPanel(new BorderLayout());
        calendarColumn.setOpaque(false);
        calendarColumn.add(new ThemeCard(null, content), BorderLayout.NORTH);
        columns.add(calendarColumn, BorderLayout.CENTER);
        page.add(columns, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        viewModel.addPropertyChangeListener(event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh();
            } else {
                SwingUtilities.invokeLater(this::refresh);
            }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshRecipeNames();
            }
        });
        refresh();
    }

    public void setWeekSidebar(JComponent sidebar) {
        weekColumn.removeAll();
        weekColumn.add(sidebar, BorderLayout.NORTH);
        columns.add(weekColumn, BorderLayout.WEST);
        columns.revalidate();
        columns.repaint();
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACE_SM, 0));
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setOpaque(false);
        JButton clear = new ThemeButton("Clear Week", false);
        clear.addActionListener(event -> {
            if (JOptionPane.showConfirmDialog(this, "Clear all meals for this week?",
                    "Clear Week", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                controller.clearPlan();
            }
        });
        JButton save = new ThemeButton("Save Meal Plan", true);
        save.addActionListener(event -> controller.savePlan());
        JButton export = new ThemeButton("Export PDF", false);
        export.addActionListener(event -> exportPdf());
        actions.add(clear);
        actions.add(export);
        actions.add(save);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, actions.getPreferredSize().height));
        return actions;
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

    public void refreshRecipeNames() {
        try {
            loadCollection();
            refresh();
        } catch (IOException exception) {
            // The picker retries and reports collection read failures.
        }
    }

    private void loadCollection() throws IOException {
        recipes = Map.copyOf(collection.load());
        try {
            imageLocations = Map.copyOf(imageCollection.load());
        }
        catch (IOException exception) {
            imageLocations = Map.of();
        }
    }

    private void refresh() {
        List<MealPlanEntry> assignments = viewModel.getAssignments();
        calendar.removeAll();
        dayHeaders.clear();
        calendar.add(heading("Meal", MINT, FOREST));
        for (int day = 0; day < DAYS; day++) {
            LocalDate date = weekStart.plusDays(day);
            JButton header = button(DAY.format(date) + " " + date.getDayOfMonth(), MINT, FOREST);
            header.addActionListener(event -> selectDay(date));
            dayHeaders.put(date, header);
            calendar.add(header);
        }

        updateDayHeaders();
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

    private void selectDay(LocalDate date) {
        selectedDay = date;
        updateDayHeaders();
    }

    private void updateDayHeaders() {
        dayHeaders.forEach((date, header) -> {
            boolean selected = date.equals(selectedDay);
            header.setBackground(selected ? FOREST : MINT);
            header.setForeground(selected ? Color.WHITE : FOREST);
        });
    }

    private JComponent createSlot(LocalDate date, String meal, String recipeId) {
        JPanel slot = new JPanel(new BorderLayout());
        slot.setBackground(Color.WHITE);
        slot.setBorder(BorderFactory.createLineBorder(OUTLINE));
        boolean assigned = recipeId != null;
        if (assigned) {
            String name = recipes.getOrDefault(recipeId, "Recipe unavailable");
            JComponent title;
            if (openRecipe == null) {
                title = new JLabel(name) {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        paintRecipeName(graphics, this, getText(), recipePhoto(recipeId));
                    }
                };
                title.setForeground(INK);
            } else {
                JButton details = button(name, Color.WHITE, INK);
                details.setUI(new BasicButtonUI() {
                    @Override
                    protected void paintText(Graphics graphics, AbstractButton button,
                                             Rectangle textBounds, String clippedText) {
                        paintRecipeName(graphics, button, button.getText(), recipePhoto(recipeId));
                    }
                });
                details.addActionListener(event -> {
                    selectDay(date);
                    if (recipes.containsKey(recipeId)) {
                        openRecipe.accept(recipeId);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "This recipe is no longer in My Collection. Use Replace to choose a saved recipe.",
                                "Recipe unavailable", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                title = details;
            }
            title.setFont(UiTheme.SMALL_LABEL_FONT);
            title.setToolTipText(name);
            slot.add(title, BorderLayout.CENTER);
        }
        JButton choose = button(assigned ? "Replace" : "+ Add Recipe", MINT, FOREST);
        choose.getAccessibleContext().setAccessibleName(
                choose.getText() + " for " + DAY.format(date) + " " + meal);
        choose.addActionListener(event -> {
            selectDay(date);
            chooseRecipe(date, meal, recipeId);
        });
        slot.add(choose, assigned ? BorderLayout.SOUTH : BorderLayout.CENTER);
        return slot;
    }

    private BufferedImage recipePhoto(String recipeId) {
        String location = imageLocations.get(recipeId);
        if (location == null) {
            return null;
        }
        if (!photos.containsKey(location)) {
            photos.put(location, Optional.empty());
            new SwingWorker<BufferedImage, Void>() {
                @Override
                protected BufferedImage doInBackground() {
                    return readPhoto(location);
                }

                @Override
                protected void done() {
                    try {
                        BufferedImage photo = get();
                        if (photo != null) {
                            photos.put(location, Optional.of(photo));
                            calendar.repaint();
                        }
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    catch (java.util.concurrent.ExecutionException exception) {
                        // A failed photo leaves the plain-text card available.
                    }
                }
            }.execute();
        }
        return photos.get(location).orElse(null);
    }

    private static BufferedImage readPhoto(String location) {
        try {
            BufferedImage image;
            if (location.regionMatches(true, 0, "file:", 0, 5)) {
                image = ImageIO.read(Path.of(URI.create(location)).toFile());
            }
            else if (location.startsWith("https://") || location.startsWith("http://")) {
                URLConnection connection = URI.create(location).toURL().openConnection();
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                try (InputStream stream = connection.getInputStream()) {
                    image = ImageIO.read(stream);
                }
            }
            else {
                image = ImageIO.read(Path.of(location).toFile());
            }
            if (image == null || Math.max(image.getWidth(), image.getHeight()) <= 384) {
                return image;
            }
            double scale = 384.0 / Math.max(image.getWidth(), image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D draw = thumbnail.createGraphics();
            try {
                draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                draw.drawImage(image, 0, 0, width, height, null);
            }
            finally {
                draw.dispose();
            }
            return thumbnail;
        }
        catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static Color paintPhoto(Graphics2D draw, JComponent component, BufferedImage photo) {
        int width = component.getWidth();
        int height = component.getHeight();
        double scale = Math.max((double) width / photo.getWidth(), (double) height / photo.getHeight());
        int cropWidth = Math.max(1, Math.min(photo.getWidth(), (int) Math.round(width / scale)));
        int cropHeight = Math.max(1, Math.min(photo.getHeight(), (int) Math.round(height / scale)));
        int left = (photo.getWidth() - cropWidth) / 2;
        int top = (photo.getHeight() - cropHeight) / 2;
        draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        draw.drawImage(photo, 0, 0, width, height,
                left, top, left + cropWidth, top + cropHeight, UiTheme.SURFACE, null);

        double brightness = 0;
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 5; column++) {
                int sample = photo.getRGB(left + (2 * column + 1) * cropWidth / 10,
                        top + (2 * row + 1) * cropHeight / 10);
                double alpha = (sample >>> 24) / 255.0;
                double luminance = 0.2126 * ((sample >> 16) & 255)
                        + 0.7152 * ((sample >> 8) & 255) + 0.0722 * (sample & 255);
                brightness += alpha * luminance + (1 - alpha) * 255;
            }
        }
        boolean light = brightness / 25 >= 145;
        draw.setColor(light ? new Color(255, 255, 255, 140) : new Color(0, 0, 0, 150));
        draw.fillRect(0, 0, width, height);
        return light ? Color.BLACK : Color.WHITE;
    }

    private static void paintRecipeName(Graphics graphics, JComponent component, String name,
                                        BufferedImage photo) {
        Insets insets = component.getInsets();
        int horizontalPadding = Math.min(4, Math.min(insets.left, insets.right));
        int verticalPadding = Math.min(4, Math.min(insets.top, insets.bottom));
        int width = component.getWidth() - 2 * horizontalPadding;
        int height = component.getHeight() - 2 * verticalPadding;
        if (width <= 0 || height <= 0 || name.isBlank()) {
            return;
        }

        Graphics2D draw = (Graphics2D) graphics.create();
        try {
            Color textColor = photo == null ? component.getForeground()
                    : paintPhoto(draw, component, photo);
            draw.setColor(textColor);
            Font font = component.getFont();
            String text = name.replaceAll("\\s+", " ").strip();
            for (int size = font.getSize(); size >= 1; size--) {
                AttributedString styled = new AttributedString(text);
                styled.addAttribute(TextAttribute.FONT, font.deriveFont((float) size));
                LineBreakMeasurer measurer =
                        new LineBreakMeasurer(styled.getIterator(), draw.getFontRenderContext());
                List<TextLayout> lines = new ArrayList<>();
                float textHeight = 0;
                while (measurer.getPosition() < text.length()) {
                    TextLayout line = measurer.nextLayout(width);
                    lines.add(line);
                    textHeight += line.getAscent() + line.getDescent() + line.getLeading();
                }

                if (textHeight <= height || size == 1) {
                    float scale = Math.min(1f, height / textHeight);
                    draw.translate(horizontalPadding, verticalPadding + (height - textHeight * scale) / 2);
                    draw.scale(scale, scale);
                    float baseline = 0;
                    for (TextLayout line : lines) {
                        baseline += line.getAscent();
                        line.draw(draw, (width / scale - line.getAdvance()) / 2, baseline);
                        baseline += line.getDescent() + line.getLeading();
                    }
                    return;
                }
            }
        }
        finally {
            draw.dispose();
        }
    }

    private void chooseRecipe(LocalDate date, String meal, String currentRecipeId) {
        String message;
        try {
            loadCollection();
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
                    setFont(UiTheme.BODY_LARGE_FONT);
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
        button.setFont(UiTheme.BUTTON_FONT);
        button.setOpaque(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        return button;
    }
}
