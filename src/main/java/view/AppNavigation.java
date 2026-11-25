package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import view.components.ThemeButton;

/** The shared window header and its three application pages. */
public final class AppNavigation extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
    private final ButtonGroup group = new ButtonGroup();
    private final Map<String, ThemeButton> buttons = new LinkedHashMap<>();
    private final Map<String, Supplier<? extends JComponent>> pending = new LinkedHashMap<>();

    public AppNavigation(boolean fullWindowHeader) {
        super(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(UiTheme.SPACE_XL, 0));
        header.setBackground(UiTheme.SURFACE);
        int topInset = fullWindowHeader ? 34 : UiTheme.SPACE_SM;
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(topInset, UiTheme.PAGE_PADDING,
                        UiTheme.SPACE_SM, UiTheme.PAGE_PADDING)));

        JLabel brand = new JLabel("WhatToEat");
        brand.setFont(UiTheme.HEADING_FONT);
        brand.setForeground(UiTheme.SECONDARY);
        links.setOpaque(false);
        header.add(brand, BorderLayout.WEST);
        header.add(links, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(pages, BorderLayout.CENTER);
    }

    public void addPage(String name, Supplier<? extends JComponent> content) {
        ThemeButton link = new ThemeButton(name, false);
        link.setModel(new JToggleButton.ToggleButtonModel());
        link.addActionListener(event -> showPage(name));
        group.add(link);
        links.add(link);
        buttons.put(name, link);
        pending.put(name, content);
    }

    public void showPage(String name) {
        Supplier<? extends JComponent> content = pending.remove(name);
        if (content != null) {
            pages.add(content.get(), name);
        }
        cards.show(pages, name);
        buttons.get(name).setSelected(true);
    }
}
