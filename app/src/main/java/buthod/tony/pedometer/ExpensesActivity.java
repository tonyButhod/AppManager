package buthod.tony.pedometer;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
        mExpensesDAO.open();
        ArrayList<ExpensesDAO.ExpenseInfo> expenses = mExpensesDAO.getExpenses();
        mExpensesDAO.close();
        for (int i = 0; i < expenses.size(); ++i) {
            ExpensesDAO.ExpenseInfo expenseInfo = expenses.get(i);
            addExpenseToLayout(expenseInfo.id, expenseInfo.type, expenseInfo.price, expenseInfo.date);
        }
    }

    /**
     * Add an expense with the right format in the expenses layout.
     * @param id Id of the expense in the database.
     * @param type Type of the expense.
     * @param price Price in cents.
     * @param date Date of the expense.
     */
    private void addExpenseToLayout(final long id, int type, int price, Date date) {
        // Display information about the expense
        String resumeText = "";
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        resumeText += formatter.format(date) + " :    ";
        resumeText += (price / 100.0f) + "€    ";
        resumeText += "--- " + mExpenseTypes.getItem(type);
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
                showDeleteExpenseDialog(id, v);
                return false;
            }
        });
        // Finally add the view in the linear layout
        mExpensesLayout.addView(expenseView, 0);
    }

    private void showAddExpenseDialog() {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_expense);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_expense, null);
        builder.setView(alertView);
        // Get useful view
        final Spinner expenseType = (Spinner) alertView.findViewById(R.id.expense_type);
        final EditText expensePrice = (EditText) alertView.findViewById(R.id.expense_price);
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
                            Date date = Calendar.getInstance().getTime();
                            mExpensesDAO.open();
                            long id = mExpensesDAO.addExpense(type, price, date);
                            mExpensesDAO.close();
                            addExpenseToLayout(id, type, price, date);
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showDeleteExpenseDialog(final long expenseID, final View v) {
        // Put the background color in gray of the selected expense
        v.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.light_gray));
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_expense);
        // Set up the buttons
        Resources res = getResources();
        builder.setNegativeButton(res.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Put back the original background
                        v.setBackground(ContextCompat.getDrawable(getBaseContext(),
                                R.drawable.gray_on_click));
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
