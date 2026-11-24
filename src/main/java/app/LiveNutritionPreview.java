package app;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import entity.Ingredient;
import entity.Recipe;

/** Development-only launcher for verifying live USDA Nutrition data. */
public final class LiveNutritionPreview {

    private LiveNutritionPreview() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LiveNutritionPreview::showPreview);
    }

    private static void showPreview() {
        final JFrame frame = new JFrame("WhatToEat - Live USDA Nutrition");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final NutritionAssembly nutritionAssembly = new NutritionAssembly(frame);
        final JButton openButton = new JButton("Open Live Nutrition");
        final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        final boolean apiKeyConfigured = isApiKeyConfigured();
        if (!apiKeyConfigured) {
            openButton.setEnabled(false);
            statusLabel.setText(
                    "USDA_API_KEY is missing. Configure it in this IntelliJ Run Configuration.");
        }
        else {
            statusLabel.setText("USDA_API_KEY is configured for this process.");
        }

        openButton.addActionListener(event -> nutritionAssembly.showForRecipe(
                createLiveRecipe(), openButton));

        final JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        content.add(openButton, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setSize(560, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static boolean isApiKeyConfigured() {
        final String apiKey = System.getenv("USDA_API_KEY");
        return apiKey != null && !apiKey.isBlank();
    }

    private static Recipe createLiveRecipe() {
        final List<Ingredient> ingredients = List.of(
                new Ingredient("Chicken breast", 200.0, "g"),
                new Ingredient("Onion", 1.0, "piece"),
                new Ingredient("Olive oil", 1.0, "tbsp"),
                new Ingredient("Butter", 1.0, "tbsp")
        );
        return new Recipe(
                "live-usda-preview",
                "Live USDA Nutrition Recipe",
                ingredients,
                List.of("Use the returned nutrition data for development verification."),
                null,
                2
        );
    }
}
