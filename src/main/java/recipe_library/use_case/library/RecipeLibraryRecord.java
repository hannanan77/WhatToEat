package recipe_library.use_case.library;

import entity.Recipe;

/**
 * A saved recipe plus the source details that the Recipe entity does not store.
 */
public final class RecipeLibraryRecord {

    private final Recipe recipe;
    private final String category;
    private final String sourceId;
    private final String sourceName;
    private final String attribution;

    public RecipeLibraryRecord(Recipe recipe,
                               String category,
                               String sourceId,
                               String sourceName,
                               String attribution) {
        this.recipe = recipe;
        this.category = category;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.attribution = attribution;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public String getCategory() {
        return category;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getAttribution() {
        return attribution;
    }
}
