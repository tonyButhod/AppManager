package buthod.tony.appManager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.SparseIntArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Tony on 09/10/2017.
 */

public class RecipesDAO extends DAOBase {

    public static class Ingredient {
        public long idIngredient;
        public long idQuantity;
        public long idUnit;
        public String name;
        public float quantity;
        public String unit;
    }

    public static class Step {
        public long id;
        public String description;
    }

    public static class Recipe {
        public long id;
        public String name;
        public int type;
        public int difficulty;
        public int time; // Preparation time
        public int grade;
        public List<Ingredient> ingredients;
        public List<Step> steps;
    }

    public RecipesDAO(Context context) {
        super(context);
    }

    public long addIngredient(int name) {
        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.INGREDIENTS_NAME, name);
        return mDb.insert(DatabaseHandler.INGREDIENTS_TABLE_NAME, null, value);
    }

    public long addUnit(int name) {
        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.UNITS_NAME, name);
        return mDb.insert(DatabaseHandler.UNITS_TABLE_NAME, null, value);
    }

    public long addRecipe(Recipe recipe) {
        // Insert the recipe itself
        ContentValues value = new ContentValues();
        value.put(DatabaseHandler.RECIPES_NAME, recipe.name);
        value.put(DatabaseHandler.RECIPES_TYPE, recipe.type);
        value.put(DatabaseHandler.RECIPES_DIFFICULTY, recipe.difficulty);
        value.put(DatabaseHandler.RECIPES_TIME, recipe.time);
        value.put(DatabaseHandler.RECIPES_GRADE, recipe.grade);
        long recipeId = mDb.insert(DatabaseHandler.INGREDIENTS_TABLE_NAME, null, value);
        // Insert quantities
        String query = "Insert Into " + DatabaseHandler.QUANTITIES_TABLE_NAME +
                " (" + DatabaseHandler.QUANTITIES_RECIPE + ", " + DatabaseHandler.QUANTITIES_QUANTITY + ", " +
                DatabaseHandler.QUANTITIES_UNIT + ", " + DatabaseHandler.QUANTITIES_INGREDIENT + ") Values ";
        for (int i = 0; i < recipe.ingredients.size(); ++i) {
            Ingredient ingredient = recipe.ingredients.get(i);
            if (i != 0)
                query += ", ";
            query += "(" + recipeId + ", " + ingredient.quantity + ", " + ingredient.idUnit + ", " +
                    ingredient.idIngredient + ")";
        }
        query += ";";
        Cursor c = mDb.rawQuery(query, new String[0]);
        c.close();
        // Insert steps
        query = "Insert Into " + DatabaseHandler.STEPS_TABLE_NAME +
                " (" + DatabaseHandler.STEPS_RECIPE + ", " + DatabaseHandler.STEPS_ORDER + ", " +
                DatabaseHandler.STEPS_DESCRIPTION + ") Values ";
        for (int i = 0; i < recipe.steps.size(); ++i) {
            Step step = recipe.steps.get(i);
            if (i != 0)
                query += ", ";
            query += "(" + recipeId + ", " + i + ", " + step.description + ")";
        }
        query += ";";
        c = mDb.rawQuery(query, new String[0]);
        c.close();

        return recipeId;
    }
}
