package buthod.tony.appManager.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import buthod.tony.appManager.Utils;
import buthod.tony.appManager.recipes.UnitsConversion;

/**
 * Created by Tony on 09/10/2017.
 */

public class RecipesDAO extends DAOBase {

    /**
     * Class containing the ingredient and the quantity to use for a recipe_activity.
     */
    public static class Ingredient {
        public long idQuantity = -1; // The id of the quantity in database QUANTITIES
        public long idIngredient = -1; // The id of the ingredient to use
        public int idUnit = -1; // The id of the unit to use.
        public String name; // The ingredient name.
        public float quantity; // The quantity to use

        public Ingredient() {

        }

        public Ingredient(long idQuantity, long idIngredient, int idUnit, String name, float quantity) {
            this.idQuantity = idQuantity;
            this.idIngredient = idIngredient;
            this.idUnit = idUnit;
            this.name = name;
            this.quantity = quantity;
        }
    }

    public static class Step {
        public long id = -1;
        public int number;
        public String description;
    }

    public static class Recipe {
        public long id;
        public String name;
        public int type;
        public int difficulty;
        public int time; // Preparation time
        public int grade;
        public int people;
        public ArrayList<Ingredient> ingredients;
        public ArrayList<Step> steps;

        public Recipe() {
            id = -1;
            name = "";
            type = -1;
            difficulty = -1;
            time = -1;
            grade = -1;
            ingredients = new ArrayList<>();
            steps = new ArrayList<>();
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
        Cursor c = mDb.rawQuery("Select * From " + DatabaseHandler.INGREDIENTS_TABLE_NAME + ";",
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
        long recipeId = recipe.id;
        if (isNew)
            recipeId = mDb.insert(DatabaseHandler.RECIPES_TABLE_NAME, null, value);
        else
            mDb.update(DatabaseHandler.RECIPES_TABLE_NAME, value,
                    DatabaseHandler.RECIPES_KEY + " = ?", new String[]{String.valueOf(recipe.id)});

        addEditQuantities(recipeId, recipe.ingredients);
        addEditSteps(recipeId, recipe.steps);
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
        Cursor c = mDb.rawQuery( String.format(Locale.getDefault(), "Select * From %s Where %s = ?;",
                DatabaseHandler.RECIPES_TABLE_NAME, DatabaseHandler.RECIPES_KEY),
                new String[] {String.valueOf(id)});
        if (c.getCount() > 0) {
            c.moveToFirst();
            recipe.id = c.getLong(0);
            recipe.name = c.getString(1);
            recipe.type = c.getInt(2);
            recipe.difficulty = c.getInt(3);
            recipe.time = c.getInt(4);
            recipe.grade = c.getInt(5);
            recipe.people = c.getInt(6);
        }
        c.close();
        // Then also get ingredients and steps for each recipe_activity.
        c = mDb.rawQuery("Select q.*, i." + DatabaseHandler.INGREDIENTS_NAME +
                        " From " + DatabaseHandler.QUANTITIES_TABLE_NAME + " as q, " +
                        DatabaseHandler.INGREDIENTS_TABLE_NAME + " as i" +
                        " Where q." + DatabaseHandler.QUANTITIES_RECIPE + " = ?" +
                        " And i." + DatabaseHandler.INGREDIENTS_KEY + " = q." + DatabaseHandler.QUANTITIES_INGREDIENT  +
                        " Order By q." + DatabaseHandler.QUANTITIES_KEY + " ASC",
                new String[] {String.valueOf(id)});
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Ingredient ingredient = new Ingredient();
            ingredient.idQuantity = c.getLong(0);
            ingredient.quantity = c.getFloat(2);
            ingredient.idUnit = c.getInt(3);
            ingredient.idIngredient = c.getLong(4);
            ingredient.name = c.getString(5);
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
            step.id = c.getLong(0);
            step.number = c.getInt(2);
            step.description = c.getString(3);
            recipe.steps.add(step);
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
            whereClause += DatabaseHandler.RECIPES_KEY + "<" + String.valueOf(lastId);
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select * From " + DatabaseHandler.RECIPES_TABLE_NAME +
                        (whereClause.isEmpty() ? "" : " Where " + whereClause) +
                        " Order By " + DatabaseHandler.RECIPES_KEY + " DESC" +
                        (limit != -1 ? " Limit " + String.valueOf(limit) : "") +
                        ";", new String[0]);
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Recipe newRecipe = new Recipe();
            newRecipe.id = c.getInt(0);
            newRecipe.name = c.getString(1);
            newRecipe.type = c.getInt(2);
            newRecipe.difficulty = c.getInt(3);
            newRecipe.time = c.getInt(4);
            newRecipe.grade = c.getInt(5);
            newRecipe.people = c.getInt(6);
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
            recipes.add(recipe);
        }
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
                    "Insert Into %s (%s, %s, %s, %s) Values ",
                    DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_RECIPE,
                    DatabaseHandler.QUANTITIES_QUANTITY, DatabaseHandler.QUANTITIES_UNIT,
                    DatabaseHandler.QUANTITIES_INGREDIENT);
            String[] values = new String[4 * indicesToInsert.size()];
            for (int i = 0; i < indicesToInsert.size(); ++i) {
                Ingredient ingredient = ingredients.get(indicesToInsert.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?)";
                values[4 * i] = String.valueOf(recipeId);
                values[4 * i + 1] = String.valueOf(ingredient.quantity);
                values[4 * i + 2] = String.valueOf(ingredient.idUnit);
                values[4 * i + 3] = String.valueOf(ingredient.idIngredient);
            }
            query += ";";
            mDb.execSQL(query, values);
        }
        if (indicesToUpdate.size() > 0) {
            // Update new quantities values
            String query = String.format(Locale.getDefault(),
                    "Insert Or Replace Into %s (%s, %s, %s, %s, %s) Values ",
                    DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.QUANTITIES_KEY,
                    DatabaseHandler.QUANTITIES_RECIPE, DatabaseHandler.QUANTITIES_QUANTITY,
                    DatabaseHandler.QUANTITIES_UNIT, DatabaseHandler.QUANTITIES_INGREDIENT);
            String[] values = new String[5 * indicesToUpdate.size()];
            for (int i = 0; i < indicesToUpdate.size(); ++i) {
                Ingredient ingredient = ingredients.get(indicesToUpdate.get(i));
                if (i != 0)
                    query += ", ";
                query += "(?, ?, ?, ?, ?)";
                values[5 * i] = String.valueOf(ingredient.idQuantity);
                values[5 * i + 1] = String.valueOf(recipeId);
                values[5 * i + 2] = String.valueOf(ingredient.quantity);
                values[5 * i + 3] = String.valueOf(ingredient.idUnit);
                values[5 * i + 4] = String.valueOf(ingredient.idIngredient);
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

    //region ADVANCED

    public ArrayList<Ingredient> getQuantitiesFromRecipes(long[] recipesId, LongSparseArray<Float> peopleRatio) {
        String query = String.format(Locale.getDefault(),
                "Select q.%8$s, q.%1$s, q.%2$s, q.%3$s, i.%4$s From %5$s as q, %6$s as i " +
                "Where q.%3$s = i.%7$s And q.%8$s In %9$s " +
                "Order By q.%3$s ASC, q.%2$s DESC;", /* Order first by ingredient id, then by unit id */
                DatabaseHandler.QUANTITIES_QUANTITY, DatabaseHandler.QUANTITIES_UNIT, DatabaseHandler.QUANTITIES_INGREDIENT,
                DatabaseHandler.INGREDIENTS_NAME, DatabaseHandler.QUANTITIES_TABLE_NAME, DatabaseHandler.INGREDIENTS_TABLE_NAME,
                DatabaseHandler.INGREDIENTS_KEY, DatabaseHandler.QUANTITIES_RECIPE,
                DAOBase.formatLongArrayForInQuery(recipesId));
        Cursor c = mDb.rawQuery(query, new String[0]);
        ArrayList<Ingredient> quantities = new ArrayList<>();
        Ingredient ingredient = null;
        long previousIngredientId = -1;
        int previousUnitId = -1;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long recipeId = c.getLong(0);
            float quantity = c.getFloat(1);
            int unitId = c.getInt(2);
            long ingredientId = c.getLong(3);
            String ingredientName = c.getString(4);
            if (ingredient != null && previousIngredientId == ingredientId) {
                // Try to convert first unit into the second one
                float factor = UnitsConversion.convert(this, unitId, previousUnitId, ingredientId);
                if (factor != 0) {
                    // Conversion with success
                    ingredient.quantity += factor * quantity * peopleRatio.get(recipeId);
                }
                else {
                    // No conversion possible
                    quantities.add(ingredient);
                    ingredient = new Ingredient(-1, ingredientId, unitId, ingredientName,
                            quantity * peopleRatio.get(recipeId));
                    previousUnitId = unitId;
                }
            }
            else {
                if (ingredient != null)
                    quantities.add(ingredient);
                ingredient = new Ingredient(-1, ingredientId, unitId, ingredientName,
                        quantity * peopleRatio.get(recipeId));
                previousIngredientId = ingredientId;
                previousUnitId = unitId;
            }
        }
        if (ingredient != null)
            quantities.add(ingredient);
        return quantities;
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

        return true;
    }

    //endregion
}
