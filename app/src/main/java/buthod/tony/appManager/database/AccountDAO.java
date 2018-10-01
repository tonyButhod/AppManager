package buthod.tony.appManager.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import buthod.tony.appManager.Utils;

/**
 * Created by Tony on 09/10/2017.
 */

public class AccountDAO extends DAOBase {
    public static final String
            TABLE_NAME = DatabaseHandler.ACCOUNT_TABLE_NAME,
            KEY = DatabaseHandler.TRANSACTION_KEY,
            TYPE = DatabaseHandler.TRANSACTION_TYPE,
            PRICE = DatabaseHandler.TRANSACTION_PRICE,
            DATE = DatabaseHandler.TRANSACTION_DATE,
            COMMENT = DatabaseHandler.TRANSACTION_COMMENT;

    public static class TransactionInfo {
        public long id;
        public int type;
        public int price;
        public Date date;
        public String comment;

        public TransactionInfo(long id, int type, int price, Date date, String comment) {
            this.id = id;
            this.type = type;
            this.price = price;
            this.date = date;
            this.comment = comment;
        }
    }

    private SimpleDateFormat mDateFormatter = null;

    public AccountDAO(Context context) {
        super(context);
        mDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
    }

    //region TRANSACTION

    public long addTransaction(int type, int price, Date date, String comment) {
        ContentValues value = new ContentValues();
        value.put(TYPE, type);
        value.put(PRICE, price);
        value.put(DATE, mDateFormatter.format(date));
        value.put(COMMENT, comment);
        return mDb.insert(TABLE_NAME, null, value);
    }

    public int deleteTransaction(long id) {
        return mDb.delete(TABLE_NAME, KEY + " = ?",  new String[] {String.valueOf(id)});
    }

    /**
     * Get the expense information corresponding to the given id.
     * @param transactionID The id of the expense stored in database.
     * @return Return the expense information.
     * If no expense found with this id, return null.
     */
    public TransactionInfo getTransaction(long transactionID) {
        Cursor c = mDb.rawQuery(
                "Select * From " + TABLE_NAME +
                " Where " + KEY + " = ?;", new String[]{String.valueOf(transactionID)});
        if (c.moveToFirst()) {
            long id = c.getInt(0);
            int type = c.getInt(1);
            int price = c.getInt(2);
            Date date = null;
            try {
                date = mDateFormatter.parse(c.getString(3));
            }
            catch (ParseException e) {
                e.fillInStackTrace();
            }
            String comment = c.getString(4);
            c.close();
            return new TransactionInfo(id, type, price, date, comment);
        }
        else {
            c.close();
            return null;
        }
    }

    /**
     * Get information about transactions starting at last id.
     * @param limit The total number of transactions returned.
     * @param lastDate The last transaction date to start returning information.
     * @param lastId The last transaction id to start returning information.
     * @return A list of all transactions.
     */
    public ArrayList<TransactionInfo> getTransactions(int limit, Date lastDate, long lastId,
                                                      String inClause, String likeClause) {
        // Construct the where clause depending on the given parameters
        String whereClause = "";
        if (lastDate != null) {
            whereClause += " (" + DATE + " < '" + mDateFormatter.format(lastDate) + "'";
            if (lastId != -1)
                whereClause += " Or ( " + DATE + " = '" + mDateFormatter.format(lastDate) + "'" +
                        " And " + KEY + " < " + String.valueOf(lastId) + " )";
            whereClause += " )";
        }
        if (inClause != null)
            whereClause += (whereClause.isEmpty() ? "" : " And ") + TYPE + " IN " + inClause;
        if (likeClause != null && !likeClause.isEmpty())
            whereClause += (whereClause.isEmpty() ? "" : " And ") + COMMENT + " LIKE '%" + likeClause + "%'";
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select * From " + TABLE_NAME +
                (whereClause.isEmpty() ? "" : " Where " + whereClause) +
                " Order By " + DATE + " DESC, " + KEY + " DESC" +
                " Limit " + String.valueOf(limit) + ";", new String[0]);
        ArrayList<TransactionInfo> transactions = new ArrayList<TransactionInfo>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getInt(0);
            int type = c.getInt(1);
            int price = c.getInt(2);
            Date date = null;
            try {
                date = mDateFormatter.parse(c.getString(3));
            }
            catch (ParseException e) {
                e.fillInStackTrace();
            }

