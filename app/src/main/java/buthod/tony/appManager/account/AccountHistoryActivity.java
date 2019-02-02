package buthod.tony.appManager.account;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.widget.PopupMenu;
import android.app.AlertDialog;
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

import buthod.tony.appManager.utils.CustomAlertDialog;
import buthod.tony.appManager.R;
import buthod.tony.appManager.database.AccountDAO;
import buthod.tony.appManager.utils.CustomSpinnerAdapter;

/**
 * Class used in AccountActivity to display the history of all transactions in a page viewer.
 */
public class AccountHistoryActivity {
    // Fields used for the page viewer
    private Activity mRootActivity = null;
    private View mAccountView = null;
    private Resources mRes = null;

    private Button mTypeSelectionButton = null;
    private EditText mSearchField = null;
    private ImageButton mAddTransaction = null;
    private LinearLayout mTransactionsLayout = null;
    private Button mAppendTransactions = null;

    private AccountDAO mDao = null;
    private CustomSpinnerAdapter mExpenseTypes = null;
    private CustomSpinnerAdapter mCreditTypes = null;

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
        mRes = mRootActivity.getResources();

        mTypeSelectionButton = (Button) mRootActivity.findViewById(R.id.type_selection);
        mSearchField = (EditText) mRootActivity.findViewById(R.id.search_field);
        mAddTransaction = (ImageButton) mAccountView.findViewById(R.id.add_transaction);
        mTransactionsLayout = (LinearLayout) mAccountView.findViewById(R.id.transactions_layout);
        mAppendTransactions = (Button) mAccountView.findViewById(R.id.append_transactions_button);
        mDateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

