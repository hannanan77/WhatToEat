package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.Ingredient;
import entity.Recipe;
import recipe_library.data_access.IngredientMeasureParser;
import recipe_library.data_access.LocalRecipeImageCache;
import recipe_library.data_access.TheMealDbRecipeMapper;
import recipe_library.use_case.library.DuplicateRecipeException;
import recipe_library.use_case.library.RecipeLibraryRecord;
import recipe_library.use_case.online_recipe.OnlineRecipe;
import use_case.add_recipe.AddRecipeDataAccessInterface;

class RecipeImportDataTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesMissingAndFractionalMeasurements() {
        assertMeasure(null, "unspecified", IngredientMeasureParser.parse(null));
        assertMeasure(null, "unspecified", IngredientMeasureParser.parse("   "));
        assertMeasure(0.75, "cup", IngredientMeasureParser.parse("3/4 cup"));
        assertMeasure(1.5, "tbsp", IngredientMeasureParser.parse("1 1/2 tbsp"));
        assertMeasure(200.0, "g", IngredientMeasureParser.parse("200g"));
        assertMeasure(null, "to taste", IngredientMeasureParser.parse("to taste"));
    }

    @Test
    void mapsAMealAndSkipsInvalidApiData() throws IOException {
        String json = "{"
                + "\"meals\":["
                + "{"
                + "\"idMeal\":\"52771\","
                + "\"strMeal\":\"Spicy Arrabiata Penne\","
                + "\"strCategory\":\"Vegetarian\","
                + "\"strMealThumb\":\"https://example.com/penne.jpg\","
                + "\"strInstructions\":\"Boil the pasta.\\n\\nToss with sauce.\","
                + "\"strIngredient1\":\"penne\","
                + "\"strMeasure1\":\"1 pound\","
                + "\"strIngredient2\":\"olive oil\","
                + "\"strMeasure2\":\"\","
                + "\"strIngredient3\":\"\","
                + "\"strMeasure3\":\"1 cup\""
                + "},"
                + "{"
                + "\"idMeal\":\"1\","
                + "\"strMeal\":\"Broken meal\","
                + "\"strInstructions\":\"\","
                + "\"strIngredient1\":\"salt\","
                + "\"strMeasure1\":\"1 tsp\""
                + "}"
                + "]}";

        List<OnlineRecipe> recipes = TheMealDbRecipeMapper.mapSearchResponse(json);

        assertEquals(1, recipes.size());
        OnlineRecipe recipe = recipes.get(0);
        assertEquals("52771", recipe.getMealId());
        assertEquals("themealdb-52771", recipe.recipeId());
        assertEquals("Vegetarian", recipe.getCategory());
        assertEquals(List.of("Boil the pasta.", "Toss with sauce."), recipe.getInstructions());
        assertEquals(2, recipe.getIngredients().size());
        assertEquals(1.0, recipe.getIngredients().get(0).getQuantity());
        assertEquals("pound", recipe.getIngredients().get(0).getUnit());
        assertNull(recipe.getIngredients().get(1).getQuantity());
        assertEquals("unspecified", recipe.getIngredients().get(1).getUnit());
        assertTrue(TheMealDbRecipeMapper.mapSearchResponse("{\"meals\":null}").isEmpty());
        assertThrows(IOException.class,
                () -> TheMealDbRecipeMapper.mapSearchResponse("not json"));
    }

    @Test
    void savesManualAndImportedRecipesAndRejectsADuplicateSource() throws Exception {
        Path file = tempDir.resolve("recipes.json");
        FileRecipeLibraryDataAccess library = new FileRecipeLibraryDataAccess(file);
        AddRecipeDataAccessInterface manualSave = library;
        manualSave.save(new Recipe(
                "local-soup",
                "Vegetable soup",
                List.of(new Ingredient("broth", 500.0, "ml")),
                List.of("Simmer the broth."),
                null,
                4));

        OnlineRecipe imported = new OnlineRecipe(
                "52771",
                "Spicy Arrabiata Penne",
                "Vegetarian",
                "https://example.com/penne.jpg",
                List.of(new Ingredient("penne", 1.0, "pound")),
                List.of("Boil the pasta."));
        library.saveImported(
                imported.toRecipe(tempDir.resolve("penne.jpg").toString()),
                imported.getMealId(),
                imported.getCategory(),
                OnlineRecipe.ATTRIBUTION);

        assertThrows(DuplicateRecipeException.class, () -> library.saveImported(
                imported.toRecipe(null),
                imported.getMealId(),
                imported.getCategory(),
                OnlineRecipe.ATTRIBUTION));

        FileRecipeLibraryDataAccess reloaded = new FileRecipeLibraryDataAccess(file);
        List<RecipeLibraryRecord> recipes = reloaded.findAll();
        assertEquals(2, recipes.size());
        assertEquals("local-soup", recipes.get(0).getRecipe().getId());
        assertNull(recipes.get(0).getSourceId());
        assertEquals("52771", recipes.get(1).getSourceId());
        assertEquals("TheMealDB", recipes.get(1).getSourceName());
        assertEquals(OnlineRecipe.ATTRIBUTION, recipes.get(1).getAttribution());
        assertEquals("Vegetarian", recipes.get(1).getCategory());
        assertTrue(reloaded.existsBySourceId("52771"));
        assertEquals(tempDir.resolve("penne.jpg").toString(),
                recipes.get(1).getRecipe().getImageUrl());
    }

    @Test
    void cachesAnImageForOfflineDisplay() throws IOException {
        Path source = tempDir.resolve("source.png");
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", source.toFile());

        LocalRecipeImageCache cache = new LocalRecipeImageCache(tempDir.resolve("images"));
        String cached = cache.cache("themealdb-52771", source.toString());

        assertTrue(Path.of(cached).toFile().isFile());
        assertTrue(ImageIO.read(Path.of(cached).toFile()) != null);
        assertNull(cache.cache("themealdb-52771", "  "));
    }

    private static void assertMeasure(Double quantity, String unit,
                                      IngredientMeasureParser.ParsedMeasure measure) {
        assertEquals(quantity, measure.getQuantity());
        assertEquals(unit, measure.getUnit());
        assertFalse(measure.getUnit().isBlank());
    }
}
