package buthod.tony.appManager.database;

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
                            TRANSACTION_DATE +" INTEGER, "+
                            TRANSACTION_COMMENT +" VARCHAR(50));",
            EXPENSES_TABLE_DROP =
                    "Drop Table If Exists "+ ACCOUNT_TABLE_NAME +";";


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
