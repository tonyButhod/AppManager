package buthod.tony.appManager.account;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

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
    private EditText mSearchEdit = null;
    private Button mTypeSelectionButton = null;
    // Classes used in the view pager
    private AccountHistoryActivity mAccountHistory = null;
    private AccountPieChartActivity mAccountPieChart = null;
    private AccountStatementActivity mAccountStatement = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mSearchEdit = (EditText) findViewById(R.id.search_field);
        mTypeSelectionButton = (Button) findViewById(R.id.type_selection);
        mHistoryButton = (Button) findViewById(R.id.account_history_button);
        mStatementButton = (Button) findViewById(R.id.account_statement_button);
        mPieChartButton = (Button) findViewById(R.id.account_piechart_button);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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

    /**
     * Called when a new page is selected. Updates the view.
     * @param position The selected page's position.
     */
    private void onPageSelected(int position) {
        mTypeSelectionButton.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        mSearchEdit.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
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
                        mAccountHistory.onCreate(AccountActivity.this);
                    view = mAccountHistory.getView();
                    collection.addView(view);
                    return view;
                case 1:
                    view = mAccountStatement.getView();
                    if (view == null)
                        mAccountStatement.onCreate(AccountActivity.this);
                    view = mAccountStatement.getView();
                    collection.addView(view);
                    return view;
                case 2:
                    view = mAccountPieChart.getView();
                    if (view == null)
                        mAccountPieChart.onCreate(AccountActivity.this);
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
}
