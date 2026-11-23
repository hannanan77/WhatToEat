package view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import entity.Ingredient;
import entity.Recipe;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionPresenter;
import interface_adapter.view_nutrition.ViewNutritionViewModel;
import org.junit.jupiter.api.Test;
import use_case.view_nutrition.IngredientNutritionData;
import use_case.view_nutrition.NutritionDataAccessInterface;
import use_case.view_nutrition.ViewNutritionInputData;
import use_case.view_nutrition.ViewNutritionInteractor;
import view.components.ThemeScrollBarUI;

class ViewNutritionViewTest {

    private static final int MODAL_WIDTH = 1040;
    private static final int MODAL_HEIGHT = 740;

    @Test
    void completeNutritionHidesAllEstimateAndIncompleteUi() throws Exception {
        final FakeNutritionDataAccess dataAccess = completeDataAccess(7);
        final RenderedNutrition rendered = render(recipeWithGramIngredients(7), dataAccess);

        assertArrayEquals(new String[] {"700 kcal", "70 g", "140 g", "35 g"},
                rendered.viewModel.getTotalValues());
        assertArrayEquals(new String[] {"350 kcal", "35 g", "70 g", "17.5 g"},
                rendered.viewModel.getPerServingValues());
        assertTrue(rendered.viewModel.isTotalComplete());
        assertFalse(rendered.viewModel.containsEstimates());

        final JTable table = component(rendered.view, "nutrition-ingredient-table",
                JTable.class);
        assertEquals(7, table.getRowCount());
        for (int row = 0; row < table.getRowCount(); row++) {
            assertEquals("Available", table.getValueAt(row, 7));
            for (int column = 0; column < table.getColumnCount(); column++) {
                final String value = String.valueOf(table.getValueAt(row, column));
                assertFalse(value.contains("N/A"));
                assertFalse(value.contains("~"));
                assertFalse(value.contains("*"));
            }
        }

        final JPanel status = component(rendered.view, "nutrition-status-panel",
                JPanel.class);
        final Component statusGap = component(rendered.view, "nutrition-status-gap",
                Component.class);
        final JLabel calculationNote = component(rendered.view,
                "nutrition-calculation-note", JLabel.class);
        assertFalse(status.isVisible());
        assertFalse(statusGap.isVisible());
        assertFalse(calculationNote.isVisible());
        assertEquals("", calculationNote.getText());

        final RenderedNutrition partial = render(partialRecipe(), partialDataAccess());
        final JPanel completeSummary = component(rendered.view, "nutrition-summary",
                JPanel.class);
        final JPanel partialSummary = component(partial.view, "nutrition-summary",
                JPanel.class);
        assertTrue(completeSummary.getY() < partialSummary.getY(),
                "The summary should use the space released by the hidden warning.");
    }

    @Test
    void estimatedAndUnavailableRowsShowAccurateMarkersAndWarning() throws Exception {
        final RenderedNutrition rendered = render(partialRecipe(), partialDataAccess());
        final JTable table = component(rendered.view, "nutrition-ingredient-table",
                JTable.class);

        assertEquals("Available", table.getValueAt(0, 7));
        assertEquals("Estimated (weight)", table.getValueAt(1, 7));
        assertEquals("Not Available", table.getValueAt(2, 7));
        for (int column = 3; column <= 6; column++) {
            assertTrue(String.valueOf(table.getValueAt(1, column)).startsWith("~"));
            assertEquals("N/A", table.getValueAt(2, column));
        }
        for (String value : rendered.viewModel.getTotalValues()) {
            assertTrue(value.startsWith("~"));
            assertTrue(value.endsWith("*"));
            assertFalse(value.startsWith("~0"));
        }
        assertArrayEquals(new String[] {"~200 kcal*", "~20 g*", "~40 g*", "~10 g*"},
                rendered.viewModel.getTotalValues());
        assertTrue(rendered.viewModel.getStatusMessage()
                .contains("Estimated: Estimated Ingredient."));
        assertTrue(rendered.viewModel.getStatusMessage()
                .contains("Unavailable: Missing Ingredient."));

        final JPanel status = component(rendered.view, "nutrition-status-panel",
                JPanel.class);
        final JLabel title = component(rendered.view, "nutrition-status-title",
                JLabel.class);
        final JTextArea message = component(rendered.view, "nutrition-status-message",
                JTextArea.class);
        final JLabel calculationNote = component(rendered.view,
                "nutrition-calculation-note", JLabel.class);
        assertTrue(status.isVisible());
        assertEquals("Partial Nutritional Estimate", title.getText());
        assertTrue(message.getText().contains("estimates"));
        assertTrue(message.getText().contains("N/A"));
        assertTrue(calculationNote.isVisible());
        assertTrue(calculationNote.getText().contains("estimated"));
        assertTrue(calculationNote.getText().contains("unavailable"));
        assertFalse(rendered.viewModel.isTotalComplete());
    }

