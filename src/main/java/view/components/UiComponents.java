package view.components;

import view.UiTheme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Styling helpers for existing Swing controls. Apply once when creating the controls. */
public final class UiComponents {
    private UiComponents() {
    }

    public static void styleTextField(JTextField field) {
        field.setFont(UiTheme.BODY_LARGE_FONT);
        field.setForeground(UiTheme.TEXT_PRIMARY);
        field.setBackground(UiTheme.SURFACE);
        field.setCaretColor(UiTheme.SECONDARY);
        field.setSelectionColor(UiTheme.MINT);
        field.setSelectedTextColor(UiTheme.TEXT_PRIMARY);
        field.setBorder(new RoundedBorder(UiTheme.INPUT_PADDING_X));
        int height = Math.max(UiTheme.CONTROL_MIN_HEIGHT,
                field.getFontMetrics(field.getFont()).getHeight() + UiTheme.INPUT_PADDING_Y * 2);
        field.setPreferredSize(new Dimension(180, height));
        field.setMinimumSize(new Dimension(40, height));
        repaintOnFocus(field, field);
    }

    /** Preserves the supplied numeric model, limits, and increment. */
    public static void styleSpinner(JSpinner spinner, String format) {
        spinner.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                JButton button = new ThemeArrowButton(true);
                installNextButtonListeners(button);
                return button;
            }
            @Override
            protected Component createPreviousButton() {
                JButton button = new ThemeArrowButton(false);
                installPreviousButtonListeners(button);
                return button;
            }
        });
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, format);
        spinner.setEditor(editor);
        JFormattedTextField field = editor.getTextField();
        field.setEditable(false);
        field.setFont(UiTheme.BODY_FONT);
        field.setForeground(UiTheme.TEXT_PRIMARY);
        field.setBackground(UiTheme.SURFACE);
        field.setBorder(new EmptyBorder(0, UiTheme.SPACE_SM, 0, UiTheme.SPACE_SM));
        spinner.setBackground(UiTheme.SURFACE);
        spinner.setBorder(new RoundedBorder(UiTheme.SPACE_XS, UiTheme.SPACE_XS));
        int height = Math.max(UiTheme.CONTROL_MIN_HEIGHT,
                Math.max(field.getFontMetrics(field.getFont()).getHeight(),
                        UiTheme.ARROW_HEIGHT * 2) + UiTheme.SPACE_XS * 2);
        spinner.setPreferredSize(new Dimension(130, height));
        spinner.setMinimumSize(new Dimension(100, height));
        repaintOnFocus(field, spinner);
        // Do not add duplicate wheel listeners if a page restyles an existing spinner.
        if (!Boolean.TRUE.equals(spinner.getClientProperty("whattoeat.wheelInstalled"))) {
            spinner.putClientProperty("whattoeat.wheelInstalled", Boolean.TRUE);
            spinner.addMouseWheelListener(event -> {
                if (!spinner.isEnabled() || !spinner.isVisible()) {
                    return;
                }
                int rotation = event.getWheelRotation();
                for (int i = 0; i < Math.abs(rotation); i++) {
                    Object next = rotation < 0 ? spinner.getNextValue() : spinner.getPreviousValue();
                    if (next == null) {
                        break;
                    }
                    spinner.setValue(next);
                }
                event.consume();
            });
        }
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new ThemeArrowButton(false);
                button.getAccessibleContext().setAccessibleName("Open options");
                return button;
            }
        });
        combo.setFont(UiTheme.BODY_FONT);
        combo.setForeground(UiTheme.TEXT_PRIMARY);
        combo.setBackground(UiTheme.SURFACE);
        combo.setBorder(new RoundedBorder(UiTheme.SPACE_XS, UiTheme.SPACE_XS));
        int height = Math.max(UiTheme.CONTROL_MIN_HEIGHT,
                combo.getFontMetrics(combo.getFont()).getHeight() + UiTheme.INPUT_PADDING_Y * 2);
        combo.setPreferredSize(new Dimension(100, height));
        combo.setMinimumSize(new Dimension(80, height));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setFont(UiTheme.BODY_FONT);
                setForeground(UiTheme.TEXT_PRIMARY);
                setBackground(selected ? UiTheme.MINT : UiTheme.SURFACE);
                setBorder(new EmptyBorder(UiTheme.SPACE_XS, UiTheme.SPACE_SM,
                        UiTheme.SPACE_XS, UiTheme.SPACE_SM));
                return this;
            }
        });
        repaintOnFocus(combo, combo);
    }

    public static JScrollPane scrollPane(Component content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setBackground(UiTheme.SURFACE);
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollPane(scroll);
        return scroll;
    }

    /** Styles both bars without changing a page's chosen scrollbar policies. */
    public static void styleScrollPane(JScrollPane scroll) {
        JScrollBar vertical = scroll.getVerticalScrollBar();
        vertical.setUI(new ThemeScrollBarUI());
        vertical.setPreferredSize(new Dimension(UiTheme.SCROLLBAR_WIDTH, 0));
        vertical.setUnitIncrement(UiTheme.SCROLL_INCREMENT);
        JScrollBar horizontal = scroll.getHorizontalScrollBar();
        horizontal.setUI(new ThemeScrollBarUI());
        horizontal.setPreferredSize(new Dimension(0, UiTheme.SCROLLBAR_WIDTH));
        horizontal.setUnitIncrement(UiTheme.SCROLL_INCREMENT);
    }

    private static void repaintOnFocus(JComponent target, JComponent borderOwner) {
        target.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                borderOwner.repaint();
            }
            @Override
            public void focusLost(FocusEvent event) {
                borderOwner.repaint();
            }
        });
    }

    /** Returns a disposable copy; callers must dispose it after painting. */
    public static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }
}
