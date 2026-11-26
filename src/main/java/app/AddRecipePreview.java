package app;

import data_access.FileRecipeLibraryDataAccess;
import interface_adapter.add_recipe.AddRecipeController;
import interface_adapter.add_recipe.AddRecipePresenter;
import interface_adapter.add_recipe.AddRecipeViewModel;
import use_case.add_recipe.AddRecipeInteractor;
import view.AddRecipeView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Runs Add Recipe using the shared file-based recipe storage.
 */
public class AddRecipePreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileRecipeLibraryDataAccess recipeStore =
                    new FileRecipeLibraryDataAccess(
                            FileRecipeLibraryDataAccess.defaultFile()
                    );

            AddRecipeViewModel viewModel = new AddRecipeViewModel();

            AddRecipePresenter presenter =
                    new AddRecipePresenter(viewModel);

            AddRecipeInteractor interactor =
                    new AddRecipeInteractor(recipeStore, presenter);

            AddRecipeController controller =
                    new AddRecipeController(interactor);

            AddRecipeView view = new AddRecipeView(viewModel);
            view.setController(controller);

            JFrame window = new JFrame("WhatToEat - Add Recipe");
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(900, 750);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}