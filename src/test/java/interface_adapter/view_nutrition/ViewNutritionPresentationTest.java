package interface_adapter.view_nutrition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import entity.Ingredient;
import entity.Recipe;
import use_case.view_nutrition.ViewNutritionInputBoundary;
import use_case.view_nutrition.ViewNutritionOutputData;
import use_case.view_nutrition.ViewNutritionOutputData.DataStatus;
import use_case.view_nutrition.ViewNutritionOutputData.IngredientBreakdown;
import use_case.view_nutrition.ViewNutritionOutputData.NutritionValues;

class ViewNutritionPresentationTest {

    @Test
    void presenterKeepsQuantityAndUnitInSeparateColumns() {
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionPresenter presenter = new ViewNutritionPresenter(viewModel);

        presenter.prepareSuccessView(sampleOutput());

        final String[] row = viewModel.getIngredientRows().get(0);
        assertEquals(8, row.length);
        assertEquals("2", row[1]);
        assertEquals("tbsp", row[2]);
        assertEquals("Estimated (weight)", row[7]);
        assertTrue(viewModel.containsEstimates());
        assertFalse(viewModel.isLoading());
        assertTrue(viewModel.getStatusMessage().contains("Estimated: Olive Oil."));
        assertFalse(viewModel.getStatusMessage().contains("uses an estimated weight"));
    }

    @Test
    void errorStateClearsPreviouslyDisplayedNutrition() {
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionPresenter presenter = new ViewNutritionPresenter(viewModel);
        presenter.prepareSuccessView(sampleOutput());

        presenter.prepareFailView(0L,
                "Nutrition information could not be retrieved.");

        assertTrue(viewModel.isError());
        assertFalse(viewModel.isDataAvailable());
        assertFalse(viewModel.containsEstimates());
        assertTrue(viewModel.getIngredientRows().isEmpty());
        assertEquals("Not Available", viewModel.getTotalValues()[0]);
    }

    @Test
    void controllerRunsTheInteractorAwayFromTheEventThread() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean ranOnEventThread = new AtomicBoolean(true);
        final ViewNutritionInputBoundary inputBoundary = inputData -> {
            ranOnEventThread.set(SwingUtilities.isEventDispatchThread());
            started.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionController controller =
                new ViewNutritionController(inputBoundary, viewModel);
        final Recipe recipe = new Recipe("recipe-1", "Test Recipe",
                Collections.singletonList(new Ingredient("Olive Oil", 2.0, "tbsp")),
                Collections.singletonList("Mix the ingredients."), null, 4);

        try {
            SwingUtilities.invokeAndWait(() -> controller.execute(recipe));
            assertTrue(viewModel.isLoading());
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(ranOnEventThread.get());
        }
        finally {
            release.countDown();
        }
    }

    @Test
    void olderResultCannotOverwriteNewerRecipe() throws Exception {
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch firstFinished = new CountDownLatch(1);
        final CountDownLatch secondFinished = new CountDownLatch(1);
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionPresenter presenter = new ViewNutritionPresenter(viewModel);
        final ViewNutritionInputBoundary inputBoundary = inputData -> {
            if ("Recipe A".equals(inputData.getRecipe().getName())) {
                firstStarted.countDown();
                await(releaseFirst);
                presenter.prepareSuccessView(sampleOutput(
                        inputData.getRequestId(), "Recipe A"));
                firstFinished.countDown();
            }
            else {
                presenter.prepareSuccessView(sampleOutput(
                        inputData.getRequestId(), "Recipe B"));
                secondFinished.countDown();
            }
        };
        final ViewNutritionController controller =
                new ViewNutritionController(inputBoundary, viewModel);

        try {
            SwingUtilities.invokeAndWait(() -> controller.execute(recipe("Recipe A")));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> controller.execute(recipe("Recipe B")));
            assertTrue(secondFinished.await(2, TimeUnit.SECONDS));
            assertEquals("Recipe B", viewModel.getRecipeName());

            releaseFirst.countDown();
            assertTrue(firstFinished.await(2, TimeUnit.SECONDS));
            assertEquals("Recipe B", viewModel.getRecipeName());
            assertFalse(viewModel.isError());
        }
        finally {
            releaseFirst.countDown();
        }
    }

    @Test
    void closeAndReopenRejectsFailureFromClosedRequest() throws Exception {
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch firstFinished = new CountDownLatch(1);
        final CountDownLatch secondFinished = new CountDownLatch(1);
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionPresenter presenter = new ViewNutritionPresenter(viewModel);
        final ViewNutritionInputBoundary inputBoundary = inputData -> {
            if ("Recipe A".equals(inputData.getRecipe().getName())) {
                firstStarted.countDown();
                await(releaseFirst);
                presenter.prepareFailView(inputData.getRequestId(), "Old request failed.");
                firstFinished.countDown();
            }
            else {
                presenter.prepareSuccessView(sampleOutput(
                        inputData.getRequestId(), "Recipe B"));
                secondFinished.countDown();
            }
        };
        final ViewNutritionController controller =
                new ViewNutritionController(inputBoundary, viewModel);

        try {
            SwingUtilities.invokeAndWait(() -> controller.execute(recipe("Recipe A")));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(controller::cancelCurrentRequest);
            SwingUtilities.invokeAndWait(() -> controller.execute(recipe("Recipe B")));
            assertTrue(secondFinished.await(2, TimeUnit.SECONDS));

            releaseFirst.countDown();
            assertTrue(firstFinished.await(2, TimeUnit.SECONDS));
            assertEquals("Recipe B", viewModel.getRecipeName());
            assertTrue(viewModel.isDataAvailable());
            assertFalse(viewModel.isError());
        }
        finally {
            releaseFirst.countDown();
        }
    }

    private static ViewNutritionOutputData sampleOutput() {
        return sampleOutput(0L, "Test Recipe");
    }

    private static ViewNutritionOutputData sampleOutput(long requestId,
                                                         String recipeName) {
        final NutritionValues total = new NutritionValues(200.0, 4.0, 8.0, 18.0);
        final NutritionValues perServing = new NutritionValues(100.0, 2.0, 4.0, 9.0);
        final IngredientBreakdown ingredient = new IngredientBreakdown(
                "Olive Oil", 2.0, "tbsp", total, DataStatus.ESTIMATED,
                true, false, "Olive Oil uses an estimated weight.");
        return new ViewNutritionOutputData(
                requestId, "recipe-1", recipeName, 2, total, perServing,
                Collections.singletonList(ingredient),
                Collections.singletonList("Olive Oil uses an estimated weight."),
                true, true, true);
    }

    private static Recipe recipe(String name) {
        return new Recipe("recipe-" + name, name,
                Collections.singletonList(new Ingredient("Olive Oil", 2.0, "tbsp")),
                Collections.singletonList("Mix the ingredients."), null, 4);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
