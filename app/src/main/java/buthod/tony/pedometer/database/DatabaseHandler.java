package buthod.tony.pedometer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Tony on 29/07/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String PEDOMETER_TABLE_NAME = "Pedometer";
    public static final String PEDOMETER_KEY = "id";
    public static final String PEDOMETER_DATE = "date";
    public static final String PEDOMETER_STEPS = "steps";
    public static final String PEDOMETER_TABLE_CREATE =
        "Create Table "+PEDOMETER_TABLE_NAME+" ("+
            PEDOMETER_KEY+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            PEDOMETER_DATE+" INTEGER, "+
            PEDOMETER_STEPS+" INTEGER);";
    public static final String PEDOMETER_TABLE_DROP =
        "Drop Table If Exists "+PEDOMETER_TABLE_NAME+";";

    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PEDOMETER_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(PEDOMETER_TABLE_DROP);
        onCreate(db);
    }
}
