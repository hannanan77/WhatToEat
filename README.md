# WhatToEat

## Team Information

### Team Members
- Baojie Geng
- Jiahuan He
- Qianhui Yang
- Jingcheng Yu

---

## Project Description

### Domain
Food & Cooking / Lifestyle

WhatToEat is a Java-based recipe management application designed to help users discover recipes, create and save their own recipes, view nutritional information, and plan weekly meals.

The application aims to simplify everyday cooking decisions through an organized and user-friendly interface.

---

## User Stories

### User Story 1 — Search and Browse Recipes
As a user, I want to browse popular recipes and search for recipes by name or ingredient so that I can quickly discover dishes I would like to prepare.

### User Story 2 — Add a Recipe
As a home cook, I want to add and save my own recipes so that I can access them again later.

### User Story 3 — View Recipe Nutrition
As a home cook, I want to view the estimated nutritional information of a selected recipe so that I can make better dietary choices.

### User Story 4 — Weekly Meal Planner
As a user, I want to assign recipes to meals throughout the week so that I can organize what I will cook.

---

## Use Cases

### Use Case 1 — Search and Browse Recipes

**Main Success Scenario:**
  1. The user opens the application and browses popular recipe cards.
  2. The user selects a recipe or searches by recipe name or ingredient.
  3. The application displays matching recipes using local data and the TheMealDB API.
  4. The user selects a recipe to view its ingredients and cooking instructions.
  5. Recipes added by users can also be searched and viewed.

**Alternative Scenarios:**
  - No matching recipes are found.
  - The search input is empty.
  - The API request fails.

### Use Case 2 — Add a Recipe

**Main Success Scenario:**
  1. The system displays a form for adding a recipe.
  2. The user enters the recipe title, ingredients, and cooking instructions.
  3. The user selects ingredient quantities and the number of servings using numeric spinner controls, and selects units from predefined options. The controls restrict selections to valid values.
  4. The user clicks “Save”.
  5. The system checks that the title is not blank and that at least one ingredient and one cooking instruction have been provided.
  6. The system creates and persistently saves the recipe.
  7. The system displays a success message and shows the new recipe in My Recipes.

**Alternative Scenarios:**
  - The user clicks “Cancel” instead of “Save”:
    - The system closes the form without creating or saving a recipe.
  - Required information is missing:
    - The system displays an error identifying the missing information and preserves the entered data so the user can complete the form and save again.
  - The recipe cannot be saved:
    - The system displays an error message and preserves the entered data so the user can retry.



### Use Case 3 — View Recipe Nutrition

**Name:** View Recipe Nutrition

**Primary Actor:** User

**Preconditions:**
- The user has selected a recipe.
- The recipe contains ingredient information.

**Trigger:** The user requests to view the recipe's nutritional information.

**Success Outcome:** The system displays estimated calories, protein, carbohydrates, and fat for the recipe, including per-serving values when available.

**Main Success Scenario:**
1. The user requests nutritional information for a selected recipe.
2. The system identifies the recipe's ingredients and quantities.
3. The system obtains nutritional information for each ingredient.
4. The system calculates the estimated nutritional values.
5. The system displays the results, including per-serving values and clearly labeled estimates.

**Extensions:**

**2a. An ingredient quantity is missing or cannot be determined.**
- The system identifies the ingredient and indicates that the nutritional information is incomplete.

**3a. Nutritional information is unavailable for an ingredient.**
- The system displays "Not Available" for the missing information and labels any incomplete totals accordingly.

**3b. Nutritional information cannot be retrieved.**
- The system informs the user that nutritional information is temporarily unavailable.
- The use case ends.

**5a. The recipe has no valid serving count.**
- The system displays total nutritional values without per-serving values.

### Use Case 4 — Weekly Meal Planner

**Main Success Scenario:**
  1. The user opens the Weekly Meal Planner.
  2. The user selects recipes for different days and meals.
  3. The application displays the weekly meal plan.
  4. The user saves the meal plan.
  5. The application stores the meal plan locally.

**Alternative Scenarios:**
  - No recipe is selected for a meal.
  - The meal plan cannot be saved.

---

## Minimal Viable Product (MVP)

| Team Member | MVP Use Case |
|-------------|--------------|
| Jiahuan He | Add Recipe |
| Qianhui Yang | View Recipe Nutrition |
| Baojie Geng | Plan weekly meal |
| Jingcheng Yu | Browse and Search Recipes |

The MVP consists of four core use cases: Browse and Search Recipes, Add a Recipe, View Recipe Nutrition, and Weekly Meal Planner.

---

## Entities

### Entity 1 — Recipe

**Instance Variables:**
- id: String
- name: String
- ingredients: List<Ingredient>
- instructions: List<String>
- imageUrl: String
- servings: Integer

**Methods:**
- getId(): String
- getName(): String
- getIngredients(): List<Ingredient>
- getInstructions(): List<String>
- getImageUrl(): String
- getServings(): Integer

### Entity 2 — Ingredient

**Instance Variables:**
- name: String 
- quantity: Double 
- unit: String

**Methods:**
- getName(): String
- getQuantity(): Double
- getUnit(): String
- hasKnownQuantity(): boolean

### Entity 3 — MealPlan

**Instance Variables:**
- weekStartDate: LocalDate
- entries: List<MealPlanEntry>

**Methods:**
- getWeekStartDate(): LocalDate
- getEntries(): List<MealPlanEntry>
- assignRecipe(entry: MealPlanEntry): void
- removeAssignment(date: LocalDate, mealType: String): void

### Entity 4 — MealPlanEntry

**Instance Variables:**
- date: LocalDate
- mealType: String
- recipeId: String

**Methods:**
- getDate(): LocalDate
- getMealType(): String
- getRecipeId(): String

---

## API Usage

### API 1: TheMealDB

**Documentation:** https://www.themealdb.com/api.php

**How it is used in the program:**

TheMealDB will be used to search for recipes and retrieve recipe information, including names, ingredients, cooking instructions, and images.

### API 2: USDA FoodData Central

**Documentation:** https://fdc.nal.usda.gov/api-guide/

**How it is used in the program:**

USDA FoodData Central will provide nutritional data for ingredients. The application will calculate estimated calories, protein, carbohydrates, and fat based on ingredient quantities.

For ingredients without explicit weight measurements, documented reference weights may be used. Estimated values will be labeled accordingly.

---

## UI Sketch
<img width="1810" height="1182" alt="image" src="https://github.com/user-attachments/assets/a859a808-3099-4478-abce-51fe627cc71a" />

## Design Diagrams

### UML Class Diagram
<img width="1580" height="1120" alt="UML Draft" src="https://github.com/user-attachments/assets/12aeb3c3-00e7-4875-890f-d0970b4d34fc" />


### Sequence Diagrams
To be added.

---

## How to Run

To be added after implementation.

---

## Notes

- Potential future features include shopping list generation and food allergy filtering.
- Nutritional information will be presented as an estimate rather than an exact measurement.

