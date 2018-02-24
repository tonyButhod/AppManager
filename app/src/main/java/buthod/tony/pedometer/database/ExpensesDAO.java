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
            DATE = DatabaseHandler.EXPENSES_DATE;

    public class ExpenseInfo {
        public long id;
        public int type;
        public int price;
        public Date date;

        public ExpenseInfo(long id, int type, int price, Date date) {
            this.id = id;
            this.type = type;
            this.price = price;
            this.date = date;
        }
    }

    public ExpensesDAO(Context context) {
        super(context);
    }

    public long addExpense(int type, int price, Date date) {
        long dateUTC = date.getTime();
        dateUTC -= dateUTC % 86400000;
        ContentValues value = new ContentValues();
        value.put(TYPE, type);
        value.put(PRICE, price);
        value.put(DATE, dateUTC);
        return mDb.insert(TABLE_NAME, null, value);
    }

    public int deleteExpense(long id) {
        return mDb.delete(TABLE_NAME, KEY + " = ?",  new String[] {String.valueOf(id)});
    }

    public ArrayList<ExpenseInfo> getExpenses() {
        Cursor c = mDb.rawQuery(
                "Select * From " + TABLE_NAME +
                " Order By " + DATE + " DESC;", new String[0]);
        ArrayList<ExpenseInfo> expenses = new ArrayList<ExpenseInfo>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getInt(0);
            int type = c.getInt(1);
            int price = c.getInt(2);
            Date date = new Date(c.getLong(3));
            expenses.add(new ExpenseInfo(id, type, price, date));
        }
        c.close();
        return expenses;
    }
}
