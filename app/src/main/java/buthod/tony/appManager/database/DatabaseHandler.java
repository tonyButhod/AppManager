package buthod.tony.appManager.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Tony on 29/07/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    //region PEDOMETER

    public static final String
            PEDOMETER_TABLE_NAME = "Pedometer",
            PEDOMETER_KEY = "id",
            PEDOMETER_DATE = "date",
            PEDOMETER_STEPS = "steps",
            PEDOMETER_TABLE_CREATE =
        "Create Table "+PEDOMETER_TABLE_NAME+" ("+
            PEDOMETER_KEY+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            PEDOMETER_DATE+" DATE, "+
            PEDOMETER_STEPS+" INTEGER);",
            PEDOMETER_TABLE_DROP =
        "Drop Table If Exists "+PEDOMETER_TABLE_NAME+";";

    //endregion

    //region ACCOUNT

    public static final String
            ACCOUNT_TABLE_NAME = "Account",
            TRANSACTION_KEY = "id",
            TRANSACTION_TYPE = "type",
            TRANSACTION_PRICE = "price",
            TRANSACTION_DATE = "date",
            TRANSACTION_COMMENT = "comment",
            EXPENSES_TABLE_CREATE =
                    "Create Table "+ ACCOUNT_TABLE_NAME +" ("+
                            TRANSACTION_KEY +" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                            TRANSACTION_TYPE +" INTEGER, "+
                            TRANSACTION_PRICE +" INTEGER, "+
                            TRANSACTION_DATE +" DATE, "+
                            TRANSACTION_COMMENT +" VARCHAR(50));",
            EXPENSES_TABLE_DROP =
                    "Drop Table If Exists "+ ACCOUNT_TABLE_NAME +";";

    //endregion

    //region RECIPES

    public static final int IngredientNameLength = 32;
    public static final String
            INGREDIENTS_TABLE_NAME = "Ingredients",
            INGREDIENTS_KEY = "id",
            INGREDIENTS_NAME = "name",
            INGREDIENTS_TABLE_CREATE =
                    "Create Table " + INGREDIENTS_TABLE_NAME + " (" +
                            INGREDIENTS_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            INGREDIENTS_NAME + " VARCHAR(" + IngredientNameLength +"));",
            INGREDIENTS_TABLE_DROP =
                    "Drop Table If Exists " + INGREDIENTS_TABLE_NAME + ";";

    public static final String
            QUANTITIES_TABLE_NAME = "Quantities",
            QUANTITIES_KEY = "id",
            QUANTITIES_RECIPE = "recipe",
            QUANTITIES_QUANTITY = "quantity",
            QUANTITIES_UNIT = "unit",
            QUANTITIES_INGREDIENT = "ingredient",
            QUANTITIES_NUMBER = "number", // The number used for quantities order in the list.
            QUANTITIES_TYPE = "type", // Type of quantity, for example optional quantity.
            QUANTITIES_TABLE_CREATE =
                    "Create Table " + QUANTITIES_TABLE_NAME + " (" +
                            QUANTITIES_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            QUANTITIES_RECIPE + " INTEGER, " +
                            QUANTITIES_QUANTITY + " REAL, " +
                            QUANTITIES_UNIT + " INTEGER, " +
                            QUANTITIES_INGREDIENT + " INTEGER, " +
                            QUANTITIES_NUMBER + " INTEGER, " +
                            QUANTITIES_TYPE + " INTEGER);",
            QUANTITIES_TABLE_DROP =
                    "Drop Table If Exists " + QUANTITIES_TABLE_NAME + ";";

    public static final int StepDescriptionLength = 256;
    public static final String
            STEPS_TABLE_NAME = "Steps",
            STEPS_KEY = "id",
            STEPS_RECIPE = "recipe",
            STEPS_NUMBER = "number",
            STEPS_DESCRIPTION = "description",
            STEPS_TABLE_CREATE =
                    "Create Table " + STEPS_TABLE_NAME + " (" +
                            STEPS_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            STEPS_RECIPE + " INTEGER, " +
                            STEPS_NUMBER + " INTEGER, " +
                            STEPS_DESCRIPTION + " VARCHAR(" + StepDescriptionLength + "));",
            STEPS_TABLE_DROP =
                    "Drop Table If Exists " + STEPS_TABLE_NAME + ";";

    public static final int RecipeNameLength = 64;
    public static final String
            RECIPES_TABLE_NAME = "Recipes",
            RECIPES_KEY = "id",
            RECIPES_NAME = "name",
            RECIPES_TYPE = "type",
            RECIPES_DIFFICULTY = "difficulty",
            RECIPES_TIME = "time",
            RECIPES_GRADE = "grade",
            RECIPES_PEOPLE = "people",
            RECIPES_NUMBER = "number", // Number used to sort recipes from most relevant to less.
            RECIPES_TABLE_CREATE =
                    "Create Table "+ RECIPES_TABLE_NAME +" ("+
                            RECIPES_KEY +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            RECIPES_NAME + " VARCHAR(" + RecipeNameLength + "), " +
                            RECIPES_TYPE + " INTEGER, " +
                            RECIPES_DIFFICULTY + " INTEGER, " +
                            RECIPES_TIME + " INTEGER, " +
                            RECIPES_GRADE + " INTEGER, " +
                            RECIPES_PEOPLE + " INTEGER, " +
                            RECIPES_NUMBER + " INTEGER NOT NULL);",
            RECIPES_TABLE_DROP =
                    "Drop Table If Exists "+ RECIPES_TABLE_NAME +";";

    public static final int RecipeSeparationDescriptionLength = 64;
    public static final String
            RECIPES_SEPARATION_TABLE_NAME = "RecipesSeparations",
            RECIPES_SEPARATION_KEY = "id",
            RECIPES_SEPARATION_RECIPE = "recipe",
            RECIPES_SEPARATION_TYPE = "type", // The separation is for ingredients or recipes
            RECIPES_SEPARATION_NUMBER = "number",
            RECIPES_SEPARATION_DESCRIPTION = "description",
            RECIPES_SEPARATION_TABLE_CREATE =
                    "Create Table " + RECIPES_SEPARATION_TABLE_NAME + " (" +
                            RECIPES_SEPARATION_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            RECIPES_SEPARATION_RECIPE + " INTEGER, " +
                            RECIPES_SEPARATION_TYPE + " INTEGER, " +
                            RECIPES_SEPARATION_NUMBER + " INTEGER, " +
                            RECIPES_SEPARATION_DESCRIPTION + " VARCHAR(" + RecipeSeparationDescriptionLength + "));",
            RECIPES_SEPARATION_TABLE_DROP =
                    "Drop Table If Exists " + RECIPES_SEPARATION_TABLE_NAME + ";";

    public static final String
            RECIPES_CONVERSIONS_TABLE_NAME = "RecipesConversions",
            RECIPES_CONVERSIONS_KEY = "id",
            RECIPES_CONVERSIONS_INGREDIENT = "ingredient",
            RECIPES_CONVERSIONS_UNIT_FROM = "unit_from",
            RECIPES_CONVERSIONS_UNIT_TO = "unit_to",
            RECIPES_CONVERSIONS_FACTOR = "factor",
            RECIPES_CONVERSIONS_TABLE_CREATE =
                    "Create Table " + RECIPES_CONVERSIONS_TABLE_NAME + " (" +
                            RECIPES_CONVERSIONS_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            RECIPES_CONVERSIONS_INGREDIENT + " INTEGER, " +
                            RECIPES_CONVERSIONS_UNIT_FROM + " INTEGER, " +
                            RECIPES_CONVERSIONS_UNIT_TO + " INTEGER, " +
                            RECIPES_CONVERSIONS_FACTOR + " REAL);",
            RECIPES_CONVERSIONS_TABLE_DROP =
                    "Drop Table If Exists " + RECIPES_CONVERSIONS_TABLE_NAME + ";";

    //endregion

    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PEDOMETER_TABLE_CREATE);
        db.execSQL(EXPENSES_TABLE_CREATE);
        db.execSQL(INGREDIENTS_TABLE_CREATE);
        db.execSQL(QUANTITIES_TABLE_CREATE);
        db.execSQL(STEPS_TABLE_CREATE);
        db.execSQL(RECIPES_TABLE_CREATE);
        db.execSQL(RECIPES_SEPARATION_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(INGREDIENTS_TABLE_CREATE);
            db.execSQL(QUANTITIES_TABLE_CREATE);
            db.execSQL(STEPS_TABLE_CREATE);
            db.execSQL(RECIPES_TABLE_CREATE);
        }
        if (oldVersion < 3) {
            db.execSQL(RECIPES_SEPARATION_TABLE_CREATE);
            if (oldVersion == 2) {
                db.execSQL("Alter Table " + RECIPES_TABLE_NAME + " Add " + RECIPES_NUMBER + " INTEGER;");
                db.execSQL("Alter Table " + QUANTITIES_TABLE_NAME + " Add " + QUANTITIES_NUMBER + " INTEGER;");
                db.execSQL("Alter Table " + QUANTITIES_TABLE_NAME + " Add " + QUANTITIES_TYPE + " INTEGER;");
                // The length of step's description has been changed, but it doesn't matter in SQLite,
                // And since it is not possible to modify directly a column, we do not modify it.
            }
        }
        if (oldVersion < 4) {
            db.execSQL(RECIPES_CONVERSIONS_TABLE_CREATE);
        }
    }
}
