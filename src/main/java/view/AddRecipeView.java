package view;

import view.components.ThemeButton;
import view.components.ThemeCard;
import view.components.ThemeParagraph;
import view.components.UiComponents;

import interface_adapter.add_recipe.AddRecipeController;
import interface_adapter.add_recipe.AddRecipeViewModel;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Add Recipe form. All presentation values come from the shared UiTheme. */
public class AddRecipeView extends JPanel {
    private AddRecipeController controller;
    private final JTextField nameField = new JTextField();
    private final JTextField imageUrlField = new JTextField();
    private final JTextField ingredientNameField = new JTextField();
    private final JTextArea instructionsArea = new JTextArea(6, 30);
    private final JSpinner servingsSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JSpinner quantitySpinner = new JSpinner(
            new SpinnerNumberModel(1.0, Double.MIN_VALUE, null, 0.25));
    private final JComboBox<String> unitComboBox = new JComboBox<>(
            new String[]{"g", "kg", "mg", "ml", "L", "tsp", "tbsp", "cup", "piece", "slice", "clove", "can", "bunch", "pinch"});
    private final JCheckBox unknownQuantityCheckBox = new ThemeCheckBox("Quantity unknown");
    private static final String SAVE_RECIPE_LABEL = "Save Recipe to My Collection";

