package buthod.tony.appManager.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import buthod.tony.appManager.utils.Utils;
import buthod.tony.appManager.recipes.UnitsConversion;

/**
 * Created by Tony on 09/10/2017.
 */

public class RecipesDAO extends DAOBase {

    /**
     * Class containing the ingredient and the quantity to use for a recipe_activity.
     */
    public static class Ingredient {
        public static int NORMAL_TYPE = 0, OPTIONAL_TYPE = 1;

        public long idQuantity = -1; // The id of the quantity in database QUANTITIES
        public long idIngredient = -1; // The id of the ingredient to use
        public int idUnit = 0; // The id of the unit to use.
        public String name; // The ingredient name.
        public float quantity; // The quantity to use
        public int number; // The number of the quantity used to order it in the list.
        public int type; // The type of ingredient (for now, used to differentiate optional ingredients).

        public Ingredient() {

        }

        public Ingredient(long idQuantity, long idIngredient, int idUnit, String name, float quantity) {
            this.idQuantity = idQuantity;
            this.idIngredient = idIngredient;
            this.idUnit = idUnit;
            this.name = name;
            this.quantity = quantity;
            this.number = 0;
            this.type = NORMAL_TYPE;
        }

        public String toString() {
            return String.format("%d : (%d) %f %d %s (%d) - %d", number, idQuantity, quantity, idUnit, name, idIngredient, type);
        }
    }

    public static class Step {
        public long id = -1;
        public int number;
        public String description;

        public String toString() {
            return String.format("%d : (%d) %s", number, id, description);
        }
    }

    public static class Separation {
        public static int INGREDIENT_TYPE = 0, STEP_TYPE = 1;
        public long id = -1;
        public int number;
        public int type;
        public String description;

        public String toString() {
            return String.format("%d : (%d) %s - %d", number, id, description, type);
        }
    }

    public static class Recipe {
        public long id;
        public String name;
        public int type;
        public int difficulty;
        public int time; // Preparation time
        public int grade;
        public int people;
        public int number;
        public ArrayList<Ingredient> ingredients;
        public ArrayList<Step> steps;
        public ArrayList<Separation> separations;

        public Recipe() {
            id = -1;
            name = "";
            type = -1;
            difficulty = -1;
            time = -1;
            grade = -1;
            number = 0;
            ingredients = new ArrayList<>();
            steps = new ArrayList<>();
            separations = new ArrayList<>();
        }
    }

    public static class IngredientConversions {
        public long ingredientId;
        public String ingredientName;
        public List<Conversion> conversions;

        public IngredientConversions() {
            ingredientId = -1;
            ingredientName = null;
            conversions = new ArrayList<>();
        }

        public IngredientConversions(long id, String name) {
            ingredientId = id;
            ingredientName = name;
            conversions = new ArrayList<>();
        }
    }

    public static class Conversion {
        public long id;
        public int unitFrom;
        public int unitTo;
        public float factor;

        public Conversion() {
            id = -1;
        }

        public Conversion(long id, int unitFrom, int unitTo, float factor) {
            this.id = id;
            this.unitFrom = unitFrom;
            this.unitTo = unitTo;
            this.factor = factor;
        }
    }

    public RecipesDAO(Context context) {
        super(context);
    }

    //region INGREDIENTS

