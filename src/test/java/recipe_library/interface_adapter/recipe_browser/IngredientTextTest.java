package recipe_library.interface_adapter.recipe_browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import entity.Ingredient;

class IngredientTextTest {

    @Test
    void unknownQuantityOmitsTheUnspecifiedSuffix() {
        List<String> lines = IngredientText.lines(List.of(
                new Ingredient("olive oil", null, "unspecified"),
                new Ingredient("salt", null, "to taste"),
                new Ingredient("penne", 1.5, "pound"),
                new Ingredient("basil", 2.0, "sprig")));

        assertEquals(List.of(
                "olive oil",
                "salt (to taste)",
                "1.5 pound penne",
                "2 sprig basil"), lines);
    }
}
