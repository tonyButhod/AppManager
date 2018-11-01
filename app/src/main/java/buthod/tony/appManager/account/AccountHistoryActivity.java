package buthod.tony.appManager.account;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import buthod.tony.appManager.R;
import buthod.tony.appManager.database.AccountDAO;

/**
 * Class used in AccountActivity to display the history of all transactions in a page viewer.
 */
public class AccountHistoryActivity {
    // Fields used for the page viewer
    private Activity mRootActivity = null;
    private View mAccountView = null;

    private Button mTypeSelectionButton = null;
    private EditText mSearchField = null;
    private ImageButton mAddTransaction = null;
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

    /**
     * @return The created view to use in the page viewer.
     */
    public View getView() {
        return mAccountView;
    }

    /**
     * Instantiate all elements needed and update the account view with transactions' history.
     * @param rootActivity The activity containing the page viewer.
     * @param dao The account DAO. DAO need to be opened by AccountActivity.
     */
    public void onCreate(Activity rootActivity, AccountDAO dao) {
        mRootActivity = rootActivity;
        mDao = dao;
        mAccountView = mRootActivity.getLayoutInflater().inflate(R.layout.account_history, null);

        mTypeSelectionButton = (Button) mRootActivity.findViewById(R.id.type_selection);
        mSearchField = (EditText) mRootActivity.findViewById(R.id.search_field);
        mAddTransaction = (ImageButton) mAccountView.findViewById(R.id.add_transaction);
        mTransactionsLayout = (LinearLayout) mAccountView.findViewById(R.id.transactions_layout);
        mAppendTransactions = (Button) mAccountView.findViewById(R.id.append_transactions_button);
        mDateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

        // Add type of expenses and credits
        mExpenseTypes = new ArrayAdapter<CharSequence>(mRootActivity, android.R.layout.simple_spinner_item);
        mExpenseTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mExpenseTypes.addAll(mRootActivity.getResources().getStringArray(R.array.expense_types));
        mCreditTypes = new ArrayAdapter<CharSequence>(mRootActivity, android.R.layout.simple_spinner_item);
        mCreditTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreditTypes.addAll(mRootActivity.getResources().getStringArray(R.array.credit_types));

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
        mAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });
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
        // Populate the linear layout of transactions
        mTransactionsLayout.removeAllViews();
        appendNewTransactions();
    }

    /**
     * Update the view with elements in database.
     */
    public void updateView() {
        // Nothing to do
    }

    /**
     * Append new transactions to the linear layout.
     * @param numberOfTransactions The number of transactions to append.
     */
    private void appendNewTransactions(int numberOfTransactions, String inClause, String likeClause) {
        ArrayList<AccountDAO.TransactionInfo> transactions =
                mDao.getTransactions(numberOfTransactions, mLastTransactionDate, mLastTransactionId,
                        inClause, likeClause);
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
        // Display information about the expense/credit
        TextView transactionView = new TextView(mRootActivity);
        transactionView.setText( formatTransactionText(trans) );
        transactionView.setTextSize(12f);
        if (trans.type < 0) {
            transactionView.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_green));
            transactionView.setBackground(ContextCompat.getDrawable(mRootActivity, R.drawable.credit_background));
        }
        else {
            transactionView.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_red));
            transactionView.setBackground(ContextCompat.getDrawable(mRootActivity, R.drawable.expense_background));
        }
        // Add a long click event to delete the transaction
        transactionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Set the view as selected
                v.setSelected(true);

                // Show a popup menu
                final PopupMenu popup = new PopupMenu(mRootActivity, v);
                popup.inflate(R.menu.transaction_menu);
                // Registering clicks on the popup menu
                final TextView currentView = (TextView) v;
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        popup.dismiss();
                        switch(item.getItemId()) {
                            case R.id.modify:
                                showModifyTransactionDialog(trans.id, currentView);
                                break;
                            case R.id.delete:
                                showDeleteTransactionDialog(trans.id, currentView);
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
     * Update a transaction view. It updates its text and color.
     */
    private void updateTransactionView(TextView view, AccountDAO.TransactionInfo transaction) {
        view.setText(formatTransactionText(transaction));
        if (transaction.type < 0) {
            view.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_green));
            view.setBackground(ContextCompat.getDrawable(mRootActivity, R.drawable.credit_background));
        }
        else {
            view.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_red));
            view.setBackground(ContextCompat.getDrawable(mRootActivity, R.drawable.expense_background));
        }
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

    private void showAddTransactionDialog() {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mRootActivity);
        builder.setTitle(R.string.add_transaction);
        // Set the view of the alert dialog
        LayoutInflater inflater = mRootActivity.getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_transaction, null);
        builder.setView(alertView);
        // Get useful view
        final RadioButton expenseRadioButton = (RadioButton) alertView.findViewById(R.id.expense_radio_button);
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
        // Populate the spinner of alert dialog with types depending on the selected radio button
        typeSpinner.setAdapter(expenseRadioButton.isChecked() ? mExpenseTypes : mCreditTypes);
        expenseRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                typeSpinner.setAdapter(b ? mExpenseTypes : mCreditTypes);
            }
        });
        // Set up the buttons
        Resources res = mRootActivity.getResources();
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
                        type = (expenseRadioButton.isChecked()) ? type : -type - 1;
                        String priceString = priceEdit.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(mRootActivity, R.string.add_price, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = Math.round(Float.parseFloat(priceString) * 100);
                            }
                            catch (NumberFormatException e) {
                                Toast.makeText(mRootActivity, R.string.price_not_valid,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Recover the date from the date picker
                            c.set(Calendar.YEAR, datePicker.getYear());
                            c.set(Calendar.MONTH, datePicker.getMonth());
                            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                            Date date = c.getTime();
                            String comment = commentEdit.getText().toString();
                            mDao.addTransaction(type, price, date, comment);

                            resetTransactionLayout();
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showModifyTransactionDialog(final long transactionID, final TextView transactionView) {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mRootActivity);
        builder.setTitle(R.string.modify_transaction);
        // Set the view of the alert dialog
        LayoutInflater inflater = mRootActivity.getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_modify_transaction, null);
        builder.setView(alertView);
        // Get useful views
        final RadioButton expenseRadioButton = (RadioButton) alertView.findViewById(R.id.expense_radio_button);
        final RadioButton creditRadioButton = (RadioButton) alertView.findViewById(R.id.credit_radio_button);
        final Spinner typeSpinner = (Spinner) alertView.findViewById(R.id.type);
        final EditText priceEdit = (EditText) alertView.findViewById(R.id.price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date);
        final EditText commentEdit = (EditText) alertView.findViewById(R.id.comment);
        // Get transaction information
        final AccountDAO.TransactionInfo transaction = mDao.getTransaction(transactionID);
        // Set default date of the date picker
        final Calendar c = Calendar.getInstance();
        c.setTime(transaction.date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(year, month, day, null);
        // Populate the spinner of alert dialog with types.
        if (transaction.type < 0) {
            expenseRadioButton.setChecked(false);
            creditRadioButton.setChecked(true);
            typeSpinner.setAdapter(mCreditTypes);
            typeSpinner.setSelection(-transaction.type - 1);
        }
        else {
            expenseRadioButton.setChecked(true);
            creditRadioButton.setChecked(false);
            typeSpinner.setAdapter(mExpenseTypes);
            typeSpinner.setSelection(transaction.type);
        }
        // Update spinner if the user choose expense or credit
        expenseRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                typeSpinner.setAdapter(b ? mExpenseTypes : mCreditTypes);
            }
        });
        // Set the saved price and comment in the edit text
        priceEdit.setText(String.valueOf(transaction.price / 100.0f));
        commentEdit.setText(transaction.comment);
        // Set up the buttons
        Resources res = mRootActivity.getResources();
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
                        type = (expenseRadioButton.isChecked()) ? type : -type - 1;
                        String priceString = priceEdit.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(mRootActivity, "Ajoutez un prix", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // Check if the price is correct
                            int price = 0;
                            try {
                                price = Math.round(Float.parseFloat(priceString) * 100);
                            }
                            catch (NumberFormatException e) {
                                Toast.makeText(mRootActivity, "Le prix n'est pas valide",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Recover the date from the date picker
                            c.set(Calendar.YEAR, datePicker.getYear());
                            c.set(Calendar.MONTH, datePicker.getMonth());
                            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                            Date date = c.getTime();
                            String comment = commentEdit.getText().toString();
                            mDao.modifyTransaction(transaction.id, type, price, date, comment);
                            AccountDAO.TransactionInfo updatedTransaction = mDao.getTransaction(transactionID);
                            // Update the view in the linear layout
                            updateTransactionView(transactionView, updatedTransaction);
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void showDeleteTransactionDialog(final long transactionID, final View v){
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mRootActivity);
        builder.setTitle(R.string.delete_transaction);
        // Set up the buttons
        Resources res = mRootActivity.getResources();
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
                        mDao.deleteTransaction(transactionID);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(mRootActivity);
        builder.setTitle("");
        // Set popup content view
        LayoutInflater inflater = mRootActivity.getLayoutInflater();
        View alertView = inflater.inflate(R.layout.check_account_type, null);
        final LinearLayout expenseTypesGrid = (LinearLayout) alertView.findViewById(R.id.expenses_grid);
        final LinearLayout creditTypesGrid = (LinearLayout) alertView.findViewById(R.id.credits_grid);
        String[] expenseTypes = mRootActivity.getResources().getStringArray(R.array.expense_types);
        String[] creditTypes = mRootActivity.getResources().getStringArray(R.array.credit_types);
        // Set up the layout to use
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // Populate expenses grid
        for (int i = 0; i < expenseTypes.length; ++i) {
            // Create the new checkbox
            CheckBox checkBox = new CheckBox(mRootActivity);
            checkBox.setText(expenseTypes[i]);
            checkBox.setChecked(mExpenseTypesSelected[i]);
            checkBox.setLayoutParams(layoutParams);
            // Add the view
            expenseTypesGrid.addView(checkBox);
        }
        // Populate credits grid
        for (int i = 0; i < creditTypes.length; ++i) {
            // Create the new checkbox
            CheckBox checkBox = new CheckBox(mRootActivity);
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
        Resources res = mRootActivity.getResources();
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
}
