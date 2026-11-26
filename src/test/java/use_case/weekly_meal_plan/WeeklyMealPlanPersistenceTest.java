package use_case.weekly_meal_plan;

import data_access.FileWeeklyMealPlanDataAccess;
import entity.MealPlan;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanController;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanPresenter;
import interface_adapter.weekly_meal_plan.WeeklyMealPlanViewModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyMealPlanPersistenceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final String WEDNESDAY = "2026-11-25";

    @TempDir
    Path directory;

    @Test
    void restoresSavedReplacementAndKeepsClearAsDraftUntilSaved() {
        WeeklyMealPlanViewModel draft = new WeeklyMealPlanViewModel();
        WeeklyMealPlanController editor = controller(draft, directory);
        editor.addEntry(WEDNESDAY, "Dinner", "pasta");
        editor.addEntry(WEDNESDAY, "Dinner", "soup");
        assertEquals(List.of("2026-11-25 | Dinner | soup"), draft.getEntries());
        editor.savePlan();

        WeeklyMealPlanViewModel reopened = new WeeklyMealPlanViewModel();
        WeeklyMealPlanController reader = controller(reopened, directory);
        reader.loadPlan();
        assertEquals("soup", reopened.getAssignments().get(0).getRecipeId());

        editor.clearPlan();
        assertTrue(draft.getAssignments().isEmpty());
        reader.loadPlan();
        assertEquals(1, reopened.getAssignments().size());

        editor.savePlan();
        reader.loadPlan();
        assertTrue(reopened.getAssignments().isEmpty());
    }

    @Test
    void reportsSaveFailureAndKeepsDraft() throws Exception {
        Path blockedDirectory = Files.createFile(directory.resolve("not-a-directory"));
        WeeklyMealPlanViewModel draft = new WeeklyMealPlanViewModel();
        WeeklyMealPlanController editor = controller(draft, blockedDirectory);
        editor.addEntry(WEDNESDAY, "Dinner", "pasta");

        editor.savePlan();

        assertEquals("Could not save the weekly meal plan.", draft.getMessage());
        assertEquals(1, draft.getAssignments().size());
    }

    private WeeklyMealPlanController controller(WeeklyMealPlanViewModel viewModel, Path path) {
        return new WeeklyMealPlanController(new WeeklyMealPlanInteractor(
                new MealPlan(MONDAY), new WeeklyMealPlanPresenter(viewModel),
                new FileWeeklyMealPlanDataAccess(path)));
    }
}
