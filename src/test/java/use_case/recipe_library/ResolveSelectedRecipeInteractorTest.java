package use_case.recipe_library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import entity.Ingredient;
import entity.Recipe;
import use_case.recipe_library.ResolveSelectedRecipeInteractor.Result;

class ResolveSelectedRecipeInteractorTest {

    @Test
    void savedSelectionReturnsTheStoredRecipeWithoutCallingTheGateway() throws Exception {
        Recipe saved = recipe("local-1", "Morning oats", "pantry/oats.jpg", 2);
        MemoryLibrary library = new MemoryLibrary();
        library.add(saved);
        FakeGateway gateway = new FakeGateway();

        Result result = new ResolveSelectedRecipeInteractor(library, gateway)
                .resolve(saved.getId(), null);

        assertEquals(saved.getId(), result.getRecipe().getId());
        assertEquals("Morning oats", result.getRecipe().getName());
        assertEquals("oats", result.getRecipe().getIngredients().get(0).getName());
        assertEquals(80.0, result.getRecipe().getIngredients().get(0).getQuantity());
        assertEquals(List.of("Simmer until soft"), result.getRecipe().getInstructions());
        assertEquals("pantry/oats.jpg", result.getRecipe().getImageUrl());
        assertEquals(2, result.getRecipe().getServings());
        assertNull(result.getError());
        assertEquals(0, gateway.lookups);
        assertEquals(0, library.saves);
    }

    @Test
    void missingSavedRecipeFails() {
        Result result = new ResolveSelectedRecipeInteractor(new MemoryLibrary(), new FakeGateway())
                .resolve("missing", "  ");

        assertNull(result.getRecipe());
        assertEquals(ResolveSelectedRecipeInteractor.SAVED_MISSING, result.getError());
    }

    @Test
    void unreadableSavedRecipeFails() {
        MemoryLibrary library = new MemoryLibrary();
        library.fail = true;

        Result result = new ResolveSelectedRecipeInteractor(library, new FakeGateway())
                .resolve("local-1", null);

        assertEquals(ResolveSelectedRecipeInteractor.SAVED_UNREADABLE, result.getError());
    }

    @Test
    void unsavedOnlineRecipeIsFetchedAndNotStored() throws Exception {
        OnlineRecipe online = penne();
        MemoryLibrary library = new MemoryLibrary();
        FakeGateway gateway = new FakeGateway();
        gateway.recipes.add(online);

        Result result = new ResolveSelectedRecipeInteractor(library, gateway)
                .resolve(online.recipeId(), online.getMealId());

        Recipe recipe = result.getRecipe();
        assertEquals("themealdb-52771", recipe.getId());
        assertEquals("Spicy Arrabiata Penne", recipe.getName());
        assertEquals("penne", recipe.getIngredients().get(0).getName());
        assertEquals(List.of("Boil the pasta."), recipe.getInstructions());
        assertEquals("https://example.com/penne.jpg", recipe.getImageUrl());
        assertNull(recipe.getServings());
        assertEquals(1, gateway.lookups);
        assertEquals(0, library.saves);
        assertTrue(library.findAll().isEmpty());
    }

    @Test
    void importedOnlineRecipePrefersTheSavedCopy() {
        OnlineRecipe online = penne();
        Recipe saved = online.toRecipe("pantry/penne.jpg");
        MemoryLibrary library = new MemoryLibrary();
        library.addImported(saved, online.getMealId());
        FakeGateway gateway = new FakeGateway();
        gateway.fail = true;

        Result result = new ResolveSelectedRecipeInteractor(library, gateway)
                .resolve(saved.getId(), online.getMealId());

        assertEquals("pantry/penne.jpg", result.getRecipe().getImageUrl());
        assertEquals(saved.getIngredients().get(0).getName(),
                result.getRecipe().getIngredients().get(0).getName());
        assertEquals(0, gateway.lookups);
    }

