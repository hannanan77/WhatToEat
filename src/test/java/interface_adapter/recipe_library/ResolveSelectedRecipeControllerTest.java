package interface_adapter.recipe_library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import entity.Ingredient;
import entity.Recipe;
import use_case.recipe_library.DuplicateRecipeException;
import use_case.recipe_library.RecipeLibraryDataAccessInterface;
import use_case.recipe_library.RecipeLibraryRecord;
import use_case.recipe_library.OnlineRecipeGateway;
import use_case.recipe_library.ResolveSelectedRecipeInteractor;

class ResolveSelectedRecipeControllerTest {

    @Test
    void deliversTheRecipeOnTheEventDispatchThread() throws Exception {
        Recipe saved = new Recipe("local-1", "Morning oats",
                List.of(new Ingredient("oats", 80.0, "g")),
                List.of("Simmer until soft"),
                null,
                2);
        ResolveSelectedRecipeController controller = new ResolveSelectedRecipeController(
                new ResolveSelectedRecipeInteractor(libraryWith(saved), unusedGateway()));
        AtomicReference<Recipe> delivered = new AtomicReference<>();
        AtomicBoolean onEdt = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> controller.resolve(
                saved.getId(),
                null,
                recipe -> {
                    delivered.set(recipe);
                    onEdt.set(SwingUtilities.isEventDispatchThread());
                    latch.countDown();
                },
                message -> latch.countDown()));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("local-1", delivered.get().getId());
        assertEquals(List.of("Simmer until soft"), delivered.get().getInstructions());
        assertTrue(onEdt.get());
    }

    @Test
    void deliversAFailureOnTheEventDispatchThread() throws Exception {
        ResolveSelectedRecipeController controller = new ResolveSelectedRecipeController(
                new ResolveSelectedRecipeInteractor(libraryWith(null), unusedGateway()));
        AtomicReference<String> failure = new AtomicReference<>();
        AtomicBoolean onEdt = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> controller.resolve(
                "missing",
                null,
                recipe -> latch.countDown(),
                message -> {
                    failure.set(message);
                    onEdt.set(SwingUtilities.isEventDispatchThread());
                    latch.countDown();
                }));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("That saved recipe could not be loaded.", failure.get());
        assertTrue(onEdt.get());
    }

    private static RecipeLibraryDataAccessInterface libraryWith(Recipe recipe) {
        return new RecipeLibraryDataAccessInterface() {
            @Override
            public List<RecipeLibraryRecord> findAll() {
                return recipe == null
                        ? List.of()
                        : List.of(new RecipeLibraryRecord(recipe, null, null, null, null));
            }

            @Override
            public Optional<RecipeLibraryRecord> findById(String id) {
                if (recipe != null && recipe.getId().equals(id)) {
                    return Optional.of(new RecipeLibraryRecord(recipe, null, null, null, null));
                }
                return Optional.empty();
            }

            @Override
            public boolean existsBySourceId(String sourceId) {
                return false;
            }

            @Override
            public void saveImported(Recipe imported, String sourceId, String category,
                                     String attribution)
                    throws IOException, DuplicateRecipeException {
            }
        };
    }

    private static OnlineRecipeGateway unusedGateway() {
        return new OnlineRecipeGateway() {
            @Override
            public List<use_case.recipe_library.OnlineRecipe> searchByName(String name) {
                return List.of();
            }

            @Override
            public Optional<use_case.recipe_library.OnlineRecipe> findByMealId(
                    String mealId) {
                return Optional.empty();
            }
        };
    }
}
