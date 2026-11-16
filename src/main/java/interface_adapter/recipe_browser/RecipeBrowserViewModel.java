package interface_adapter.recipe_browser;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import interface_adapter.browse_recipes.RecipeDisplayData;

/**
 * Display state shared by online search, import, and the saved collection.
 */
public class RecipeBrowserViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<RecipeDisplayData> recipes = new ArrayList<>();
    private String status = "Your collection is empty.";

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public List<RecipeDisplayData> getRecipes() {
        return Collections.unmodifiableList(new ArrayList<>(recipes));
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        support.firePropertyChange("state", null, this);
    }

    public void setRecipes(List<RecipeDisplayData> recipes, String status) {
        this.recipes = new ArrayList<>(recipes);
        this.status = status;
        support.firePropertyChange("state", null, this);
    }
}
