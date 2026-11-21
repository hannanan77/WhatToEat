package interface_adapter.recipe_library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import use_case.recipe_library.SearchOnlineRecipesInputBoundary;
import use_case.recipe_library.SearchOnlineRecipesOutputData;

class SearchOnlineRecipesControllerTest {

    @Test
    void anOlderSearchCannotReplaceANewerResult() throws Exception {
        RecipeBrowserViewModel viewModel = new RecipeBrowserViewModel();
        SearchOnlineRecipesPresenter presenter = new SearchOnlineRecipesPresenter(viewModel);
        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        CountDownLatch fastPublished = new CountDownLatch(1);
        CountDownLatch slowPublished = new CountDownLatch(1);
        AtomicInteger publications = new AtomicInteger();

        SearchOnlineRecipesInputBoundary interactor = input -> {
            String query = input.getQuery();
            if ("slow".equals(query)) {
                slowStarted.countDown();
                await(releaseSlow);
            }
            presenter.prepareSuccessView(new SearchOnlineRecipesOutputData(query, List.of()));
            publications.incrementAndGet();
            if ("fast".equals(query)) {
                fastPublished.countDown();
            }
            else {
                slowPublished.countDown();
            }
        };
        SearchOnlineRecipesController controller = new SearchOnlineRecipesController(
                interactor, presenter, viewModel);

        SwingUtilities.invokeLater(() -> controller.execute("slow"));
        assertTrue(slowStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeLater(() -> controller.execute("fast"));
        assertTrue(fastPublished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });

        releaseSlow.countDown();
        assertTrue(slowPublished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(2, publications.get());
        assertEquals("No recipes found for \"fast\".", viewModel.getStatus());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Search did not finish.");
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
