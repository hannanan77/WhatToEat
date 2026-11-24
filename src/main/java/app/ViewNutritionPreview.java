package app;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import view.ViewNutritionDialog;
import view.ViewNutritionView;

/**
 * Standalone launcher for previewing deterministic Nutrition UI states.
 */
public class ViewNutritionPreview {

    private static final long LOADING_DELAY_MILLIS = 5000L;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ViewNutritionPreview::showPreview);
    }

    private static void showPreview() {
        final ViewNutritionViewModel viewModel = new ViewNutritionViewModel();
        final ViewNutritionOutputBoundary outputBoundary =
                new ViewNutritionPresenter(viewModel);
        final PreviewNutritionDataAccess nutritionDataAccess =
                new PreviewNutritionDataAccess();
        final ViewNutritionInputBoundary inputBoundary =
                new ViewNutritionInteractor(nutritionDataAccess, outputBoundary);
        final ViewNutritionController controller =
                new ViewNutritionController(inputBoundary, viewModel);

        final ViewNutritionView view = new ViewNutritionView(viewModel);
        view.setViewNutritionController(controller);

        final JFrame frame = new JFrame("WhatToEat - Nutrition Scenarios");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final JComboBox<PreviewScenario> scenarioSelector =
                new JComboBox<>(PreviewScenario.values());
        final JButton openButton = new JButton("Open Nutrition Preview");
        frame.setContentPane(createSelectorPanel(scenarioSelector, openButton));
        frame.setSize(560, 220);
        frame.setLocationRelativeTo(null);

        final ViewNutritionDialog dialog = new ViewNutritionDialog(frame, view);
        openButton.addActionListener(event -> {
            final PreviewScenario scenario =
                    (PreviewScenario) scenarioSelector.getSelectedItem();
            if (scenario != null) {
                nutritionDataAccess.selectScenario(scenario);
                dialog.showForRecipe(createRecipe(scenario), openButton);
            }
        });
        frame.setVisible(true);
    }

    private static JPanel createSelectorPanel(
            JComboBox<PreviewScenario> scenarioSelector, JButton openButton) {
        final JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        controls.add(new JLabel("Scenario:"));
        controls.add(scenarioSelector);
        controls.add(openButton);

        final JLabel help = new JLabel(
                "Loading waits 5 seconds. Error succeeds after pressing Retry.",
                JLabel.CENTER);
        content.add(controls, BorderLayout.CENTER);
        content.add(help, BorderLayout.SOUTH);
        return content;
    }

    private static Recipe createRecipe(PreviewScenario scenario) {
        final List<Ingredient> ingredients;
        switch (scenario) {
            case ALL_AVAILABLE:
                ingredients = createGramIngredients("Available Ingredient", 7);
                break;
            case ESTIMATED_ONLY:
                ingredients = List.of(
                        new Ingredient("Measured Chicken", 200.0, "g"),
                        new Ingredient("Estimated Onion", 1.0, "piece"),
                        new Ingredient("Reference Grain", 150.0, "g")
                );
                break;
            case PARTIAL_MISSING:
                ingredients = List.of(
                        new Ingredient("Available Rice", 200.0, "g"),
                        new Ingredient("Partial Peas", 100.0, "g"),
                        new Ingredient("Unavailable Spice", 10.0, "g")
                );
                break;
            case LOADING:
                ingredients = List.of(
                        new Ingredient("Loading Ingredient", 100.0, "g")
                );
                break;
            case ERROR:
                ingredients = List.of(
                        new Ingredient("Retry Ingredient", 100.0, "g")
                );
                break;
            case LONG_RECIPE:
                ingredients = createGramIngredients("Long Ingredient", 20);
                break;
            default:
                throw new IllegalArgumentException("Unknown preview scenario.");
        }

        return new Recipe(
                "preview-" + scenario.name().toLowerCase().replace('_', '-'),
                scenario.getRecipeName(),
                ingredients,
                List.of("Prepare the preview ingredients."),
                null,
                4
        );
    }

    private static List<Ingredient> createGramIngredients(String namePrefix, int count) {
        final List<Ingredient> ingredients = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            ingredients.add(new Ingredient(
                    String.format("%s %02d", namePrefix, index),
                    100.0,
                    "g"));
        }
        return ingredients;
    }

    private enum PreviewScenario {
        ALL_AVAILABLE("All Available", "All Available Recipe"),
        ESTIMATED_ONLY("Estimated Only", "Estimated Only Recipe"),
        PARTIAL_MISSING("Partial / Missing", "Partial Nutrition Recipe"),
        LOADING("Loading", "Delayed Nutrition Recipe"),
        ERROR("Error", "Retryable Error Recipe"),
        LONG_RECIPE("Long Recipe", "Twenty Ingredient Recipe");

        private final String label;
        private final String recipeName;

        PreviewScenario(String label, String recipeName) {
            this.label = label;
            this.recipeName = recipeName;
        }

        String getRecipeName() {
            return recipeName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Supplies deterministic preview values without calling an external API. */
    private static class PreviewNutritionDataAccess
            implements NutritionDataAccessInterface {

        private final AtomicBoolean loadingDelayPending = new AtomicBoolean();
        private final AtomicBoolean errorPending = new AtomicBoolean();
        private volatile PreviewScenario selectedScenario = PreviewScenario.ALL_AVAILABLE;

        void selectScenario(PreviewScenario scenario) {
            selectedScenario = scenario;
            loadingDelayPending.set(scenario == PreviewScenario.LOADING);
            errorPending.set(scenario == PreviewScenario.ERROR);
        }

        @Override
        public Optional<IngredientNutritionData> findNutrition(String ingredientName) {
            final PreviewScenario scenario = selectedScenario;
            if (scenario == PreviewScenario.LOADING
                    && loadingDelayPending.compareAndSet(true, false)) {
                delayLoadingResponse();
            }
            if (scenario == PreviewScenario.ERROR
                    && errorPending.compareAndSet(true, false)) {
                throw new IllegalStateException("Simulated preview request failure.");
            }

            if (scenario == PreviewScenario.PARTIAL_MISSING) {
                if ("Partial Peas".equals(ingredientName)) {
                    return nutrition(81.0, null, 14.0, null,
                            false, null);
                }
                if ("Unavailable Spice".equals(ingredientName)) {
                    return Optional.empty();
                }
            }
            if (scenario == PreviewScenario.ESTIMATED_ONLY
                    && "Reference Grain".equals(ingredientName)) {
                return nutrition(340.0, 12.0, 70.0, 2.0,
                        true, "Deterministic reference nutrition for preview.");
            }
            return nutrition(100.0, 10.0, 20.0, 5.0,
                    false, null);
        }

        @Override
        public Optional<Double> estimateWeightInGrams(Ingredient ingredient) {
            if (selectedScenario == PreviewScenario.ESTIMATED_ONLY
                    && "Estimated Onion".equals(ingredient.getName())) {
                return Optional.of(110.0);
            }
            return Optional.empty();
        }

        private void delayLoadingResponse() {
            try {
                Thread.sleep(LOADING_DELAY_MILLIS);
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "The simulated loading delay was interrupted.", exception);
            }
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
