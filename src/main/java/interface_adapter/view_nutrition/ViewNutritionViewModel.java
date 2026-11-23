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
    private long activeRequestId;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public synchronized String getRecipeName() {
        return recipeName;
    }

    public synchronized String getServings() {
        return servings;
    }

    public synchronized String[] getTotalValues() {
        return totalValues.clone();
    }

    public synchronized String[] getPerServingValues() {
        return perServingValues.clone();
    }

    public synchronized List<String[]> getIngredientRows() {
        final List<String[]> copy = new ArrayList<>();
        for (String[] row : ingredientRows) {
            copy.add(row.clone());
        }
        return Collections.unmodifiableList(copy);
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized boolean isTotalComplete() {
        return totalComplete;
    }

    public synchronized boolean isDataAvailable() {
        return dataAvailable;
    }

    public synchronized boolean containsEstimates() {
        return containsEstimates;
    }

    public synchronized boolean isLoading() {
        return loading;
    }

    public synchronized boolean isError() {
        return error;
    }

    synchronized void setNutrition(long requestId, String recipeName, String servings,
                                   String[] totalValues, String[] perServingValues,
                                   List<String[]> ingredientRows, String statusMessage,
                                   boolean totalComplete, boolean dataAvailable,
                                   boolean containsEstimates) {
        if (requestId != activeRequestId) {
            return;
        }
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

    synchronized void setLoading(long requestId, String recipeName, Integer servings) {
        activeRequestId = requestId;
        this.recipeName = recipeName;
        this.servings = servings == null
                ? "Servings unavailable"
                : servings + (servings == 1 ? " serving" : " servings");
        clearNutritionData();
        totalValues = loadingValues();
        perServingValues = loadingValues();
        statusMessage = "Retrieving nutrition information from USDA.";
        loading = true;
        error = false;
        notifyStateChanged();
    }

    synchronized void setError(long requestId, String errorMessage) {
        if (requestId != activeRequestId) {
            return;
        }
        clearNutritionData();
        statusMessage = errorMessage;
        loading = false;
        error = true;
        notifyStateChanged();
    }

    /** Prevents a background request from publishing after the dialog closes. */
    synchronized void invalidateRequest(long requestId) {
        activeRequestId = requestId;
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

    private static String[] loadingValues() {
        return new String[] {
            "Calculating...", "Calculating...", "Calculating...", "Calculating..."
        };
    }
}
