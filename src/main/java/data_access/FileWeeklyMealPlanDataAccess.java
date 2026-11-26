package data_access;

import entity.MealPlan;
import entity.MealPlanEntry;
import use_case.weekly_meal_plan.WeeklyMealPlanDataAccessInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/** Persists a separate plan for each week in a local directory. */
public class FileWeeklyMealPlanDataAccess implements WeeklyMealPlanDataAccessInterface {
    private final Path directory;

    public FileWeeklyMealPlanDataAccess(Path directory) {
        this.directory = directory;
    }

    @Override
    public MealPlan load(LocalDate weekStartDate) throws IOException {
        MealPlan plan = new MealPlan(weekStartDate);
        Properties entries = new Properties();
        try (InputStream input = Files.newInputStream(planFile(weekStartDate))) {
            entries.load(input);
            for (String key : entries.stringPropertyNames()) {
                String[] slot = key.split(" ", 2);
                if (slot.length != 2) {
                    throw new IOException("Invalid meal plan slot.");
                }
                plan.addEntry(new MealPlanEntry(
                        LocalDate.parse(slot[0]), slot[1], entries.getProperty(key)));
            }
        } catch (NoSuchFileException exception) {
            return plan;
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            throw new IOException("Invalid saved meal plan.", exception);
        }
        return plan;
    }

    @Override
    public void save(MealPlan mealPlan) throws IOException {
        Properties entries = new Properties();
        for (MealPlanEntry entry : mealPlan.getEntries()) {
            entries.setProperty(entry.getDate() + " " + entry.getMealType(), entry.getRecipeId());
        }

        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "meal-plan-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                entries.store(output, null);
            }
            Files.move(temporaryFile, planFile(mealPlan.getWeekStartDate()),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private Path planFile(LocalDate weekStartDate) {
        return directory.resolve(weekStartDate + ".properties");
    }
}
