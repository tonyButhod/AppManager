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

    public static final String
            INGREDIENTS_TABLE_NAME = "Ingredients",
            INGREDIENTS_KEY = "id",
            INGREDIENTS_NAME = "name",
            INGREDIENTS_TABLE_CREATE =
                    "Create Table " + INGREDIENTS_TABLE_NAME + " (" +
                            INGREDIENTS_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            INGREDIENTS_NAME + " VARCHAR(32));",
            INGREDIENTS_TABLE_DROP =
                    "Drop Table If Exists " + INGREDIENTS_TABLE_NAME + ";";

    public static final String
            QUANTITIES_TABLE_NAME = "Quantities",
            QUANTITIES_KEY = "id",
            QUANTITIES_RECIPE = "recipe",
            QUANTITIES_QUANTITY = "quantity",
            QUANTITIES_UNIT = "unit",
            QUANTITIES_INGREDIENT = "ingredient",
            QUANTITIES_TABLE_CREATE =
                    "Create Table " + QUANTITIES_TABLE_NAME + " (" +
                            QUANTITIES_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            QUANTITIES_RECIPE + " INTEGER, " +
                            QUANTITIES_QUANTITY + " REAL, " +
                            QUANTITIES_UNIT + " INTEGER, " +
                            QUANTITIES_INGREDIENT + " INTEGER);",
            QUANTITIES_TABLE_DROP =
                    "Drop Table If Exists " + QUANTITIES_TABLE_NAME + ";";

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
                            STEPS_DESCRIPTION + " VARCHAR(128));",
            STEPS_TABLE_DROP =
                    "Drop Table If Exists " + STEPS_TABLE_NAME + ";";

    public static final String
            RECIPES_TABLE_NAME = "Recipes",
            RECIPES_KEY = "id",
            RECIPES_NAME = "name",
            RECIPES_TYPE = "type",
            RECIPES_DIFFICULTY = "difficulty",
            RECIPES_TIME = "time",
            RECIPES_GRADE = "grade",
            RECIPES_PEOPLE = "people",
            RECIPES_TABLE_CREATE =
                    "Create Table "+ RECIPES_TABLE_NAME +" ("+
                            RECIPES_KEY +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            RECIPES_NAME + " VARCHAR(64), " +
                            RECIPES_TYPE + " INTEGER, " +
                            RECIPES_DIFFICULTY + " INTEGER, " +
                            RECIPES_TIME + " INTEGER, " +
                            RECIPES_GRADE + " INTEGER, " +
                            RECIPES_PEOPLE + " INTEGER);",
            RECIPES_TABLE_DROP =
                    "Drop Table If Exists "+ RECIPES_TABLE_NAME +";";

    public static final String
            RECIPES_SEPARATION_TABLE_NAME = "RecipesSeparations",
            RECIPES_SEPARATION_ID = "id",
            RECIPES_SEPARATION_RECIPE = "recipe",
            RECIPES_SEPARATION_TYPE = "type", // The separation is for ingredients or recipes
            RECIPES_SEPARATION_NUMBER = "number",
            RECIPES_SEPARATION_DESCRIPTION = "description",
            STEPS_TABLE_CREATE =
                    "Create Table " + STEPS_TABLE_NAME + " (" +
                            STEPS_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            STEPS_RECIPE + " INTEGER, " +
                            STEPS_NUMBER + " INTEGER, " +
                            STEPS_DESCRIPTION + " VARCHAR(128));",
            STEPS_TABLE_DROP =
                    "Drop Table If Exists " + STEPS_TABLE_NAME + ";";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(INGREDIENTS_TABLE_CREATE);
            db.execSQL(QUANTITIES_TABLE_CREATE);
            db.execSQL(STEPS_TABLE_CREATE);
            db.execSQL(RECIPES_TABLE_CREATE);
        }
    }
}
