package view;

import entity.MealPlanEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.AttributedString;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders a printable snapshot of the planner without Swing controls. */
final class WeeklyMealPlanPdf {
    private static final int WIDTH = 1500;
    private static final int HEIGHT = 900;
    private static final int MARGIN = 48;
    private static final int COLUMN = (WIDTH - 2 * MARGIN) / 8;
    private static final Color FOREST = Color.decode("#355E43");
    private static final Color MINT = Color.decode("#E6F1E8");
    private static final Color LINE = Color.decode("#D5E3D8");
    private static final Color INK = Color.decode("#26332B");
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 23);
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private WeeklyMealPlanPdf() {
    }

    static void write(Path destination, LocalDate weekStart, List<String> meals,
                      List<MealPlanEntry> assignments, Map<String, String> recipes)
            throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            draw(graphics, weekStart, meals, assignments, recipes);
        } finally {
            graphics.dispose();
        }

        Path target = destination.toAbsolutePath();
        Path temporaryFile = Files.createTempFile(target.getParent(), "meal-plan-", ".pdf");
        try {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(new PDRectangle(792, 612));
                document.addPage(page);
                float width = 744;
                float height = width * HEIGHT / WIDTH;
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(LosslessFactory.createFromImage(document, image),
                            24, 612 - 24 - height, width, height);
                }
                document.save(temporaryFile.toFile());
            }
            Files.move(temporaryFile, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void draw(Graphics2D graphics, LocalDate weekStart, List<String> meals,
                             List<MealPlanEntry> assignments, Map<String, String> recipes) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(FOREST);
        graphics.setFont(BODY.deriveFont(Font.BOLD, 48f));
        graphics.drawString("Weekly Meal Planner", MARGIN, 80);
        graphics.setFont(BODY);
        graphics.drawString(DAY.format(weekStart) + " - " + DAY.format(weekStart.plusDays(6))
                + ", " + weekStart.plusDays(6).getYear(), MARGIN, 120);

        int tableTop = 164;
        int headerHeight = 66;
        int rowHeight = (HEIGHT - MARGIN - tableTop - headerHeight) / meals.size();
        cell(graphics, "Meal", 0, tableTop, headerHeight, true);
        for (int day = 0; day < 7; day++) {
            cell(graphics, DAY.format(weekStart.plusDays(day)), day + 1,
                    tableTop, headerHeight, true);
        }

        Map<LocalDate, Map<String, String>> byDay = new HashMap<>();
        for (MealPlanEntry entry : assignments) {
            byDay.computeIfAbsent(entry.getDate(), date -> new HashMap<>())
                    .put(entry.getMealType(),
                            recipes.getOrDefault(entry.getRecipeId(), entry.getRecipeId()));
        }
        for (int row = 0; row < meals.size(); row++) {
            String meal = meals.get(row);
            int top = tableTop + headerHeight + row * rowHeight;
            cell(graphics, meal, 0, top, rowHeight, true);
            for (int day = 0; day < 7; day++) {
                String name = byDay.getOrDefault(weekStart.plusDays(day), Map.of())
                        .getOrDefault(meal, "-");
                cell(graphics, name, day + 1, top, rowHeight, false);
            }
        }
    }

    private static void cell(Graphics2D graphics, String text, int column, int top,
                             int height, boolean heading) {
        int left = MARGIN + column * COLUMN;
        graphics.setColor(heading ? MINT : Color.WHITE);
        graphics.fillRect(left, top, COLUMN, height);
        graphics.setColor(LINE);
        graphics.drawRect(left, top, COLUMN, height);
        graphics.setColor(heading ? FOREST : INK);

        String singleLine = text.replaceAll("\\s+", " ").strip();
        AttributedString attributed = new AttributedString(singleLine.isEmpty() ? "-" : singleLine);
        Font font = heading ? BODY.deriveFont(Font.BOLD, 21f) : BODY;
        attributed.addAttribute(TextAttribute.FONT, font);
        LineBreakMeasurer lines = new LineBreakMeasurer(attributed.getIterator(),
                graphics.getFontRenderContext());
        int end = attributed.getIterator().getEndIndex();
        float baseline = top + 16;
        while (lines.getPosition() < end) {
            TextLayout line = lines.nextLayout(COLUMN - 24);
            baseline += line.getAscent();
            if (baseline + line.getDescent() > top + height - 12) {
                graphics.setFont(BODY);
                graphics.drawString("...", left + 12, top + height - 10);
                break;
            }
            line.draw(graphics, left + 12, baseline);
            baseline += line.getDescent() + line.getLeading();
        }
    }
}
