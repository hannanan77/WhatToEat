package use_case.search_online_recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileRecipeLibraryDataAccess;
import entity.Ingredient;
import use_case.browse_saved_recipes.BrowseSavedRecipesInteractor;
import use_case.browse_saved_recipes.BrowseSavedRecipesOutputBoundary;
import use_case.browse_saved_recipes.BrowseSavedRecipesOutputData;
import use_case.import_recipe.ImportRecipeInteractor;
import use_case.import_recipe.ImportRecipeOutputBoundary;
import use_case.import_recipe.ImportRecipeOutputData;
import use_case.online_recipe.OnlineRecipe;
import use_case.online_recipe.OnlineRecipeGateway;
import use_case.recipe_library.RecipeImageCache;
import use_case.recipe_library.RecipeLibraryRecord;

class RecipeSearchWorkflowTest {

    @TempDir
    Path tempDir;

    @Test
    void emptySearchDoesNotCallTheGateway() {
        FakeGateway gateway = new FakeGateway();
        RecordingSearch presenter = new RecordingSearch();
        SearchOnlineRecipesInteractor interactor = new SearchOnlineRecipesInteractor(
                gateway, library(), presenter);

        interactor.execute(new SearchOnlineRecipesInputData("   "));

        assertEquals(0, gateway.searches);
        assertEquals("Enter a recipe name.", presenter.failure);
    }

    @Test
    void emptyApiResultsAreReportedWithoutSaving() {
        FakeGateway gateway = new FakeGateway();
        RecordingSearch presenter = new RecordingSearch();
        SearchOnlineRecipesInteractor interactor = new SearchOnlineRecipesInteractor(
                gateway, library(), presenter);

        interactor.execute(new SearchOnlineRecipesInputData("missing"));

        assertEquals(1, gateway.searches);
        assertTrue(presenter.success.getMatches().isEmpty());
    }

    @Test
    void networkFailureIsReportedAndNothingIsSaved() {
        FakeGateway gateway = new FakeGateway();
        gateway.fail = true;
        FileRecipeLibraryDataAccess library = library();
        RecordingSearch presenter = new RecordingSearch();
        new SearchOnlineRecipesInteractor(gateway, library, presenter)
                .execute(new SearchOnlineRecipesInputData("pasta"));

        assertEquals("TheMealDB could not be reached. Check your connection and try again.",
                presenter.failure);
        assertTrue(read(library).isEmpty());
    }

    @Test
    void importSavesOnceAndTheCollectionReloadsOffline() throws Exception {
        OnlineRecipe recipe = penne();
        FakeGateway gateway = new FakeGateway();
        gateway.results.add(recipe);
        FileRecipeLibraryDataAccess library = library();
        FakeImageCache images = new FakeImageCache();
        images.path = tempDir.resolve("cached.jpg").toString();
        RecordingImport importer = new RecordingImport();
        new ImportRecipeInteractor(gateway, library, images, importer)
                .execute(new use_case.import_recipe.ImportRecipeInputData(recipe.getMealId()));

        assertEquals("themealdb-52771", importer.success.getRecipeId());
        assertEquals(images.path, read(library).get(0).getRecipe().getImageUrl());

        RecordingImport duplicate = new RecordingImport();
        new ImportRecipeInteractor(gateway, library, images, duplicate)
                .execute(new use_case.import_recipe.ImportRecipeInputData("52771"));
        assertEquals("This recipe is already in your collection.", duplicate.failure);

        RecordingBrowse browse = new RecordingBrowse();
        new BrowseSavedRecipesInteractor(new FileRecipeLibraryDataAccess(
                tempDir.resolve("recipes.json")), browse).execute();
        assertEquals(1, browse.success.getRecipes().size());
        assertEquals(OnlineRecipe.ATTRIBUTION,
                browse.success.getRecipes().get(0).getAttribution());
        assertFalse(browse.success.getRecipes().get(0).getRecipe().getInstructions().isEmpty());
    }

    @Test
    void searchMarksARecipeThatWasAlreadyImported() throws Exception {
        OnlineRecipe recipe = penne();
        FileRecipeLibraryDataAccess library = library();
        library.saveImported(recipe.toRecipe(null), recipe.getMealId(),
                recipe.getCategory(), OnlineRecipe.ATTRIBUTION);
        FakeGateway gateway = new FakeGateway();
        gateway.results.add(recipe);
        RecordingSearch presenter = new RecordingSearch();

        new SearchOnlineRecipesInteractor(gateway, library, presenter)
                .execute(new SearchOnlineRecipesInputData("penne"));

        assertTrue(presenter.success.getMatches().get(0).isAlreadyImported());
    }

    private FileRecipeLibraryDataAccess library() {
        return new FileRecipeLibraryDataAccess(tempDir.resolve("recipes.json"));
    }

    private static List<RecipeLibraryRecord> read(FileRecipeLibraryDataAccess library) {
        try {
            return library.findAll();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
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

    private static final class FakeGateway implements OnlineRecipeGateway {
        private final List<OnlineRecipe> results = new ArrayList<>();
        private boolean fail;
        private int searches;

        @Override
        public List<OnlineRecipe> searchByName(String name) throws IOException {
            searches++;
            if (fail) {
                throw new IOException("offline");
            }
            return results;
        }

        @Override
        public Optional<OnlineRecipe> findByMealId(String mealId) {
            for (OnlineRecipe recipe : results) {
                if (recipe.getMealId().equals(mealId)) {
                    return Optional.of(recipe);
                }
            }
            return Optional.empty();
        }
    }

    private static final class FakeImageCache implements use_case.recipe_library.RecipeImageCache {
        private String path;

        @Override
        public String cache(String recipeId, String remoteUrl) {
            return path;
        }
    }

    private static final class RecordingSearch implements SearchOnlineRecipesOutputBoundary {
        private SearchOnlineRecipesOutputData success;
        private String failure;

        @Override
        public void prepareSuccessView(SearchOnlineRecipesOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failure = errorMessage;
        }
    }

    private static final class RecordingImport implements ImportRecipeOutputBoundary {
        private ImportRecipeOutputData success;
        private String failure;

        @Override
        public void prepareSuccessView(ImportRecipeOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failure = errorMessage;
        }
    }

    private static final class RecordingBrowse implements BrowseSavedRecipesOutputBoundary {
        private BrowseSavedRecipesOutputData success;

        @Override
        public void prepareSuccessView(BrowseSavedRecipesOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            throw new AssertionError(errorMessage);
        }
    }
}
