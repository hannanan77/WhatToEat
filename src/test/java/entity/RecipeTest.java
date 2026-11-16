package entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTest {

    @Test
    void constructsAValidRecipeAndExposesItsValues() {
        Ingredient flour = ingredient("flour", 200.0, "g");
        Ingredient milk = ingredient("milk", 250.0, "ml");
        List<Ingredient> ingredients = new ArrayList<>(List.of(flour, milk));
        List<String> instructions = new ArrayList<>(List.of(" Mix ", "Bake"));

        Recipe recipe = new Recipe(
                " recipe-1 ",
                " Pancakes ",
                ingredients,
                instructions,
                " https://example.com/pancakes.jpg ",
                4
        );

        assertEquals("recipe-1", recipe.getId());
        assertEquals("Pancakes", recipe.getName());
        assertEquals(List.of(flour, milk), recipe.getIngredients());
        assertEquals(List.of("Mix", "Bake"), recipe.getInstructions());
        assertEquals("https://example.com/pancakes.jpg", recipe.getImageUrl());
        assertEquals(4, recipe.getServings());
    }

    @Test
    void allowsAMissingImageAndAnUnknownServingCount() {
        Recipe recipe = recipe("local-1", "Soup", null, null);

        assertNull(recipe.getImageUrl());
        assertNull(recipe.getServings());
    }

    @Test
    void copiesTheIngredientListAndPreservesIngredientIdentity() {
        Ingredient flour = ingredient("flour", 100.0, "g");
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(flour);
        Recipe recipe = new Recipe(
                "r1",
                "Bread",
                ingredients,
                List.of("Knead"),
                null,
                1
        );

        ingredients.clear();
        ingredients.add(ingredient("salt", 5.0, "g"));

        List<Ingredient> stored = recipe.getIngredients();
        assertEquals(1, stored.size());
        assertSame(flour, stored.get(0));
        assertEquals("flour", stored.get(0).getName());
    }

    @Test
    void copiesInstructionsAndTrimsEachStep() {
        List<String> instructions = new ArrayList<>();
        instructions.add("  Chop  ");
        instructions.add("Stir");
        Recipe recipe = new Recipe(
                "r1",
                "Salad",
                List.of(ingredient("lettuce", 1.0, "head")),
                instructions,
                null,
                2
        );

        instructions.set(0, "Discard");

        assertEquals(List.of("Chop", "Stir"), recipe.getInstructions());
    }

    @Test
    void rejectsANullOrBlankRecipeId() {
        assertThrows(IllegalArgumentException.class,
                () -> recipe(null, "Soup", "image", 2));
        assertThrows(IllegalArgumentException.class,
                () -> recipe("   ", "Soup", "image", 2));
    }

    @Test
    void rejectsANullOrBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> recipe("r1", null, "image", 2));
        assertThrows(IllegalArgumentException.class,
                () -> recipe("r1", "  ", "image", 2));
    }

    @Test
    void rejectsAMissingEmptyOrNullIngredient() {
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe(
                        "r1",
                        "Soup",
                        null,
                        List.of("Boil"),
                        null,
                        1
                ));
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe(
                        "r1",
                        "Soup",
                        List.of(),
                        List.of("Boil"),
                        null,
                        1
                ));
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe(
                        "r1",
                        "Soup",
                        ingredients,
                        List.of("Boil"),
                        null,
                        1
                ));
    }

    @Test
    void rejectsAMissingEmptyOrBlankInstruction() {
        List<Ingredient> ingredients = List.of(ingredient("water", 1.0, "cup"));

        assertThrows(IllegalArgumentException.class,
                () -> new Recipe("r1", "Soup", ingredients, null, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe("r1", "Soup", ingredients, List.of(), null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe("r1", "Soup", ingredients, List.of("  "), null, 1));

        List<String> instructions = new ArrayList<>();
        instructions.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new Recipe("r1", "Soup", ingredients, instructions, null, 1));
    }

    @Test
    void rejectsABlankImageAndANonPositiveServingCount() {
        assertThrows(IllegalArgumentException.class,
                () -> recipe("r1", "Soup", "   ", 2));
        assertThrows(IllegalArgumentException.class,
                () -> recipe("r1", "Soup", null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> recipe("r1", "Soup", null, -1));
    }

    @Test
    void returnedCollectionsCannotModifyTheRecipe() {
        Recipe recipe = recipe("r1", "Soup", null, 2);

        assertThrows(UnsupportedOperationException.class,
                () -> recipe.getIngredients().add(ingredient("salt", 1.0, "g")));
        assertThrows(UnsupportedOperationException.class,
                () -> recipe.getInstructions().add("Season"));

        assertEquals(1, recipe.getIngredients().size());
        assertEquals(List.of("Cook"), recipe.getInstructions());
    }

    private static Recipe recipe(String id, String name, String imageUrl, Integer servings) {
        return new Recipe(
                id,
                name,
                List.of(ingredient("water", 500.0, "ml")),
                List.of("Cook"),
                imageUrl,
                servings
        );
    }

    private static Ingredient ingredient(String name, Double quantity, String unit) {
        return new Ingredient(name, quantity, unit);
    }
}