    @Test
    void missingOnlineRecipeFailsWithoutSaving() {
        FakeGateway gateway = new FakeGateway();

        Result result = new ResolveSelectedRecipeInteractor(new MemoryLibrary(), gateway)
                .resolve("themealdb-1", "1");

        assertEquals(ResolveSelectedRecipeInteractor.ONLINE_MISSING, result.getError());
        assertEquals(1, gateway.lookups);
    }

    @Test
    void onlineNetworkFailureIsReported() {
        FakeGateway gateway = new FakeGateway();
        gateway.fail = true;

        Result result = new ResolveSelectedRecipeInteractor(new MemoryLibrary(), gateway)
                .resolve("themealdb-52771", "52771");

        assertEquals("TheMealDB could not be reached. Check your connection and try again.",
                result.getError());
    }

    @Test
    void unreadableLibraryBlocksTheOnlineLookup() {
        MemoryLibrary library = new MemoryLibrary();
        library.fail = true;
        FakeGateway gateway = new FakeGateway();

        Result result = new ResolveSelectedRecipeInteractor(library, gateway)
                .resolve("themealdb-52771", "52771");

        assertEquals(ResolveSelectedRecipeInteractor.SAVED_UNREADABLE, result.getError());
        assertEquals(0, gateway.lookups);
    }

    private static Recipe recipe(String id, String name, String imageUrl, Integer servings) {
        return new Recipe(id, name,
                List.of(new Ingredient("oats", 80.0, "g")),
                List.of("Simmer until soft"),
                imageUrl,
                servings);
    }

    private static OnlineRecipe penne() {
        return new OnlineRecipe(
                "52771",
                "Spicy Arrabiata Penne",
                "Vegetarian",
                "https://example.com/penne.jpg",
                List.of(new Ingredient("penne", 1.0, "pound")),
                List.of("Boil the pasta."));
    }

    private static final class MemoryLibrary implements RecipeLibraryDataAccessInterface {

        private final Map<String, RecipeLibraryRecord> records = new LinkedHashMap<>();
        private boolean fail;
        private int saves;

        void add(Recipe recipe) {
            records.put(recipe.getId(), new RecipeLibraryRecord(
                    recipe, null, null, null, null));
        }

        void addImported(Recipe recipe, String sourceId) {
            records.put(recipe.getId(), new RecipeLibraryRecord(
                    recipe, "Vegetarian", sourceId, OnlineRecipe.SOURCE_NAME,
                    OnlineRecipe.ATTRIBUTION));
        }

        @Override
        public List<RecipeLibraryRecord> findAll() throws IOException {
            if (fail) {
                throw new IOException("unreadable");
            }
            return new ArrayList<>(records.values());
        }

        @Override
        public Optional<RecipeLibraryRecord> findById(String id) throws IOException {
            if (fail) {
                throw new IOException("unreadable");
            }
            return Optional.ofNullable(records.get(id));
        }

        @Override
        public boolean existsBySourceId(String sourceId) throws IOException {
            if (fail) {
                throw new IOException("unreadable");
            }
            for (RecipeLibraryRecord record : records.values()) {
                if (sourceId.equals(record.getSourceId())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void saveImported(Recipe recipe, String sourceId, String category, String attribution)
                throws IOException, DuplicateRecipeException {
            saves++;
        }
    }

    private static final class FakeGateway implements OnlineRecipeGateway {

        private final List<OnlineRecipe> recipes = new ArrayList<>();
        private boolean fail;
        private int lookups;

        @Override
        public List<OnlineRecipe> searchByName(String name) {
            return List.of();
        }

        @Override
        public Optional<OnlineRecipe> findByMealId(String mealId) throws IOException {
            lookups++;
            if (fail) {
                throw new IOException("offline");
            }
            for (OnlineRecipe recipe : recipes) {
                if (recipe.getMealId().equals(mealId)) {
                    return Optional.of(recipe);
                }
            }
            return Optional.empty();
        }
    }
}
