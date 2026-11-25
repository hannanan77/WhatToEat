package app;

import data_access.FileRecipeLibraryDataAccess;
import interface_adapter.add_recipe.AddRecipeController;
import interface_adapter.add_recipe.AddRecipePresenter;
import interface_adapter.add_recipe.AddRecipeViewModel;
import use_case.add_recipe.AddRecipeDataAccessInterface;
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
            AddRecipeView view = createView(new FileRecipeLibraryDataAccess(
                    FileRecipeLibraryDataAccess.defaultFile()), () -> {});

            JFrame window = new JFrame("WhatToEat - Add Recipe");
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(view);
            window.setSize(900, 750);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    static AddRecipeView createView(AddRecipeDataAccessInterface recipeStore,
                                    Runnable onRecipeSaved) {
        AddRecipeViewModel viewModel = new AddRecipeViewModel();
        AddRecipeInteractor interactor =
                new AddRecipeInteractor(recipeStore, new AddRecipePresenter(viewModel));
        AddRecipeView view = new AddRecipeView(viewModel);
        view.setController(new AddRecipeController(interactor));
        viewModel.addPropertyChangeListener(event -> {
            if (viewModel.isSuccessful()) {
                SwingUtilities.invokeLater(onRecipeSaved);
            }
        });
        return view;
    }
}
