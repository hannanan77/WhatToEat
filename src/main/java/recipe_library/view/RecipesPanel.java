package recipe_library.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import entity.Recipe;
import recipe_library.interface_adapter.browse_recipes.RecipeDisplayData;
import recipe_library.interface_adapter.recipe_browser.RecipeBrowserViewModel;
import recipe_library.interface_adapter.resolve_selected_recipe.RecipeSelectionResolver;

/**
 * Reusable recipe browser. It is a panel, not an application frame.
 *
 * <p>The panel opens with an empty collection. Recipe data should arrive
 * later through this display contract:
 *
 * <p>RecipesPanel receives {@link RecipeDisplayData} from a future
 * BrowseRecipesViewModel. That view model is updated by a
 * BrowseRecipesPresenter after BrowseRecipesController calls
 * BrowseRecipesInteractor, which reads RecipeDataAccessInterface.
 *
 * <p>{@link #setRecipes(List)} is the public refresh point. Another use case,
 * such as adding a recipe, can ask the browse flow to load again and then
 * pass the new list here.
 */
public class RecipesPanel extends JPanel {

    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel(" ");
    private Runnable onlineSearchAction = () -> {
    };
    private Runnable showCollectionAction = () -> {
    };
    private Consumer<String> importAction = externalId -> {
    };
    private Consumer<Recipe> recipeSelectionAction = recipe -> {
    };
    private Runnable recipeSelectionClearedAction = () -> {
    };
    private Consumer<String> recipeSelectionFailedAction = message -> {
    };
    private RecipeSelectionResolver recipeResolver;
    private final EditorialScrollContent cardList = new EditorialScrollContent();
    private final EditorialScrollContent detailContent = new EditorialScrollContent();
    private final JScrollPane listScroll = scrollPane(cardList, "recipes-list-scroll");
    private final JScrollPane detailScroll = scrollPane(detailContent, "recipes-detail-scroll");
    private final List<RecipeDisplayData> recipes = new ArrayList<>();

    private String selectedId;
    private boolean frameSelection;
    private int emptyFrameCount;
    private int imageRequest;
    private int selectionRequest;