    @Test
    void completeEstimatedNutritionUsesAnEstimateNoticeButNotAPartialWarning()
            throws Exception {
        final FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("Measured Ingredient", completeNutrition());
        dataAccess.addNutrition("Estimated Ingredient", completeNutrition());
        dataAccess.addEstimatedWeight("Estimated Ingredient", 100.0);
        final Recipe recipe = recipe(List.of(
                new Ingredient("Measured Ingredient", 100.0, "g"),
                new Ingredient("Estimated Ingredient", 1.0, "piece")
        ));

        final RenderedNutrition rendered = render(recipe, dataAccess);
        final JLabel title = component(rendered.view, "nutrition-status-title",
                JLabel.class);
        final JLabel note = component(rendered.view, "nutrition-calculation-note",
                JLabel.class);

        assertTrue(rendered.viewModel.isTotalComplete());
        assertTrue(rendered.viewModel.containsEstimates());
        assertEquals("Estimated Nutrition", title.getText());
        assertFalse(title.getText().contains("Partial"));
        assertEquals("Some nutrition values are estimated.", note.getText());
        for (String value : rendered.viewModel.getTotalValues()) {
            assertTrue(value.startsWith("~"));
            assertFalse(value.contains("*"));
        }
    }

    @Test
    void partialNutritionWithoutEstimatesDoesNotClaimValuesWereEstimated()
            throws Exception {
        final FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("Available Ingredient", completeNutrition());
        final Recipe recipe = recipe(List.of(
                new Ingredient("Available Ingredient", 100.0, "g"),
                new Ingredient("Missing Ingredient", 100.0, "g")
        ));

        final RenderedNutrition rendered = render(recipe, dataAccess);
        final JLabel title = component(rendered.view, "nutrition-status-title",
                JLabel.class);
        final JTextArea message = component(rendered.view, "nutrition-status-message",
                JTextArea.class);
        final JLabel note = component(rendered.view, "nutrition-calculation-note",
                JLabel.class);

        assertEquals("Partial Nutrition Data", title.getText());
        assertTrue(message.getText().contains("N/A"));
        assertFalse(message.getText().toLowerCase(Locale.ROOT).contains("estimat"));
        assertTrue(note.getText().contains("unavailable"));
        assertFalse(note.getText().toLowerCase(Locale.ROOT).contains("estimat"));
    }

