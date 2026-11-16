package use_case.add_recipe;

import entity.Ingredient;
import entity.Recipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates and saves a recipe from the user's input.
 */
public class AddRecipeInteractor implements AddRecipeInputBoundary {

    private final AddRecipeDataAccessInterface recipeDataAccess;
    private final AddRecipeOutputBoundary presenter;

    public AddRecipeInteractor(
            AddRecipeDataAccessInterface recipeDataAccess,
            AddRecipeOutputBoundary presenter) {
        this.recipeDataAccess = recipeDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(AddRecipeInputData inputData) {
        if (inputData == null) {
            presenter.prepareFailView("Recipe information is required.");
            return;
        }

        Recipe recipe;

        try {
            List<Ingredient> ingredients = createIngredients(inputData);

            // An empty optional image field means there is no image.
            String imageUrl = inputData.getImageUrl();
            if (imageUrl != null && imageUrl.isBlank()) {
                imageUrl = null;
            }

            recipe = new Recipe(
                    UUID.randomUUID().toString(),
                    inputData.getName(),
                    ingredients,
                    inputData.getInstructions(),
                    imageUrl,
                    inputData.getServings()
            );
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        try {
            recipeDataAccess.save(recipe);
        }
        catch (IOException exception) {
            presenter.prepareFailView(
                    "The recipe could not be saved. Please try again.");
            return;
        }

        AddRecipeOutputData outputData = new AddRecipeOutputData(
                recipe.getId(),
                recipe.getName()
        );

        presenter.prepareSuccessView(outputData);
    }

    /**
     * Converts ingredient input into validated Ingredient objects.
     */
    private List<Ingredient> createIngredients(
            AddRecipeInputData inputData) {
        if (inputData.getIngredients() == null
                || inputData.getIngredients().isEmpty()) {
            throw new IllegalArgumentException(
                    "Please add at least one ingredient.");
        }

        List<Ingredient> ingredients = new ArrayList<>();

        for (AddRecipeInputData.IngredientInputData ingredientInput
                : inputData.getIngredients()) {
            if (ingredientInput == null) {
                throw new IllegalArgumentException(
                        "Ingredient information must not be missing.");
            }

            Ingredient ingredient = new Ingredient(
                    ingredientInput.getName(),
                    ingredientInput.getQuantity(),
                    ingredientInput.getUnit()
            );

            ingredients.add(ingredient);
        }

        return ingredients;
    }
}