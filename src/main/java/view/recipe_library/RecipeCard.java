package view.recipe_library;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import interface_adapter.recipe_library.RecipeDisplayData;
import view.UiTheme;
import view.components.ThemeButton;

/**
 * One recipe summary in the browsing list.
 */
final class RecipeCard extends JPanel {

    private final String recipeId;
    private final boolean frame;
    private boolean selected;
    private boolean hovered;

    RecipeCard(RecipeDisplayData recipe, Runnable onSelect) {
        this(recipe, onSelect, null, null, null);
    }

    RecipeCard(RecipeDisplayData recipe, Runnable onSelect, String actionLabel,
               Runnable onAction, Runnable onDelete) {
        this(recipe.getId(), false);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(label(recipe.getName(), UiTheme.SECTION_HEADING_FONT,
                UiTheme.TEXT_PRIMARY, "recipe-card-name-" + recipeId));
        text.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        text.add(label(categoryText(recipe.getCategory()),
                UiTheme.BODY_FONT, UiTheme.TEXT_SECONDARY,
                "recipe-card-category-" + recipeId));
        text.add(Box.createVerticalStrut(UiTheme.SPACE_XS));
        text.add(label(servingsText(recipe.getServings()),
                UiTheme.LABEL_FONT, UiTheme.SECONDARY,
                "recipe-card-servings-" + recipeId));
        add(text, BorderLayout.CENTER);
        JPanel actions = actionLabel == null ? null
                : new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACE_XS, 0));
        installPointer(this, pointer(onSelect, actions));
        if (actions != null) {
            setToolTipText("Drag left or right-click for recipe options");
            actions.setOpaque(false);
            actions.setVisible(false);
            JButton visibility = new ThemeButton(actionLabel, false);
            visibility.addActionListener(event -> onAction.run());
            actions.add(visibility);
            if (onDelete != null) {
                JButton delete = new ThemeButton("Delete", false);
                delete.setForeground(UiTheme.ERROR);
                delete.addActionListener(event -> onDelete.run());
                actions.add(delete);
            }
            JPanel actionArea = new JPanel(new java.awt.GridBagLayout());
            actionArea.setOpaque(false);
            actionArea.add(actions);
            add(actionArea, BorderLayout.EAST);
        }
    }

    /** A card outline with empty bars and no recipe content. */
    static RecipeCard emptyFrame(String frameId, Runnable onSelect) {
        RecipeCard card = new RecipeCard(frameId, true);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(bar(168, 14, UiTheme.TEXT_PRIMARY));
        text.add(Box.createVerticalStrut(UiTheme.SPACE_MD));
        text.add(bar(96, 10, UiTheme.TEXT_SECONDARY));
        text.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        text.add(bar(72, 10, UiTheme.SECONDARY));
        card.add(text, BorderLayout.CENTER);
        card.installPointer(card, card.pointer(onSelect, null));
        return card;
    }

    private RecipeCard(String recipeId, boolean frame) {
        this.recipeId = recipeId;
        this.frame = frame;
        setName("recipe-card-" + recipeId);
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.CARD_PADDING,
                UiTheme.SPACE_LG, UiTheme.CARD_PADDING));
        setAlignmentX(LEFT_ALIGNMENT);
        refreshColors();
    }

    private MouseAdapter pointer(Runnable onSelect, JPanel action) {
        return new MouseAdapter() {
            private int pressX;
            private int pressY;

            @Override
            public void mousePressed(MouseEvent event) {
                pressX = event.getXOnScreen();
                pressY = event.getYOnScreen();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (action == null || event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int dx = event.getXOnScreen() - pressX;
                int dy = event.getYOnScreen() - pressY;
                if (Math.abs(dx) >= 36 && Math.abs(dx) > Math.abs(dy)) {
                    action.setVisible(dx < 0);
                    revalidate();
                    repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                refreshColors();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                Component source = (Component) event.getSource();
                java.awt.Point point = javax.swing.SwingUtilities.convertPoint(
                        source, event.getPoint(), RecipeCard.this);
                if (!contains(point)) {
                    hovered = false;
                    refreshColors();
                }
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (action != null && event.getButton() == MouseEvent.BUTTON3) {
                    action.setVisible(!action.isVisible());
                    revalidate();
                    repaint();
                } else if (event.getButton() != MouseEvent.BUTTON3) {
                    onSelect.run();
                }
            }
        };
    }

    String getRecipeId() {
        return recipeId;
    }

    boolean isFrame() {
        return frame;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
        refreshColors();
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(getBackground());
        graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                EditorialTheme.CORNER_ARC, EditorialTheme.CORNER_ARC);
        graphics2D.setColor(selected ? UiTheme.SECONDARY : UiTheme.BORDER);
        if (selected) {
            graphics2D.setStroke(new java.awt.BasicStroke(UiTheme.FOCUS_WIDTH));
        }
        graphics2D.drawRoundRect(1, 1, getWidth() - 4, getHeight() - 4,
                EditorialTheme.CORNER_ARC, EditorialTheme.CORNER_ARC);
        graphics2D.dispose();
        super.paintComponent(graphics);
    }

    private void refreshColors() {
        if (selected && hovered) {
            setBackground(EditorialTheme.CARD_SELECTED_HOVER);
        }
        else if (selected) {
            setBackground(EditorialTheme.CARD_SELECTED);
        }
        else if (hovered) {
            setBackground(EditorialTheme.CARD_HOVER);
        }
        else {
            setBackground(EditorialTheme.CARD);
        }
        repaint();
    }

    private void installPointer(Component component, MouseAdapter pointer) {
        component.addMouseListener(pointer);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                installPointer(child, pointer);
            }
        }
    }

    private static JPanel bar(int width, int height, java.awt.Color color) {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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

    private static JLabel label(String text, Font font, java.awt.Color color, String name) {
        JLabel label = new JLabel(text);
        label.setName(name);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    static String categoryText(String category) {
        if (category == null || category.isBlank()) {
            return "No category";
        }
        return category;
    }

    static String servingsText(Integer servings) {
        if (servings == null) {
            return "Servings unavailable";
        }
        if (servings == 1) {
            return "1 serving";
        }
        return servings + " servings";
    }
}
