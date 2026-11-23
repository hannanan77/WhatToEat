package interface_adapter.add_recipe;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Holds the result that the Add Recipe view should display.
 */
public class AddRecipeViewModel {

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private String message = "";
    private boolean successful;

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void showResult(String message, boolean successful) {
        this.message = message;
        this.successful = successful;

        // Notify the view even if the same message occurs again.
        support.firePropertyChange("result", null, message);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}