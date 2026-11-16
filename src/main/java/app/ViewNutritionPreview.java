package app;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import entity.Ingredient;
import entity.Recipe;
import interface_adapter.view_nutrition.ViewNutritionController;
import interface_adapter.view_nutrition.ViewNutritionPresenter;
import interface_adapter.view_nutrition.ViewNutritionViewModel;
import use_case.view_nutrition.IngredientNutritionData;
import use_case.view_nutrition.NutritionDataAccessInterface;
import use_case.view_nutrition.ViewNutritionInputBoundary;
import use_case.view_nutrition.ViewNutritionInteractor;
import use_case.view_nutrition.ViewNutritionOutputBoundary;
import view.ViewNutritionView;

/**
 * Standalone launcher for previewing the View Recipe Nutrition panel.
 */
public class ViewNutritionPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ViewNutritionPreview::showPreview);
    }

    private static void showPreview() {
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionOutputBoundary outputBoundary =
                new ViewNutritionPresenter(viewModel);
        final NutritionDataAccessInterface nutritionDataAccess =
                new PreviewNutritionDataAccess();
        final ViewNutritionInputBoundary inputBoundary =
                new ViewNutritionInteractor(nutritionDataAccess, outputBoundary);
        final ViewNutritionController controller =
                new ViewNutritionController(inputBoundary);

        final ViewNutritionView view = new ViewNutritionView(viewModel);
        view.setViewNutritionController(controller);
        view.setSelectedRecipe(createSampleRecipe());

        final JFrame frame = new JFrame("WhatToEat - View Nutrition Preview");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(view);
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static Recipe createSampleRecipe() {
        final List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("Chicken Breast", 200.0, "g"),
                new Ingredient("Whole Wheat Pasta", 250.0, "g"),
                new Ingredient("Yellow Onion", 1.0, "piece"),
                new Ingredient("Olive Oil", 2.0, "tbsp"),
                new Ingredient("Fresh Basil", null, "g")
        );
        final List<String> instructions = Arrays.asList(
                "Cook the pasta.",
                "Cook the chicken and vegetables.",
                "Combine and serve."
        );
        return new Recipe("preview-chicken-pasta", "Chicken Pasta",
                ingredients, instructions, null, 4);
    }

    /** Supplies demonstration values without calling an external API. */
    private static class PreviewNutritionDataAccess
            implements NutritionDataAccessInterface {

        @Override
        public Optional<IngredientNutritionData> findNutrition(String ingredientName) {
            switch (ingredientName) {
                case "Chicken Breast":
                    return nutrition(165.0, 31.0, 0.0, 3.6,
                            false, null);
                case "Whole Wheat Pasta":
                    return nutrition(350.0, 12.0, 72.0, 1.5,
                            true, "Demonstration nutrition estimate.");
                case "Yellow Onion":
                    return nutrition(40.0, 1.1, 9.3, 0.1,
                            false, null);
                case "Olive Oil":
                    return nutrition(884.0, 0.0, 0.0, 100.0,
                            true, "Demonstration nutrition estimate.");
                default:
                    return Optional.empty();
            }
        }

        @Override
        public Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
            if ("Yellow Onion".equals(ingredient.getName())) {
                return Optional.of(110.0);
            }
            if ("Olive Oil".equals(ingredient.getName())) {
                return Optional.of(27.0);
            }
            return Optional.empty();
        }

        private Optional<IngredientNutritionData> nutrition(
                Double calories, Double protein, Double carbohydrates, Double fat,
                boolean estimatedNutritionData, String note) {
            return Optional.of(new IngredientNutritionData(
                    calories, protein, carbohydrates, fat,
                    estimatedNutritionData, note));
        }
    }
}
