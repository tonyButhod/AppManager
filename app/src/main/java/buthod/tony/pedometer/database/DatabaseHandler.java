package buthod.tony.pedometer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Tony on 29/07/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String
            PEDOMETER_TABLE_NAME = "Pedometer",
            PEDOMETER_KEY = "id",
            PEDOMETER_DATE = "date",
            PEDOMETER_STEPS = "steps",
            PEDOMETER_TABLE_CREATE =
        "Create Table "+PEDOMETER_TABLE_NAME+" ("+
            PEDOMETER_KEY+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            PEDOMETER_DATE+" INTEGER, "+
            PEDOMETER_STEPS+" INTEGER);",
            PEDOMETER_TABLE_DROP =
        "Drop Table If Exists "+PEDOMETER_TABLE_NAME+";";

    public static final String
            EXPENSES_TABLE_NAME = "Expenses",
            EXPENSES_KEY = "id",
            EXPENSES_TYPE = "type",
            EXPENSES_PRICE = "price",
            EXPENSES_DATE = "date",
            EXPENSES_COMMENT = "comment",
            EXPENSES_TABLE_CREATE =
                    "Create Table "+EXPENSES_TABLE_NAME+" ("+
                            EXPENSES_KEY+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                            EXPENSES_TYPE+" INTEGER, "+
                            EXPENSES_PRICE+" INTEGER, "+
                            EXPENSES_DATE+" INTEGER, "+
                            EXPENSES_COMMENT+" VARCHAR(50));",
            EXPENSES_TABLE_DROP =
                    "Drop Table If Exists "+EXPENSES_TABLE_NAME+";";


    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PEDOMETER_TABLE_CREATE);
        db.execSQL(EXPENSES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(PEDOMETER_TABLE_DROP);
        db.execSQL(EXPENSES_TABLE_DROP);
        onCreate(db);
    }
}
