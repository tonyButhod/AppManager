package buthod.tony.appManager.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import buthod.tony.appManager.Utils;

/**
 * Created by Tony on 29/07/2017.
 */

public class PedometerDAO extends DAOBase {
    public static final String TABLE_NAME = DatabaseHandler.PEDOMETER_TABLE_NAME;
    public static final String KEY = DatabaseHandler.PEDOMETER_KEY;
    public static final String DATE = DatabaseHandler.PEDOMETER_DATE;
    public static final String STEPS = DatabaseHandler.PEDOMETER_STEPS;

    private SimpleDateFormat mDateFormatter = null;

    public PedometerDAO(Context context) {
        super(context);
        mDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
    }

    public void storeSteps(Date date, int steps) {
        // First check if an existing row corresponds to the current date.
        Cursor c = mDb.rawQuery("Select "+KEY+" From "+TABLE_NAME+
                " Where "+DATE+" = ?;", new String[]{mDateFormatter.format(date)});
        if (c.getCount() == 0) {
            ContentValues value = new ContentValues();
            value.put(DATE, mDateFormatter.format(date));
            value.put(STEPS, steps);
            mDb.insert(TABLE_NAME, null, value);
        }
        else {
            c.moveToFirst();
            long id = c.getLong(0);
            ContentValues value = new ContentValues();
            value.put(STEPS, steps);
            mDb.update(TABLE_NAME, value, KEY+" = ?", new String[]{String.valueOf(id)});
        }
    }

    public int getDailySteps(Date date) {
        Cursor c = mDb.rawQuery(
            "Select "+STEPS+" FROM "+TABLE_NAME+
            " Where "+DATE+" = ?;", new String[]{mDateFormatter.format(date)});
        if (c.getCount() == 0) {
            return 0;
        }
        else {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    public SortedMap<Date, Integer> getSteps(Date startDate, Date endDate) {
        Cursor c = mDb.rawQuery(
            "Select * FROM "+TABLE_NAME+
            " Where "+DATE+" >= ? And "+DATE+" <= ?"+
            " Order By "+DATE+" ASC;",
            new String[]{mDateFormatter.format(startDate), mDateFormatter.format(endDate)});
        SortedMap<Date, Integer> steps = new TreeMap<Date, Integer>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Date date = null;
            try {
                date = mDateFormatter.parse(c.getString(1));
            }
            catch (ParseException e) {
                e.fillInStackTrace();
            }
            steps.put(date, c.getInt(2));
        }
        c.close();
        return steps;
    }
    public SortedMap<Date, Integer> getSteps(Date startDate) {
        return getSteps(startDate, new Date(Long.MAX_VALUE));
    }
    public SortedMap<Date, Integer> getSteps() {
        return getSteps(new Date(0));
    }

    //region LOAD_SAVE_EXTERNAL_STORAGE

    public static String EXTERNAL_PEDOMETER_FILENAME = "Pedometer.json";

    /**
     * Save pedometer data to external storage in file EXTERNAL_PEDOMETER_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean saveDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to write the file
        Utils.verifyStoragePermissions(activity);

        JSONObject obj = new JSONObject();
        JSONArray pedometersObj = new JSONArray();
        Cursor c = db.rawQuery("Select * From " + DatabaseHandler.PEDOMETER_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject pedometerObj = new JSONObject();
            pedometerObj.put(DatabaseHandler.PEDOMETER_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.PEDOMETER_KEY)));
            pedometerObj.put(DatabaseHandler.PEDOMETER_STEPS, c.getInt(c.getColumnIndex(DatabaseHandler.PEDOMETER_STEPS)));
            pedometerObj.put(DatabaseHandler.PEDOMETER_DATE, c.getString(c.getColumnIndex(DatabaseHandler.PEDOMETER_DATE)));
            pedometersObj.put(pedometerObj);
        }
        obj.put(DatabaseHandler.PEDOMETER_TABLE_NAME, pedometersObj);
        c.close();

        return Utils.writeToExternalStorage(activity, EXTERNAL_PEDOMETER_FILENAME, obj.toString());
    }

    /**
     * Load pedometer data from external storage in file EXTERNAL_PEDOMETER_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean loadDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to read the file
        Utils.verifyStoragePermissions(activity);

        String fileContent = Utils.readFromExternalStorage(activity, EXTERNAL_PEDOMETER_FILENAME);
        if (fileContent == null)
            return false;

        JSONObject obj = new JSONObject(fileContent);
        JSONArray pedometersObj = obj.getJSONArray(DatabaseHandler.PEDOMETER_TABLE_NAME);
        if (pedometersObj.length() > 0) {
            for (int i = 0; i < pedometersObj.length(); ++i) {
                JSONObject pedometerObj = pedometersObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.PEDOMETER_KEY, pedometerObj.getLong(DatabaseHandler.PEDOMETER_KEY));
                values.put(DatabaseHandler.PEDOMETER_DATE, pedometerObj.getString(DatabaseHandler.PEDOMETER_DATE));
                values.put(DatabaseHandler.PEDOMETER_STEPS, pedometerObj.getInt(DatabaseHandler.PEDOMETER_STEPS));
                try {
                    db.insertOrThrow(DatabaseHandler.PEDOMETER_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }

        return true;
    }

    //endregion
}
