package view;

import interface_adapter.add_recipe.AddRecipeController;
import interface_adapter.add_recipe.AddRecipeViewModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The form for adding a personal recipe.
 */
public class AddRecipeView extends JPanel {

    private AddRecipeController controller;

    private final JTextField nameField = new JTextField(25);

    private final JSpinner servingsSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 100, 1)
    );

    private final JTextField imageUrlField = new JTextField(25);

    private final JTextArea instructionsArea = new JTextArea(6, 30);

    private final JTextField ingredientNameField = new JTextField(18);

    private final JSpinner quantitySpinner = new JSpinner(
            new SpinnerNumberModel(1.0, 0.25, 10000.0, 0.25)
    );

    private final JComboBox<String> unitComboBox = new JComboBox<>(
            new String[]{
                    "g", "kg", "mg", "ml", "L",
                    "tsp", "tbsp", "cup", "piece"
            }
    );

    private final JCheckBox unknownQuantityCheckBox =
            new JCheckBox("Quantity unknown");

    private final JButton addIngredientButton =
            new JButton("Add Ingredient");

    private final JButton removeIngredientButton =
            new JButton("Remove Selected");

    private final DefaultTableModel ingredientTableModel =
            new DefaultTableModel(
                    new Object[]{"Ingredient", "Quantity", "Unit"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable ingredientTable =
            new JTable(ingredientTableModel);

    private final JButton saveButton = new JButton("Save Recipe");

    private final JLabel statusLabel =
            new JLabel("Fill in your recipe details.");

    /**
     * Creates a standalone view without a connected controller.
     */
    public AddRecipeView() {
        this(new AddRecipeViewModel());
    }

    /**
     * Creates a view using the same ViewModel as its presenter.
     */
    public AddRecipeView(AddRecipeViewModel viewModel) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        configureSpinner(servingsSpinner, "0");
        configureSpinner(quantitySpinner, "0.##");

        JLabel titleLabel = new JLabel("Add My Own Recipe");
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 24f)
        );
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(
                new BoxLayout(formPanel, BoxLayout.Y_AXIS)
        );

        formPanel.add(createRow("Recipe name:", nameField));
        formPanel.add(Box.createVerticalStrut(10));

        JPanel servingsPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );
        servingsPanel.add(servingsSpinner);

        formPanel.add(createRow("Servings:", servingsPanel));
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createRow(
                "Image (optional):", createImagePanel()
        ));
        formPanel.add(Box.createVerticalStrut(15));

        formPanel.add(createIngredientsPanel());
        formPanel.add(Box.createVerticalStrut(15));

        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);

        JPanel instructionsPanel =
                new JPanel(new BorderLayout(5, 5));

        instructionsPanel.setBorder(
                BorderFactory.createTitledBorder("Instructions")
        );

        instructionsPanel.add(
                new JLabel("Enter one cooking step per line."),
                BorderLayout.NORTH
        );

        instructionsPanel.add(
                new JScrollPane(instructionsArea),
                BorderLayout.CENTER
        );

        formPanel.add(instructionsPanel);
        add(new JScrollPane(formPanel), BorderLayout.CENTER);

        JPanel bottomPanel =
                new JPanel(new BorderLayout(10, 10));

        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(saveButton, BorderLayout.EAST);

        saveButton.setEnabled(false);
        add(bottomPanel, BorderLayout.SOUTH);

        addIngredientButton.addActionListener(
                event -> addIngredient()
        );

        removeIngredientButton.addActionListener(
                event -> removeSelectedIngredient()
        );

        unknownQuantityCheckBox.addActionListener(
                event -> updateQuantityVisibility()
        );

        saveButton.addActionListener(
                event -> submitRecipe()
        );

        // React to results prepared by the presenter.
        viewModel.addPropertyChangeListener(event -> {
            if (!"result".equals(event.getPropertyName())) {
                return;
            }

            String message = viewModel.getMessage();
            boolean successful = viewModel.isSuccessful();

            Runnable updateView = () -> {
                statusLabel.setText(message);
                statusLabel.setToolTipText(message);

                if (successful) {
                    clearForm();
                }
            };

            if (SwingUtilities.isEventDispatchThread()) {
                updateView.run();
            }
            else {
                SwingUtilities.invokeLater(updateView);
            }
        });
    }

    /**
     * Connects this view to its controller.
     */
    public void setController(AddRecipeController controller) {
        this.controller = controller;
        saveButton.setEnabled(controller != null);
    }

    /**
     * Builds the local image selection controls.
     */
    private JPanel createImagePanel() {
        imageUrlField.setEditable(false);

        JButton chooseImageButton = new JButton("Choose Image");
        JButton removeImageButton = new JButton("Remove");

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 5, 0)
        );
        buttons.add(chooseImageButton);
        buttons.add(removeImageButton);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(imageUrlField, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);

        chooseImageButton.addActionListener(
                event -> chooseImage()
        );

        removeImageButton.addActionListener(event -> {
            imageUrlField.setText("");
            imageUrlField.setToolTipText(null);
            statusLabel.setText("Image removed from the form.");
        });

        return panel;
    }

    /**
     * Records the selected file's URI.
     * The file is not copied or uploaded at this stage.
     */
    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a recipe image");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        FileNameExtensionFilter imageFilter =
                new FileNameExtensionFilter(
                        "Image files (*.jpg, *.jpeg, *.png)",
                        "jpg", "jpeg", "png"
                );

        chooser.setFileFilter(imageFilter);

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();

        if (!selectedFile.isFile()
                || !selectedFile.canRead()
                || !imageFilter.accept(selectedFile)) {
            statusLabel.setText(
                    "Please choose a readable JPG or PNG file."
            );
            return;
        }

        imageUrlField.setText(selectedFile.toURI().toString());
        imageUrlField.setToolTipText(selectedFile.getAbsolutePath());

        statusLabel.setText(
                "Image selected: " + selectedFile.getName()
        );
    }

    /**
     * Builds the ingredient controls and table.
     */
    private JPanel createIngredientsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.setBorder(
                BorderFactory.createTitledBorder("Ingredients")
        );

        JPanel inputs = new JPanel();
        inputs.setLayout(
                new BoxLayout(inputs, BoxLayout.Y_AXIS)
        );

        inputs.add(createRow(
                "Ingredient name:", ingredientNameField
        ));

        JPanel quantityControls = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );

        quantityControls.add(quantitySpinner);
        quantityControls.add(Box.createHorizontalStrut(10));
        quantityControls.add(unitComboBox);
        quantityControls.add(Box.createHorizontalStrut(10));
        quantityControls.add(unknownQuantityCheckBox);

        inputs.add(Box.createVerticalStrut(8));
        inputs.add(createRow(
                "Quantity / unit:", quantityControls
        ));

        JPanel addButtonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        addButtonPanel.add(addIngredientButton);

        inputs.add(Box.createVerticalStrut(8));
        inputs.add(addButtonPanel);

        panel.add(inputs, BorderLayout.NORTH);

        ingredientTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        ingredientTable.setRowHeight(26);
        ingredientTable.getTableHeader().setReorderingAllowed(false);

        // Hide the unit when an ingredient's quantity is unknown.
        ingredientTable.getColumnModel().getColumn(2).setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column) {

                        int modelRow =
                                table.convertRowIndexToModel(row);

                        Object quantity =
                                ingredientTableModel.getValueAt(
                                        modelRow, 1
                                );

                        Object displayedUnit =
                                quantity == null ? "" : value;

                        return super.getTableCellRendererComponent(
                                table,
                                displayedUnit,
                                isSelected,
                                hasFocus,
                                row,
                                column
                        );
                    }
                }
        );

        JScrollPane tableScrollPane =
                new JScrollPane(ingredientTable);

        tableScrollPane.setPreferredSize(
                new Dimension(500, 130)
        );

        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel removeButtonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        removeButtonPanel.add(removeIngredientButton);

        panel.add(removeButtonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Adds an ingredient to the temporary form data.
     */
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

        imageUrlField.setText("");
        imageUrlField.setToolTipText(null);

        instructionsArea.setText("");

        ingredientNameField.setText("");
        ingredientTableModel.setRowCount(0);

        quantitySpinner.setValue(1.0);
        unitComboBox.setSelectedItem("g");
        unknownQuantityCheckBox.setSelected(false);
        updateQuantityVisibility();

        nameField.requestFocusInWindow();
    }

    /**
     * Uses arrows or the mouse wheel instead of typed numbers.
     */
    private void configureSpinner(JSpinner spinner, String format) {
        JSpinner.NumberEditor editor =
                new JSpinner.NumberEditor(spinner, format);

        spinner.setEditor(editor);
        editor.getTextField().setEditable(false);
        spinner.setPreferredSize(new Dimension(110, 30));

        spinner.addMouseWheelListener(event -> {
            if (!spinner.isEnabled() || !spinner.isVisible()) {
                return;
            }

            int rotation = event.getWheelRotation();

            for (int step = 0; step < Math.abs(rotation); step++) {
                Object nextValue;

                if (rotation < 0) {
                    nextValue = spinner.getNextValue();
                }
                else {
                    nextValue = spinner.getPreviousValue();
                }

                if (nextValue == null) {
                    break;
                }

                spinner.setValue(nextValue);
            }

            event.consume();
        });
    }

    /**
     * Places a label and an input component on the same row.
     */
    private JPanel createRow(String labelText, JComponent input) {
        JPanel row = new JPanel(new BorderLayout(10, 0));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(160, 30));

        row.add(label, BorderLayout.WEST);
        row.add(input, BorderLayout.CENTER);

        row.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40)
        );

        return row;
    }
}