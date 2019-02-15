package buthod.tony.appManager.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.AccountDAO;

/**
 * Main account activity containing a page viewer with different sections.
 */
public class AccountActivity extends RootActivity {

    private ImageButton mBackButton = null;

    // Pager management
    private ViewPager mViewPager = null;
    private AccountPagerAdapter mAccountPagerAdapter = null;
    private Button mHistoryButton = null, mStatementButton = null, mPieChartButton = null;
    // Custom element in toolbar for account history page
    private EditText mSearchField = null;
    private Button mTypeSelectionButton = null;
    // Database part
    private AccountDAO mDao = null;
    // Classes used in the view pager
    private AccountHistoryActivity mAccountHistory = null;
    private AccountPieChartActivity mAccountPieChart = null;
    private AccountStatementActivity mAccountStatement = null;

    // Fields used during the selection of specific types.
    public boolean[] mExpenseTypesSelected;
    public boolean[] mCreditTypesSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mSearchField = (EditText) findViewById(R.id.search_field);
        mTypeSelectionButton = (Button) findViewById(R.id.type_selection);
        mHistoryButton = (Button) findViewById(R.id.account_history_button);
        mStatementButton = (Button) findViewById(R.id.account_statement_button);
        mPieChartButton = (Button) findViewById(R.id.account_piechart_button);
        mDao = new AccountDAO(this);
        mDao.open();

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

        // Page management
        mAccountHistory = new AccountHistoryActivity();
        mAccountPieChart = new AccountPieChartActivity();
        mAccountStatement = new AccountStatementActivity();
        mAccountPagerAdapter = new AccountPagerAdapter();
        mViewPager.setAdapter(mAccountPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                AccountActivity.this.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        onPageSelected(0);
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(0);
            }
        });
        mStatementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(1);
            }
        });
        mPieChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(2);
            }
        });
    }

    //region PAGE SELECTION

    /**
     * Called when a new page is selected. Updates the view.
     * @param position The selected page's position.
     */
    private void onPageSelected(int position) {
        mSearchField.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);
        mHistoryButton.setSelected(position == 0);
        mStatementButton.setSelected(position == 1);
        mPieChartButton.setSelected(position == 2);
        if (position == 0) {
            mAccountHistory.updateView();
        }
        else if (position == 1) {
            mAccountStatement.updateView();
        }
        else if (position == 2) {
            mAccountPieChart.updateView();
        }
    }

    /**
     * Custom pager adapter for account page.
     * Allow to switch easily between pages.
     */
    public class AccountPagerAdapter extends PagerAdapter {

        public AccountPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            View view;
            switch (position) {
                case 0:
                    view = mAccountHistory.getView();
                    if (view == null)
                        mAccountHistory.onCreate(AccountActivity.this, mDao);
                    view = mAccountHistory.getView();
                    collection.addView(view);
                    return view;
                case 1:
                    view = mAccountStatement.getView();
                    if (view == null)
                        mAccountStatement.onCreate(AccountActivity.this, mDao);
                    view = mAccountStatement.getView();
                    collection.addView(view);
                    return view;
                case 2:
                    view = mAccountPieChart.getView();
                    if (view == null)
                        mAccountPieChart.onCreate(AccountActivity.this, mDao);
                    view = mAccountPieChart.getView();
                    collection.addView(view);
                    return view;
                default:
                    return null;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            switch (position) {
                case 0:
                    collection.removeView(mAccountHistory.getView());
                    break;
                case 1:
                    collection.removeView(mAccountStatement.getView());
                    break;
                case 2:
                    collection.removeView(mAccountPieChart.getView());
                    break;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Title custom view pager";
        }
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

    @Override
    public void onDestroy() {
        mDao.close();
        super.onDestroy();
    }

    //region TYPES SELECTION

    public List<Runnable> onTypesUpdatedListeners = new ArrayList<>();

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
        Resources res = getResources();
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
                        for (int i = 0; i < onTypesUpdatedListeners.size(); ++i)
                            onTypesUpdatedListeners.get(i).run();
                        dialog.dismiss();
                    }
                });
        builder.setNeutralButton(res.getString(R.string.reset),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectAllButton.performClick();
                        updateSelectedTypes(expenseTypesGrid, creditTypesGrid);
                        for (int i = 0; i < onTypesUpdatedListeners.size(); ++i)
                            onTypesUpdatedListeners.get(i).run();
                        dialog.dismiss();
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
    public String buildInClauseFromSelectedTypes() {
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
}
