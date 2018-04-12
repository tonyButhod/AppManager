package buthod.tony.appManager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

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
        mDateFormatter = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
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
}
