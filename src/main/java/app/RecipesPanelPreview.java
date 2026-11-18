package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import view.recipe_library.RecipesPanel;

/**
 * Development preview for the empty RecipesPanel.
 * This is not the application's shared frame.
 */
public class RecipesPanelPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RecipesPanelPreview::showPreview);
    }

    private static void showPreview() {
        RecipesPanel panel = new RecipesPanel();
        panel.showEmptyCardFrames(6);

        JFrame frame = new JFrame("WhatToEat - Recipes Panel Preview");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
