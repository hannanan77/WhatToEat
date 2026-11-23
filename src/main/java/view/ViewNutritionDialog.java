package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import entity.Recipe;

/**
 * Modal host for the reusable Nutrition panel.
 */
public class ViewNutritionDialog extends JDialog {

    private static final int DIALOG_WIDTH = 1040;
    private static final int DIALOG_HEIGHT = 740;
    private static final int DIALOG_ARC = UiTheme.CARD_RADIUS;

    private final ViewNutritionView nutritionView;
    private Component returnFocusComponent;
    private boolean transparentWindowSupported;
    private boolean shapedWindowSupported;

    public ViewNutritionDialog(Window owner, ViewNutritionView nutritionView) {
        super(owner, "Recipe Nutrition", Dialog.ModalityType.APPLICATION_MODAL);
        this.nutritionView = nutritionView;

        setUndecorated(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        configureWindowCapabilities();

        nutritionView.setOpaque(false);
        final JPanel roundedContent = new RoundedDialogPanel();
        roundedContent.add(nutritionView, BorderLayout.CENTER);
        setContentPane(roundedContent);
        getRootPane().setBorder(BorderFactory.createEmptyBorder());
        getRootPane().setOpaque(false);
        getLayeredPane().setOpaque(false);
        setMinimumSize(new Dimension(920, 680));
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        updateWindowShape();
        setLocationRelativeTo(owner);
        setFocusCycleRoot(true);

        nutritionView.setCloseAction(this::closeDialog);
        registerEscapeAction();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateWindowShape();
            }
        });
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
        updateWindowShape();
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

    private void configureWindowCapabilities() {
        try {
            final GraphicsDevice device = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
            transparentWindowSupported = device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
            shapedWindowSupported = device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        }
        catch (UnsupportedOperationException | SecurityException exception) {
            transparentWindowSupported = false;
            shapedWindowSupported = false;
        }

        if (transparentWindowSupported) {
            setBackground(new Color(0, 0, 0, 0));
        }
        else {
            setBackground(UiTheme.BACKGROUND);
        }
    }

    private void updateWindowShape() {
        if (!shapedWindowSupported || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        try {
            setShape(new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight(), DIALOG_ARC, DIALOG_ARC));
        }
        catch (UnsupportedOperationException | SecurityException exception) {
            shapedWindowSupported = false;
        }
    }

    /** Paints the dialog surface while leaving rounded corners transparent. */
    private static class RoundedDialogPanel extends JPanel {
        RoundedDialogPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(UiTheme.BACKGROUND);
            graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    DIALOG_ARC, DIALOG_ARC);
            graphics2D.setColor(UiTheme.BORDER);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    DIALOG_ARC, DIALOG_ARC);
            graphics2D.dispose();
        }
    }
}
