package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import view.components.ThemeButton;
import view.components.ThemeCard;
import view.components.UiComponents;

/** Week selection, the month picker, and actions on individual weeks. */
public final class WeekSidebar extends ThemeCard {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    public WeekSidebar(List<LocalDate> visible, List<LocalDate> hidden, LocalDate selected,
                       boolean showHidden, Consumer<Boolean> toggleHidden,
                       Consumer<LocalDate> open, Consumer<LocalDate> add,
                       Consumer<LocalDate> hide, Consumer<LocalDate> delete) {
        this(new JPanel(new BorderLayout(0, UiTheme.SPACE_SM)), visible, hidden, selected,
                showHidden, toggleHidden, open, add, hide, delete);
    }

    private WeekSidebar(JPanel content, List<LocalDate> visible, List<LocalDate> hidden,
                        LocalDate selected, boolean showHidden, Consumer<Boolean> toggleHidden,
                        Consumer<LocalDate> open, Consumer<LocalDate> add,
                        Consumer<LocalDate> hide, Consumer<LocalDate> delete) {
        super("Choose a Week", content);
        content.setOpaque(false);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (LocalDate date : visible) {
            addRow(rows, date, date.equals(selected), false, open, hide, delete);
        }
        if (showHidden) {
            for (LocalDate date : hidden) {
                addRow(rows, date, false, true, open, add, delete);
            }
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        UiComponents.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(196, Math.min(280, rows.getPreferredSize().height)));
        content.add(scroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        controls.setOpaque(false);
        JButton addWeek = new ThemeButton("+", false);
        addWeek.setToolTipText("Add a week");
        addWeek.getAccessibleContext().setAccessibleName("Add a week");
        addWeek.addActionListener(event -> pickWeek(selected == null ? LocalDate.now() : selected, add));
        controls.add(addWeek, BorderLayout.NORTH);
        if (!hidden.isEmpty()) {
            JCheckBox toggle = new JCheckBox("Show Hidden Weeks", showHidden);
            toggle.setFont(UiTheme.BODY_FONT);
            toggle.setOpaque(false);
            toggle.addActionListener(event -> toggleHidden.accept(toggle.isSelected()));
            controls.add(toggle, BorderLayout.SOUTH);
        }
        content.add(controls, BorderLayout.SOUTH);
    }

    private static void addRow(JPanel rows, LocalDate monday, boolean selected, boolean hidden,
                               Consumer<LocalDate> open, Consumer<LocalDate> visibility,
                               Consumer<LocalDate> delete) {
        String range = DATE.format(monday) + " – " + DATE.format(monday.plusDays(6));
        JPanel row = new JPanel(new BorderLayout(0, UiTheme.SPACE_XS)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JButton week = new ThemeButton(range, selected);
        week.setToolTipText(monday.getYear() + " · Drag left for week options");
        week.addActionListener(event -> {
            if (!hidden && !selected) {
                open.accept(monday);
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, UiTheme.SPACE_XS, 0));
        actions.setOpaque(false);
        actions.setVisible(hidden);
        JButton visibilityButton = new ThemeButton(hidden ? "Restore" : "Hide", false);
        visibilityButton.addActionListener(event -> visibility.accept(monday));
        actions.add(visibilityButton);
        JButton deleteButton = new ThemeButton("Delete", false);
        deleteButton.setForeground(UiTheme.ERROR);
        deleteButton.addActionListener(event -> delete.accept(monday));
        actions.add(deleteButton);

        MouseAdapter swipe = new MouseAdapter() {
            private int startX;
            private int startY;

            @Override
            public void mousePressed(MouseEvent event) {
                startX = event.getXOnScreen();
                startY = event.getYOnScreen();
                if (SwingUtilities.isRightMouseButton(event)) {
                    actions.setVisible(!actions.isVisible());
                    row.revalidate();
                    row.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                int dx = event.getXOnScreen() - startX;
                int dy = event.getYOnScreen() - startY;
                if (Math.abs(dx) >= 32 && Math.abs(dx) > Math.abs(dy)) {
                    week.getModel().setArmed(false);
                    actions.setVisible(dx < 0);
                    row.revalidate();
                    row.repaint();
                }
            }
        };
        week.addMouseListener(swipe);
        week.addMouseMotionListener(swipe);
        row.add(week, BorderLayout.NORTH);
        row.add(actions, BorderLayout.CENTER);
        rows.add(row);
        rows.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
    }

    private void pickWeek(LocalDate initial, Consumer<LocalDate> add) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add a Week",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel picker = new JPanel(new BorderLayout(0, UiTheme.SPACE_MD));
        picker.setBackground(UiTheme.BACKGROUND);
        picker.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG,
                UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectors.setOpaque(false);
        JComboBox<String> month = new JComboBox<>();
        for (Month value : Month.values()) {
            month.addItem(value.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        month.setSelectedIndex(initial.getMonthValue() - 1);
        month.getAccessibleContext().setAccessibleName("Month");
        JSpinner year = new JSpinner(new SpinnerNumberModel(initial.getYear(), 1, 9999, 1));
        year.setEditor(new JSpinner.NumberEditor(year, "0"));
        year.getAccessibleContext().setAccessibleName("Year");
        selectors.add(month);
        selectors.add(year);
        picker.add(selectors, BorderLayout.NORTH);

        JPanel days = new JPanel(new GridLayout(7, 7, UiTheme.SPACE_XS, UiTheme.SPACE_XS));
        days.setOpaque(false);
        Runnable render = () -> {
            YearMonth shown = YearMonth.of((Integer) year.getValue(), month.getSelectedIndex() + 1);
            days.removeAll();
            for (DayOfWeek weekday : DayOfWeek.values()) {
                days.add(new JLabel(weekday.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        SwingConstants.CENTER));
            }
            LocalDate first = shown.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int index = 0; index < 42; index++) {
                LocalDate date = first.plusDays(index);
                JButton day = new ThemeButton(Integer.toString(date.getDayOfMonth()), date.equals(initial));
                day.setEnabled(YearMonth.from(date).equals(shown));
                day.setToolTipText(date.toString());
                day.getAccessibleContext().setAccessibleName(date.toString());
                day.addActionListener(event -> {
                    dialog.dispose();
                    add.accept(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                });
                days.add(day);
            }
            days.revalidate();
            days.repaint();
        };
        month.addActionListener(event -> render.run());
        year.addChangeListener(event -> render.run());
        render.run();
        picker.add(days, BorderLayout.CENTER);
        picker.add(new JLabel("Select any day to add its Monday–Sunday week."), BorderLayout.SOUTH);
        dialog.setContentPane(picker);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