            String comment = c.getString(4);
            transactions.add(new TransactionInfo(id, type, price, date, comment));
        }
        c.close();
        return transactions;
    }
    public ArrayList<TransactionInfo> getTransactions(int limit, Date lastDate, long lastId,
                                                      String inCLause) {
        return getTransactions(limit, lastDate, lastId, inCLause, null);
    }
    public ArrayList<TransactionInfo> getTransactions(int limit, Date lastDate, long lastId) {
        return getTransactions(limit, lastDate, lastId, null);
    }
    public ArrayList<TransactionInfo> getTransactions(int limit) {
        return getTransactions(limit, null, -1);
    }

    public void modifyTransaction(long id, int type, int price, Date date, String comment) {
        ContentValues value = new ContentValues();
        value.put(TYPE, type);
        value.put(PRICE, price);
        value.put(DATE, mDateFormatter.format(date));
        value.put(COMMENT, comment);
        mDb.update(TABLE_NAME, value, KEY + " = ?", new String[] {String.valueOf(id)});
    }

    //endregion

    //region STATEMENT

    /**
     * Get financial statements group by years.
     * The statements take into account only credits or expenses depending on given parameters.
     * @param yearsNumber The number of computation year starting with the current year.
     * @param credits If true, compute statements on credits. Else, compute it on expenses.
     * @return An array of statements of size 'yearsNumber'.
     */
    public int[] getYearsStatement(int yearsNumber, boolean credits) {
        // Get the starting and ending dates
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
        Date endDate = cal.getTime();
        cal.add(Calendar.YEAR, -yearsNumber+1);
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR));
        Date startDate = cal.getTime();
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select strftime('%Y', " + DATE + ") as year, SUM(" + PRICE + ")" +
                        " From " + TABLE_NAME +
                        " Where " + DATE + ">= ? And " + DATE + "<= ? " +
                        "   And " + TYPE + (credits?" < 0":">= 0") + // Get positive or negative statements
                        " Group by year;",
                new String[]{mDateFormatter.format(startDate), mDateFormatter.format(endDate)});
        int[] yearsStatement = new int[yearsNumber];
        int index = 0;
        int currentYear = cal.get(Calendar.YEAR); // Get the starting year
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            int year = Integer.valueOf(c.getString(0));
            while (currentYear < year) {
                yearsStatement[index] = 0;
                index++;
                currentYear++;
            }
            yearsStatement[index] = c.getInt(1);
            index++;
            currentYear++;
        }
        // Initialize the rest of the array
        while (index < yearsNumber) {
            yearsStatement[index] = 0;
            index++;
        }
        c.close();
        return yearsStatement;
    }

    /**
     * Get financial statements group by months.
     * The statements take into account only credits or expenses depending on given parameters.
     * @param monthsNumber The number of computation year starting with the current month.
     * @param credits If true, compute statements on credits. Else, compute it on expenses.
     * @return An array of statements of size 'monthsNumber'.
     */
    public int[] getMonthsStatement(int monthsNumber, boolean credits) {
        // Get the starting and ending dates
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.MONTH, -monthsNumber+1);
        Date startDate = cal.getTime();
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select strftime('%Y', " + DATE + ") as year, strftime('%m', " + DATE + ") as month, SUM(" + PRICE + ")" +
                " From " + TABLE_NAME +
                " Where " + DATE + ">= ? And " + DATE + "<= ? " +
                "   And " + TYPE + (credits?" < 0":" >= 0") + // Get positive or negative statements
                " Group by year, month;",
                new String[]{mDateFormatter.format(startDate), mDateFormatter.format(endDate)});
        int[] monthsStatement = new int[monthsNumber];
        int index = 0;
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            int year = Integer.valueOf(c.getString(0));
            int month = Integer.valueOf(c.getString(1));
            while (currentYear < year || currentMonth < month) {
                currentMonth++;
                if (currentMonth > 12) {
                    currentMonth = 1;
                    currentYear++;
                }
                monthsStatement[index] = 0;
                index++;
            }
            monthsStatement[index] = c.getInt(2);
            index++;
            currentMonth++;
            if (currentMonth == 12) {
                currentMonth = 0;
                currentYear++;
            }
        }
        // Initialize the rest of the array
        while (index < monthsNumber) {
            monthsStatement[index] = 0;
            index++;
        }
        c.close();
        return monthsStatement;
    }

    /**
     * Get financial statements group by days.
     * The statements take into account only credits or expenses depending on given parameters.
     * @param daysNumber The number of computation days starting with the current date.
     * @param credits If true, compute statements on credits. Else, compute it on expenses.
     * @return An array of statements of size 'daysNumber'.
     */
    public int[] getDaysStatement(int daysNumber, boolean credits) {
        // Get the starting and ending dates
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        cal.add(Calendar.DATE, -daysNumber+1);
        Date startDate = cal.getTime();
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select " + DATE + " as date, SUM(" + PRICE + ")" +
                        " From " + TABLE_NAME +
                        " Where " + DATE + ">= ? And " + DATE + "<= ? " +
                        "   And " + TYPE + (credits?" < 0":" >= 0") + 
                        " Group by date;",
                new String[]{mDateFormatter.format(startDate), mDateFormatter.format(endDate)});
        int[] daysStatement = new int[daysNumber];
        Calendar currentDate = cal;
        int index = 0;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String date = c.getString(0);
            while (mDateFormatter.format(cal.getTime()).compareTo(date) < 0) {
                currentDate.add(Calendar.DATE, 1);
                daysStatement[index] = 0;
                index++;
            }
            daysStatement[index] = c.getInt(1);
            index++;
            currentDate.add(Calendar.DATE, 1);
        }
        // Initialize the rest of the array
        while (index < daysNumber) {
            daysStatement[index] = 0;
            index++;
        }
        c.close();
        return daysStatement;
    }

    /**
     * Get the statement between 2 dates by category.
     * @param start The starting date.
     * @param end The ending date.
     * @return A SparseIntArray with the type as key and the price as value.
     */
    public SparseIntArray getStatementByCategory(Date start, Date end) {
        SparseIntArray result = new SparseIntArray();
        // Execute query
        Cursor c = mDb.rawQuery(
                "Select " + TYPE + ", SUM(" + PRICE + ")" +
                        " From " + TABLE_NAME +
                        " Where " + DATE + ">= ? And " + DATE + "<= ? " +
                        " Group By " + TYPE + ";",
                new String[]{mDateFormatter.format(start), mDateFormatter.format(end)}
        );
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result.append(c.getInt(0), c.getInt(1));
        }
        c.close();

        return result;
    }

    //endregion

    //region LOAD_SAVE_EXTERNAL_STORAGE

    public static String EXTERNAL_ACCOUNT_FILENAME = "Account.json";

    /**
     * Save transactions data to external storage in file EXTERNAL_ACCOUNT_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean saveDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to write the file
        Utils.verifyStoragePermissions(activity);

        JSONObject obj = new JSONObject();
        JSONArray transactionsObj = new JSONArray();
        Cursor c = db.rawQuery("Select * From " + DatabaseHandler.ACCOUNT_TABLE_NAME, new String[0]);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            JSONObject transactionObj = new JSONObject();
            transactionObj.put(DatabaseHandler.TRANSACTION_KEY, c.getLong(c.getColumnIndex(DatabaseHandler.TRANSACTION_KEY)));
            transactionObj.put(DatabaseHandler.TRANSACTION_TYPE, c.getInt(c.getColumnIndex(DatabaseHandler.TRANSACTION_TYPE)));
            transactionObj.put(DatabaseHandler.TRANSACTION_PRICE, c.getInt(c.getColumnIndex(DatabaseHandler.TRANSACTION_PRICE)));
            transactionObj.put(DatabaseHandler.TRANSACTION_DATE, c.getString(c.getColumnIndex(DatabaseHandler.TRANSACTION_DATE)));
            transactionObj.put(DatabaseHandler.TRANSACTION_COMMENT, c.getString(c.getColumnIndex(DatabaseHandler.TRANSACTION_COMMENT)));
            transactionsObj.put(transactionObj);
        }
        obj.put(DatabaseHandler.ACCOUNT_TABLE_NAME, transactionsObj);
        c.close();

        return Utils.writeToExternalStorage(activity, EXTERNAL_ACCOUNT_FILENAME, obj.toString());
    }

    /**
     * Load transactions data from external storage in file EXTERNAL_ACCOUNT_FILENAME.
     * @param activity The current activity.
     * @param db The database.
     * @return True in case of success, false otherwise.
     */
    public static boolean loadDataExternalStorage(Activity activity, SQLiteDatabase db) throws JSONException {
        // First check if we have the permission to read the file
        Utils.verifyStoragePermissions(activity);

        String fileContent = Utils.readFromExternalStorage(activity, EXTERNAL_ACCOUNT_FILENAME);
        if (fileContent == null)
            return false;

        JSONObject obj = new JSONObject(fileContent);
        JSONArray transactionsObj = obj.getJSONArray(DatabaseHandler.ACCOUNT_TABLE_NAME);
        if (transactionsObj.length() > 0) {
            for (int i = 0; i < transactionsObj.length(); ++i) {
                JSONObject transactionObj = transactionsObj.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHandler.TRANSACTION_KEY, transactionObj.getLong(DatabaseHandler.TRANSACTION_KEY));
                values.put(DatabaseHandler.TRANSACTION_TYPE, transactionObj.getInt(DatabaseHandler.TRANSACTION_TYPE));
                values.put(DatabaseHandler.TRANSACTION_PRICE, transactionObj.getInt(DatabaseHandler.TRANSACTION_PRICE));
                values.put(DatabaseHandler.TRANSACTION_DATE, transactionObj.getString(DatabaseHandler.TRANSACTION_DATE));
                values.put(DatabaseHandler.TRANSACTION_COMMENT, transactionObj.getString(DatabaseHandler.TRANSACTION_COMMENT));
                try {
                    db.insertOrThrow(DatabaseHandler.ACCOUNT_TABLE_NAME, null, values);
                }
                catch (SQLException e) { }
            }
        }

        return true;
    }

    //endregion
}
