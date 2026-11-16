package app;

import view.AddRecipeView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * A development preview for the add recipe page.
 */
public class AddRecipePreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("WhatToEat - Add Recipe Preview");

            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(new AddRecipeView());
            window.setSize(800, 650);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}