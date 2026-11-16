package entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A recipe that can be saved, searched, and assigned to a meal plan by its id.
 * Nutrition is calculated by other components from {@link Ingredient}; this
 * entity stores only the recipe data described in the project README.
 */
public class Recipe {

    private final String id;
    private final String name;
    private final List<Ingredient> ingredients;
    private final List<String> instructions;
    private final String imageUrl;
    private final Integer servings;

    /**
     * Creates a recipe.
     *
     * <p>Parameter order: {@code id}, {@code name}, {@code ingredients},
     * {@code instructions}, {@code imageUrl}, {@code servings}.
     *
     * @param id the recipe identifier; must not be null or blank
     * @param name the recipe name; must not be null or blank
     * @param ingredients at least one ingredient; null elements are rejected
     * @param instructions at least one cooking instruction; blank steps are rejected
     * @param imageUrl the image location, or null when the recipe has no image
     * @param servings the serving count, or null when it is unknown;
     *                 a provided count must be positive
     * @throws IllegalArgumentException if any argument violates these rules
     */
    public Recipe(String id,
                  String name,
                  List<Ingredient> ingredients,
                  List<String> instructions,
                  String imageUrl,
                  Integer servings) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Recipe ID must not be null or blank.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recipe name must not be null or blank.");
        }

        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipe must contain at least one ingredient.");
        }

        List<Ingredient> copiedIngredients = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) {
                throw new IllegalArgumentException("Ingredient must not be null.");
            }
            copiedIngredients.add(ingredient);
        }

        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipe must contain at least one instruction.");
        }

        List<String> copiedInstructions = new ArrayList<>();
        for (String instruction : instructions) {
            if (instruction == null || instruction.isBlank()) {
                throw new IllegalArgumentException(
                        "Instruction must not be null or blank.");
            }
            copiedInstructions.add(instruction.trim());
        }

        if (imageUrl != null && imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL must not be blank.");
        }

        if (servings != null && servings <= 0) {
            throw new IllegalArgumentException(
                    "Servings must be positive when provided.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.ingredients = copiedIngredients;
        this.instructions = copiedInstructions;
        this.imageUrl = imageUrl == null ? null : imageUrl.trim();
        this.servings = servings;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a snapshot of the ingredients.
     * Changes to the returned list do not affect this recipe.
     */
    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(new ArrayList<>(ingredients));
    }

    /**
     * Returns a snapshot of the cooking instructions.
     * Changes to the returned list do not affect this recipe.
     */
    public List<String> getInstructions() {
        return Collections.unmodifiableList(new ArrayList<>(instructions));
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getServings() {
        return servings;
    }
}