        // Add type of expenses and credits
        mExpenseTypes = new CustomSpinnerAdapter(mRootActivity,
                R.layout.simple_spinner_item, R.layout.simple_spinner_dropdown_item,
                mRes.getStringArray(R.array.expense_types));
        mCreditTypes = new CustomSpinnerAdapter(mRootActivity,
                R.layout.simple_spinner_item, R.layout.simple_spinner_dropdown_item,
                mRes.getStringArray(R.array.credit_types));

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
                addTransactionDialog();
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
            transactionView.setTextColor(mRes.getColor(R.color.dark_soft_green));
            transactionView.setBackground(mRes.getDrawable(R.drawable.credit_background));
        }
        else {
            transactionView.setTextColor(mRes.getColor(R.color.dark_soft_red));
            transactionView.setBackground(mRes.getDrawable(R.drawable.expense_background));
        }
        // Add a long click event to delete the transaction
        transactionView.setTag(trans.id);
        transactionView.setOnLongClickListener(mTransactionOnLongClick);
        // Finally add the view in the linear layout
        mTransactionsLayout.addView(transactionView);
    }

    private View.OnLongClickListener mTransactionOnLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final long id = (long) v.getTag();
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
                            modifyTransactionDialog(id, currentView);
                            break;
                        case R.id.duplicate:
                            duplicateTransactionDialog(id);
                            break;
                        case R.id.delete:
                            deleteTransactionDialog(id, currentView);
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
    };

    /**
     * Update a transaction view. It updates its text and color.
     */
    private void updateTransactionView(TextView view, AccountDAO.TransactionInfo transaction) {
        view.setText(formatTransactionText(transaction));
        if (transaction.type < 0) {
            view.setTextColor(mRes.getColor(R.color.dark_soft_green));
            view.setBackground(mRes.getDrawable(R.drawable.credit_background));
        }
        else {
            view.setTextColor(mRes.getColor(R.color.dark_soft_red));
            view.setBackground(mRes.getDrawable(R.drawable.expense_background));
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

    /**
     * Interface used as a callback on result of a transaction popup.
     */
    private interface TransactionCallback {
        void onPositiveResult(AccountDAO.TransactionInfo editedTransaction);
        void onNegativeResult(AccountDAO.TransactionInfo initialTransaction);
    }

    /**
     * Shw the transaction dialog for edition.
     * @param titleId The title id of the resource.
     * @param initialTransaction The initial transaction to modify. If new transaction, set it to null.
     * @param callback The callback to call on result of the popup.
     */
    private void showTransactionDialog(@StringRes int titleId, final AccountDAO.TransactionInfo initialTransaction,
                                       final TransactionCallback callback) {
        // Initialize an alert dialog
        CustomAlertDialog.Builder builder = new CustomAlertDialog.Builder(mRootActivity);
        builder.setTitle(titleId);
        // Set the view of the alert dialog
        LayoutInflater inflater = mRootActivity.getLayoutInflater();
        final View alertView = inflater.inflate(R.layout.add_edit_transaction, null);
        builder.setView(alertView);
        // Get useful views
        final RadioButton expenseRadioButton = (RadioButton) alertView.findViewById(R.id.expense_radio_button);
        final RadioButton creditRadioButton = (RadioButton) alertView.findViewById(R.id.credit_radio_button);
        final Spinner typeSpinner = (Spinner) alertView.findViewById(R.id.type);
        final EditText priceEdit = (EditText) alertView.findViewById(R.id.price);
        final DatePicker datePicker = (DatePicker) alertView.findViewById(R.id.date);
        final EditText commentEdit = (EditText) alertView.findViewById(R.id.comment);
        final Calendar c = Calendar.getInstance();
        int spinnerSelection = 0;
        // Get transaction information
        if (initialTransaction != null) {
            c.setTime(initialTransaction.date);
            if (initialTransaction.type < 0) {
                expenseRadioButton.setChecked(false);
                creditRadioButton.setChecked(true);
                spinnerSelection = -initialTransaction.type - 1;
            }
            else {
                expenseRadioButton.setChecked(true);
                creditRadioButton.setChecked(false);
                spinnerSelection = initialTransaction.type;
            }
            priceEdit.setText(String.valueOf(initialTransaction.price / 100.0f));
            commentEdit.setText(initialTransaction.comment);
        }
        // Initialize fields
        typeSpinner.setAdapter(expenseRadioButton.isChecked() ? mExpenseTypes : mCreditTypes);
        typeSpinner.setSelection(spinnerSelection);
        datePicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH), null);
        // Update spinner if the user choose expense or credit
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
                        if (callback != null)
                            callback.onNegativeResult(initialTransaction);
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.save), null);
        final CustomAlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int type = typeSpinner.getSelectedItemPosition();
                        type = (expenseRadioButton.isChecked()) ? type : -type - 1;
                        String priceString = priceEdit.getText().toString();
                        if (priceString.isEmpty()) {
                            Toast.makeText(mRootActivity, "Ajoutez un prix", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int price;
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
                        long id = (initialTransaction != null ? initialTransaction.id : -1);
                        AccountDAO.TransactionInfo editedTransaction = new AccountDAO.TransactionInfo(
                                id, type, price, date, comment);
                        if (callback != null)
                            callback.onPositiveResult(editedTransaction);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    /**
     * Add a transaction with a popup dialog.
     */
    private void addTransactionDialog() {
        showTransactionDialog(R.string.add_transaction, null,
                new TransactionCallback() {
                    @Override
                    public void onPositiveResult(AccountDAO.TransactionInfo editedTransaction) {
                        mDao.addTransaction(editedTransaction);
                        resetTransactionLayout();
                    }

                    @Override
                    public void onNegativeResult(AccountDAO.TransactionInfo initialTransaction) {
                        // Do nothing
                    }
                });
    }

    /**
     * Modify a transaction with a popup dialog.
     * @param transactionId The transaction id to modify.
     * @param transactionView The transaction view to modify.
     */
    private void modifyTransactionDialog(final long transactionId, final TextView transactionView) {
        showTransactionDialog(R.string.modify_transaction, mDao.getTransaction(transactionId),
                new TransactionCallback() {
                    @Override
                    public void onPositiveResult(AccountDAO.TransactionInfo editedTransaction) {
                        mDao.modifyTransaction(editedTransaction);
                        updateTransactionView(transactionView, editedTransaction);
                    }

                    @Override
                    public void onNegativeResult(AccountDAO.TransactionInfo initialTransaction) {
                        // Do nothing
                    }
                });
    }

    /**
     * Delete a transaction with a popup confirmation dialog.
     * @param transactionId The transaction id to delete.
     * @param v The corresponding view in linear layout.
     */
    private void deleteTransactionDialog(final long transactionId, final View v){
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
        builder.setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDao.deleteTransaction(transactionId);
                mTransactionsLayout.removeView(v);
                dialog.dismiss();
            }
        });
        // Show alert dialog
        builder.create().show();
    }

    /**
     * Duplicate a transaction to create a new one.
     * @param transactionId The transaction id to duplicate.
     */
    private void duplicateTransactionDialog(final long transactionId) {
        AccountDAO.TransactionInfo transaction = mDao.getTransaction(transactionId);
        transaction.date = Calendar.getInstance().getTime(); // Set transaction to current date
        showTransactionDialog(R.string.add_transaction, transaction,
                new TransactionCallback() {
                    @Override
                    public void onPositiveResult(AccountDAO.TransactionInfo editedTransaction) {
                        mDao.addTransaction(editedTransaction);
                        resetTransactionLayout();
                    }

                    @Override
                    public void onNegativeResult(AccountDAO.TransactionInfo initialTransaction) {
                        // Do nothing
                    }
                });
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
        final Button selectAllButton = (Button) alertView.findViewById(R.id.select_all);
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
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.apply),
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateSelectedTypes(expenseTypesGrid, creditTypesGrid);
                        resetTransactionLayout();
                        dialog.dismiss();
                    }
        });
        builder.setNeutralButton(res.getString(R.string.reset),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectAllButton.performClick();
                        updateSelectedTypes(expenseTypesGrid, creditTypesGrid);
                        resetTransactionLayout();
                        dialogInterface.dismiss();
                    }
                });
        // Show alert dialog
        builder.create().show();
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
