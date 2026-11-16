package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import interface_adapter.browse_recipes.RecipeDisplayData;

class RecipesPanelTest {

    private static final List<JFrame> FRAMES = new ArrayList<>();

    @Test
    void startsEmptyWithoutASelectedRecipe() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());

            assertTrue(panel instanceof javax.swing.JPanel);
            assertEquals("No recipes yet", label(panel, "recipes-list-message").getText());
            assertEquals("Select a recipe to explore.",
                    label(panel, "recipes-detail-message").getText());
            assertNull(find(panel, "recipe-card-oats"));
        });
    }

    @Test
    void rendersSuppliedRecipesAndShowsDetailsAfterAClick() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", "Breakfast", 2, null,
                            List.of("80 g oats"), List.of("Simmer until soft")),
                    recipe("salad", "Herb salad", null, null, null,
                            List.of("Mixed herbs"), List.of("Toss gently"))
            ));

            assertEquals("Morning oats", label(panel, "recipe-card-name-oats").getText());
            assertEquals("Breakfast", label(panel, "recipe-card-category-oats").getText());
            assertEquals("2 servings", label(panel, "recipe-card-servings-oats").getText());
            assertEquals("No category", label(panel, "recipe-card-category-salad").getText());
            assertEquals("Servings unavailable",
                    label(panel, "recipe-card-servings-salad").getText());

            click(find(panel, "recipe-card-salad"));

            assertEquals(EditorialTheme.CARD_SELECTED,
                    find(panel, "recipe-card-salad").getBackground());
            assertEquals("Herb salad", label(panel, "recipe-detail-name").getText());
            assertEquals("Servings unavailable",
                    label(panel, "recipe-detail-servings").getText());
            assertTrue(text(panel, "recipe-detail-ingredients").contains("Mixed herbs"));
            assertTrue(text(panel, "recipe-detail-instructions").contains("Toss gently"));
            assertEquals("No image available",
                    label(panel, "recipe-detail-image-message").getText());
            assertNull(find(panel, "recipes-detail-message"));
        });
    }

    @Test
    void searchFiltersByNameAndKeepsAnEmptyResultState() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", "Breakfast", 1, null,
                            List.of("Oats"), List.of("Cook")),
                    recipe("salad", "Herb salad", "Lunch", 2, null,
                            List.of("Herbs"), List.of("Toss"))
            ));

            JTextField search = (JTextField) find(panel, "recipes-search");
            search.setText("breakfast");
            assertNull(find(panel, "recipe-card-oats"));
            assertNull(find(panel, "recipe-card-salad"));
            assertEquals("No matching recipes",
                    label(panel, "recipes-list-message").getText());

            search.setText("HERB");
            assertNull(find(panel, "recipe-card-oats"));
            assertNotNull(find(panel, "recipe-card-salad"));
        });
    }

    @Test
    void hoverChangesTheCardColor() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(recipe("oats", "Morning oats", null, 1, null,
                    List.of("Oats"), List.of("Cook"))));
            Component card = find(panel, "recipe-card-oats");

            mouse(card, MouseEvent.MOUSE_ENTERED);
            assertEquals(EditorialTheme.CARD_HOVER, card.getBackground());

            mouse(card, MouseEvent.MOUSE_EXITED);
            assertEquals(EditorialTheme.CARD, card.getBackground());
        });
    }

    @Test
    void bothColumnsScrollIndependently() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            List<RecipeDisplayData> recipes = new ArrayList<>();
            List<String> steps = new ArrayList<>();
            for (int step = 1; step <= 40; step++) {
                steps.add("Complete preparation step " + step);
            }
            for (int index = 1; index <= 24; index++) {
                recipes.add(recipe("recipe-" + index, "Recipe " + index, "Supper",
                        index, null, List.of("Ingredient " + index), steps));
            }
            panel.setRecipes(recipes);
            click(find(panel, "recipe-card-recipe-1"));
            layout(panel);

            JScrollPane listScroll = (JScrollPane) find(panel, "recipes-list-scroll");
            JScrollPane detailScroll = (JScrollPane) find(panel, "recipes-detail-scroll");
            assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    listScroll.getVerticalScrollBarPolicy());
            assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    detailScroll.getVerticalScrollBarPolicy());
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                    listScroll.getHorizontalScrollBarPolicy());
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                    detailScroll.getHorizontalScrollBarPolicy());
            assertTrue(listScroll.getViewport().getHeight() > 100);
            assertTrue(detailScroll.getViewport().getHeight() > 100);
            assertTrue(listScroll.getViewport().getView()
                    .getPreferredSize().height > listScroll.getViewport().getHeight());
            assertTrue(detailScroll.getViewport().getView()
                    .getPreferredSize().height > detailScroll.getViewport().getHeight());
            assertTrue(scrollableRange(listScroll) > 20);
            assertTrue(scrollableRange(detailScroll) > 20);

            JScrollBar listBar = listScroll.getVerticalScrollBar();
            JScrollBar detailBar = detailScroll.getVerticalScrollBar();
            listBar.setValue(Math.min(48, listBar.getMaximum()));
            assertEquals(0, detailBar.getValue());
            detailBar.setValue(Math.min(48, detailBar.getMaximum()));
            assertTrue(listBar.getValue() > 0);
        });
    }

    @Test
    void columnsKeepTheRequestedProportion() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            int left = find(panel, "recipes-left").getWidth();
            int right = find(panel, "recipes-right").getWidth();
            double ratio = left / (double) (left + right);
            assertTrue(ratio > 0.30 && ratio < 0.40, "left ratio was " + ratio);
        });
    }

    @Test
    void refreshingTheListShowsNewRecipesAndUpdatesTheSelection() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            List<RecipeDisplayData> first = new ArrayList<>();
            first.add(recipe("soup", "Vegetable soup", "Dinner", 4, null,
                    List.of("Broth"), List.of("Simmer")));
            panel.setRecipes(first);
            click(find(panel, "recipe-card-soup"));

            first.add(recipe("bread", "Brown bread", "Bakery", 1, "   ",
                    List.of("Flour"), List.of("Bake")));
            assertNull(find(panel, "recipe-card-bread"));

            panel.setRecipes(List.of(
                    recipe("soup", "Vegetable soup", "Dinner", 4, null,
                            List.of("Broth", "Carrots"), List.of("Simmer", "Season")),
                    recipe("bread", "Brown bread", "Bakery", 1, null,
                            List.of("Flour"), List.of("Bake"))
            ));

            assertNotNull(find(panel, "recipe-card-bread"));
            assertEquals(EditorialTheme.CARD_SELECTED,
                    find(panel, "recipe-card-soup").getBackground());
            assertTrue(text(panel, "recipe-detail-ingredients").contains("Carrots"));
            assertTrue(text(panel, "recipe-detail-instructions").contains("Season"));
        });
    }

    @Test
    void missingAndLocalImagesAreHandledWithoutSampleRecipes() throws Exception {
        File imageFile = File.createTempFile("recipe-image", ".png");
        imageFile.deleteOnExit();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", imageFile);

        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(recipe("photo", "Plated dish", null, 2,
                    imageFile.getAbsolutePath(), List.of("Greens"), List.of("Plate"))));
            click(find(panel, "recipe-card-photo"));

            JLabel image = label(panel, "recipe-detail-image-message");
            assertNotNull(image.getIcon());
            assertTrue(image.getText() == null || image.getText().isEmpty());

            panel.setRecipes(List.of(recipe("photo", "Plated dish", null, 2,
                    "Z:\\missing\\recipe-photo.png", List.of("Greens"), List.of("Plate"))));
            assertEquals("No image available",
                    label(panel, "recipe-detail-image-message").getText());
            assertNull(label(panel, "recipe-detail-image-message").getIcon());
        });
    }

    @Test
    void nullAndEmptyCollectionsRestoreTheEmptyState() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(recipe("soup", "Vegetable soup", null, 2, null,
                    List.of("Broth"), List.of("Simmer"))));
            click(find(panel, "recipe-card-soup"));

            panel.setRecipes(null);
            assertEquals("No recipes yet", label(panel, "recipes-list-message").getText());
            assertEquals("Select a recipe to explore.",
                    label(panel, "recipes-detail-message").getText());
        });
    }

    private static RecipesPanel layout(RecipesPanel panel) {
        JFrame frame = panel.getTopLevelAncestor() instanceof JFrame
                ? (JFrame) panel.getTopLevelAncestor()
                : null;
        if (frame == null) {
            frame = new JFrame();
            FRAMES.add(frame);
            frame.setContentPane(panel);
        }
        frame.setSize(1100, 700);
        if (!frame.isDisplayable()) {
            frame.addNotify();
        }
        frame.validate();
        return panel;
    }

    private static int scrollableRange(JScrollPane scrollPane) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        return bar.getMaximum() - bar.getVisibleAmount();
    }

    private static RecipeDisplayData recipe(String id, String name, String category,
                                            Integer servings, String imageUrl,
                                            List<String> ingredients,
                                            List<String> instructions) {
        return new RecipeDisplayData(id, name, category, servings, imageUrl,
                ingredients, instructions);
    }

    private static void click(Component component) {
        mouse(component, MouseEvent.MOUSE_CLICKED);
    }

    private static void mouse(Component component, int eventId) {
        int x = eventId == MouseEvent.MOUSE_EXITED ? -20 : 4;
        int y = eventId == MouseEvent.MOUSE_EXITED ? -20 : 4;
        MouseEvent event = new MouseEvent(component, eventId, System.currentTimeMillis(),
                0, x, y, 1, false);
        for (MouseListener listener : component.getMouseListeners()) {
            if (eventId == MouseEvent.MOUSE_ENTERED) {
                listener.mouseEntered(event);
            }
            else if (eventId == MouseEvent.MOUSE_EXITED) {
                listener.mouseExited(event);
            }
            else if (eventId == MouseEvent.MOUSE_CLICKED) {
                listener.mouseClicked(event);
            }
        }
    }

    private static JLabel label(Component root, String name) {
        return (JLabel) find(root, name);
    }

    private static String text(Component root, String name) {
        StringBuilder builder = new StringBuilder();
        appendText(find(root, name), builder);
        return builder.toString();
    }

    private static void appendText(Component component, StringBuilder builder) {
        if (component instanceof JLabel) {
            builder.append(((JLabel) component).getText()).append('\n');
        }
        else if (component instanceof JTextArea) {
            builder.append(((JTextArea) component).getText()).append('\n');
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                appendText(child, builder);
            }
        }
    }

    private static Component find(Component component, String name) {
        if (component == null) {
            return null;
        }
        if (name.equals(component.getName())) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component found = find(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) {
        try {
            SwingUtilities.invokeAndWait(action);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        finally {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (JFrame frame : FRAMES) {
                        frame.dispose();
                    }
                    FRAMES.clear();
                });
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
