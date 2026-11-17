package recipe_library.use_case.online_recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import entity.Ingredient;
import entity.Recipe;

/**
 * A recipe returned by TheMealDB before the user chooses to import it.
 */
public final class OnlineRecipe {

    public static final String SOURCE_NAME = "TheMealDB";
    public static final String ATTRIBUTION =
            "Recipe data from TheMealDB (https://www.themealdb.com).";

    private final String mealId;
    private final String name;
    private final String category;
    private final String remoteImageUrl;
    private final List<Ingredient> ingredients;
    private final List<String> instructions;

    public OnlineRecipe(String mealId,
                        String name,
                        String category,
                        String remoteImageUrl,
                        List<Ingredient> ingredients,
                        List<String> instructions) {
        this.mealId = mealId;
        this.name = name;
        this.category = category;
        this.remoteImageUrl = remoteImageUrl;
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.instructions = Collections.unmodifiableList(new ArrayList<>(instructions));
    }

    public String getMealId() {
        return mealId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getRemoteImageUrl() {
        return remoteImageUrl;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public String recipeId() {
        return "themealdb-" + mealId;
    }

    /**
     * Builds the entity that will be stored after an explicit import.
     * TheMealDB does not provide a serving count.
     */
    public Recipe toRecipe(String imageUrl) {
        return new Recipe(recipeId(), name, ingredients, instructions, imageUrl, null);
    }
}
