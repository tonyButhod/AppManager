package buthod.tony.pedometer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Tony on 29/07/2017.
 */

public abstract class DAOBase {
    protected final static int VERSION = 1;
    protected final static String NAME = "database.db";

    protected SQLiteDatabase mDb = null;
    protected DatabaseHandler mHandler = null;

    public DAOBase(Context context) {
        mHandler = new DatabaseHandler(context, NAME, null, VERSION);
    }

    public SQLiteDatabase open() {
        mDb = mHandler.getWritableDatabase();
        return mDb;
    }

    public void close() {
        mDb.close();
        mDb = null;
    }

    public SQLiteDatabase getDb() {
        return mDb;
    }

}
