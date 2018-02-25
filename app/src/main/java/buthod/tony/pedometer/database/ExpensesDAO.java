package buthod.tony.pedometer.database;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Tony on 09/10/2017.
 */

public class ExpensesDAO extends DAOBase {
    public static final String
            TABLE_NAME = DatabaseHandler.EXPENSES_TABLE_NAME,
            KEY = DatabaseHandler.EXPENSES_KEY,
            TYPE = DatabaseHandler.EXPENSES_TYPE,
            PRICE = DatabaseHandler.EXPENSES_PRICE,
            DATE = DatabaseHandler.EXPENSES_DATE,
            COMMENT = DatabaseHandler.EXPENSES_COMMENT;

    public class ExpenseInfo {
        public long id;
        public int type;
        public int price;
        public Date date;
        public String comment;

        public ExpenseInfo(long id, int type, int price, Date date, String comment) {
            this.id = id;
            this.type = type;
            this.price = price;
            this.date = date;
            this.comment = comment;
        }
    }

    public ExpensesDAO(Context context) {
        super(context);
    }

    public long addExpense(int type, int price, Date date, String comment) {
        long dateUTC = date.getTime();
        dateUTC -= dateUTC % 86400000;
        ContentValues value = new ContentValues();
        value.put(TYPE, type);
        value.put(PRICE, price);
        value.put(DATE, dateUTC);
        value.put(COMMENT, comment);
        return mDb.insert(TABLE_NAME, null, value);
    }

    public int deleteExpense(long id) {
        return mDb.delete(TABLE_NAME, KEY + " = ?",  new String[] {String.valueOf(id)});
    }

    /**
     * Get the expense information corresponding to the given id.
     * @param expenseID The id of the expense stored in database.
     * @return Return the expense information.
     * If no expense found with this id, return null.
     */
    public ExpenseInfo getExpense(long expenseID) {
        Cursor c = mDb.rawQuery(
                "Select * From " + TABLE_NAME +
                " Where " + KEY + " = ?;", new String[]{String.valueOf(expenseID)});
        if (c.moveToFirst()) {
            long id = c.getInt(0);
            int type = c.getInt(1);
            int price = c.getInt(2);
            Date date = new Date(c.getLong(3));
            String comment = c.getString(4);
            c.close();
            return new ExpenseInfo(id, type, price, date, comment);
        }
        else {
            c.close();
            return null;
        }
    }

    public ArrayList<ExpenseInfo> getExpenses() {
        Cursor c = mDb.rawQuery(
                "Select * From " + TABLE_NAME +
                " Order By " + DATE + " DESC, " + KEY + " DESC;", new String[0]);
        ArrayList<ExpenseInfo> expenses = new ArrayList<ExpenseInfo>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getInt(0);
            int type = c.getInt(1);
            int price = c.getInt(2);
            Date date = new Date(c.getLong(3));
            String comment = c.getString(4);
            expenses.add(new ExpenseInfo(id, type, price, date, comment));
        }
        c.close();
        return expenses;
    }

    public void modifyExpense(long id, int type, int price, Date date, String comment) {
        long dateUTC = date.getTime();
        dateUTC -= dateUTC % 86400000;
        ContentValues value = new ContentValues();
        value.put(TYPE, type);
        value.put(PRICE, price);
        value.put(DATE, dateUTC);
        value.put(COMMENT, comment);
        mDb.update(TABLE_NAME, value, KEY + " = ?", new String[] {String.valueOf(id)});
    }
}
