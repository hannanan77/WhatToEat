package interface_adapter.browse_recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeDisplayDataTest {

    @Test
    void keepsDisplayLinesIndependentOfTheCaller() {
        List<String> ingredients = new ArrayList<>();
        ingredients.add("  Flour  ");
        ingredients.add(" ");
        ingredients.add(null);
        RecipeDisplayData recipe = new RecipeDisplayData(
                " bread ", " Brown bread ", "  ", null, "  ",
                ingredients, null);

        ingredients.add("Salt");

        assertEquals("bread", recipe.getId());
        assertEquals("Brown bread", recipe.getName());
        assertNull(recipe.getCategory());
        assertNull(recipe.getServings());
        assertNull(recipe.getImageUrl());
        assertEquals(List.of("Flour"), recipe.getIngredients());
        assertEquals(List.of(), recipe.getInstructions());
        assertThrows(UnsupportedOperationException.class,
                () -> recipe.getIngredients().add("Salt"));
    }

    @Test
    void rejectsAMissingIdentityOrName() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecipeDisplayData(" ", "Soup", null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RecipeDisplayData("soup", " ", null, null, null, null, null));
    }
}
