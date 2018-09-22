package buthod.tony.appManager.account;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.AccountDAO;

/**
 * Created by Tony on 09/10/2017.
 */

public class AccountHistoryActivity extends RootActivity {

    private ImageButton mBackButton = null;
    private Button mTypeSelectionButton = null;
    private EditText mSearchField = null;
    private Button mAddExpense = null;
    private Button mAddCredit = null;
    private LinearLayout mTransactionsLayout = null;
    private Button mAppendTransactions = null;

    private AccountDAO mDao = null;
    private ArrayAdapter<CharSequence> mExpenseTypes = null;
    private ArrayAdapter<CharSequence> mCreditTypes = null;

    // Fields used to append new transactions if the user goes to the end of the linear layout.
    private Date mLastTransactionDate = null;
    private long mLastTransactionId = -1;

    // Fields used during the selection of specific types.
    private boolean[] mExpenseTypesSelected;
    private boolean[] mCreditTypesSelected;

    private SimpleDateFormat mDateFormatter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_history);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mTypeSelectionButton = (Button) findViewById(R.id.type_selection);
        mSearchField = (EditText) findViewById(R.id.search_field);
        mAddExpense = (Button) findViewById(R.id.add_expense);
        mAddCredit = (Button) findViewById(R.id.add_credit);
        mTransactionsLayout = (LinearLayout) findViewById(R.id.transactions_layout);
        mAppendTransactions = (Button) findViewById(R.id.append_transactions_button);
        mDao = new AccountDAO(getBaseContext());
        mDateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

        // Add type of expenses and credits
        mExpenseTypes = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mExpenseTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mExpenseTypes.addAll(getResources().getStringArray(R.array.expense_types));
        mCreditTypes = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mCreditTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreditTypes.addAll(getResources().getStringArray(R.array.credit_types));

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Show the type selection popup
        mTypeSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTypeSelectionDialog();
            }
        });
        // Update search elements
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchFieldChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        // Add an expense or a credit part
        mAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog(false);
            }
        });
        mAddCredit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog(true);
            }
        });
        // Populate the linear layout of transactions
        mTransactionsLayout.removeAllViews();
        appendNewTransactions();
        // Add listener to display more transactions
        mAppendTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendNewTransactions( buildInClauseFromSelectedTypes(), mSearchField.getText().toString() );
            }
        });

        // Initialize selected types
        mExpenseTypesSelected = new boolean[mExpenseTypes.getCount()];
        mCreditTypesSelected = new boolean[mCreditTypes.getCount()];
        for (int i = 0; i < mExpenseTypesSelected.length; ++i) mExpenseTypesSelected[i] = true;
        for (int i = 0; i < mCreditTypesSelected.length; ++i) mCreditTypesSelected[i] = true;
    }

    /**
     * Append new transactions to the linear layout.
     * @param numberOfTransactions The number of transactions to append.
     */
    private void appendNewTransactions(int numberOfTransactions, String inClause, String likeClause) {
        mDao.open();
        ArrayList<AccountDAO.TransactionInfo> transactions =
                mDao.getTransactions(numberOfTransactions, mLastTransactionDate, mLastTransactionId,
                        inClause, likeClause);
        mDao.close();
        for (int i = 0; i < transactions.size(); ++i) {
            addTransactionToLayout(transactions.get(i));
        }
        // Save last transaction date and id in case we want to append new transactions
        if (transactions.size() > 0) {
            AccountDAO.TransactionInfo lastTransaction = transactions.get(transactions.size() - 1);
            mLastTransactionDate = lastTransaction.date;
            mLastTransactionId = lastTransaction.id;
        }
        // If the size is lower than the wanted number, hide the button to show more transactions
        if (transactions.size() < numberOfTransactions)
            mAppendTransactions.setVisibility(View.GONE);
        else
            mAppendTransactions.setVisibility(View.VISIBLE);
    }
    private void appendNewTransactions(String inClause, String likeClause) {
        appendNewTransactions(50, inClause, likeClause);
    }
    private void appendNewTransactions() {
        appendNewTransactions(50, null, null);
    }

    /**
     * Add an expense/credit with the right format in the transactions layout.
     * @param trans Class containing all expenses/credits information (id, type, price, date).
     */
    private void addTransactionToLayout(final AccountDAO.TransactionInfo trans) {
        // Check if it is an expense or a credit
        final boolean isCredit = (trans.type < 0);
        // Display information about the expense/credit
        TextView transactionView = new TextView(getBaseContext());
        transactionView.setText( formatTransactionText(trans) );
        transactionView.setTextSize(12f);
        if (isCredit) {
            transactionView.setTextColor(ContextCompat.getColor(getBaseContext(),
                    R.color.dark_soft_green));
            transactionView.setBackground(ContextCompat.getDrawable(getBaseContext(),
                    R.drawable.credit_background));
        }
        else {
            transactionView.setTextColor(ContextCompat.getColor(getBaseContext(),
                    R.color.dark_soft_red));
            transactionView.setBackground(ContextCompat.getDrawable(getBaseContext(),
                    R.drawable.expense_background));
        }
        // Add a long click event to delete the transaction
        transactionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Set the view as selected
                v.setSelected(true);

                // Show a popup menu
                final PopupMenu popup = new PopupMenu(AccountHistoryActivity.this, v);
                popup.inflate(R.menu.transaction_menu);
                // Registering clicks on the popup menu
                final TextView currentView = (TextView) v;
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        popup.dismiss();
                        switch(item.getItemId()) {
                            case R.id.modify:
                                showModifyTransactionDialog(trans.id, currentView, isCredit);
                                break;
                            case R.id.delete:
                                showDeleteTransactionDialog(trans.id, currentView, isCredit);
                                break;
                        }
                        return true;
                    }
                });
                popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        // Deselect the view
                        currentView.setSelected(false);
                    }
                });
                popup.show();
                return false;
            }
        });
        // Finally add the view in the linear layout
        mTransactionsLayout.addView(transactionView);
    }

    /**
     * Format a transaction as a text easily readable.
     * @param trans The transaction data.
     * @return The string resuming the transaction.
     */
    private String formatTransactionText(AccountDAO.TransactionInfo trans) {
        String resumeText = "";
        resumeText += mDateFormatter.format(trans.date) + " :    ";
        resumeText += (trans.price / 100.0f) + "€    ";
        if (trans.type < 0) {
            // It is a credit
            resumeText += "--- " + mCreditTypes.getItem(- trans.type - 1);
        }
        else {
            // It is an expense
            resumeText += "--- " + mExpenseTypes.getItem(trans.type);
        }
        resumeText += "\n   " + trans.comment;
        return resumeText;
    }

    /**
     * Update the layout of transactions.
     * Reset the number of transactions displayed and uses the type selection and search string
     * for update.
     */
    private void resetTransactionLayout() {
        mTransactionsLayout.removeAllViews();
        mLastTransactionId = -1;
        mLastTransactionDate = null;
        appendNewTransactions( buildInClauseFromSelectedTypes(), mSearchField.getText().toString() );
    }

    //region ADD_MODIFY_REMOVE_DIALOGS

    private void showAddTransactionDialog(final boolean isCredit) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isCredit ? R.string.add_credit : R.string.add_expense);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_transaction, null);
        builder.setView(alertView);
        // Get useful view
        final Spinner typeSpinner = (Spinner) alertView.findViewById(R.id.type);
        final EditText priceEdit = (EditText) alertView.findViewById(R.id.price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date);
        final EditText commentEdit = (EditText) alertView.findViewById(R.id.comment);
        // Set default date of the date picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, null);
        // Populate the spinner of alert dialog with types.
        typeSpinner.setAdapter(isCredit ? mCreditTypes : mExpenseTypes);
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
                        int type = typeSpinner.getSelectedItemPosition();
                        type = (isCredit) ? -type - 1 : type;
                        String priceString = priceEdit.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(getApplicationContext(), R.string.add_price, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = Math.round(Float.parseFloat(priceString) * 100);
                            }
                            catch (NumberFormatException e) {
                                Toast.makeText(getApplicationContext(), R.string.price_not_valid,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Recover the date from the date picker
                            c.set(Calendar.YEAR, datePicker.getYear());
                            c.set(Calendar.MONTH, datePicker.getMonth());
                            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                            Date date = c.getTime();
                            String comment = commentEdit.getText().toString();
                            mDao.open();
                            mDao.addTransaction(type, price, date, comment);
                            mDao.close();

                            resetTransactionLayout();
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showModifyTransactionDialog(final long transactionID, final TextView transactionView,
                                             final boolean isCredit) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isCredit ? R.string.modify_expense : R.string.modify_credit);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_transaction, null);
        builder.setView(alertView);
        // Get useful views
        final Spinner typeSpinner = (Spinner) alertView.findViewById(R.id.type);
        final EditText priceEdit = (EditText) alertView.findViewById(R.id.price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date);
        final EditText commentEdit = (EditText) alertView.findViewById(R.id.comment);
        // Get transaction information
        mDao.open();
        final AccountDAO.TransactionInfo transaction = mDao.getTransaction(transactionID);
        mDao.close();
        // Set default date of the date picker
        final Calendar c = Calendar.getInstance();
        c.setTime(transaction.date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, null);
        // Populate the spinner of alert dialog with types.
        if (isCredit) {
            typeSpinner.setAdapter(mCreditTypes);
            typeSpinner.setSelection(-transaction.type - 1);
        }
        else {
            typeSpinner.setAdapter(mExpenseTypes);
            typeSpinner.setSelection(transaction.type);
        }
        // Set the saved price and comment in the edit text
        priceEdit.setText(String.valueOf(transaction.price / 100.0f));
        commentEdit.setText(transaction.comment);
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
                        int type = typeSpinner.getSelectedItemPosition();
                        type = (isCredit) ? -type - 1 : type;
                        String priceString = priceEdit.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Ajoutez un prix", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = Math.round(Float.parseFloat(priceString) * 100);
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
                            String comment = commentEdit.getText().toString();
                            mDao.open();
                            mDao.modifyTransaction(transaction.id, type, price, date, comment);
                            AccountDAO.TransactionInfo updatedTransaction = mDao.getTransaction(transactionID);
                            mDao.close();
                            // Update the view in the linear layout
                            transactionView.setText( formatTransactionText(updatedTransaction) );
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showDeleteTransactionDialog(final long transactionID, final View v,
                                             boolean isCredit){
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isCredit ? R.string.delete_credit : R.string.delete_expense);
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
                        mDao.open();
                        mDao.deleteTransaction(transactionID);
                        mDao.close();
                        mTransactionsLayout.removeView(v);
                        alertDialog.dismiss();
                    }
                });
            }
        });
        // Show alert dialog
        alertDialog.show();
    }

    //endregion

    //region TYPE_SELECTION

    /**
     * Show a popup with different checkbox allowing the user to select which type
     *  he wants to keep in the history of expenses/credits.
     */
    private void showTypeSelectionDialog() {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("");
        // Set popup content view
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.check_account_type, null);
        final LinearLayout expenseTypesGrid = (LinearLayout) alertView.findViewById(R.id.expenses_grid);
        final LinearLayout creditTypesGrid = (LinearLayout) alertView.findViewById(R.id.credits_grid);
        String[] expenseTypes = getResources().getStringArray(R.array.expense_types);
        String[] creditTypes = getResources().getStringArray(R.array.credit_types);
        // Set up the layout to use
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // Populate expenses grid
        for (int i = 0; i < expenseTypes.length; ++i) {
            // Create the new checkbox
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(expenseTypes[i]);
            checkBox.setChecked(mExpenseTypesSelected[i]);
            checkBox.setLayoutParams(layoutParams);
            // Add the view
            expenseTypesGrid.addView(checkBox);
        }
        // Populate credits grid
        for (int i = 0; i < creditTypes.length; ++i) {
            // Create the new checkbox
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(creditTypes[i]);
            checkBox.setChecked(mCreditTypesSelected[i]);
            checkBox.setLayoutParams(layoutParams);
            // Add the view
            creditTypesGrid.addView(checkBox);
        }
        // Add listeners to buttons present in the view
        Button selectAllButton = (Button) alertView.findViewById(R.id.select_all);
        Button selectNothingButton = (Button) alertView.findViewById(R.id.select_nothing);
        Button selectExpensesButton = (Button) alertView.findViewById(R.id.select_expenses);
        Button selectCreditsButton = (Button) alertView.findViewById(R.id.select_credits);
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckBoxesState(expenseTypesGrid, true);
                setCheckBoxesState(creditTypesGrid, true);
            }
        });
        selectNothingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckBoxesState(expenseTypesGrid, false);
                setCheckBoxesState(creditTypesGrid, false);
            }
        });
        selectExpensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckBoxesState(expenseTypesGrid, true);
                setCheckBoxesState(creditTypesGrid, false);
            }
        });
        selectCreditsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckBoxesState(expenseTypesGrid, false);
                setCheckBoxesState(creditTypesGrid, true);
            }
        });
        builder.setView(alertView);
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
                        updateSelectedTypes(expenseTypesGrid, creditTypesGrid);
                        resetTransactionLayout();
                        alertDialog.dismiss();
                    }
                });
            }
        });
        // Show alert dialog
        alertDialog.show();
    }

    private void setCheckBoxesState(LinearLayout layout, boolean state) {
        for (int i = 0; i < layout.getChildCount(); ++i)
            ((CheckBox) layout.getChildAt(i)).setChecked(state);
        layout.invalidate();
    }

    /**
     * Update private fields using checkbox in grid layouts.
     * @param expenseTypes Expenses types linear layout containing check boxes.
     * @param creditTypes Credits types linear layout containing check boxes.
     */
    private void updateSelectedTypes(LinearLayout expenseTypes, LinearLayout creditTypes) {
        for (int i = 0; i < expenseTypes.getChildCount(); ++i)
            mExpenseTypesSelected[i] = ((CheckBox) expenseTypes.getChildAt(i)).isChecked();
        for (int i = 0; i < creditTypes.getChildCount(); ++i)
            mCreditTypesSelected[i] = ((CheckBox) creditTypes.getChildAt(i)).isChecked();
    }

    /**
     * Browse expenses and credits grid layout to find selected types.
     * @return The where clause build with selected types : '(type1,typ2,...)'.
     */
    private String buildInClauseFromSelectedTypes() {
        String inClause = "";
        boolean isFirst = true;
        for (int i = 0; i < mExpenseTypesSelected.length; ++i) {
            if (mExpenseTypesSelected[i]) {
                inClause += (isFirst ? "" : ",") + i;
                isFirst = false;
            }
        }
        for (int i = 0; i < mCreditTypesSelected.length; ++i) {
            if (mCreditTypesSelected[i]) {
                inClause += (isFirst ? "" : ",") + (-i - 1);
                isFirst = false;
            }
        }

        return "(" + inClause + ")";
    }

    //endregion

    //region SEARCH

    private Handler handler = new Handler();

    private void onSearchFieldChanged(final String newSearchString) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (newSearchString.compareTo(mSearchField.getText().toString()) == 0)
                    resetTransactionLayout();
            }
        }, 400);
    }

    //endregion

    /**
     * Clear focus of any edit text when the user click next to it.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
