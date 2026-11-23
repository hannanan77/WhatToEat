package use_case.add_recipe;

import entity.Ingredient;
import entity.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddRecipeInteractorTest {

    private FakeRecipeDataAccess dao;
    private RecordingPresenter presenter;
    private AddRecipeInteractor interactor;

    @BeforeEach
    void setUp() {
        // Each test starts with fresh objects.
        dao = new FakeRecipeDataAccess();
        presenter = new RecordingPresenter();
        interactor = new AddRecipeInteractor(dao, presenter);
    }

    @Test
    void validRecipeIsSavedAndSuccessIsPresented() {
        AddRecipeInputData input = createInput(
                "Tomato Soup",
                validIngredients(),
                List.of("Chop tomatoes.", "Cook in water."),
                "file:///test/tomato.png",
                2
        );

        interactor.execute(input);

        assertEquals(1, dao.saveCalls);
        assertNotNull(dao.savedRecipe);

        Recipe saved = dao.savedRecipe;
        assertNotNull(saved.getId());
        assertFalse(saved.getId().isBlank());
        assertEquals("Tomato Soup", saved.getName());
        assertEquals(Integer.valueOf(2), saved.getServings());
        assertEquals("file:///test/tomato.png", saved.getImageUrl());
        assertEquals(
                List.of("Chop tomatoes.", "Cook in water."),
                saved.getInstructions()
        );

        assertEquals(1, saved.getIngredients().size());
        Ingredient ingredient = saved.getIngredients().get(0);
        assertEquals("Tomato", ingredient.getName());
        assertEquals(Double.valueOf(2.0), ingredient.getQuantity());
        assertEquals("piece", ingredient.getUnit());

        assertEquals(1, presenter.successCalls);
        assertEquals(0, presenter.failureCalls);
        assertNotNull(presenter.outputData);
        assertEquals(saved.getId(), presenter.outputData.getRecipeId());
        assertEquals(saved.getName(), presenter.outputData.getRecipeName());
    }

    @Test
    void nullInputIsRejectedWithoutSaving() {
        interactor.execute(null);

        assertRejectedWithoutSaving();
        assertEquals(
                "Recipe information is required.",
                presenter.errorMessage
        );
    }

    @Test
    void blankRecipeNameIsRejectedWithoutSaving() {
        interactor.execute(createInput(
                "   ",
                validIngredients(),
                List.of("Cook."),
                null,
                1
        ));

        assertRejectedWithoutSaving();
    }

    @Test
    void nullIngredientListIsRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                null,
                List.of("Cook."),
                null,
                1
        ));

        assertRejectedWithoutSaving();
        assertEquals(
                "Please add at least one ingredient.",
                presenter.errorMessage
        );
    }

    @Test
    void emptyIngredientListIsRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                Collections.emptyList(),
                List.of("Cook."),
                null,
                1
        ));

        assertRejectedWithoutSaving();
        assertEquals(
                "Please add at least one ingredient.",
                presenter.errorMessage
        );
    }

    @Test
    void nullIngredientIsRejectedWithoutSaving() {
        // List.of(null) is not allowed, so use singletonList here.
        interactor.execute(createInput(
                "Soup",
                Collections.singletonList(null),
                List.of("Cook."),
                null,
                1
        ));

        assertRejectedWithoutSaving();
        assertEquals(
                "Ingredient information must not be missing.",
                presenter.errorMessage
        );
    }

    @Test
    void zeroIngredientQuantityIsRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                List.of(new AddRecipeInputData.IngredientInputData(
                        "Tomato", 0.0, "piece"
                )),
                List.of("Cook."),
                null,
                1
        ));

        assertRejectedWithoutSaving();
    }

    @Test
    void unknownIngredientQuantityCanBeSaved() {
        interactor.execute(createInput(
                "Soup",
                List.of(new AddRecipeInputData.IngredientInputData(
                        "Salt", null, "unspecified"
                )),
                List.of("Add salt."),
                null,
                1
        ));

        assertEquals(1, dao.saveCalls);
        assertNotNull(dao.savedRecipe);

        Ingredient saved = dao.savedRecipe.getIngredients().get(0);
        assertEquals("Salt", saved.getName());
        assertNull(saved.getQuantity());
        assertFalse(saved.hasKnownQuantity());

        assertEquals(1, presenter.successCalls);
        assertEquals(0, presenter.failureCalls);
    }

    @Test
    void emptyInstructionsAreRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                Collections.emptyList(),
                null,
                1
        ));

        assertRejectedWithoutSaving();
    }

    @Test
    void blankInstructionIsRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                List.of("   "),
                null,
                1
        ));

        assertRejectedWithoutSaving();
    }

    @Test
    void zeroServingsAreRejectedWithoutSaving() {
        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                List.of("Cook."),
                null,
                0
        ));

        assertRejectedWithoutSaving();
    }

    @Test
    void blankImageIsSavedAsNull() {
        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                List.of("Cook."),
                "   ",
                1
        ));

        assertEquals(1, dao.saveCalls);
        assertNotNull(dao.savedRecipe);
        assertNull(dao.savedRecipe.getImageUrl());
        assertEquals(1, presenter.successCalls);
        assertEquals(0, presenter.failureCalls);
    }

    @Test
    void nullImageIsAccepted() {
        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                List.of("Cook."),
                null,
                1
        ));

        assertEquals(1, dao.saveCalls);
        assertNotNull(dao.savedRecipe);
        assertNull(dao.savedRecipe.getImageUrl());
        assertEquals(1, presenter.successCalls);
        assertEquals(0, presenter.failureCalls);
    }

    @Test
    void storageFailureShowsErrorInsteadOfSuccess() {
        dao.failOnSave = true;

        interactor.execute(createInput(
                "Soup",
                validIngredients(),
                List.of("Cook."),
                null,
                1
        ));

        assertEquals(1, dao.saveCalls);
        assertNull(dao.savedRecipe);
        assertEquals(0, presenter.successCalls);
        assertNull(presenter.outputData);
        assertEquals(1, presenter.failureCalls);
        assertEquals(
                "The recipe could not be saved. Please try again.",
                presenter.errorMessage
        );
    }

    private void assertRejectedWithoutSaving() {
        assertEquals(0, dao.saveCalls);
        assertNull(dao.savedRecipe);
        assertEquals(0, presenter.successCalls);
        assertNull(presenter.outputData);
        assertEquals(1, presenter.failureCalls);
        assertNotNull(presenter.errorMessage);
        assertFalse(presenter.errorMessage.isBlank());
    }

    private static List<AddRecipeInputData.IngredientInputData>
    validIngredients() {
        return List.of(new AddRecipeInputData.IngredientInputData(
                "Tomato", 2.0, "piece"
        ));
    }

    private static AddRecipeInputData createInput(
            String name,
            List<AddRecipeInputData.IngredientInputData> ingredients,
            List<String> instructions,
            String imageUrl,
            Integer servings) {

        return new AddRecipeInputData(
                name,
                ingredients,
                instructions,
                imageUrl,
                servings
        );
    }

    /**
     * Stores a recipe in memory only.
     * No files or real recipe collections are changed.
     */
    private static class FakeRecipeDataAccess
            implements AddRecipeDataAccessInterface {

        private Recipe savedRecipe;
        private int saveCalls;
        private boolean failOnSave;

        @Override
        public void save(Recipe recipe) throws IOException {
            saveCalls++;

            if (failOnSave) {
                throw new IOException("Simulated storage failure.");
            }

            savedRecipe = recipe;
        }
    }

    /**
     * Records the Interactor's output without opening a UI.
     */
    private static class RecordingPresenter
            implements AddRecipeOutputBoundary {

        private AddRecipeOutputData outputData;
        private String errorMessage;
        private int successCalls;
        private int failureCalls;

        @Override
        public void prepareSuccessView(AddRecipeOutputData outputData) {
            this.outputData = outputData;
            successCalls++;
        }

        @Override
        public void prepareFailView(String message) {
            errorMessage = message;
            failureCalls++;
        }
    }
}