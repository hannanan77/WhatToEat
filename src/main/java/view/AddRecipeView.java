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
            new SpinnerNumberModel(1, 1, 100, 1));
    private final JSpinner quantitySpinner = new JSpinner(
            new SpinnerNumberModel(1.0, 0.25, 10000.0, 0.25));
    private final JComboBox<String> unitComboBox = new JComboBox<>(
            new String[]{"g", "kg", "mg", "ml", "L", "tsp", "tbsp", "cup", "piece"});
    private final JCheckBox unknownQuantityCheckBox = new JCheckBox("Quantity unknown");
    private final JButton saveButton = new ThemeButton("Save Recipe", true);
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
        configureSpinner(quantitySpinner, "0.##");
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
        saveButton.setEnabled(controller != null && !imageLoading);
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
            statusLabel.setText("Please choose a readable JPG or PNG file.");
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
                saveButton.setEnabled(controller != null);
                try {
                    BufferedImage image = get();
                    imagePreview.setImage(image);
                    imageUrlField.setText(file.toURI().toString());
                    imageUrlField.setCaretPosition(0);
                    imageUrlField.setToolTipText(file.getAbsolutePath());
                    statusLabel.setText("Image selected: " + file.getName());
                }
                catch (Exception exception) {
                    statusLabel.setText("Cannot read this image. Please choose another JPG or PNG.");
                    statusLabel.setForeground(UiTheme.ERROR);
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
        saveButton.setEnabled(controller != null);
    }
    private void addIngredient() {
        String ingredientName =
                ingredientNameField.getText().trim();

        if (ingredientName.isEmpty()) {
            statusLabel.setText(
                    "Please enter an ingredient name."
            );
            ingredientNameField.requestFocusInWindow();
            return;
        }

        Double quantity = null;
        String unit = "unspecified";

        if (!unknownQuantityCheckBox.isSelected()) {
            quantity = ((Number) quantitySpinner.getValue())
                    .doubleValue();

            unit = (String) unitComboBox.getSelectedItem();
        }

        ingredientTableModel.addRow(
                new Object[]{ingredientName, quantity, unit}
        );

        statusLabel.setText("Ingredient added to the form.");

        ingredientNameField.setText("");
        quantitySpinner.setValue(1.0);
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

        revalidate();
        repaint();
    }

    /**
     * Reads the form and passes its values to the controller.
     */
    private void submitRecipe() {
        if (imageLoading) {
            statusLabel.setText("Please wait for the image preview to finish loading.");
            return;
        }
        if (controller == null) {
            statusLabel.setText("Saving is not connected yet.");
            return;
        }

        // Avoid silently leaving an unfinished ingredient out.
        if (!ingredientNameField.getText().trim().isEmpty()) {
            statusLabel.setText(
                    "Click Add Ingredient or clear the ingredient name first."
            );
            return;
        }

        List<AddRecipeController.IngredientFormData> ingredients =
                new ArrayList<>();

        for (int row = 0;
             row < ingredientTableModel.getRowCount();
             row++) {

            String ingredientName =
                    (String) ingredientTableModel.getValueAt(row, 0);

            Double quantity =
                    (Double) ingredientTableModel.getValueAt(row, 1);

            String unit =
                    (String) ingredientTableModel.getValueAt(row, 2);

            ingredients.add(
                    new AddRecipeController.IngredientFormData(
                            ingredientName,
                            quantity,
                            unit
                    )
            );
        }

        Integer servings =
                ((Number) servingsSpinner.getValue()).intValue();

        controller.execute(
                nameField.getText(),
                ingredients,
                instructionsArea.getText(),
                imageUrlField.getText(),
                servings
        );
    }

    /**
     * Clears the form only after a successful save.
     */
    private void clearForm() {
        nameField.setText("");
        servingsSpinner.setValue(1);

        clearImage();

        instructionsArea.setText("");

        ingredientNameField.setText("");
        ingredientTableModel.setRowCount(0);

        quantitySpinner.setValue(1.0);
        unitComboBox.setSelectedItem("g");
        unknownQuantityCheckBox.setSelected(false);
        updateQuantityVisibility();

        nameField.requestFocusInWindow();
    }

    private static void configureSpinner(JSpinner spinner, String format) {
        UiComponents.styleSpinner(spinner, format);
    }

    private void configureUnits() {
        UiComponents.styleComboBox(unitComboBox);
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
        return UiComponents.scrollPane(content);
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
        ImagePreview() {
            setOpaque(false);
            setToolTipText("Preview of the selected recipe image");
        }
        void setImage(BufferedImage image) {
            this.image = image;
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
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, width, height, arc, arc));
                g.drawImage(image, (width - scaledWidth) / 2, (height - scaledHeight) / 2,
                        scaledWidth, scaledHeight, null);
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
}
