package interface_adapter.view_nutrition;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores display-ready nutrition values for the Swing view.
 */
public class ViewNutritionViewModel {

    public static final String VIEW_NAME = "view nutrition";

    private final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);

    private String recipeName = "No recipe selected";
    private String servings = "Servings unavailable";
    private String[] totalValues = unavailableValues();
    private String[] perServingValues = unavailableValues();
    private List<String[]> ingredientRows = new ArrayList<>();
    private String statusMessage = "Select a recipe to view its nutrition.";
    private boolean totalComplete;
    private boolean dataAvailable;
    private boolean containsEstimates;
    private boolean loading;
    private boolean error;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getServings() {
        return servings;
    }

    public String[] getTotalValues() {
        return totalValues.clone();
    }

    public String[] getPerServingValues() {
        return perServingValues.clone();
    }

    public List<String[]> getIngredientRows() {
        final List<String[]> copy = new ArrayList<>();
        for (String[] row : ingredientRows) {
            copy.add(row.clone());
        }
        return Collections.unmodifiableList(copy);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isTotalComplete() {
        return totalComplete;
    }

    public boolean isDataAvailable() {
        return dataAvailable;
    }

    public boolean containsEstimates() {
        return containsEstimates;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isError() {
        return error;
    }

    void setNutrition(String recipeName, String servings,
                      String[] totalValues, String[] perServingValues,
                      List<String[]> ingredientRows, String statusMessage,
                      boolean totalComplete, boolean dataAvailable,
                      boolean containsEstimates) {
        this.recipeName = recipeName;
        this.servings = servings;
        this.totalValues = totalValues.clone();
        this.perServingValues = perServingValues.clone();
        this.ingredientRows = copyRows(ingredientRows);
        this.statusMessage = statusMessage;
        this.totalComplete = totalComplete;
        this.dataAvailable = dataAvailable;
        this.containsEstimates = containsEstimates;
        this.loading = false;
        this.error = false;
        notifyStateChanged();
    }

    void setLoading(String recipeName, Integer servings) {
        this.recipeName = recipeName;
        this.servings = servings == null
                ? "Servings unavailable"
                : servings + (servings == 1 ? " serving" : " servings");
        clearNutritionData();
        statusMessage = "Retrieving nutrition information from USDA.";
        loading = true;
        error = false;
        notifyStateChanged();
    }

    void setError(String errorMessage) {
        clearNutritionData();
        statusMessage = errorMessage;
        loading = false;
        error = true;
        notifyStateChanged();
    }

    private void clearNutritionData() {
        totalValues = unavailableValues();
        perServingValues = unavailableValues();
        ingredientRows = new ArrayList<>();
        totalComplete = false;
        dataAvailable = false;
        containsEstimates = false;
    }

    private List<String[]> copyRows(List<String[]> rows) {
        final List<String[]> copy = new ArrayList<>();
        for (String[] row : rows) {
            copy.add(row.clone());
        }
        return copy;
    }

    private void notifyStateChanged() {
        propertyChangeSupport.firePropertyChange("state", null, this);
    }

    private static String[] unavailableValues() {
        return new String[] {
            "Not Available", "Not Available", "Not Available", "Not Available"
        };
    }
}
