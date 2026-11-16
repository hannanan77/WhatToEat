package use_case.import_recipe;

import java.io.IOException;
import java.util.Optional;

import entity.Recipe;
import use_case.online_recipe.OnlineRecipe;
import use_case.online_recipe.OnlineRecipeGateway;
import use_case.recipe_library.DuplicateRecipeException;
import use_case.recipe_library.RecipeImageCache;
import use_case.recipe_library.RecipeLibraryDataAccessInterface;
import use_case.search_online_recipes.SearchOnlineRecipesInteractor;

/**
 * Saves one previously previewed TheMealDB recipe when the user imports it.
 */
public class ImportRecipeInteractor implements ImportRecipeInputBoundary {

    private final OnlineRecipeGateway gateway;
    private final RecipeLibraryDataAccessInterface library;
    private final RecipeImageCache imageCache;
    private final ImportRecipeOutputBoundary presenter;

    public ImportRecipeInteractor(OnlineRecipeGateway gateway,
                                  RecipeLibraryDataAccessInterface library,
                                  RecipeImageCache imageCache,
                                  ImportRecipeOutputBoundary presenter) {
        this.gateway = gateway;
        this.library = library;
        this.imageCache = imageCache;
        this.presenter = presenter;
    }

    @Override
    public void execute(ImportRecipeInputData inputData) {
        String mealId = inputData == null ? null : inputData.getMealId();
        if (mealId == null || mealId.isBlank()) {
            presenter.prepareFailView("Choose a recipe before importing it.");
            return;
        }

        try {
            if (library.existsBySourceId(mealId.trim())) {
                presenter.prepareFailView("This recipe is already in your collection.");
                return;
            }

            Optional<OnlineRecipe> found = gateway.findByMealId(mealId.trim());
            if (found.isEmpty()) {
                presenter.prepareFailView("That recipe is no longer available.");
                return;
            }

            OnlineRecipe onlineRecipe = found.get();
            String imageUrl = cachedOrRemoteImage(onlineRecipe);
            Recipe recipe = onlineRecipe.toRecipe(imageUrl);
            library.saveImported(
                    recipe,
                    onlineRecipe.getMealId(),
                    onlineRecipe.getCategory(),
                    OnlineRecipe.ATTRIBUTION);
            presenter.prepareSuccessView(new ImportRecipeOutputData(
                    recipe.getId(), recipe.getName()));
        }
        catch (DuplicateRecipeException exception) {
            presenter.prepareFailView("This recipe is already in your collection.");
        }
        catch (IOException exception) {
            presenter.prepareFailView(SearchOnlineRecipesInteractor.failureMessage(exception));
        }
    }

    private String cachedOrRemoteImage(OnlineRecipe recipe) {
        try {
            String cached = imageCache.cache(recipe.recipeId(), recipe.getRemoteImageUrl());
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        }
        catch (IOException exception) {
            // The recipe can still be saved and previewed without a local image.
        }
        return recipe.getRemoteImageUrl();
    }
}