    @Test
    void loadingErrorAndRetryUseStableSummaryPlaceholders() throws Exception {
        final FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("Estimated Ingredient", completeNutrition());
        dataAccess.addEstimatedWeight("Estimated Ingredient", 100.0);
        final Recipe recipe = recipe(List.of(
                new Ingredient("Estimated Ingredient", 1.0, "piece")
        ));
        final RenderedNutrition rendered = render(recipe, dataAccess);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final CountDownLatch retryStarted = new CountDownLatch(1);
        final CountDownLatch releaseRetry = new CountDownLatch(1);
        final CountDownLatch retryFinished = new CountDownLatch(1);
        final AtomicInteger attemptCount = new AtomicInteger();
        final AtomicInteger loadingNotifications = new AtomicInteger();
        final ViewNutritionPresenter presenter =
                new ViewNutritionPresenter(rendered.viewModel);
        final ViewNutritionInteractor successfulInteractor =
                new ViewNutritionInteractor(dataAccess, presenter);
        final ViewNutritionController controller = new ViewNutritionController(inputData -> {
            if (attemptCount.incrementAndGet() == 1) {
                started.countDown();
                await(release);
                presenter.prepareFailView(inputData.getRequestId(),
                        "Nutrition service is unavailable.");
                finished.countDown();
            }
            else {
                retryStarted.countDown();
                await(releaseRetry);
                successfulInteractor.execute(inputData);
                retryFinished.countDown();
            }
        }, rendered.viewModel);
        rendered.view.setViewNutritionController(controller);
        rendered.viewModel.addPropertyChangeListener(event -> {
            if (rendered.viewModel.isLoading()) {
                loadingNotifications.incrementAndGet();
            }
        });

        try {
            onEventThread(() -> {
                rendered.view.requestNutrition();
                return null;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertHeaderNoteHidden(rendered.view);
            assertEquals("Calculating nutrition",
                    component(rendered.view, "nutrition-status-title", JLabel.class)
                            .getText());
            assertArrayEquals(new String[] {
                "Calculating...", "Calculating...", "Calculating...", "Calculating..."
            }, rendered.viewModel.getTotalValues());
            assertArrayEquals(new String[] {
                "Calculating...", "Calculating...", "Calculating...", "Calculating..."
            }, rendered.viewModel.getPerServingValues());
            assertEquals(8, countLabelsWithText(rendered.view, "Calculating..."));
            assertEquals(1, loadingNotifications.get());
            final JProgressBar loadingIndicator = component(rendered.view,
                    "nutrition-loading-indicator", JProgressBar.class);
            assertTrue(loadingIndicator.isVisible());
            assertFalse(loadingIndicator.isIndeterminate());

            release.countDown();
            assertTrue(finished.await(2, TimeUnit.SECONDS));
            onEventThread(() -> null);
            assertHeaderNoteHidden(rendered.view);
            assertEquals("Nutrition could not be loaded",
                    component(rendered.view, "nutrition-status-title", JLabel.class)
                            .getText());
            assertTrue(rendered.viewModel.isError());
            assertEquals(0, countLabelsWithText(rendered.view, "Calculating..."));
            assertEquals(8, countLabelsWithText(rendered.view, "Not Available"));

            onEventThread(() -> {
                component(rendered.view, "nutrition-retry-button", JButton.class)
                        .doClick();
                return null;
            });
            assertTrue(retryStarted.await(2, TimeUnit.SECONDS));
            assertEquals("Calculating nutrition",
                    component(rendered.view, "nutrition-status-title", JLabel.class)
                            .getText());
            assertEquals(8, countLabelsWithText(rendered.view, "Calculating..."));
            assertEquals(2, loadingNotifications.get());

            releaseRetry.countDown();
            assertTrue(retryFinished.await(2, TimeUnit.SECONDS));
            onEventThread(() -> null);
            assertFalse(rendered.viewModel.isLoading());
            assertFalse(rendered.viewModel.isError());
            assertEquals(0, countLabelsWithText(rendered.view, "Calculating..."));
            assertEquals(0, countLabelsWithText(rendered.view, "Not Available"));
        }
        finally {
            release.countDown();
            releaseRetry.countDown();
        }
    }

    @Test
    void ingredientTableScrollsOnlyWhenTheRecipeNeedsIt() throws Exception {
        final RenderedNutrition normal = render(recipeWithGramIngredients(7),
                completeDataAccess(7));
        final JScrollPane normalScroll = component(normal.view, "nutrition-table-scroll",
                JScrollPane.class);
        final JTable normalTable = component(normal.view, "nutrition-ingredient-table",
                JTable.class);
        final Rectangle normalView = normalScroll.getViewport().getViewRect();
        final Rectangle normalLastRow = normalTable.getCellRect(6, 0, true);

        assertEquals(7, normalTable.getRowCount());
        assertFalse(normalScroll.getVerticalScrollBar().isVisible());
        assertTrue(normalView.y + normalView.height
                >= normalLastRow.y + normalLastRow.height);

        final FakeNutritionDataAccess largeDataAccess = completeDataAccess(19);
        final RenderedNutrition large = render(recipeWithGramIngredients(20),
                largeDataAccess);
        final JScrollPane largeScroll = component(large.view, "nutrition-table-scroll",
                JScrollPane.class);
        final JTable largeTable = component(large.view, "nutrition-ingredient-table",
                JTable.class);
        final JPanel status = component(large.view, "nutrition-status-panel", JPanel.class);
        final JPanel summary = component(large.view, "nutrition-summary", JPanel.class);
        final Rectangle statusBounds = status.getBounds();
        final Rectangle summaryBounds = summary.getBounds();

        assertEquals(20, largeTable.getRowCount());
        assertTrue(status.isVisible());
        assertEquals("Not Available", largeTable.getValueAt(19, 7));
        assertTrue(largeScroll.isOpaque());
        assertTrue(largeScroll.getViewport().isOpaque());
        assertTrue(largeTable.isOpaque());
        assertEquals(JViewport.SIMPLE_SCROLL_MODE,
                largeScroll.getViewport().getScrollMode());
        assertTrue(largeScroll.getVerticalScrollBar().isVisible());
        assertTrue(largeScroll.getVerticalScrollBar().getUI() instanceof ThemeScrollBarUI);
        assertFalse(largeScroll.getHorizontalScrollBar().isVisible());
        assertTrue(largeScroll.getColumnHeader().isVisible());
        assertTrue(largeTable.getTableHeader().isVisible());
        assertEquals("Ingredient 20", largeTable.getValueAt(19, 0));

        onEventThread(() -> {
            final JScrollBar bar = largeScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum() - bar.getVisibleAmount());
            layoutTree(largeScroll);
            return null;
        });
        final Rectangle largeView = largeScroll.getViewport().getViewRect();
        final Rectangle largeLastRow = largeTable.getCellRect(19, 0, true);
        assertTrue(largeView.y + largeView.height
                >= largeLastRow.y + largeLastRow.height);
        assertEquals(statusBounds, status.getBounds());
        assertEquals(summaryBounds, summary.getBounds());
    }

    private static void assertHeaderNoteHidden(ViewNutritionView view) throws Exception {
        final JLabel note = component(view, "nutrition-calculation-note", JLabel.class);
        assertFalse(note.isVisible());
        assertEquals("", note.getText());
    }

    private static RenderedNutrition render(Recipe recipe,
                                              FakeNutritionDataAccess dataAccess)
            throws Exception {
        final RenderedNutrition rendered = onEventThread(() -> {
            final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
            final ViewNutritionView view = new ViewNutritionView(viewModel);
            final ViewNutritionPresenter presenter =
                    new ViewNutritionPresenter(viewModel);
            final ViewNutritionInteractor interactor =
                    new ViewNutritionInteractor(dataAccess, presenter);
            view.setSelectedRecipe(recipe);
            interactor.execute(new ViewNutritionInputData(0L, recipe));
            return new RenderedNutrition(view, viewModel);
        });
        onEventThread(() -> {
            rendered.view.setSize(MODAL_WIDTH, MODAL_HEIGHT);
            layoutTree(rendered.view);
            return null;
        });
        return rendered;
    }

    private static Recipe partialRecipe() {
        return recipe(List.of(
                new Ingredient("Available Ingredient", 100.0, "g"),
                new Ingredient("Estimated Ingredient", 1.0, "piece"),
                new Ingredient("Missing Ingredient", 100.0, "g")
        ));
    }

    private static FakeNutritionDataAccess partialDataAccess() {
        final FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        dataAccess.addNutrition("Available Ingredient", completeNutrition());
        dataAccess.addNutrition("Estimated Ingredient", completeNutrition());
        dataAccess.addEstimatedWeight("Estimated Ingredient", 100.0);
        return dataAccess;
    }

    private static Recipe recipeWithGramIngredients(int count) {
        final List<Ingredient> ingredients = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            ingredients.add(new Ingredient("Ingredient " + index, 100.0, "g"));
        }
        return recipe(ingredients);
    }

