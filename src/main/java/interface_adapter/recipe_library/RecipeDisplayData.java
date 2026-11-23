package interface_adapter.recipe_library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Display values for one recipe card and its detail pane.
 *
 * <p>This is the UI-facing contract for a future BrowseRecipesPresenter.
 * The presenter will build these values from use-case output. RecipesPanel
 * renders them and does not load or store recipes itself.
 *
 * <p>The current Recipe entity has no category. A future presenter may supply
 * a category, or pass null when none is available.
 */
public final class RecipeDisplayData {

    private final String id;
    private final String name;
    private final String category;
    private final Integer servings;
    private final String imageUrl;
    private final List<String> ingredients;
    private final List<String> instructions;
    private final String attribution;
    private final String externalId;
    private final boolean importable;

    /**
     * Creates display data.
     *
     * <p>Parameter order: {@code id}, {@code name}, {@code category},
     * {@code servings}, {@code imageUrl}, {@code ingredients},
     * {@code instructions}.
     *
     * @param id recipe identifier used to keep a selection stable across refreshes
     * @param name recipe name shown on the card and in the detail pane
     * @param category optional category; null or blank when unavailable
     * @param servings optional serving count; null when unavailable
     * @param imageUrl filesystem path or http(s) URL; null or blank when missing
     * @param ingredients ingredient lines already prepared for display
     * @param instructions cooking steps in display order
     * @throws IllegalArgumentException if the id or name is missing
     */
    public RecipeDisplayData(String id,
                             String name,
                             String category,
                             Integer servings,
                             String imageUrl,
                             List<String> ingredients,
                             List<String> instructions) {
        this(id, name, category, servings, imageUrl, ingredients, instructions,
                null, null, false);
    }

    /**
     * Creates display data, including optional online-import details.
     *
     * @param attribution source credit, or null for a locally created recipe
     * @param externalId TheMealDB meal id when this result can be imported
     * @param importable whether Import to Collection should be offered
     */
    public RecipeDisplayData(String id,
                             String name,
                             String category,
                             Integer servings,
                             String imageUrl,
                             List<String> ingredients,
                             List<String> instructions,
                             String attribution,
                             String externalId,
                             boolean importable) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Recipe id must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recipe name must not be blank.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.category = blankToNull(category);
        this.servings = servings;
        this.imageUrl = blankToNull(imageUrl);
        this.ingredients = copyLines(ingredients);
        this.instructions = copyLines(instructions);
        this.attribution = blankToNull(attribution);
        this.externalId = blankToNull(externalId);
        this.importable = importable && this.externalId != null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Integer getServings() {
        return servings;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public String getAttribution() {
        return attribution;
    }

    public String getExternalId() {
        return externalId;
    }

    public boolean isImportable() {
        return importable;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static List<String> copyLines(List<String> lines) {
        List<String> copy = new ArrayList<>();
        if (lines == null) {
            return Collections.unmodifiableList(copy);
        }
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                copy.add(line.trim());
            }
        }
        return Collections.unmodifiableList(copy);
    }
}
