package use_case.browse_saved_recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import use_case.recipe_library.RecipeLibraryRecord;

public class BrowseSavedRecipesOutputData {

    private final List<RecipeLibraryRecord> recipes;

    public BrowseSavedRecipesOutputData(List<RecipeLibraryRecord> recipes) {
        this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
    }

    public List<RecipeLibraryRecord> getRecipes() {
        return recipes;
    }
}