    private static Recipe recipe(List<Ingredient> ingredients) {
        return new Recipe("nutrition-view-test", "Nutrition View Test", ingredients,
                List.of("Cook the ingredients."), null, 2);
    }

    private static FakeNutritionDataAccess completeDataAccess(int count) {
        final FakeNutritionDataAccess dataAccess = new FakeNutritionDataAccess();
        for (int index = 1; index <= count; index++) {
            dataAccess.addNutrition("Ingredient " + index, completeNutrition());
        }
        return dataAccess;
    }

    private static IngredientNutritionData completeNutrition() {
        return new IngredientNutritionData(100.0, 10.0, 20.0, 5.0,
                false, null);
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layoutTree((Container) child);
            }
        }
    }

    private static <T extends Component> T component(Container root, String name,
                                                       Class<T> componentType) {
        final Component found = findComponent(root, name);
        assertNotNull(found, "Missing component named " + name);
        assertTrue(componentType.isInstance(found),
                "Component " + name + " has the wrong type.");
        return componentType.cast(found);
    }

    private static Component findComponent(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container) {
                final Component found = findComponent((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int countLabelsWithText(Container root, String text) {
        int count = 0;
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText())) {
                count++;
            }
            if (child instanceof Container) {
                count += countLabelsWithText((Container) child, text);
            }
        }
        return count;
    }

    private static <T> T onEventThread(Callable<T> action) throws Exception {
        final FutureTask<T> task = new FutureTask<>(action);
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            SwingUtilities.invokeAndWait(task);
        }
        return task.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RenderedNutrition {
        private final ViewNutritionView view;
        private final ViewNutritionViewModel viewModel;

        private RenderedNutrition(ViewNutritionView view,
                                  ViewNutritionViewModel viewModel) {
            this.view = view;
            this.viewModel = viewModel;
        }
    }

    private static final class FakeNutritionDataAccess
            implements NutritionDataAccessInterface {
        private final Map<String, IngredientNutritionData> nutritionByName =
                new HashMap<>();
        private final Map<String, Double> estimatedWeightByName = new HashMap<>();

        private void addNutrition(String ingredientName,
                                  IngredientNutritionData nutrition) {
            nutritionByName.put(key(ingredientName), nutrition);
        }

        private void addEstimatedWeight(String ingredientName, double weightInGrams) {
            estimatedWeightByName.put(key(ingredientName), weightInGrams);
        }

        @Override
        public Optional<IngredientNutritionData> findNutrition(String ingredientName) {
            return Optional.ofNullable(nutritionByName.get(key(ingredientName)));
        }

        @Override
        public Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
            return Optional.ofNullable(estimatedWeightByName.get(
                    key(ingredient.getName())));
        }

        private static String key(String value) {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