    public long addIngredient(String name) {
        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.INGREDIENTS_NAME, name);
        return mDb.insert(DatabaseHandler.INGREDIENTS_TABLE_NAME, null, value);
    }

    public CharSequence[] getIngredients() {
        Cursor c = mDb.rawQuery("Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME +
                        " Order By " + DatabaseHandler.INGREDIENTS_NAME + " ASC;",
                new String[0]);
        CharSequence[] ingredients = new CharSequence[c.getCount()];
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); ++i) {
            ingredients[i] = c.getString(1);
            c.moveToNext();
        }
        c.close();
        return ingredients;
    }

    public LongSparseArray<String> getIngredientsFromId(long[] ids) {
        Cursor c = mDb.rawQuery(
                "Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME +
                " Where " + DatabaseHandler.INGREDIENTS_KEY + " In " + DAOBase.formatLongArrayForInQuery(ids) + ";",
                new String[0]);
        LongSparseArray<String> ingredients = new LongSparseArray<>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            ingredients.append(
                    c.getLong(c.getColumnIndex(DatabaseHandler.INGREDIENTS_KEY)),
                    c.getString(c.getColumnIndex(DatabaseHandler.INGREDIENTS_NAME))
            );
        }
        c.close();
        return ingredients;
    }

    /**
     * Get the id of the ingredient. If the ingredient does not exists, insert it.
     * This method is not case sensitive during comparison.
     */
    public long getIngredientId(String name) {
        Cursor c = mDb.rawQuery("Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME +
                " Where LOWER(?) = LOWER(" + DatabaseHandler.INGREDIENTS_NAME + ");",
                new String[] { name });
        long id = -1;
        if (c.getCount() > 0) {
            c.moveToFirst();
            id = c.getLong(0);
        }
        c.close();
        if (id == -1) {
            id = addIngredient(name);
        }
        return id;
    }

    /**
     * Delete an ingredient and its registered conversions.
     * @param id The ingredient id.
     */
    public void deleteIngredient(long id) {
        // Delete the ingredient itself.
        mDb.execSQL(String.format(Locale.getDefault(),
                "Delete From %s Where %s = %s;",
                DatabaseHandler.INGREDIENTS_TABLE_NAME, DatabaseHandler.INGREDIENTS_KEY,
                String.valueOf(id)));
        // Delete all registered conversions for this ingredient.
        mDb.execSQL(String.format(Locale.getDefault(),
                "Delete From %s Where %s = %s;",
                DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT,
                String.valueOf(id)));
    }

    /**
     * Delete unused ingredients from database.
     * It deletes all ingredients that are not used in any one recipe.
     */
    public void deleteUnusedIngredients() {
        Cursor c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                    "Select %2$s From %1$s Where %2$s IN " +
                    "(Select %1$s.%2$s From %1$s Left Join %3$s On %1$s.%2$s = %3$s.%4$s Where %3$s.%4$s Is Null);",
                    DatabaseHandler.INGREDIENTS_TABLE_NAME, DatabaseHandler.INGREDIENTS_KEY,
                    DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_INGREDIENT),
                new String[0]
                );
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long unusedIngredientId = c.getLong(0);
            deleteIngredient(unusedIngredientId);
        }
    }

    //endregion

    //region RECIPES

    /**
     * Add / edit a recipe_activity.
     * @param recipe
     * @return
     */
    public long addEditRecipe(Recipe recipe) {
        boolean isNew = (recipe.id == -1);

        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.RECIPES_NAME, recipe.name);
        value.put(DatabaseHandler.RECIPES_TYPE, recipe.type);
        value.put(DatabaseHandler.RECIPES_DIFFICULTY, recipe.difficulty);
        value.put(DatabaseHandler.RECIPES_TIME, recipe.time);
        value.put(DatabaseHandler.RECIPES_GRADE, recipe.grade);
        value.put(DatabaseHandler.RECIPES_PEOPLE, recipe.people);
        value.put(DatabaseHandler.RECIPES_NUMBER, recipe.number);
        long recipeId = recipe.id;
        if (isNew)
            recipeId = mDb.insert(DatabaseHandler.RECIPES_TABLE_NAME, null, value);
        else
            mDb.update(DatabaseHandler.RECIPES_TABLE_NAME, value,
                    DatabaseHandler.RECIPES_KEY + " = ?", new String[]{String.valueOf(recipe.id)});

        addEditQuantities(recipeId, recipe.ingredients);
        addEditSteps(recipeId, recipe.steps);
        addEditSeparations(recipeId, recipe.separations);
        return recipeId;
    }

    /**
     * Get a recipe_activity with ingredients and steps.
     * @param id
     * @return
     */
    public Recipe getRecipe(long id) {
        Recipe recipe = new Recipe();
        // Execute query
        Cursor c = mDb.rawQuery(
                String.format(Locale.getDefault(), "Select * From %s Where %s = ?;",
                        DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_KEY),
                new String[] {String.valueOf(id)});
        if (c.getCount() > 0) {
            c.moveToFirst();
            recipe.id = c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_KEY));
            recipe.name = c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_NAME));
            recipe.type = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TYPE));
            recipe.difficulty = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_DIFFICULTY));
            recipe.time = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TIME));
            recipe.grade = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_GRADE));
            recipe.people = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_PEOPLE));
            recipe.number = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_NUMBER));
        }
        c.close();
        // Then also get ingredients, steps and separations for each recipe_activity.
        c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                        "Select q.*, i.%s From %s as q, %s as i " +
                        "Where q.%s = ? And i.%s = q.%s Order By q.%s ASC, q.%s ASC;",
                        DatabaseHandler.INGREDIENTS_NAME, DatabaseHandler.QUANTITIES_TABLE_NAME,
                        DatabaseHandler.INGREDIENTS_TABLE_NAME, DatabaseHandler.QUANTITIES_RECIPE,
                        DatabaseHandler.INGREDIENTS_KEY, DatabaseHandler.QUANTITIES_INGREDIENT,
                        DatabaseHandler.QUANTITIES_NUMBER, DatabaseHandler.QUANTITIES_KEY),
                new String[] {String.valueOf(id)});
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Ingredient ingredient = new Ingredient();
            ingredient.idQuantity = c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_KEY));
            ingredient.quantity = c.getFloat(c.getColumnIndex(DatabaseHandler.QUANTITIES_QUANTITY));
            ingredient.idUnit = c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_UNIT));
            ingredient.idIngredient = c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_INGREDIENT));
            ingredient.number = c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_NUMBER));
            ingredient.type = c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_TYPE));
            ingredient.name = c.getString(c.getColumnIndex(DatabaseHandler.INGREDIENTS_NAME));
            recipe.ingredients.add(ingredient);
        }
        c.close();
        c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                        "Select * From %s Where %s = ? Order By %s ASC, %s ASC",
                        DatabaseHandler.STEPS_TABLE_NAME, DatabaseHandler.STEPS_RECIPE,
                        DatabaseHandler.STEPS_NUMBER, DatabaseHandler.STEPS_KEY),
                new String[] {String.valueOf(id)});
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Step step = new Step();
            step.id = c.getLong(c.getColumnIndex(DatabaseHandler.STEPS_KEY));
            step.number = c.getInt(c.getColumnIndex(DatabaseHandler.STEPS_NUMBER));
            step.description = c.getString(c.getColumnIndex(DatabaseHandler.STEPS_DESCRIPTION));
            recipe.steps.add(step);
        }
        c.close();
        c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                        "Select * From %s Where %s = ? Order By %s ASC, %s ASC",
                        DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, DatabaseHandler.RECIPES_SEPARATION_RECIPE,
                        DatabaseHandler.RECIPES_SEPARATION_NUMBER, DatabaseHandler.RECIPES_SEPARATION_KEY),
                new String[] {String.valueOf(id)});
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Separation separation = new Separation();
            separation.id = c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_KEY));
            separation.number = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_NUMBER));
            separation.description = c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION));
            separation.type = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_TYPE));
            recipe.separations.add(separation);
        }
        c.close();
        return recipe;
    }

    /**
     * Get list of recipes_activity without ingredients and steps.
     * @param limit The limit of the number of recipes_activity to get.
     * @param lastId The last recipe_activity id from which we start the get.
     */
    public ArrayList<Recipe> getRecipes(int limit, long lastId) {
        // Construct the where clause depending on the given parameters
        String whereClause = "";
        if (lastId != -1)
            whereClause += "Where " + DatabaseHandler.RECIPES_KEY + "<" + String.valueOf(lastId);
        String limitClause = "";
        if (limit != -1)
            limitClause = "Limit " + String.valueOf(limit);
        // Execute query
        Cursor c = mDb.rawQuery(
                String.format(Locale.getDefault(),
                        "Select * From %s " + whereClause + " Order By %s DESC, %s DESC " + limitClause + ";",
                        DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_NUMBER,
                        DatabaseHandler.RECIPES_KEY),
                        new String[0]);
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Recipe newRecipe = new Recipe();
            newRecipe.id = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_KEY));
            newRecipe.name = c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_NAME));
            newRecipe.type = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TYPE));
            newRecipe.difficulty = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_DIFFICULTY));
            newRecipe.time = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TIME));
            newRecipe.grade = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_GRADE));
            newRecipe.people = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_PEOPLE));
            newRecipe.number = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_NUMBER));
            recipes.add(newRecipe);
        }
        c.close();
        return recipes;
    }
    public ArrayList<Recipe> getRecipes(int limit) {
        return getRecipes(limit, -1);
    }
    public ArrayList<Recipe> getRecipes() {
        return getRecipes(-1);
    }

    public ArrayList<Recipe> getRecipes(long[] recipesId) {
        String recipesIdString = "";
        for (int i = 0; i < recipesId.length; ++i) {
            recipesIdString += (i != 0 ? "," : "") + String.valueOf(recipesId[i]);
        }
        String query = String.format(Locale.getDefault(),
                "Select * From %1$s " +
                        "Where %2$s In (%3$s);",
                DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_KEY, recipesIdString);
        Cursor c = mDb.rawQuery(query, new String[0]);
        ArrayList<Recipe> recipes = new ArrayList<>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Recipe recipe = new Recipe();
            recipe.id = c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_KEY));
            recipe.name = c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_NAME));
            recipe.type = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TYPE));
            recipe.people = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_PEOPLE));
            recipe.grade = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_GRADE));
            recipe.difficulty = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_DIFFICULTY));
            recipe.time = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TIME));
            recipe.number = c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_NUMBER));
            recipes.add(recipe);
        }
        c.close();
        return recipes;
    }

    public void deleteRecipe(long recipeId) {
        // Delete the recipe_activity itself
        mDb.delete(DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_KEY + " = ?",
                new String[] {String.valueOf(recipeId)});
        // Delete all ingredients and steps.
        mDb.delete(DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_RECIPE + " = ?",
                new String[] {String.valueOf(recipeId)});
        mDb.delete(DatabaseHandler.STEPS_TABLE_NAME, DatabaseHandler.STEPS_RECIPE + " = ?",
                new String[] {String.valueOf(recipeId)});
        mDb.delete(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, DatabaseHandler.RECIPES_SEPARATION_RECIPE + " = ?",
                new String[] {String.valueOf(recipeId)});
    }

    public void incrementRecipeNumber(long recipeId) {
        mDb.execSQL(
                String.format(Locale.getDefault(),
                        "Update %1$s Set %2$s = %2$s + 1 Where %3$s = ?;",
                        DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_NUMBER,
                        DatabaseHandler.RECIPES_KEY),
                new String[] {String.valueOf(recipeId)});
    }

    //endregion

    //region QUANTITIES

    public void addEditQuantities(long recipeId, ArrayList<Ingredient> ingredients) {
        ArrayList<Integer> indicesToInsert = new ArrayList<>(), indicesToUpdate = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); ++i) {
            if (ingredients.get(i).idQuantity == -1)
                indicesToInsert.add(i);
            else
                indicesToUpdate.add(i);
        }
        if (indicesToInsert.size() > 0) {
            // Insert new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Into %s (%s, %s, %s, %s, %s, %s) Values ",
                    DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_RECIPE,
                    DatabaseHandler.QUANTITIES_QUANTITY, DatabaseHandler.QUANTITIES_UNIT,
                    DatabaseHandler.QUANTITIES_INGREDIENT, DatabaseHandler.QUANTITIES_NUMBER,
                    DatabaseHandler.QUANTITIES_TYPE);
            String[] values = new String[6 * indicesToInsert.size()];
            for (int i = 0; i < indicesToInsert.size(); ++i) {
                Ingredient ingredient = ingredients.get(indicesToInsert.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?, ?, ?)";
                values[6 * i] = String.valueOf(recipeId);
                values[6 * i + 1] = String.valueOf(ingredient.quantity);
                values[6 * i + 2] = String.valueOf(ingredient.idUnit);
                values[6 * i + 3] = String.valueOf(ingredient.idIngredient);
                values[6 * i + 4] = String.valueOf(ingredient.number);
                values[6 * i + 5] = String.valueOf(ingredient.type);
            }
            query += ";";
            mDb.execSQL(query, values);
        }
        if (indicesToUpdate.size() > 0) {
            // Update new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Or Replace Into %s (%s, %s, %s, %s, %s, %s, %s) Values ",
                    DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_KEY,
                    DatabaseHandler.QUANTITIES_RECIPE, DatabaseHandler.QUANTITIES_QUANTITY,
                    DatabaseHandler.QUANTITIES_UNIT, DatabaseHandler.QUANTITIES_INGREDIENT,
                    DatabaseHandler.QUANTITIES_NUMBER, DatabaseHandler.QUANTITIES_TYPE);
            String[] values = new String[7 * indicesToUpdate.size()];
            for (int i = 0; i < indicesToUpdate.size(); ++i) {
                Ingredient ingredient = ingredients.get(indicesToUpdate.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?, ?, ?, ?)";
                values[7 * i] = String.valueOf(ingredient.idQuantity);
                values[7 * i + 1] = String.valueOf(recipeId);
                values[7 * i + 2] = String.valueOf(ingredient.quantity);
                values[7 * i + 3] = String.valueOf(ingredient.idUnit);
                values[7 * i + 4] = String.valueOf(ingredient.idIngredient);
                values[7 * i + 5] = String.valueOf(ingredient.number);
                values[7 * i + 6] = String.valueOf(ingredient.type);
            }
            mDb.execSQL(query, values);
        }
    }

    public void deleteQuantities(List<Long> ingredientsId) {
        if (ingredientsId.size() == 0)
            return;
        String whereClause = DatabaseHandler.QUANTITIES_KEY + " IN (";
        for (int i = 0; i < ingredientsId.size(); ++i)
            whereClause += (i != 0 ? "," : "") + String.valueOf(ingredientsId.get(i));
        whereClause += ")";
        mDb.delete(DatabaseHandler.QUANTITIES_TABLE_NAME, whereClause, new String[0]);
    }

    //endregion

    //region STEPS

    public void addEditSteps(long recipeId, ArrayList<Step> steps) {
        ArrayList<Integer> indicesToInsert = new ArrayList<>(), indicesToUpdate = new ArrayList<>();
        for (int i = 0; i < steps.size(); ++i) {
            if (steps.get(i).id == -1)
                indicesToInsert.add(i);
            else
                indicesToUpdate.add(i);
        }
        if (indicesToInsert.size() > 0) {
            // Insert new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Into %s (%s, %s, %s) Values ",
                    DatabaseHandler.STEPS_TABLE_NAME, DatabaseHandler.STEPS_RECIPE,
                    DatabaseHandler.STEPS_NUMBER, DatabaseHandler.STEPS_DESCRIPTION);
            String[] values = new String[3 * indicesToInsert.size()];
            for (int i = 0; i < indicesToInsert.size(); ++i) {
                Step step = steps.get(indicesToInsert.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?)";
                values[3 * i] = String.valueOf(recipeId);
                values[3 * i + 1] = String.valueOf(step.number);
                values[3 * i + 2] = step.description;
            }
            query += ";";
            mDb.execSQL(query, values);
        }
        if (indicesToUpdate.size() > 0) {
            // Update new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Or Replace Into %s (%s, %s, %s, %s) Values ",
                    DatabaseHandler.STEPS_TABLE_NAME, DatabaseHandler.STEPS_KEY,
                    DatabaseHandler.STEPS_RECIPE, DatabaseHandler.STEPS_NUMBER,
                    DatabaseHandler.STEPS_DESCRIPTION);
            String[] values = new String[4 * indicesToUpdate.size()];
            for (int i = 0; i < indicesToUpdate.size(); ++i) {
                Step step = steps.get(indicesToUpdate.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?)";
                values[4 * i] = String.valueOf(step.id);
                values[4 * i + 1] = String.valueOf(recipeId);
                values[4 * i + 2] = String.valueOf(step.number);
                values[4 * i + 3] = step.description;
            }
            mDb.execSQL(query, values);
        }
    }

    public void deleteSteps(List<Long> stepsId) {
        if (stepsId.size() == 0)
            return;
        String whereClause = DatabaseHandler.STEPS_KEY + " IN (";
        for (int i = 0; i < stepsId.size(); ++i)
            whereClause += (i != 0 ? "," : "") + String.valueOf(stepsId.get(i));
        whereClause += ")";
        mDb.delete(DatabaseHandler.STEPS_TABLE_NAME, whereClause, new String[0]);
    }

    //endregion

    //region SEPARATIONS

    public void addEditSeparations(long recipeId, ArrayList<Separation> separations) {
        ArrayList<Integer> indicesToInsert = new ArrayList<>(), indicesToUpdate = new ArrayList<>();
        for (int i = 0; i < separations.size(); ++i) {
            if (separations.get(i).id == -1)
                indicesToInsert.add(i);
            else
                indicesToUpdate.add(i);
        }
        if (indicesToInsert.size() > 0) {
            // Insert new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Into %s (%s, %s, %s, %s) Values ",
                    DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, DatabaseHandler.RECIPES_SEPARATION_RECIPE,
                    DatabaseHandler.RECIPES_SEPARATION_TYPE, DatabaseHandler.RECIPES_SEPARATION_NUMBER,
                    DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION);
            String[] values = new String[4 * indicesToInsert.size()];
            for (int i = 0; i < indicesToInsert.size(); ++i) {
                Separation separation = separations.get(indicesToInsert.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?)";
                values[4 * i] = String.valueOf(recipeId);
                values[4 * i + 1] = String.valueOf(separation.type);
                values[4 * i + 2] = String.valueOf(separation.number);
                values[4 * i + 3] = separation.description;
            }
            query += ";";
            mDb.execSQL(query, values);
        }
        if (indicesToUpdate.size() > 0) {
            // Update new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Or Replace Into %s (%s, %s, %s, %s, %s) Values ",
                    DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, DatabaseHandler.RECIPES_SEPARATION_KEY,
                    DatabaseHandler.RECIPES_SEPARATION_RECIPE, DatabaseHandler.RECIPES_SEPARATION_TYPE,
                    DatabaseHandler.RECIPES_SEPARATION_NUMBER, DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION);
            String[] values = new String[5 * indicesToUpdate.size()];
            for (int i = 0; i < indicesToUpdate.size(); ++i) {
                Separation separation = separations.get(indicesToUpdate.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?, ?)";
                values[5 * i] = String.valueOf(separation.id);
                values[5 * i + 1] = String.valueOf(recipeId);
                values[5 * i + 2] = String.valueOf(separation.type);
                values[5 * i + 3] = String.valueOf(separation.number);
                values[5 * i + 4] = separation.description;
            }
            mDb.execSQL(query, values);
        }
    }

    public void deleteSeparations(List<Long> separationsId) {
        if (separationsId.size() == 0)
            return;
        String whereClause = DatabaseHandler.RECIPES_SEPARATION_KEY + " IN (";
        for (int i = 0; i < separationsId.size(); ++i)
            whereClause += (i != 0 ? "," : "") + String.valueOf(separationsId.get(i));
        whereClause += ")";
        mDb.delete(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, whereClause, new String[0]);
    }

    //endregion

    //region SHOPPING

    public ArrayList<Ingredient> getQuantitiesFromRecipes(long[] recipesId, LongSparseArray<Float> peopleRatio) {
        String query = String.format(Locale.getDefault(),
                "Select q.%8$s, q.%1$s, q.%2$s, q.%3$s, i.%4$s, q.%10$s From %5$s as q, %6$s as i " +
                "Where q.%3$s = i.%7$s And q.%8$s In %9$s " +
                "Order By q.%3$s ASC, q.%2$s DESC;", /* Order first by ingredient id, then by unit id */
                DatabaseHandler.QUANTITIES_QUANTITY, DatabaseHandler.QUANTITIES_UNIT, DatabaseHandler.QUANTITIES_INGREDIENT,
                DatabaseHandler.INGREDIENTS_NAME, DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.INGREDIENTS_TABLE_NAME,
                DatabaseHandler.INGREDIENTS_KEY, DatabaseHandler.QUANTITIES_RECIPE, DAOBase.formatLongArrayForInQuery(recipesId),
                DatabaseHandler.QUANTITIES_TYPE);
        Cursor c = mDb.rawQuery(query, new String[0]);
        ArrayList<Ingredient> quantities = new ArrayList<>();
        Ingredient ingredient = null;
        float optionalQuantity = 0f;
        long previousIngredientId = -1;
        int previousUnitId = -1;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long recipeId = c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_RECIPE));
            float quantity = c.getFloat(c.getColumnIndex(DatabaseHandler.QUANTITIES_QUANTITY));
            int unitId = c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_UNIT));
            long ingredientId = c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_INGREDIENT));
            int quantityType = c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_TYPE));
            String ingredientName = c.getString(c.getColumnIndex(DatabaseHandler.INGREDIENTS_NAME));
            boolean addNewIngredient = false;
            float quantityFactor;
            if (ingredient != null && previousIngredientId == ingredientId) {
                // Try to convert first unit into the second one
                quantityFactor = UnitsConversion.convert(this, unitId, previousUnitId, ingredientId);
                if (quantityFactor == 0) {
                    addNewIngredient = true;
                    quantityFactor = 1;
                }
            }
            else {
                addNewIngredient = true;
                quantityFactor = 1f;
            }
            if (addNewIngredient) {
                // Ingredients or units does not correspond, thus need to add a new ingredient
                if (ingredient != null) {
                    // Add the previous ingredient to quantities.
                    if (ingredient.quantity != 0)
                        quantities.add(ingredient);
                    // Add the previous optional ingredient to quantities if not zero.
                    if (optionalQuantity != 0) {
                        Ingredient optionalIngredient = new Ingredient(ingredient.idQuantity,
                                ingredient.idIngredient, ingredient.idUnit, ingredient.name, optionalQuantity);
                        optionalIngredient.type = Ingredient.OPTIONAL_TYPE;
                        quantities.add(optionalIngredient);
                    }
                }
                // Initialize the new ingredient found.
                ingredient = new Ingredient(-1, ingredientId, unitId, ingredientName, 0);
                optionalQuantity = 0;
                previousIngredientId = ingredientId;
                previousUnitId = unitId;
                quantityFactor = 1;
            }
            // Finally add quantities, depending on if the ingredient is optional or not.
            if (quantityType == Ingredient.NORMAL_TYPE)
                ingredient.quantity += quantityFactor * quantity * peopleRatio.get(recipeId);
            else
                optionalQuantity += quantityFactor * quantity * peopleRatio.get(recipeId);
        }
        if (ingredient != null) {
            if (ingredient.quantity != 0)
                quantities.add(ingredient);
            if (optionalQuantity != 0) {
                Ingredient optionalIngredient = new Ingredient(ingredient.idQuantity,
                        ingredient.idIngredient, ingredient.idUnit, ingredient.name, optionalQuantity);
                optionalIngredient.type = Ingredient.OPTIONAL_TYPE;
                quantities.add(optionalIngredient);
            }
        }
        return quantities;
    }

    //endregion

    //region CONVERSION

    public List<IngredientConversions> getIngredientsWithConversions(long[] ingredientsId) {
        List<IngredientConversions> ingredients = new ArrayList<>();
        LongSparseArray<Integer> idToIndex = new LongSparseArray<Integer>();
        String ingredientsIdInQuery = (ingredientsId != null ? formatLongArrayForInQuery(ingredientsId) : null);
        // First get ingredients
        String whereClause  = "";
        if (ingredientsIdInQuery != null)
            whereClause = " Where " + DatabaseHandler.INGREDIENTS_KEY + " In " + ingredientsIdInQuery;
        Cursor c = mDb.rawQuery("Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME +
                        whereClause + " Order By " + DatabaseHandler.INGREDIENTS_NAME + " ASC;",
                new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getLong(c.getColumnIndex(DatabaseHandler.INGREDIENTS_KEY));
            ingredients.add(new IngredientConversions(
                    id, c.getString(c.getColumnIndex(DatabaseHandler.INGREDIENTS_NAME))));
            idToIndex.put(id, ingredients.size() - 1);
        }
        c.close();
        // Then get conversions linked with ingredients
        whereClause = "";
        if (ingredientsIdInQuery != null)
            whereClause = " Where " + DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT + " In " + ingredientsIdInQuery;
        c = mDb.rawQuery("Select * From " + DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME +
                        whereClause + " Order By " + DatabaseHandler.RECIPES_CONVERSIONS_KEY + " ASC;",
                new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Conversion conversion = new Conversion(
                    c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_KEY)),
                    c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM)),
                    c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO)),
                    c.getFloat(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR))
            );
            long ingredientId = c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT));
            if (idToIndex.indexOfKey(ingredientId) >= 0) {
                ingredients.get(idToIndex.get(ingredientId)).conversions.add(conversion);
            }
        }
        c.close();

        return ingredients;
    }
    public List<IngredientConversions> getIngredientsWithConversions() {
        return getIngredientsWithConversions(null);
    }

    public long addEditConversion(long ingredientId, Conversion conversion) {
        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT, ingredientId);
        value.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM, conversion.unitFrom);
        value.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO, conversion.unitTo);
        value.put(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR, conversion.factor);
        long conversionId = conversion.id;
        if (conversionId < 0)
            conversionId = mDb.insert(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, null, value);
        else
            mDb.update(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, value,
                    DatabaseHandler.RECIPES_CONVERSIONS_KEY + " = ?",
                    new String[]{String.valueOf(conversionId)});
        return conversionId;
    }

    public int deleteConversion(long conversionId) {
        return mDb.delete(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME,
                DatabaseHandler.RECIPES_CONVERSIONS_KEY + " = ?",
                new String[]{String.valueOf(conversionId)});
    }

    /**
     * Get the list of ingredient for the given ingredient id.
     */
    public List<Conversion> getConversions(long ingredientId) {
        Cursor c = mDb.rawQuery(
                "Select * From " + DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME +
                        " Where " + DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT + " = ?;",
                new String[]{String.valueOf(ingredientId)});
        List<Conversion> conversions = new ArrayList<>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            conversions.add(new Conversion(
                c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_KEY)),
                c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM)),
                c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO)),
                c.getFloat(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR))
            ));
        }
        c.close();
        return conversions;
    }

    //endregion

    //region LOAD_SAVE_EXTERNAL_STORAGE

    public static String EXTERNAL_RECIPES_FILENAME = "Recipes.json";

    /**
     * Save recipes_activity data to external storage in fil EXTERNAL_RECIPES_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean saveDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to write the file
        Utils.verifyStoragePermissions(activity);

        JSONObject obj = new JSONObject();
        // Recipes
        JSONArray recipesObj = new JSONArray();
        Cursor c = db.rawQuery("Select * From " + DatabaseHandler.RECIPES_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject recipeObj = new JSONObject();
            recipeObj.put(DatabaseHandler.RECIPES_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_KEY)));
            recipeObj.put(DatabaseHandler.RECIPES_NAME, c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_NAME)));
            recipeObj.put(DatabaseHandler.RECIPES_TYPE, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TYPE)));
            recipeObj.put(DatabaseHandler.RECIPES_DIFFICULTY, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_DIFFICULTY)));
            recipeObj.put(DatabaseHandler.RECIPES_GRADE, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_GRADE)));
            recipeObj.put(DatabaseHandler.RECIPES_TIME, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_TIME)));
            recipesObj.put(recipeObj);
        }
        obj.put(DatabaseHandler.RECIPES_TABLE_NAME, recipesObj);
        c.close();
        // Ingredients
        JSONArray ingredientsObj = new JSONArray();
        c = db.rawQuery("Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject ingredientObj = new JSONObject();
            ingredientObj.put(DatabaseHandler.INGREDIENTS_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.INGREDIENTS_KEY)));
            ingredientObj.put(DatabaseHandler.INGREDIENTS_NAME, c.getString(c.getColumnIndex(DatabaseHandler.INGREDIENTS_NAME)));
            ingredientsObj.put(ingredientObj);
        }
        obj.put(DatabaseHandler.INGREDIENTS_TABLE_NAME, ingredientsObj);
        c.close();
        // Quantities
        JSONArray quantitiesObj = new JSONArray();
        c = db.rawQuery("Select * From " + DatabaseHandler.QUANTITIES_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject quantityObj = new JSONObject();
            quantityObj.put(DatabaseHandler.QUANTITIES_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_KEY)));
            quantityObj.put(DatabaseHandler.QUANTITIES_RECIPE, c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_RECIPE)));
            quantityObj.put(DatabaseHandler.QUANTITIES_QUANTITY, c.getDouble(c.getColumnIndex(DatabaseHandler.QUANTITIES_QUANTITY)));
            quantityObj.put(DatabaseHandler.QUANTITIES_UNIT, c.getInt(c.getColumnIndex(DatabaseHandler.QUANTITIES_UNIT)));
            quantityObj.put(DatabaseHandler.QUANTITIES_INGREDIENT, c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_INGREDIENT)));
            quantityObj.put(DatabaseHandler.QUANTITIES_NUMBER, c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_NUMBER)));
            quantityObj.put(DatabaseHandler.QUANTITIES_TYPE, c.getLong(c.getColumnIndex(DatabaseHandler.QUANTITIES_TYPE)));
            quantitiesObj.put(quantityObj);
        }
        obj.put(DatabaseHandler.QUANTITIES_TABLE_NAME, quantitiesObj);
        c.close();
        // Steps
        JSONArray stepsObj = new JSONArray();
        c = db.rawQuery("Select * From " + DatabaseHandler.STEPS_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject stepObj = new JSONObject();
            stepObj.put(DatabaseHandler.STEPS_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.STEPS_KEY)));
            stepObj.put(DatabaseHandler.STEPS_RECIPE, c.getLong(c.getColumnIndex(DatabaseHandler.STEPS_RECIPE)));
            stepObj.put(DatabaseHandler.STEPS_NUMBER, c.getInt(c.getColumnIndex(DatabaseHandler.STEPS_NUMBER)));
            stepObj.put(DatabaseHandler.STEPS_DESCRIPTION, c.getString(c.getColumnIndex(DatabaseHandler.STEPS_DESCRIPTION)));
            stepsObj.put(stepObj);
        }
        obj.put(DatabaseHandler.STEPS_TABLE_NAME, stepsObj);
        c.close();
        // Separations
        JSONArray separationsObj = new JSONArray();
        c = db.rawQuery("Select * From " + DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject separationObj = new JSONObject();
            separationObj.put(DatabaseHandler.RECIPES_SEPARATION_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_KEY)));
            separationObj.put(DatabaseHandler.RECIPES_SEPARATION_RECIPE, c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_RECIPE)));
            separationObj.put(DatabaseHandler.RECIPES_SEPARATION_NUMBER, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_NUMBER)));
            separationObj.put(DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION, c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION)));
            separationObj.put(DatabaseHandler.RECIPES_SEPARATION_TYPE, c.getString(c.getColumnIndex(DatabaseHandler.RECIPES_SEPARATION_TYPE)));
            separationsObj.put(separationObj);
        }
        obj.put(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, separationsObj);
        c.close();
        // Conversions
        JSONArray conversionsObj = new JSONArray();
        c = db.rawQuery("Select * From " + DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject conversionObj = new JSONObject();
            conversionObj.put(DatabaseHandler.RECIPES_CONVERSIONS_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_KEY)));
            conversionObj.put(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT, c.getLong(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT)));
            conversionObj.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM)));
            conversionObj.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO, c.getInt(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO)));
            conversionObj.put(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR, c.getDouble(c.getColumnIndex(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR)));
            conversionsObj.put(conversionObj);
        }
        obj.put(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, conversionsObj);
        c.close();

        return Utils.writeToExternalStorage(activity, EXTERNAL_RECIPES_FILENAME, obj.toString());
    }

    /**
     * Load recipes_activity data from external storage in file EXTERNAL_RECIPES_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean loadDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to read the file
        Utils.verifyStoragePermissions(activity);

        String fileContent = Utils.readFromExternalStorage(activity, EXTERNAL_RECIPES_FILENAME);
        if (fileContent == null)
            return false;

        JSONObject obj = new JSONObject(fileContent);
        // Recipes
        JSONArray recipesObj = obj.getJSONArray(DatabaseHandler.RECIPES_TABLE_NAME);
        if (recipesObj.length() > 0) {
            for (int i = 0; i < recipesObj.length(); ++i) {
                JSONObject recipeObj = recipesObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.RECIPES_KEY, recipeObj.getLong(DatabaseHandler.RECIPES_KEY));
                values.put(DatabaseHandler.RECIPES_NAME, recipeObj.getString(DatabaseHandler.RECIPES_NAME));
                values.put(DatabaseHandler.RECIPES_DIFFICULTY, recipeObj.getInt(DatabaseHandler.RECIPES_DIFFICULTY));
                values.put(DatabaseHandler.RECIPES_GRADE, recipeObj.getInt(DatabaseHandler.RECIPES_GRADE));
                values.put(DatabaseHandler.RECIPES_TIME, recipeObj.getInt(DatabaseHandler.RECIPES_TIME));
                values.put(DatabaseHandler.RECIPES_TYPE, recipeObj.getInt(DatabaseHandler.RECIPES_TYPE));
                try {
                    db.insertOrThrow(DatabaseHandler.RECIPES_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }
        // Ingredients
        JSONArray ingredientsObj = obj.getJSONArray(DatabaseHandler.INGREDIENTS_TABLE_NAME);
        if (ingredientsObj.length() > 0) {
            for (int i = 0; i < ingredientsObj.length(); ++i) {
                JSONObject ingredientObj = ingredientsObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.INGREDIENTS_KEY, ingredientObj.getLong(DatabaseHandler.INGREDIENTS_KEY));
                values.put(DatabaseHandler.INGREDIENTS_NAME, ingredientObj.getString(DatabaseHandler.INGREDIENTS_NAME));
                try {
                    db.insertOrThrow(DatabaseHandler.INGREDIENTS_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }
        // Quantities
        JSONArray quantitiesObj = obj.getJSONArray(DatabaseHandler.QUANTITIES_TABLE_NAME);
        if (quantitiesObj.length() > 0) {
            for (int i = 0; i < quantitiesObj.length(); ++i) {
                JSONObject quantityObj = quantitiesObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.QUANTITIES_KEY, quantityObj.getLong(DatabaseHandler.QUANTITIES_KEY));
                values.put(DatabaseHandler.QUANTITIES_RECIPE, quantityObj.getLong(DatabaseHandler.QUANTITIES_RECIPE));
                values.put(DatabaseHandler.QUANTITIES_QUANTITY, quantityObj.getDouble(DatabaseHandler.QUANTITIES_QUANTITY));
                values.put(DatabaseHandler.QUANTITIES_UNIT, quantityObj.getInt(DatabaseHandler.QUANTITIES_UNIT));
                values.put(DatabaseHandler.QUANTITIES_INGREDIENT, quantityObj.getLong(DatabaseHandler.QUANTITIES_INGREDIENT));
                values.put(DatabaseHandler.QUANTITIES_NUMBER, quantityObj.getLong(DatabaseHandler.QUANTITIES_NUMBER));
                values.put(DatabaseHandler.QUANTITIES_TYPE, quantityObj.getLong(DatabaseHandler.QUANTITIES_TYPE));
                try {
                    db.insertOrThrow(DatabaseHandler.QUANTITIES_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }
        // Steps
        JSONArray stepsObj = obj.getJSONArray(DatabaseHandler.STEPS_TABLE_NAME);
        if (stepsObj.length() > 0) {
            for (int i = 0; i < stepsObj.length(); ++i) {
                JSONObject stepObj = stepsObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.STEPS_KEY, stepObj.getLong(DatabaseHandler.STEPS_KEY));
                values.put(DatabaseHandler.STEPS_NUMBER, stepObj.getInt(DatabaseHandler.STEPS_NUMBER));
                values.put(DatabaseHandler.STEPS_RECIPE, stepObj.getLong(DatabaseHandler.STEPS_RECIPE));
                values.put(DatabaseHandler.STEPS_DESCRIPTION, stepObj.getString(DatabaseHandler.STEPS_DESCRIPTION));
                try {
                    db.insertOrThrow(DatabaseHandler.STEPS_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }
        // Separations
        JSONArray separationsObj = obj.getJSONArray(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME);
        if (separationsObj.length() > 0) {
            for (int i = 0; i < separationsObj.length(); ++i) {
                JSONObject separationObj = separationsObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.RECIPES_SEPARATION_KEY, separationObj.getLong(DatabaseHandler.RECIPES_SEPARATION_KEY));
                values.put(DatabaseHandler.RECIPES_SEPARATION_NUMBER, separationObj.getInt(DatabaseHandler.RECIPES_SEPARATION_NUMBER));
                values.put(DatabaseHandler.RECIPES_SEPARATION_RECIPE, separationObj.getLong(DatabaseHandler.RECIPES_SEPARATION_RECIPE));
                values.put(DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION, separationObj.getString(DatabaseHandler.RECIPES_SEPARATION_DESCRIPTION));
                values.put(DatabaseHandler.RECIPES_SEPARATION_TYPE, separationObj.getString(DatabaseHandler.RECIPES_SEPARATION_TYPE));
                try {
                    db.insertOrThrow(DatabaseHandler.RECIPES_SEPARATION_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }
        // Conversions
        JSONArray conversionsObj = obj.getJSONArray(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME);
        if (conversionsObj.length() > 0) {
            for (int i = 0; i < conversionsObj.length(); ++i) {
                JSONObject conversionObj = conversionsObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.RECIPES_CONVERSIONS_KEY, conversionObj.getLong(DatabaseHandler.RECIPES_CONVERSIONS_KEY));
                values.put(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT, conversionObj.getLong(DatabaseHandler.RECIPES_CONVERSIONS_INGREDIENT));
                values.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM, conversionObj.getInt(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_FROM));
                values.put(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO, conversionObj.getInt(DatabaseHandler.RECIPES_CONVERSIONS_UNIT_TO));
                values.put(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR, conversionObj.getDouble(DatabaseHandler.RECIPES_CONVERSIONS_FACTOR));
                try {
                    db.insertOrThrow(DatabaseHandler.RECIPES_CONVERSIONS_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }

        return true;
    }

    //endregion
}