    public RecipesPanel() {
        setName("recipes-panel");
        setBackground(EditorialTheme.BACKGROUND);
        setOpaque(true);
        setPreferredSize(new Dimension(1100, 700));

        searchField.setName("recipes-search");
        styleSearchField(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                rebuildCards();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                rebuildCards();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                rebuildCards();
            }
        });

        JPanel left = column(searchColumn(), listScroll, 24, 24, 24, 12);
        left.setName("recipes-left");
        JPanel right = column(null, detailScroll, 24, 12, 24, 24);
        right.setName("recipes-right");
        layoutColumns(left, right);
        rebuildCards();
        renderDetails();
    }

    /**
     * Replaces the recipes shown in this panel.
     * Null is treated as an empty collection. Call this on the Swing thread
     * when a future BrowseRecipesViewModel publishes a new list.
     */
    /**
     * Shows blank card outlines for layout preview.
     * The cards contain no recipe names, ingredients, or instructions.
     */
    public void showEmptyCardFrames(int count) {
        Runnable update = () -> {
            boolean dropSelection = selectedId != null;
            recipes.clear();
            selectedId = null;
            frameSelection = false;
            emptyFrameCount = Math.max(0, count);
            if (dropSelection) {
                selectionRequest++;
                recipeSelectionClearedAction.run();
            }
            rebuildCards();
            renderDetails();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    public void bind(RecipeBrowserViewModel viewModel) {
        viewModel.addPropertyChangeListener(event -> {
            setStatusMessage(viewModel.getStatus());
            setRecipes(viewModel.getRecipes());
        });
        setStatusMessage(viewModel.getStatus());
        setRecipes(viewModel.getRecipes());
    }

    public void setOnlineSearchAction(Runnable action) {
        onlineSearchAction = action == null ? () -> { } : action;
    }

    public void setShowCollectionAction(Runnable action) {
        showCollectionAction = action == null ? () -> { } : action;
    }

    public void setImportAction(Consumer<String> action) {
        importAction = action == null ? externalId -> { } : action;
    }

    /**
     * Receives the complete recipe after the current selection has been loaded.
     * It is not called with an id, display data, or a failed lookup.
     */
    public void setRecipeSelectionAction(Consumer<Recipe> action) {
        recipeSelectionAction = action == null ? recipe -> { } : action;
    }

    /**
     * Runs when the selection is no longer valid, including the moment another
     * recipe is chosen.
     */
    public void setRecipeSelectionClearedAction(Runnable action) {
        recipeSelectionClearedAction = action == null ? () -> { } : action;
    }

    /**
     * Runs when the selected recipe cannot be loaded.
     * The argument is a short message for the caller to show or ignore.
     */
    public void setRecipeSelectionFailedAction(Consumer<String> action) {
        recipeSelectionFailedAction = action == null ? message -> { } : action;
    }

    /**
     * Loads a complete recipe away from this view.
     * Without a resolver, the panel still shows the selection and does not publish one.
     */
    public void setRecipeResolver(RecipeSelectionResolver resolver) {
        recipeResolver = resolver;
    }

    public String getSearchText() {
        return searchField.getText();
    }

    public void setStatusMessage(String message) {
        Runnable update = () -> statusLabel.setText(message == null || message.isBlank()
                ? " " : message);
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    public void setRecipes(List<RecipeDisplayData> recipes) {
        Runnable update = () -> applyRecipes(recipes);
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void applyRecipes(List<RecipeDisplayData> recipes) {
        String previousId = selectedId;
        emptyFrameCount = 0;
        frameSelection = false;
        this.recipes.clear();
        if (recipes != null) {
            for (RecipeDisplayData recipe : recipes) {
                if (recipe != null) {
                    this.recipes.add(recipe);
                }
            }
        }
        if (findById(selectedId) == null) {
            selectedId = null;
        }
        if (previousId != null && selectedId == null) {
            selectionRequest++;
            recipeSelectionClearedAction.run();
        }
        rebuildCards();
        renderDetails();
    }

    private void layoutColumns(JPanel left, JPanel right) {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0.35;
        add(left, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.65;
        add(right, constraints);
    }

    private JPanel searchColumn() {
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);

        JLabel title = new JLabel("Recipes");
        title.setFont(EditorialTheme.serif(Font.BOLD, 34f));
        title.setForeground(EditorialTheme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JPanel rule = new JPanel();
        rule.setBackground(EditorialTheme.GOLD);
        rule.setPreferredSize(new Dimension(42, 2));
        rule.setMaximumSize(new Dimension(42, 2));
        rule.setAlignmentX(LEFT_ALIGNMENT);

        JLabel searchLabel = new JLabel("SEARCH");
        searchLabel.setFont(EditorialTheme.sans(Font.PLAIN, 12f));
        searchLabel.setForeground(EditorialTheme.GOLD);
        searchLabel.setAlignmentX(LEFT_ALIGNMENT);

        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(rule);
        heading.add(Box.createVerticalStrut(22));
        heading.add(searchLabel);
        heading.add(Box.createVerticalStrut(8));
        heading.add(searchField);
        heading.add(Box.createVerticalStrut(10));

        JButton searchOnline = new JButton("Search TheMealDB");
        searchOnline.setName("recipes-search-online");
        styleActionButton(searchOnline);
        searchOnline.addActionListener(event -> onlineSearchAction.run());

        JButton collection = new JButton("My Collection");
        collection.setName("recipes-show-collection");
        styleActionButton(collection);
        collection.addActionListener(event -> showCollectionAction.run());

        statusLabel.setName("recipes-status");
        statusLabel.setFont(EditorialTheme.sans(Font.PLAIN, 13f));
        statusLabel.setForeground(EditorialTheme.MUTED);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        heading.add(searchOnline);
        heading.add(Box.createVerticalStrut(8));
        heading.add(collection);
        heading.add(Box.createVerticalStrut(10));
        heading.add(statusLabel);
        return heading;
    }

    private JPanel column(JPanel header, JScrollPane scroll,
                          int top, int left, int bottom, int right) {
        JPanel column = new JPanel(new BorderLayout(0, 18));
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        if (header != null) {
            column.add(header, BorderLayout.NORTH);
        }
        column.add(scroll, BorderLayout.CENTER);
        return column;
    }

    private void rebuildCards() {
        cardList.removeAll();
        cardList.setLayout(new GridBagLayout());
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<RecipeDisplayData> visible = new ArrayList<>();
        for (RecipeDisplayData recipe : recipes) {
            if (query.isEmpty()
                    || recipe.getName().toLowerCase(Locale.ROOT).contains(query)) {
                visible.add(recipe);
            }
        }

        if (emptyFrameCount > 0 && recipes.isEmpty()) {
            addFrameCards();
        }
        else if (visible.isEmpty()) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            cardList.add(listMessage(query.isEmpty()
                    ? "No recipes yet"
                    : "No matching recipes"), constraints);
        }
        else {
            int row = 0;
            for (RecipeDisplayData recipe : visible) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridy = row;
                constraints.weightx = 1;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.anchor = GridBagConstraints.NORTHWEST;
                constraints.insets = new Insets(0, 0, 12, 0);
                RecipeCard card = new RecipeCard(recipe, () -> selectRecipe(recipe.getId()));
                card.setSelected(recipe.getId().equals(selectedId));
                cardList.add(card, constraints);
                row++;
            }
            GridBagConstraints glue = new GridBagConstraints();
            glue.gridy = row;
            glue.weightx = 1;
            glue.weighty = 1;
            glue.fill = GridBagConstraints.BOTH;
            JPanel space = new JPanel();
            space.setOpaque(false);
            cardList.add(space, glue);
        }
        cardList.revalidate();
        cardList.repaint();
    }

    private void addFrameCards() {
        int row = 0;
        for (int index = 1; index <= emptyFrameCount; index++) {
            String frameId = "frame-" + index;
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridy = row;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 12, 0);
            RecipeCard card = RecipeCard.emptyFrame(frameId, () -> selectRecipe(frameId));
            card.setSelected(frameId.equals(selectedId));
            cardList.add(card, constraints);
            row++;
        }
        GridBagConstraints glue = new GridBagConstraints();
        glue.gridy = row;
        glue.weightx = 1;
        glue.weighty = 1;
        glue.fill = GridBagConstraints.BOTH;
        JPanel space = new JPanel();
        space.setOpaque(false);
        cardList.add(space, glue);
    }

    private JLabel listMessage(String text) {
        JLabel message = new JLabel(text, SwingConstants.CENTER);
        message.setName("recipes-list-message");
        message.setFont(EditorialTheme.serif(Font.PLAIN, 22f));
        message.setForeground(EditorialTheme.MUTED);
        return message;
    }

    private void selectRecipe(String recipeId) {
        selectedId = recipeId;
        frameSelection = false;
        int request = ++selectionRequest;
        recipeSelectionClearedAction.run();
        for (Component component : cardList.getComponents()) {
            if (component instanceof RecipeCard) {
                RecipeCard card = (RecipeCard) component;
                boolean selected = recipeId.equals(card.getRecipeId());
                card.setSelected(selected);
                if (selected && card.isFrame()) {
                    frameSelection = true;
                }
            }
        }
        renderDetails();
        if (frameSelection || recipeResolver == null) {
            return;
        }
        RecipeDisplayData selected = findById(recipeId);
        if (selected == null) {
            return;
        }
        recipeResolver.resolve(
                selected.getId(),
                selected.getExternalId(),
                recipe -> deliverSelection(request, recipe),
                message -> deliverFailure(request, message));
    }

    private void deliverSelection(int request, Recipe recipe) {
        Runnable update = () -> {
            if (request != selectionRequest) {
                return;
            }
            if (recipe == null || selectedId == null || !recipe.getId().equals(selectedId)) {
                recipeSelectionFailedAction.accept("The recipe could not be loaded.");
                return;
            }
            recipeSelectionAction.accept(recipe);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void deliverFailure(int request, String message) {
        Runnable update = () -> {
            if (request != selectionRequest) {
                return;
            }
            recipeSelectionFailedAction.accept(message == null || message.isBlank()
                    ? "The recipe could not be loaded."
                    : message);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        }
        else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void renderDetails() {
        imageRequest++;
        detailContent.removeAll();
        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBorder(BorderFactory.createEmptyBorder(4, 8, 24, 8));

        RecipeDisplayData selected = findById(selectedId);
        if (frameSelection) {
            detailContent.add(imageArea(null));
            detailContent.add(Box.createVerticalStrut(22));
            detailContent.add(frameBar(280, 28, EditorialTheme.TEXT));
            detailContent.add(Box.createVerticalStrut(12));
            detailContent.add(frameBar(140, 14, EditorialTheme.GOLD));
            detailContent.add(Box.createVerticalStrut(18));
            detailContent.add(horizontalRule());
            detailContent.add(Box.createVerticalStrut(18));
            detailContent.add(sectionTitle("INGREDIENTS"));
            detailContent.add(Box.createVerticalStrut(12));
            detailContent.add(frameBar(220, 12, EditorialTheme.MUTED));
            detailContent.add(Box.createVerticalStrut(22));
            detailContent.add(sectionTitle("INSTRUCTIONS"));
            detailContent.add(Box.createVerticalStrut(12));
            detailContent.add(frameBar(360, 12, EditorialTheme.MUTED));
            detailContent.add(Box.createVerticalStrut(10));
            detailContent.add(frameBar(300, 12, EditorialTheme.MUTED));
        }
        else if (selected == null) {
            detailContent.add(Box.createVerticalGlue());
            JLabel message = new JLabel("Select a recipe to explore.");
            message.setName("recipes-detail-message");
            message.setFont(EditorialTheme.serif(Font.PLAIN, 30f));
            message.setForeground(EditorialTheme.TEXT);
            message.setAlignmentX(CENTER_ALIGNMENT);
            JLabel support = new JLabel("The list is waiting for a selection.");
            support.setFont(EditorialTheme.sans(Font.PLAIN, 14f));
            support.setForeground(EditorialTheme.MUTED);
            support.setAlignmentX(CENTER_ALIGNMENT);
            detailContent.add(message);
            detailContent.add(Box.createVerticalStrut(10));
            detailContent.add(support);
            detailContent.add(Box.createVerticalGlue());
        }
        else {
            detailContent.add(imageArea(selected.getImageUrl()));
            detailContent.add(Box.createVerticalStrut(22));
            detailContent.add(detailName(selected.getName()));
            detailContent.add(Box.createVerticalStrut(8));
            detailContent.add(detailServings(selected.getServings()));
            if (selected.getAttribution() != null) {
                detailContent.add(Box.createVerticalStrut(8));
                detailContent.add(attributionLabel(selected.getAttribution()));
            }
            if (selected.isImportable()) {
                detailContent.add(Box.createVerticalStrut(14));
                detailContent.add(importButton(selected.getExternalId()));
            }
            detailContent.add(Box.createVerticalStrut(18));
            detailContent.add(horizontalRule());
            detailContent.add(Box.createVerticalStrut(18));
            detailContent.add(sectionTitle("INGREDIENTS"));
            detailContent.add(Box.createVerticalStrut(10));
            detailContent.add(linesPanel(selected.getIngredients(),
                    "recipe-detail-ingredients", "No ingredients provided."));
            detailContent.add(Box.createVerticalStrut(22));
            detailContent.add(sectionTitle("INSTRUCTIONS"));
            detailContent.add(Box.createVerticalStrut(10));
            detailContent.add(instructionPanel(selected.getInstructions()));
        }

        detailContent.revalidate();
        detailContent.repaint();
        detailScroll.getVerticalScrollBar().setValue(0);
    }

    private JLabel detailName(String name) {
        JLabel label = new JLabel(name);
        label.setName("recipe-detail-name");
        label.setFont(EditorialTheme.serif(Font.BOLD, 34f));
        label.setForeground(EditorialTheme.TEXT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel detailServings(Integer servings) {
        JLabel label = new JLabel(RecipeCard.servingsText(servings));
        label.setName("recipe-detail-servings");
        label.setFont(EditorialTheme.sans(Font.PLAIN, 15f));
        label.setForeground(EditorialTheme.GOLD);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel frameBar(int width, int height, java.awt.Color color) {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D graphics2D = (java.awt.Graphics2D) graphics.create();
                graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(getBackground());
                graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight());
                graphics2D.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBackground(color);
        bar.setPreferredSize(new Dimension(width, height));
        bar.setMaximumSize(new Dimension(width, height));
        bar.setMinimumSize(new Dimension(width, height));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        return bar;
    }

    private JPanel imageArea(String imageUrl) {
        JPanel area = new JPanel(new BorderLayout());
        area.setName("recipe-detail-image");
        area.setBackground(EditorialTheme.IMAGE_WELL);
        area.setOpaque(true);
        area.setPreferredSize(new Dimension(120, 260));
        area.setMinimumSize(new Dimension(120, 260));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EditorialTheme.GOLD, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel imageLabel = new JLabel("No image available", SwingConstants.CENTER);
        imageLabel.setName("recipe-detail-image-message");
        imageLabel.setForeground(EditorialTheme.MUTED);
        imageLabel.setFont(EditorialTheme.sans(Font.PLAIN, 14f));
        area.add(imageLabel, BorderLayout.CENTER);
        showImage(imageUrl, imageLabel, imageRequest);
        return area;
    }

    private void showImage(String imageUrl, JLabel imageLabel, int request) {
        if (imageUrl == null) {
            return;
        }
        if (isRemote(imageUrl)) {
            new SwingWorker<Image, Void>() {
                @Override
                protected Image doInBackground() {
                    return readImage(imageUrl);
                }

                @Override
                protected void done() {
                    if (request != imageRequest) {
                        return;
                    }
                    applyImage(imageLabel, readWorkerImage(this));
                }
            }.execute();
            return;
        }
        applyImage(imageLabel, readImage(imageUrl));
    }

    private static Image readWorkerImage(SwingWorker<Image, Void> worker) {
        try {
            return worker.get();
        }
        catch (Exception exception) {
            return null;
        }
    }

    private void applyImage(JLabel imageLabel, Image image) {
        if (image == null) {
            imageLabel.setIcon(null);
            imageLabel.setText("No image available");
            return;
        }
        imageLabel.setText(null);
        imageLabel.setIcon(new ImageIcon(scale(image, 720, 236)));
    }

    private static boolean isRemote(String imageUrl) {
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static Image readImage(String imageUrl) {
        try {
            if (isRemote(imageUrl)) {
                return ImageIO.read(new URL(imageUrl));
            }
            File file = localImageFile(imageUrl);
            if (file == null || !file.isFile()) {
                return null;
            }
            return ImageIO.read(file);
        }
        catch (Exception exception) {
            return null;
        }
    }

    /**
     * Add Recipe stores a local picture as a file URI.
     * A normal filesystem path is still accepted.
     */
    private static File localImageFile(String imageUrl) {
        if (imageUrl.regionMatches(true, 0, "file:", 0, 5)) {
            try {
                return new File(new URI(imageUrl));
            }
            catch (URISyntaxException exception) {
                return null;
            }
        }
        return new File(imageUrl);
    }

    private static Image scale(Image source, int maxWidth, int maxHeight) {
        int width = source.getWidth(null);
        int height = source.getHeight(null);
        if (width <= 0 || height <= 0) {
            return source;
        }
        double scale = Math.min(maxWidth / (double) width, maxHeight / (double) height);
        scale = Math.min(scale, 1.0);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage scaled = new BufferedImage(
                targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaled;
    }

    private JLabel attributionLabel(String attribution) {
        JLabel label = new JLabel(attribution);
        label.setName("recipe-detail-attribution");
        label.setFont(EditorialTheme.sans(Font.PLAIN, 13f));
        label.setForeground(EditorialTheme.MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JButton importButton(String externalId) {
        JButton button = new JButton("Import to Collection");
        button.setName("recipe-import-button");
        styleActionButton(button);
        button.addActionListener(event -> importAction.accept(externalId));
        return button;
    }

    private JLabel sectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(EditorialTheme.sans(Font.PLAIN, 12f));
        label.setForeground(EditorialTheme.GOLD);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel horizontalRule() {
        JPanel rule = new JPanel();
        rule.setBackground(EditorialTheme.GOLD);
        rule.setPreferredSize(new Dimension(1, 1));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setAlignmentX(LEFT_ALIGNMENT);
        return rule;
    }

    private JPanel linesPanel(List<String> lines, String name, String emptyText) {
        JPanel panel = new JPanel();
        panel.setName(name);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        if (lines.isEmpty()) {
            panel.add(bodyLabel(emptyText, EditorialTheme.MUTED));
            return panel;
        }
        for (String line : lines) {
            panel.add(bodyLabel(line, EditorialTheme.TEXT));
            panel.add(Box.createVerticalStrut(6));
        }
        return panel;
    }

    private JPanel instructionPanel(List<String> instructions) {
        JPanel panel = new JPanel();
        panel.setName("recipe-detail-instructions");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        if (instructions.isEmpty()) {
            panel.add(bodyLabel("No instructions provided.", EditorialTheme.MUTED));
            return panel;
        }
        int number = 1;
        for (String instruction : instructions) {
            panel.add(instructionRow(number, instruction));
            panel.add(Box.createVerticalStrut(12));
            number++;
        }
        return panel;
    }

    private JPanel instructionRow(int number, String instruction) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel index = new JLabel(String.format("%02d", number));
        index.setFont(EditorialTheme.serif(Font.BOLD, 16f));
        index.setForeground(EditorialTheme.GOLD);
        index.setVerticalAlignment(SwingConstants.TOP);
        row.add(index, BorderLayout.WEST);
        row.add(bodyLabel(instruction, EditorialTheme.TEXT), BorderLayout.CENTER);
        Dimension preferred = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return row;
    }

    private JTextArea bodyLabel(String text, java.awt.Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(EditorialTheme.sans(Font.PLAIN, 15f));
        area.setForeground(color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFocusable(false);
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    private RecipeDisplayData findById(String recipeId) {
        if (recipeId == null) {
            return null;
        }
        for (RecipeDisplayData recipe : recipes) {
            if (recipeId.equals(recipe.getId())) {
                return recipe;
            }
        }
        return null;
    }

    private static JScrollPane scrollPane(EditorialScrollContent content, String name) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setName(name);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(EditorialTheme.BACKGROUND);
        scrollPane.getViewport().setBackground(EditorialTheme.BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private static void styleSearchField(JTextField field) {
        field.setBackground(EditorialTheme.CARD);
        field.setForeground(EditorialTheme.TEXT);
        field.setCaretColor(EditorialTheme.GOLD);
        field.setFont(EditorialTheme.sans(Font.PLAIN, 15f));
        field.setSelectedTextColor(EditorialTheme.BACKGROUND);
        field.setSelectionColor(EditorialTheme.GOLD);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EditorialTheme.GOLD, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        field.setToolTipText("Filter the current list, or search TheMealDB by name");
        Dimension size = field.getPreferredSize();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
        field.setAlignmentX(LEFT_ALIGNMENT);
    }

    private static void styleActionButton(JButton button) {
        button.setBackground(EditorialTheme.CARD);
        button.setForeground(EditorialTheme.TEXT);
        button.setFont(EditorialTheme.sans(Font.PLAIN, 14f));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EditorialTheme.GOLD, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.setAlignmentX(LEFT_ALIGNMENT);
        Dimension size = button.getPreferredSize();
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
        button.setFocusPainted(false);
    }
}
