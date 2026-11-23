package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import entity.Recipe;

/**
 * Modal host for the reusable Nutrition panel.
 */
public class ViewNutritionDialog extends JDialog {

    private static final int DIALOG_WIDTH = 1040;
    private static final int DIALOG_HEIGHT = 740;

    private final ViewNutritionView nutritionView;
    private Component returnFocusComponent;

    public ViewNutritionDialog(Window owner, ViewNutritionView nutritionView) {
        super(owner, "Recipe Nutrition", Dialog.ModalityType.APPLICATION_MODAL);
        this.nutritionView = nutritionView;

        setUndecorated(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setContentPane(nutritionView);
        getRootPane().setBorder(BorderFactory.createLineBorder(
                new Color(225, 233, 226)));
        setMinimumSize(new Dimension(920, 680));
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(owner);
        setFocusCycleRoot(true);

        nutritionView.setCloseAction(this::closeDialog);
        registerEscapeAction();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                restoreFocus();
            }
        });
    }

    /** Opens the modal for a selected recipe and starts the calculation. */
    public void showForRecipe(Recipe recipe, Component opener) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showForRecipe(recipe, opener));
            return;
        }

        returnFocusComponent = opener;
        nutritionView.setSelectedRecipe(recipe);
        setLocationRelativeTo(getOwner());
        nutritionView.requestNutrition();
        SwingUtilities.invokeLater(nutritionView::requestInitialFocus);
        setVisible(true);
    }

    public void closeDialog() {
        setVisible(false);
        restoreFocus();
    }

    private void registerEscapeAction() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeNutritionDialog");
        getRootPane().getActionMap().put("closeNutritionDialog", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                closeDialog();
            }
        });
    }

    private void restoreFocus() {
        if (returnFocusComponent != null) {
            SwingUtilities.invokeLater(returnFocusComponent::requestFocusInWindow);
        }
    }
}