    private final JButton saveButton = new ThemeButton(SAVE_RECIPE_LABEL, true);
    private final JTextArea statusLabel = new JTextArea("Fill in your recipe details.");
    private final ImagePreview imagePreview = new ImagePreview();
    private final DefaultTableModel ingredientTableModel = new DefaultTableModel(
            new Object[]{"Ingredient", "Quantity", "Unit"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ingredientTable = new JTable(ingredientTableModel);
    private int imageRequest;
    private boolean imageLoading;
    private boolean saving;
    private JScrollPane ingredientsScroll;
    private JScrollPane instructionsScroll;
    private final java.util.Map<JComponent, javax.swing.border.Border> invalidBorders =
            new java.util.LinkedHashMap<>();
    private final java.util.Map<Component, Boolean> enabledBeforeSave = new java.util.IdentityHashMap<>();

    public AddRecipeView() {
        this(new AddRecipeViewModel());
    }

    public AddRecipeView(AddRecipeViewModel viewModel) {
        super(new BorderLayout(0, UiTheme.SPACE_XL));
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING,
                UiTheme.PAGE_PADDING, UiTheme.PAGE_PADDING));
        styleField(nameField);
        styleField(imageUrlField);
        styleField(ingredientNameField);
        imageUrlField.setEditable(false);
        configureSpinner(servingsSpinner, "0");
        configureSpinner(quantitySpinner, "0.############");
        enableTypedSpinner(servingsSpinner, true);
        enableTypedSpinner(quantitySpinner, false);
        configureUnits();
        unknownQuantityCheckBox.setOpaque(false);
        unknownQuantityCheckBox.setFont(UiTheme.BODY_FONT);
        unknownQuantityCheckBox.setForeground(UiTheme.TEXT_PRIMARY);
        unknownQuantityCheckBox.addActionListener(event -> updateQuantityVisibility());

        JPanel header = panel(new BorderLayout(0, UiTheme.SPACE_SM));
        JLabel title = new JLabel("Add My Own Recipe");
        title.setFont(UiTheme.HEADING_FONT);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.NORTH);
        ThemeParagraph subtitle = text("Save your favourite homemade recipes to your collection.");
        header.add(subtitle, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        FormContent form = new FormContent();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(0, 0, 0, UiTheme.SPACE_SM));
        form.add(createDetailsPanel());
        form.add(Box.createVerticalStrut(UiTheme.SPACE_XL));
        form.add(createIngredientsPanel());
        form.add(Box.createVerticalStrut(UiTheme.SPACE_XL));
        form.add(createInstructionsPanel());
        JScrollPane scroll = scrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = panel(new BorderLayout(UiTheme.SPACE_LG, 0));
        styleStatus();
        footer.add(statusLabel, BorderLayout.CENTER);
        JPanel actions = panel(new BorderLayout());
        actions.add(saveButton, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        saveButton.setEnabled(false);
        installValidationListeners();
        saveButton.addActionListener(event -> submitRecipe());

        viewModel.addPropertyChangeListener(event -> {
            if (!"result".equals(event.getPropertyName())) {
                return;
            }
            String message = viewModel.getMessage();
            boolean successful = viewModel.isSuccessful();
            Runnable update = () -> {
                statusLabel.setText(message);
                statusLabel.setToolTipText(message);
                statusLabel.setForeground(successful ? UiTheme.SECONDARY : UiTheme.ERROR);
                if (successful) {
                    clearForm();
                }
                else {
                    // The output boundary supplies a general message, not a field identifier.
                    // Local validation marks known fields; storage failures use this dialog.
                    showErrorDialog("Recipe not saved", message);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                update.run();
            }
            else {
                SwingUtilities.invokeLater(update);
            }
        });
    }

    public void setController(AddRecipeController controller) {
        this.controller = controller;
        saveButton.setEnabled(controller != null && !imageLoading && !saving);
    }

    /** Left: recipe information. Right: a proportionally scaled photo. */
    private JPanel createDetailsPanel() {
        JPanel fields = stack();
        fields.add(labelled("Recipe name", nameField));
        fields.add(Box.createVerticalStrut(UiTheme.SPACE_LG));
        JPanel servingRow = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        servingRow.add(servingsSpinner);
        fields.add(labelled("Servings", servingRow));
        fields.add(Box.createVerticalStrut(UiTheme.SPACE_LG));
        fields.add(labelled("Image (optional)", imageUrlField));
        fields.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        // Two columns keep both actions visible even in a smaller window.
        JPanel buttons = panel(new GridLayout(1, 2, UiTheme.SPACE_SM, 0));
        JButton choose = new ThemeButton("Choose Image", false);
        JButton remove = new ThemeButton("Remove Image", false);
        choose.addActionListener(event -> chooseImage());
        remove.addActionListener(event -> {
            clearImage();
            statusLabel.setText("Image removed from the form.");
        });
        buttons.add(choose);
        buttons.add(remove);
        fields.add(buttons);

        JPanel content = panel(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.62;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, UiTheme.SPACE_XL);
        int fieldsHeight = fields.getPreferredSize().height;
        fields.setPreferredSize(new Dimension(390, fieldsHeight));
        fields.setMinimumSize(new Dimension(240, fieldsHeight));
        content.add(fields, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.38;
        right.fill = GridBagConstraints.BOTH;
        imagePreview.setPreferredSize(new Dimension(260, 270));
        imagePreview.setMinimumSize(new Dimension(140, 200));
        content.add(imagePreview, right);
        return card("Recipe details", content);
    }

    private JPanel createIngredientsPanel() {
        JPanel content = stack();
        content.add(labelled("Ingredient name", ingredientNameField));
        content.add(Box.createVerticalStrut(UiTheme.SPACE_LG));
        JPanel amounts = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        amounts.add(quantitySpinner);
        amounts.add(Box.createHorizontalStrut(UiTheme.SPACE_SM));
        amounts.add(unitComboBox);
        content.add(labelled("Quantity / unit", amounts));
        content.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        JPanel unknownRow = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        unknownRow.add(unknownQuantityCheckBox);
        content.add(unknownRow);
        JButton add = new ThemeButton("Add Ingredient", false);
        add.addActionListener(event -> addIngredient());
        content.add(rightAligned(add));
        content.add(Box.createVerticalStrut(UiTheme.SPACE_LG));

        ingredientTable.setFont(UiTheme.BODY_FONT);
        ingredientTable.setForeground(UiTheme.TEXT_PRIMARY);
        ingredientTable.setBackground(UiTheme.SURFACE);
        ingredientTable.setRowHeight(Math.max(UiTheme.TABLE_ROW_MIN_HEIGHT,
                ingredientTable.getFontMetrics(ingredientTable.getFont()).getHeight()
                        + UiTheme.SPACE_LG));
        ingredientTable.setSelectionBackground(UiTheme.MINT);
        ingredientTable.setSelectionForeground(UiTheme.TEXT_PRIMARY);
        ingredientTable.setGridColor(UiTheme.BORDER);
        ingredientTable.setShowVerticalLines(false);
        ingredientTable.setFillsViewportHeight(true);
        ingredientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ingredientTable.getTableHeader().setReorderingAllowed(false);
        ingredientTable.getTableHeader().setFont(UiTheme.LABEL_FONT);
        ingredientTable.getTableHeader().setBackground(UiTheme.MINT);
        ingredientTable.getTableHeader().setForeground(UiTheme.SECONDARY);
        ingredientTable.getTableHeader().setPreferredSize(new Dimension(100, Math.max(UiTheme.TABLE_HEADER_MIN_HEIGHT,
                ingredientTable.getTableHeader().getFontMetrics(UiTheme.LABEL_FONT).getHeight()
                        + UiTheme.SPACE_LG)));
        ingredientTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean selected, boolean focus, int row, int column) {
                int modelRow = table.convertRowIndexToModel(row);
                int modelColumn = table.convertColumnIndexToModel(column);
                Object display = modelColumn == 2
                        && ingredientTableModel.getValueAt(modelRow, 1) == null ? "" : value;
                super.getTableCellRendererComponent(table, display, selected, focus, row, column);
                setBorder(new EmptyBorder(0, UiTheme.SPACE_MD, 0, UiTheme.SPACE_MD));
                return this;
            }
        });
        JScrollPane tableScroll = scrollPane(ingredientTable);
        ingredientsScroll = tableScroll;
        tableScroll.setPreferredSize(new Dimension(400, 170));
        content.add(tableScroll);
        content.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        JButton remove = new ThemeButton("Remove Selected", false);
        remove.addActionListener(event -> removeSelectedIngredient());
        content.add(rightAligned(remove));
        return card("Ingredients", content);
    }

    private JPanel createInstructionsPanel() {
        JPanel body = panel(new BorderLayout(0, UiTheme.SPACE_MD));
        body.add(text("Enter one cooking step per line."), BorderLayout.NORTH);
        instructionsArea.setFont(UiTheme.BODY_FONT);
        instructionsArea.setForeground(UiTheme.TEXT_PRIMARY);
        instructionsArea.setBackground(UiTheme.SURFACE);
        instructionsArea.setCaretColor(UiTheme.SECONDARY);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setBorder(new EmptyBorder(UiTheme.INPUT_PADDING_Y, UiTheme.INPUT_PADDING_X,
                UiTheme.INPUT_PADDING_Y, UiTheme.INPUT_PADDING_X));
        JScrollPane scroll = scrollPane(instructionsArea);
        instructionsScroll = scroll;
        scroll.setPreferredSize(new Dimension(400, 180));
        body.add(scroll, BorderLayout.CENTER);
        return card("Instructions", body);
    }

    /** Decode outside the Swing event thread so selecting a photo does not freeze the form. */
    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a recipe image");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.isFile() || !file.canRead() || !filter.accept(file)) {
            showErrorDialog("Image not selected", "Please choose a readable JPG or PNG file.");
            return;
        }
        int request = ++imageRequest;
        imageLoading = true;
        saveButton.setEnabled(false);
        statusLabel.setText("Loading image preview...");
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws IOException {
                return readPreview(file);
            }

            @Override
            protected void done() {
                // Ignore an older selection if the user chose again or removed it.
                if (request != imageRequest) {
                    return;
                }
                imageLoading = false;
                saveButton.setEnabled(controller != null && !imageLoading && !saving);
                try {
                    BufferedImage image = get();
                    imagePreview.setImage(image);
                    imageUrlField.setText(file.toURI().toString());
                    imageUrlField.setCaretPosition(0);
                    imageUrlField.setToolTipText(file.getAbsolutePath());
                    statusLabel.setText("Image selected: " + file.getName());
                }
                catch (Exception exception) {
                    showErrorDialog("Image not selected",
                            "Cannot read this image. Please choose another JPG or PNG.");
                }
            }
        }.execute();
    }

    /** Subsample large photos: only a preview is kept in memory, never rewriting the original. */
    private static BufferedImage readPreview(File file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                throw new IOException("Cannot open image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int largest = Math.max(reader.getWidth(0), reader.getHeight(0));
                int sample = Math.max(1, (int) Math.ceil(largest / 1200.0));
                ImageReadParam options = reader.getDefaultReadParam();
                options.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage image = reader.read(0, options);
                if (image == null) {
                    throw new IOException("Cannot decode image.");
                }
                return image;
            }
            finally {
                reader.dispose();
            }
        }
    }

    private void clearImage() {
        imageRequest++;
        imageLoading = false;
        imageUrlField.setText("");
        imageUrlField.setToolTipText(null);
        imagePreview.setImage(null);
        saveButton.setEnabled(controller != null && !imageLoading && !saving);
    }
    private void addIngredient() {
        clearError(ingredientNameField);
        clearError(quantitySpinner);
        clearError(unitComboBox);
        List<String> errors = new ArrayList<>();
        JComponent first = null;
        String name = ingredientNameField.getText().trim();
        if (name.isEmpty()) {
            markError(ingredientNameField, "Enter an ingredient name.");
            errors.add("Enter an ingredient name.");
            first = ingredientNameField;
        }
        Double quantity = null;
        String unit = "unspecified";
        if (!unknownQuantityCheckBox.isSelected()) {
            try {
                quantitySpinner.commitEdit();
                quantity = ((Number) quantitySpinner.getValue()).doubleValue();
                if (!Double.isFinite(quantity) || quantity <= 0) {
                    throw new java.text.ParseException("Invalid quantity", 0);
                }
            }
            catch (java.text.ParseException exception) {
                markError(quantitySpinner, "Enter a number greater than 0, e.g. 1 or 0.5.");
                errors.add("Quantity: enter a number greater than 0, e.g. 1 or 0.5.");
                if (first == null) { first = quantitySpinner; }
            }
            Object value = unitComboBox.getEditor().getItem();
            unit = value == null ? "" : value.toString().trim();
            if (unit.isEmpty()) {
                markError(unitComboBox, "Choose or type a unit, e.g. piece or 个.");
                errors.add("Choose or type a unit, e.g. piece or 个.");
                if (first == null) { first = unitComboBox; }
            }
        }
        if (!errors.isEmpty()) {
            showValidationErrors(first, errors);
            return;
        }
        ingredientTableModel.addRow(new Object[]{name, quantity, unit});
        statusLabel.setForeground(UiTheme.SECONDARY);
        statusLabel.setText("Ingredient added to the form.");
        ingredientNameField.setText("");
        resetSpinner(quantitySpinner, 1.0);
        unknownQuantityCheckBox.setSelected(false);
        updateQuantityVisibility();
        ingredientNameField.requestFocusInWindow();
    }

    /**
     * Removes the selected ingredient from the form.
     */
    private void removeSelectedIngredient() {
        int selectedRow = ingredientTable.getSelectedRow();

        if (selectedRow == -1) {
            statusLabel.setText(
                    "Please select an ingredient to remove."
            );
            return;
        }

        int modelRow =
                ingredientTable.convertRowIndexToModel(selectedRow);

        ingredientTableModel.removeRow(modelRow);
        statusLabel.setText("Ingredient removed from the form.");
    }

    /**
     * Hides quantity and unit controls when the amount is unknown.
     */
    private void updateQuantityVisibility() {
        boolean known = !unknownQuantityCheckBox.isSelected();

        quantitySpinner.setVisible(known);
        unitComboBox.setVisible(known);
        if (!known) {
            clearError(quantitySpinner);
            clearError(unitComboBox);
        }

        revalidate();
        repaint();
    }

    /**
     * Reads the form and passes its values to the controller.
     */
    private void submitRecipe() {
        if (saving) { return; }
        if (imageLoading) {
            showErrorDialog("Please wait", "Please wait for the image preview to finish loading.");
            return;
        }
        if (controller == null) {
            showErrorDialog("Saving unavailable", "Saving is not connected yet.");
            return;
        }
        clearAllErrors();
        List<String> errors = new ArrayList<>();
        JComponent first = null;
        if (nameField.getText().trim().isEmpty()) {
            markError(nameField, "Enter a recipe name.");
            errors.add("Enter a recipe name.");
            first = nameField;
        }
        try {
            servingsSpinner.commitEdit();
        }
        catch (java.text.ParseException exception) {
            markError(servingsSpinner, "Servings must be a whole number of at least 1.");
            errors.add("Servings must be a whole number of at least 1.");
            if (first == null) { first = servingsSpinner; }
        }
        if (!ingredientNameField.getText().trim().isEmpty()) {
            markError(ingredientNameField, "Click Add Ingredient to include this ingredient.");
            errors.add("Click Add Ingredient, or clear the unfinished ingredient name.");
            if (first == null) { first = ingredientNameField; }
        }
        if (ingredientTableModel.getRowCount() == 0) {
            markError(ingredientsScroll, "Add at least one ingredient.");
            errors.add("Add at least one ingredient.");
            if (first == null) { first = ingredientNameField; }
        }
        if (instructionsArea.getText().isBlank()) {
            markError(instructionsScroll, "Enter at least one cooking step.");
            errors.add("Enter at least one cooking step.");
            if (first == null) { first = instructionsArea; }
        }
        String imageLocation = imageUrlField.getText().trim();
        if (!imageLocation.isEmpty()) {
            try {
                File file = new File(java.net.URI.create(imageLocation));
                if (!file.isFile() || !file.canRead()) { throw new IllegalArgumentException(); }
            }
            catch (IllegalArgumentException exception) {
                markError(imageUrlField, "The selected picture is missing. Choose it again or remove it.");
                errors.add("The selected picture is missing. Choose it again or remove it.");
                if (first == null) { first = imageUrlField; }
            }
        }
        if (!errors.isEmpty()) {
            showValidationErrors(first, errors);
            return;
        }
        List<AddRecipeController.IngredientFormData> ingredients = new ArrayList<>();
        for (int row = 0; row < ingredientTableModel.getRowCount(); row++) {
            ingredients.add(new AddRecipeController.IngredientFormData(
                    (String) ingredientTableModel.getValueAt(row, 0),
                    (Double) ingredientTableModel.getValueAt(row, 1),
                    (String) ingredientTableModel.getValueAt(row, 2)));
        }
        // Snapshot every Swing value on the EDT before running file I/O in the worker.
        String name = nameField.getText();
        String instructions = instructionsArea.getText();
        Integer servings = ((Number) servingsSpinner.getValue()).intValue();
        AddRecipeController activeController = controller;
        setSaving(true);
        statusLabel.setForeground(UiTheme.SECONDARY);
        statusLabel.setText("Saving recipe...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                activeController.execute(name, ingredients, instructions, imageLocation, servings);
                return null;
            }
            @Override
            protected void done() {
                setSaving(false);
                try {
                    get();
                }
                catch (Exception exception) {
                    showErrorDialog("Recipe not saved",
                            "The save operation did not finish normally. Check My Collection before retrying.");
                }
            }
        }.execute();
    }

    /**
     * Clears the form only after a successful save.
     */
    private void clearForm() {
        clearAllErrors();
        nameField.setText("");
        resetSpinner(servingsSpinner, 1);

        clearImage();

        instructionsArea.setText("");

        ingredientNameField.setText("");
        ingredientTableModel.setRowCount(0);

        resetSpinner(quantitySpinner, 1.0);
        unitComboBox.setSelectedItem("g");
        unknownQuantityCheckBox.setSelected(false);
        updateQuantityVisibility();

        nameField.requestFocusInWindow();
    }

    private static void resetSpinner(JSpinner spinner, Number value) {
        spinner.setValue(value);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setValue(value);
    }

    private void markError(JComponent component, String message) {
        if (!invalidBorders.containsKey(component)) {
            invalidBorders.put(component, component.getBorder());
        }
        component.putClientProperty("validationError", Boolean.TRUE);
        component.setToolTipText(message);
        // Preserve all original insets to avoid moving the layout when showing an error.
        javax.swing.border.Border original = invalidBorders.get(component);
        component.setBorder(new javax.swing.border.AbstractBorder() {
            @Override
            public Insets getBorderInsets(Component c) {
                return original == null ? new Insets(0, 0, 0, 0) : original.getBorderInsets(c);
            }
            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                Insets current = getBorderInsets(c);
                insets.set(current.top, current.left, current.bottom, current.right);
                return insets;
            }
            @Override
            public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
                Graphics2D g = smooth(graphics);
                g.setColor(UiTheme.ERROR);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x + 1, y + 1, w - 3, h - 3,
                        UiTheme.CONTROL_RADIUS * 2, UiTheme.CONTROL_RADIUS * 2);
                g.dispose();
            }
        });
        component.repaint();
    }

    private void clearError(JComponent component) {
        if (invalidBorders.containsKey(component)) {
            component.setBorder(invalidBorders.remove(component));
            component.setToolTipText(null);
            component.putClientProperty("validationError", Boolean.FALSE);
            component.repaint();
        }
    }

    private void clearAllErrors() {
        for (JComponent component : new ArrayList<>(invalidBorders.keySet())) {
            clearError(component);
        }
    }

    private void showValidationErrors(JComponent first, List<String> errors) {
        showErrorDialog("Please check your recipe", String.join("\n", errors));
        if (first != null) {
            first.scrollRectToVisible(new Rectangle(0, 0, first.getWidth(), first.getHeight()));
            if (first instanceof JSpinner) {
                ((JSpinner.DefaultEditor) ((JSpinner) first).getEditor()).getTextField().requestFocusInWindow();
            }
            else if (first instanceof JComboBox) {
                ((JComboBox<?>) first).getEditor().getEditorComponent().requestFocusInWindow();
            }
            else { first.requestFocusInWindow(); }
        }
    }

    /** Protected so a UI test can record feedback without opening a modal dialog. */
    protected void showErrorDialog(String title, String message) {
        statusLabel.setForeground(UiTheme.ERROR);
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
        ThemeParagraph body = new ThemeParagraph(message);
        body.setForeground(UiTheme.TEXT_PRIMARY);
        body.setSize(new Dimension(400, Short.MAX_VALUE));
        body.setPreferredSize(new Dimension(400, body.getPreferredSize().height));
        JOptionPane.showMessageDialog(this, body, title, JOptionPane.ERROR_MESSAGE);
    }

    private void installValidationListeners() {
        clearWhenEdited(nameField, nameField);
        clearWhenEdited(ingredientNameField, ingredientNameField);
        clearWhenEdited(imageUrlField, imageUrlField);
        clearWhenEdited(instructionsArea, instructionsScroll);
        clearWhenEdited(((JSpinner.DefaultEditor) servingsSpinner.getEditor()).getTextField(), servingsSpinner);
        clearWhenEdited(((JSpinner.DefaultEditor) quantitySpinner.getEditor()).getTextField(), quantitySpinner);
        clearWhenEdited((JTextField) unitComboBox.getEditor().getEditorComponent(), unitComboBox);
        ingredientTableModel.addTableModelListener(event -> clearError(ingredientsScroll));
    }

    private void clearWhenEdited(javax.swing.text.JTextComponent input, JComponent target) {
        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearError(target); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearError(target); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { clearError(target); }
        });
    }

    private void setSaving(boolean value) {
        saving = value;
        if (value) {
            rememberAndDisableInputs(this);
            saveButton.setText("Saving...");
        }
        else {
            for (java.util.Map.Entry<Component, Boolean> entry : enabledBeforeSave.entrySet()) {
                entry.getKey().setEnabled(entry.getValue());
            }
            enabledBeforeSave.clear();
            saveButton.setText(SAVE_RECIPE_LABEL);
        }
        saveButton.setEnabled(controller != null && !saving && !imageLoading);
    }

    private void rememberAndDisableInputs(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton || child instanceof javax.swing.text.JTextComponent
                    || child instanceof JComboBox || child instanceof JSpinner || child instanceof JTable) {
                enabledBeforeSave.put(child, child.isEnabled());
            }
            // Capture descendants before composite controls propagate their disabled state.
            if (child instanceof Container) { rememberAndDisableInputs((Container) child); }
            if (enabledBeforeSave.containsKey(child)) { child.setEnabled(false); }
        }
    }

    private static void configureSpinner(JSpinner spinner, String format) {
        UiComponents.styleSpinner(spinner, format);
    }

    private void configureUnits() {
        UiComponents.styleComboBox(unitComboBox);
        enableEditableComboBox(unitComboBox);
    }

    private void styleStatus() {
        statusLabel.setFont(UiTheme.BODY_FONT);
        statusLabel.setForeground(UiTheme.SECONDARY);
        statusLabel.setOpaque(false);
        statusLabel.setEditable(false);
        statusLabel.setFocusable(false);
        statusLabel.setLineWrap(true);
        statusLabel.setWrapStyleWord(true);
        statusLabel.setRows(2);
    }

    private static void styleField(JTextField field) {
        UiComponents.styleTextField(field);
    }

    private static ThemeParagraph text(String value) {
        return new ThemeParagraph(value);
    }

    private static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel labelled(String title, JComponent input) {
        JPanel row = panel(new BorderLayout(0, UiTheme.SPACE_SM));
        JLabel label = new JLabel(title);
        label.setFont(UiTheme.LABEL_FONT);
        label.setForeground(UiTheme.TEXT_PRIMARY);
        label.setLabelFor(input);
        row.add(label, BorderLayout.NORTH);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    private static JPanel rightAligned(JButton button) {
        JPanel row = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.add(button);
        return row;
    }

    private static JPanel stack() {
        return new JPanel(new GridBagLayout()) {
            private int row;
            {
                setOpaque(false);
            }
            @Override
            protected void addImpl(Component component, Object constraints, int index) {
                GridBagConstraints position = new GridBagConstraints();
                position.gridx = 0;
                position.gridy = row++;
                position.weightx = 1;
                position.fill = GridBagConstraints.HORIZONTAL;
                position.anchor = GridBagConstraints.NORTHWEST;
                super.addImpl(component, position, index);
            }
        };
    }

    private static JPanel card(String title, JPanel content) {
        return new ThemeCard(title, content);
    }

    private static JScrollPane scrollPane(Component content) {
        JScrollPane scroll = UiComponents.scrollPane(content);
        SmoothScrolling.install(scroll);
        return scroll;
    }

    private static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    private static class FormContent extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(800, 600);
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return UiTheme.SCROLL_INCREMENT;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(UiTheme.SCROLL_INCREMENT, visible.height - UiTheme.SCROLL_INCREMENT);
        }
    }

    /** Paints the selected photo with its aspect ratio preserved, inside a rounded frame. */
    private static class ImagePreview extends JPanel {
        private BufferedImage image;
        private BufferedImage scaled;
        ImagePreview() {
            setOpaque(false);
            setToolTipText("Preview of the selected recipe image");
        }
        void setImage(BufferedImage image) {
            this.image = image;
            this.scaled = null;
            repaint();
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = smooth(graphics);
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            int arc = UiTheme.CARD_RADIUS * 2;
            g.setColor(UiTheme.MINT);
            g.fillRoundRect(0, 0, width, height, arc, arc);
            if (image != null) {
                double ratio = Math.min(Math.max(1, width - 24) / (double) image.getWidth(),
                        Math.max(1, height - 24) / (double) image.getHeight());
                int scaledWidth = Math.max(1, (int) Math.round(image.getWidth() * ratio));
                int scaledHeight = Math.max(1, (int) Math.round(image.getHeight() * ratio));
                if (scaled == null || scaled.getWidth() != scaledWidth || scaled.getHeight() != scaledHeight) {
                    scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D resize = scaled.createGraphics();
                    resize.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    resize.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                    resize.dispose();
                }
                g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, width, height, arc, arc));
                g.drawImage(scaled, (width - scaledWidth) / 2, (height - scaledHeight) / 2, null);
            }
            else {
                g.setColor(UiTheme.SECONDARY);
                g.setFont(UiTheme.SECTION_HEADING_FONT);
                String title = "Image preview";
                g.drawString(title, (width - g.getFontMetrics().stringWidth(title)) / 2, height / 2);
                g.setFont(UiTheme.BODY_FONT);
                g.setColor(UiTheme.TEXT_SECONDARY);
                String hint = "Choose a JPG or PNG";
                g.drawString(hint, (width - g.getFontMetrics().stringWidth(hint)) / 2, height / 2 + 25);
            }
            g.setColor(UiTheme.BORDER);
            g.drawRoundRect(0, 0, width, height, arc, arc);
            g.dispose();
        }
    }

    // Page-local helpers: no changes to shared UiTheme or components are required.
    /** Opt-in typed numbers. Commit the editor before reading spinner.getValue(). */
    private static void enableTypedSpinner(JSpinner spinner, boolean integer) {
        JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        java.text.DecimalFormat format = new java.text.DecimalFormat(integer ? "0" : "0.############");
        format.setGroupingUsed(false);
        format.setDecimalFormatSymbols(java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT));
        javax.swing.text.NumberFormatter formatter = new javax.swing.text.NumberFormatter(format) {
            @Override
            public Object stringToValue(String text) throws java.text.ParseException {
                try {
                    String cleaned = text.trim();
                    // Reject partial parses, grouping separators, NaN, and fractional servings.
                    if (!cleaned.matches(integer ? "[+]?[0-9]+" : "[+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
                        throw new IllegalArgumentException();
                    }
                    java.math.BigDecimal value = new java.math.BigDecimal(cleaned);
                    Number number;
                    if (integer) {
                        number = Integer.valueOf(value.intValueExact());
                    }
                    else {
                        double decimal = value.doubleValue();
                        if (!Double.isFinite(decimal) || decimal <= 0) {
                            throw new IllegalArgumentException();
                        }
                        number = Double.valueOf(decimal);
                    }
                    SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
                    if (model.getMinimum() instanceof Number && number.doubleValue()
                            < ((Number) model.getMinimum()).doubleValue()
                            || model.getMaximum() instanceof Number && number.doubleValue()
                            > ((Number) model.getMaximum()).doubleValue()) {
                        throw new IllegalArgumentException();
                    }
                    return number;
                }
                catch (RuntimeException exception) {
                    throw new java.text.ParseException("Enter a valid positive number.", 0);
                }
            }
        };
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(false);
        field.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(formatter));
        field.setValue(spinner.getValue());
        field.setEditable(true);
        field.setFocusLostBehavior(JFormattedTextField.PERSIST);
        // Scrolling the form should not silently alter an ingredient's amount.
        for (java.awt.event.MouseWheelListener listener : spinner.getMouseWheelListeners()) {
            spinner.removeMouseWheelListener(listener);
        }
        spinner.putClientProperty("whattoeat.wheelInstalled", Boolean.FALSE);
    }

    /** Keep editable units opt-in so other pages' fixed dropdowns retain their behavior. */
    private static void enableEditableComboBox(JComboBox<String> combo) {
        combo.setEditable(true);
        JTextField field = (JTextField) combo.getEditor().getEditorComponent();
        field.setFont(UiTheme.BODY_FONT);
        field.setForeground(UiTheme.TEXT_PRIMARY);
        field.setBackground(UiTheme.SURFACE);
        field.setBorder(new EmptyBorder(0, UiTheme.SPACE_SM, 0, UiTheme.SPACE_SM));
        field.setCaretColor(UiTheme.SECONDARY);
        field.setToolTipText("Choose a unit or type your own, e.g. piece or 个");
        combo.setPreferredSize(new Dimension(160, UiTheme.CONTROL_MIN_HEIGHT));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { combo.repaint(); }
            public void focusLost(java.awt.event.FocusEvent e) { combo.repaint(); }
        });
    }


    private static final class ThemeCheckBox extends JCheckBox {
        public ThemeCheckBox(String text) {
            super(text);
            setOpaque(false);
            setFont(UiTheme.BODY_FONT);
            setForeground(UiTheme.TEXT_PRIMARY);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setIconTextGap(UiTheme.SPACE_SM);
            setBorder(new EmptyBorder(UiTheme.SPACE_SM, 2, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Icon icon = new CheckIcon();
            setIcon(icon);
            setSelectedIcon(icon);
            setDisabledIcon(icon);
            setDisabledSelectedIcon(icon);
            setRolloverIcon(icon);
            setRolloverSelectedIcon(icon);
            setPressedIcon(icon);
        }

        private static class CheckIcon implements Icon {
            public int getIconWidth() { return 24; }
            public int getIconHeight() { return 24; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                AbstractButton button = (AbstractButton) component;
                Graphics2D g = UiComponents.smooth(graphics);
                g.translate(x, y);
                g.setColor(!button.isEnabled() ? UiTheme.DISABLED_BACKGROUND
                        : button.isSelected() ? UiTheme.SECONDARY : UiTheme.MINT);
                g.fillRoundRect(2, 2, 20, 20, 8, 8);
                g.setColor(button.isEnabled() ? UiTheme.SECONDARY : UiTheme.DISABLED_TEXT);
                g.setStroke(new BasicStroke(button.getModel().isRollover() ? 2f : 1f));
                g.drawRoundRect(2, 2, 20, 20, 8, 8);
                if (button.isSelected()) {
                    g.setColor(button.isEnabled() ? UiTheme.SURFACE : UiTheme.DISABLED_TEXT);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(7, 12, 10, 15);
                    g.drawLine(10, 15, 17, 8);
                }
                if (button.isFocusOwner()) {
                    g.setColor(UiTheme.FOCUS_RING);
                    g.drawRoundRect(0, 0, 23, 23, 10, 10);
                }
                g.dispose();
            }
        }
    }

    private static final class SmoothScrolling implements MouseWheelListener {
        private final JScrollPane pane;
        private final Timer timer;
        private double position;
        private double target;
        private boolean adjusting;

        private SmoothScrolling(JScrollPane pane) {
            this.pane = pane;
            timer = new Timer(15, event -> tick());
            timer.setCoalesce(true);
            pane.setWheelScrollingEnabled(false);
            // Aqua can consume wheel events even with Swing wheel scrolling disabled.
            for (MouseWheelListener listener : pane.getMouseWheelListeners()) {
                pane.removeMouseWheelListener(listener);
            }
            pane.addMouseWheelListener(this);
            pane.getVerticalScrollBar().addAdjustmentListener(event -> {
                if (!adjusting) {
                    stop();
                }
            });
            pane.addHierarchyListener(event -> {
                if (!pane.isShowing()) {
                    stop();
                }
            });
        }

        public static void install(JScrollPane pane) {
            if (pane.getClientProperty("whattoeat.smoothScroll") == null) {
                pane.putClientProperty("whattoeat.smoothScroll", new SmoothScrolling(pane));
            }
        }

        private int maximum() {
            JScrollBar bar = pane.getVerticalScrollBar();
            return Math.max(bar.getMinimum(), bar.getMaximum() - bar.getVisibleAmount());
        }

        private void stop() {
            timer.stop();
            position = pane.getVerticalScrollBar().getValue();
            target = position;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            if (event.isConsumed() || event.isShiftDown()) {
                return;
            }
            if (!timer.isRunning()) {
                position = pane.getVerticalScrollBar().getValue();
                target = position;
            }
            double rotation = event.getPreciseWheelRotation();
            int minimum = pane.getVerticalScrollBar().getMinimum();
            int maximum = maximum();
            if ((rotation < 0 && target <= minimum) || (rotation > 0 && target >= maximum)) {
                JScrollPane parent = (JScrollPane) SwingUtilities.getAncestorOfClass(
                        JScrollPane.class, pane.getParent());
                if (parent != null) {
                    MouseWheelEvent forwarded = new MouseWheelEvent(parent, event.getID(), event.getWhen(),
                            event.getModifiersEx(), 0, 0, event.getXOnScreen(), event.getYOnScreen(),
                            event.getClickCount(), event.isPopupTrigger(), event.getScrollType(),
                            event.getScrollAmount(), event.getWheelRotation(), rotation);
                    parent.dispatchEvent(forwarded);
                }
                event.consume();
                return;
            }
            double distance = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                    ? pane.getViewport().getHeight()
                    : UiTheme.SCROLL_INCREMENT * Math.max(1, event.getScrollAmount());
            target = Math.max(minimum, Math.min(maximum, target + rotation * distance));
            timer.start();
            event.consume();
        }

        private void tick() {
            if (!pane.isShowing()) {
                stop();
                return;
            }
            target = Math.max(pane.getVerticalScrollBar().getMinimum(), Math.min(maximum(), target));
            position += (target - position) * 0.35;
            boolean finished = Math.abs(target - position) < 0.8;
            if (finished) {
                position = target;
            }
            adjusting = true;
            try {
                pane.getVerticalScrollBar().setValue((int) Math.round(position));
            }
            finally {
                adjusting = false;
            }
            if (finished) {
                timer.stop();
            }
        }
    }
}
