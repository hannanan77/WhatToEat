package use_case.recipe_library;

public class ImportRecipeInputData {

    private final String mealId;

    public ImportRecipeInputData(String mealId) {
        this.mealId = mealId;
    }

    public String getMealId() {
        return mealId;
    }
}
