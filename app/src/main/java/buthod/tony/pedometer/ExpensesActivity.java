package buthod.tony.pedometer;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import buthod.tony.pedometer.database.ExpensesDAO;

/**
 * Created by Tony on 09/10/2017.
 */

public class ExpensesActivity extends RootActivity {

    private Button mAddExpense = null;
    private LinearLayout mExpensesLayout = null;

    private ExpensesDAO mExpensesDAO = null;
    private ArrayAdapter<CharSequence> mExpenseTypes = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expenses);

        mAddExpense = (Button) findViewById(R.id.add_expense);
        mExpensesLayout = (LinearLayout) findViewById(R.id.expenses_layout);
        mExpensesDAO = new ExpensesDAO(getBaseContext());

        // Add type of expenses
        mExpenseTypes = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mExpenseTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        String[] types = getResources().getStringArray(R.array.expense_types);
        mExpenseTypes.addAll(types);

        // Add an expense part
        mAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddExpenseDialog();
            }
        });
        // Show all expenses done
        updateExpensesLayout();
    }

    /**
     * Read all expenses stored in database and update the corresponding layout.
     */
    private void updateExpensesLayout() {
        mExpensesDAO.open();
        ArrayList<ExpensesDAO.ExpenseInfo> expenses = mExpensesDAO.getExpenses();
        mExpensesDAO.close();
        mExpensesLayout.removeAllViews();
        for (int i = 0; i < expenses.size(); ++i) {
            ExpensesDAO.ExpenseInfo expenseInfo = expenses.get(i);
            addExpenseToLayout(expenseInfo);
        }
    }

    /**
     * Add an expense with the right format in the expenses layout.
     * @param expense Class containing all expense informations (id, type, price, date).
     */
    private void addExpenseToLayout(final ExpensesDAO.ExpenseInfo expense) {
        // Display information about the expense
        String resumeText = "";
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        resumeText += formatter.format(expense.date) + " :    ";
        resumeText += (expense.price / 100.0f) + "€    ";
        resumeText += "--- " + mExpenseTypes.getItem(expense.type);
        resumeText += "\n   " + expense.comment;
        TextView expenseView = new TextView(getBaseContext());
        expenseView.setText(resumeText);
        expenseView.setTextColor(Color.GRAY);
        expenseView.setTextSize(12f);
        expenseView.setBackground(ContextCompat.getDrawable(getBaseContext(),
                R.drawable.gray_on_click));
        // Add a long click event to delete the expense
        expenseView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Put the background color in gray of the selected expense
                v.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.light_gray));

                // Show a popup menu
                final PopupMenu popup = new PopupMenu(ExpensesActivity.this, v);
                popup.inflate(R.menu.expense_menu);
                // Registering clicks on the popup menu
                final View currentView = v;
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        popup.dismiss();
                        switch(item.getItemId()) {
                            case R.id.modify:
                                showModifyExpenseDialog(expense.id);
                                break;
                            case R.id.delete:
                                showDeleteExpenseDialog(expense.id, currentView);
                                break;
                        }
                        return true;
                    }
                });
                popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        // Put back the original background
                        currentView.setBackground(ContextCompat.getDrawable(getBaseContext(),
                                R.drawable.gray_on_click));
                    }
                });
                popup.show();
                return false;
            }
        });
        // Finally add the view in the linear layout
        mExpensesLayout.addView(expenseView);
    }

    private void showAddExpenseDialog() {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_expense);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_expense, null);
        builder.setView(alertView);
        // Get useful view
        final Spinner expenseType = (Spinner) alertView.findViewById(R.id.expense_type);
        final EditText expensePrice = (EditText) alertView.findViewById(R.id.expense_price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date_picker);
        final EditText expenseComment = (EditText) alertView.findViewById(R.id.expense_comment);
        // Set default date of the date picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, null);
        // Populate the spinner of alert dialog with types.
        expenseType.setAdapter(mExpenseTypes);
        // Set up the buttons
        Resources res = getResources();
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.add), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        int type = expenseType.getSelectedItemPosition();
                        String priceString = expensePrice.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Ajoutez un prix", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = (int) (Float.parseFloat(priceString) * 100);
                            }
                            catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(), "Le prix n'est pas valide",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Recover the date from the date picker
                            c.set(Calendar.YEAR, datePicker.getYear());
                            c.set(Calendar.MONTH, datePicker.getMonth());
                            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                            Date date = c.getTime();
                            String comment = expenseComment.getText().toString();
                            mExpensesDAO.open();
                            mExpensesDAO.addExpense(type, price, date, comment);
                            mExpensesDAO.close();
                            updateExpensesLayout();
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showModifyExpenseDialog(final long expenseID) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.modify_expense);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_expense, null);
        builder.setView(alertView);
        // Get useful views
        final Spinner expenseType = (Spinner) alertView.findViewById(R.id.expense_type);
        final EditText expensePrice = (EditText) alertView.findViewById(R.id.expense_price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date_picker);
        final EditText expenseComment = (EditText) alertView.findViewById(R.id.expense_comment);
        // Get expense information
        mExpensesDAO.open();
        final ExpensesDAO.ExpenseInfo expenseInfo = mExpensesDAO.getExpense(expenseID);
        mExpensesDAO.close();
        // Set default date of the date picker
        final Calendar c = Calendar.getInstance();
        c.setTime(expenseInfo.date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, null);
        // Populate the spinner of alert dialog with types.
        expenseType.setAdapter(mExpenseTypes);
        expenseType.setSelection(expenseInfo.type);
        // Set the saved price and comment in the edit text
        expensePrice.setText(String.valueOf(expenseInfo.price / 100.0f));
        expenseComment.setText(expenseInfo.comment);
        // Set up the buttons
        Resources res = getResources();
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.modify), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        int type = expenseType.getSelectedItemPosition();
                        String priceString = expensePrice.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Ajoutez un prix", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = (int) (Float.parseFloat(priceString) * 100);
                            }
                            catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(), "Le prix n'est pas valide",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Recover the date from the date picker
                            c.set(Calendar.YEAR, datePicker.getYear());
                            c.set(Calendar.MONTH, datePicker.getMonth());
                            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                            Date date = c.getTime();
                            String comment = expenseComment.getText().toString();
                            mExpensesDAO.open();
                            mExpensesDAO.modifyExpense(expenseInfo.id, type, price, date, comment);
                            mExpensesDAO.close();
                            updateExpensesLayout();
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showDeleteExpenseDialog(final long expenseID, final View v) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_expense);
        // Set up the buttons
        Resources res = getResources();
        builder.setNegativeButton(res.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.yes), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        mExpensesDAO.open();
                        mExpensesDAO.deleteExpense(expenseID);
                        mExpensesDAO.close();
                        mExpensesLayout.removeView(v);
                        alertDialog.dismiss();
                    }
                });
            }
        });
        // Show alert dialog
        alertDialog.show();
    }
}
