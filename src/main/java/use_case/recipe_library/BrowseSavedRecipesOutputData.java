package use_case.recipe_library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BrowseSavedRecipesOutputData {

    private final List<RecipeLibraryRecord> recipes;

    public BrowseSavedRecipesOutputData(List<RecipeLibraryRecord> recipes) {
        this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
    }

    public List<RecipeLibraryRecord> getRecipes() {
        return recipes;
    }
}
