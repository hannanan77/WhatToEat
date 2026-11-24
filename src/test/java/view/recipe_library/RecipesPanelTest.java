package view.recipe_library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import entity.Ingredient;
import entity.Recipe;
import interface_adapter.recipe_library.RecipeDisplayData;

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
    void fileUriImagesLoadLikeLocalPaths() throws Exception {
        File imageFile = File.createTempFile("recipe-uri", ".png");
        imageFile.deleteOnExit();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", imageFile);
        String fileUri = imageFile.toURI().toString();

        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            panel.setRecipes(List.of(recipe("photo", "Plated dish", null, 2,
                    fileUri, List.of("Greens"), List.of("Plate"))));
            click(find(panel, "recipe-card-photo"));

            JLabel image = label(panel, "recipe-detail-image-message");
            assertNotNull(image.getIcon());
            assertTrue(image.getText() == null || image.getText().isEmpty());
        });
    }

    @Test
    void onlineResultOffersImportWithoutSavingByItself() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            String[] imported = new String[1];
            panel.setImportAction(externalId -> imported[0] = externalId);
            panel.setRecipes(List.of(new RecipeDisplayData(
                    "themealdb-52771",
                    "Spicy Arrabiata Penne",
                    "Vegetarian",
                    null,
                    null,
                    List.of("1 pound penne"),
                    List.of("Boil the pasta."),
                    "Recipe data from TheMealDB (https://www.themealdb.com).",
                    "52771",
                    true)));

            click(find(panel, "recipe-card-themealdb-52771"));
            assertEquals("Recipe data from TheMealDB (https://www.themealdb.com).",
                    label(panel, "recipe-detail-attribution").getText());
            ((JButton) find(panel, "recipe-import-button")).doClick();
            assertEquals("52771", imported[0]);
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

    @Test
    void resolvedSelectionPublishesTheCompleteRecipeAndClearsWhenItIsRemoved() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            List<Recipe> selected = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int[] clears = {0};
            panel.setRecipeSelectionAction(selected::add);
            panel.setRecipeSelectionFailedAction(failures::add);
            panel.setRecipeSelectionClearedAction(() -> clears[0]++);
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onSuccess.accept(entity(recipeId)));
            panel.setRecipes(List.of(recipe("oats", "Morning oats", "Breakfast", 2, null,
                    List.of("80 g oats"), List.of("Simmer until soft"))));

            click(find(panel, "recipe-card-oats"));

            assertEquals(1, clears[0]);
            assertTrue(failures.isEmpty());
            assertEquals("oats", selected.get(0).getId());
            assertEquals("Morning oats", selected.get(0).getName());
            assertEquals(1, selected.get(0).getIngredients().size());
            assertEquals("Simmer until soft", selected.get(0).getInstructions().get(0));

            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", "Breakfast", 2, null,
                            List.of("80 g oats"), List.of("Simmer until soft")),
                    recipe("bread", "Brown bread", "Bakery", 1, null,
                            List.of("Flour"), List.of("Bake"))));
            assertEquals(1, clears[0]);
            assertEquals(EditorialTheme.CARD_SELECTED,
                    find(panel, "recipe-card-oats").getBackground());

            panel.setRecipes(null);
            assertEquals(2, clears[0]);
            assertEquals(1, selected.size());
            assertEquals("Select a recipe to explore.",
                    label(panel, "recipes-detail-message").getText());
        });
    }

    @Test
    void onlineSelectionSuppliesTheMealIdAndAFailureDoesNotPublishARecipe() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            List<Recipe> selected = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            String[] mealId = new String[1];
            panel.setRecipeSelectionAction(selected::add);
            panel.setRecipeSelectionFailedAction(failures::add);
            panel.setRecipeResolver((recipeId, externalMealId, onSuccess, onFailure) -> {
                mealId[0] = externalMealId;
                onFailure.accept("That recipe is no longer available.");
            });
            panel.setRecipes(List.of(new RecipeDisplayData(
                    "themealdb-52771",
                    "Spicy Arrabiata Penne",
                    "Vegetarian",
                    null,
                    null,
                    List.of("1 pound penne"),
                    List.of("Boil the pasta."),
                    "Recipe data from TheMealDB (https://www.themealdb.com).",
                    "52771",
                    true)));

            click(find(panel, "recipe-card-themealdb-52771"));

            assertEquals("52771", mealId[0]);
            assertTrue(selected.isEmpty());
            assertEquals(List.of("That recipe is no longer available."), failures);
        });
    }

    @Test
    void anOlderLookupCannotReplaceANewerSelection() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            List<Recipe> selected = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            List<Runnable> pending = new ArrayList<>();
            panel.setRecipeSelectionAction(selected::add);
            panel.setRecipeSelectionFailedAction(failures::add);
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    pending.add(() -> onSuccess.accept(entity(recipeId))));
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", null, 1, null,
                            List.of("Oats"), List.of("Cook")),
                    recipe("salad", "Herb salad", null, 1, null,
                            List.of("Herbs"), List.of("Toss"))));

            click(find(panel, "recipe-card-oats"));
            click(find(panel, "recipe-card-salad"));
            pending.get(0).run();
            assertTrue(selected.isEmpty());
            pending.get(1).run();
            assertEquals("salad", selected.get(0).getId());

            pending.clear();
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    pending.add(() -> onFailure.accept("offline " + recipeId)));
            click(find(panel, "recipe-card-oats"));
            click(find(panel, "recipe-card-salad"));
            pending.get(0).run();
            assertTrue(failures.isEmpty());
            pending.get(1).run();
            assertEquals(List.of("offline salad"), failures);
            assertEquals(1, selected.size());
        });
    }

    @Test
    void viewNutritionStaysDisabledUntilTheSelectedRecipeResolves() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            JButton nutrition = nutritionButton(panel);
            assertEquals("View Nutrition", nutrition.getText());
            assertFalse(nutrition.isEnabled());

            panel.setRecipes(List.of(recipe("oats", "Morning oats", "Breakfast", 2, null,
                    List.of("80 g oats"), List.of("Simmer until soft"))));
            click(find(panel, "recipe-card-oats"));
            assertFalse(nutritionButton(panel).isEnabled());

            List<Runnable> pending = new ArrayList<>();
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    pending.add(() -> onSuccess.accept(entity(recipeId))));
            click(find(panel, "recipe-card-oats"));
            nutrition = nutritionButton(panel);
            assertFalse(nutrition.isEnabled());

            pending.get(0).run();
            assertTrue(nutrition.isEnabled());
        });
    }

    @Test
    void viewNutritionClickPassesTheResolvedRecipeAndTheButton() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            Recipe oats = entity("oats", "Morning oats");
            Recipe[] opened = new Recipe[1];
            Component[] source = new Component[1];
            panel.setViewNutritionAction((recipe, button) -> {
                opened[0] = recipe;
                source[0] = button;
            });
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onSuccess.accept(oats));
            panel.setRecipes(List.of(recipe("oats", "Morning oats", "Breakfast", 2, null,
                    List.of("80 g oats"), List.of("Simmer until soft"))));

            click(find(panel, "recipe-card-oats"));
            JButton nutrition = nutritionButton(panel);
            assertTrue(nutrition.isEnabled());
            nutrition.doClick();

            assertSame(oats, opened[0]);
            assertSame(nutrition, source[0]);
        });
    }

    @Test
    void changingOrClearingTheSelectionDisablesViewNutrition() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            Recipe oats = entity("oats", "Morning oats");
            List<Runnable> pending = new ArrayList<>();
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onSuccess.accept(oats));
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", null, 1, null,
                            List.of("Oats"), List.of("Cook")),
                    recipe("salad", "Herb salad", null, 1, null,
                            List.of("Herbs"), List.of("Toss"))));

            click(find(panel, "recipe-card-oats"));
            assertTrue(nutritionButton(panel).isEnabled());

            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    pending.add(() -> onSuccess.accept(entity(recipeId, "Herb salad"))));
            click(find(panel, "recipe-card-salad"));
            assertFalse(nutritionButton(panel).isEnabled());

            pending.get(0).run();
            assertTrue(nutritionButton(panel).isEnabled());

            List<Recipe> opened = new ArrayList<>();
            panel.setViewNutritionAction((recipe, button) -> opened.add(recipe));
            panel.setRecipes(null);
            JButton nutrition = nutritionButton(panel);
            assertFalse(nutrition.isEnabled());
            nutrition.doClick();
            assertTrue(opened.isEmpty());
        });
    }

    @Test
    void failedResolutionCannotOpenAPreviouslySelectedRecipe() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            Recipe oats = entity("oats", "Morning oats");
            List<Recipe> opened = new ArrayList<>();
            panel.setViewNutritionAction((recipe, button) -> opened.add(recipe));
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onSuccess.accept(oats));
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", null, 1, null,
                            List.of("Oats"), List.of("Cook")),
                    recipe("salad", "Herb salad", null, 1, null,
                            List.of("Herbs"), List.of("Toss"))));

            click(find(panel, "recipe-card-oats"));
            nutritionButton(panel).doClick();
            assertSame(oats, opened.get(0));

            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onFailure.accept("That recipe is no longer available."));
            click(find(panel, "recipe-card-salad"));
            JButton nutrition = nutritionButton(panel);
            assertFalse(nutrition.isEnabled());
            nutrition.doClick();
            assertEquals(1, opened.size());
            assertSame(oats, opened.get(0));
        });
    }

    @Test
    void aStaleResolutionCannotEnableViewNutritionOrOpenAnOldRecipe() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            Recipe oats = entity("oats", "Morning oats");
            Recipe salad = entity("salad", "Herb salad");
            List<Recipe> opened = new ArrayList<>();
            List<Runnable> pending = new ArrayList<>();
            panel.setViewNutritionAction((recipe, button) -> opened.add(recipe));
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    pending.add(() -> onSuccess.accept(
                            "salad".equals(recipeId) ? salad : oats)));
            panel.setRecipes(List.of(
                    recipe("oats", "Morning oats", null, 1, null,
                            List.of("Oats"), List.of("Cook")),
                    recipe("salad", "Herb salad", null, 1, null,
                            List.of("Herbs"), List.of("Toss"))));

            click(find(panel, "recipe-card-oats"));
            click(find(panel, "recipe-card-salad"));
            JButton nutrition = nutritionButton(panel);
            assertFalse(nutrition.isEnabled());

            pending.get(0).run();
            assertFalse(nutrition.isEnabled());
            nutrition.doClick();
            assertTrue(opened.isEmpty());

            pending.get(1).run();
            assertTrue(nutrition.isEnabled());
            nutrition.doClick();
            assertEquals(1, opened.size());
            assertSame(salad, opened.get(0));

            pending.get(0).run();
            nutrition.doClick();
            assertEquals(2, opened.size());
            assertSame(salad, opened.get(1));
        });
    }

    @Test
    void viewNutritionClickWithoutACallbackDoesNotThrow() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            nutritionButton(panel).doClick();

            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) ->
                    onSuccess.accept(entity(recipeId)));
            panel.setRecipes(List.of(recipe("oats", "Morning oats", "Breakfast", 1, null,
                    List.of("Oats"), List.of("Cook"))));
            click(find(panel, "recipe-card-oats"));
            nutritionButton(panel).doClick();

            panel.setViewNutritionAction(null);
            nutritionButton(panel).doClick();
        });
    }

    @Test
    void emptyFramesDoNotResolveARecipe() {
        onEdt(() -> {
            RecipesPanel panel = layout(new RecipesPanel());
            int[] calls = {0};
            panel.showEmptyCardFrames(2);
            panel.setRecipeResolver((recipeId, mealId, onSuccess, onFailure) -> calls[0]++);
            click(find(panel, "recipe-card-frame-1"));
            assertEquals(0, calls[0]);
            assertNull(find(panel, "recipe-detail-name"));
        });
    }

    private static JButton nutritionButton(Component root) {
        return (JButton) find(root, "recipe-view-nutrition");
    }

    private static Recipe entity(String id) {
        return entity(id, "Morning oats");
    }

    private static Recipe entity(String id, String name) {
        return new Recipe(id, name,
                List.of(new Ingredient("oats", 80.0, "g")),
                List.of("Simmer until soft"),
                null,
                2);
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
