package buthod.tony.appManager.account;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import buthod.tony.appManager.R;
import buthod.tony.appManager.database.AccountDAO;

/**
 * Class used in AccountActivity to display the statement of all transactions in a page viewer.
 */
public class AccountStatementActivity {
    // Fields used for the page viewer
    private AccountActivity mRootActivity = null;
    private View mAccountView = null;

    private AccountDAO mDao = null;

    private Button mPeriodNumberButton = null;
    private Spinner mPeriodTypeSpinner = null;
    private TextView mStatementValueView = null;

    private LineChart mLineChart = null;
    private LinearLayout mLineChartInfo = null;
    private TextView mSelectedCreditValueText, mSelectedExpenseValueText, mSelectedDateText;

    private TextView mMeanExpenseView = null;
    private TextView mMeanCreditView = null;
    private TextView mLastExpenseView = null;
    private TextView mLastCreditView = null;
    private CheckBox mCumulativeCheckbox = null;

    private List<Entry> mExpenseEntries, mCreditEntries;
    private IAxisValueFormatter xAxisValueFormatter;

    /**
     * @return The created view to use in the page viewer.
     */
    public View getView() {
        return mAccountView;
    }

    /**
     * Instantiate all elements needed and update the account view with transactions' history.
     * @param rootActivity The activity containing the page viewer.
     */
    public void onCreate(AccountActivity rootActivity, AccountDAO dao) {
        mRootActivity = rootActivity;
        mDao = dao;
        mAccountView = mRootActivity.getLayoutInflater().inflate(R.layout.account_statement, null);

        mPeriodNumberButton = (Button) mAccountView.findViewById(R.id.period_number_button);
        mPeriodTypeSpinner = (Spinner) mAccountView.findViewById(R.id.period_type_spinner);
        mStatementValueView = (TextView) mAccountView.findViewById(R.id.statement_value);
        mLineChart = (LineChart) mAccountView.findViewById(R.id.line_chart);
        mLineChartInfo = (LinearLayout) mAccountView.findViewById(R.id.line_chart_info);
        mSelectedCreditValueText = (TextView) mAccountView.findViewById(R.id.selected_credit_value_text);
        mSelectedExpenseValueText = (TextView) mAccountView.findViewById(R.id.selected_expense_value_text);
        mSelectedDateText = (TextView) mAccountView.findViewById(R.id.selected_date_text);
        mMeanExpenseView = (TextView) mAccountView.findViewById(R.id.mean_expense);
        mMeanCreditView = (TextView) mAccountView.findViewById(R.id.mean_credit);
        mLastExpenseView = (TextView) mAccountView.findViewById(R.id.last_expense);
        mLastCreditView = (TextView) mAccountView.findViewById(R.id.last_credit);
        mCumulativeCheckbox = (CheckBox) mAccountView.findViewById(R.id.cumulative_checkbox);
        mCumulativeCheckbox.setVisibility(View.GONE);

        // Add listener in the activity
        mPeriodNumberButton.setText("31");
        mPeriodNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNumberPickerDialog();
            }
        });
        mPeriodTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Set default values for the selected choice
                if (position == 0)
                    mPeriodNumberButton.setText("31");
                else if (position == 1)
                    mPeriodNumberButton.setText("6");
                else if (position == 2)
                    mPeriodNumberButton.setText("3");

                computeStatements();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        mPeriodTypeSpinner.setSelection(1);
        mPeriodNumberButton.setText("6");
        mCumulativeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                computeStatements();
            }
        });

        // Set line chart parameters
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setOnClickListener(null);
        mLineChart.setOnChartGestureListener(null);
        mLineChart.setOnChartValueSelectedListener(onChartValueSelectedListener);
        mLineChart.setOnHoverListener(null);

        // Add to types listeners the update the pie chart on type selection
        mRootActivity.onTypesUpdatedListeners.add(new Runnable() {
            @Override
            public void run() {
                updateView();
            }
        });
    }

    /**
     * Update the view with elements in database.
     */
    public void updateView() {
        computeStatements();
    }

    private void showNumberPickerDialog() {
        Resources res = mRootActivity.getResources();
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mRootActivity);
        builder.setTitle("");
        // Set the view of the alert dialog
        final NumberPicker numberPicker = new NumberPicker(mRootActivity);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(100); // The maximum number set by default
        numberPicker.setValue(Integer.valueOf(mPeriodNumberButton.getText().toString()));
        builder.setView(numberPicker);
        // Set up the buttons
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(res.getString(R.string.ok), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        mPeriodNumberButton.setText(String.valueOf(numberPicker.getValue()));
                        computeStatements();
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    /**
     * Computes the statements over the selected period.
     * Updates the line chart with cumulative statements.
     * Also updates the final statement, and the mean expense and credit.
     */
    private void computeStatements() {
        int[] creditStatements = null;
        int[] expenseStatements = null;
        Resources resources = mRootActivity.getResources();
        // Compute financial statement depending on the the selected information
        int periodNumber = Integer.valueOf(mPeriodNumberButton.getText().toString());
        boolean isCumulative = mCumulativeCheckbox.isChecked();
        String selectedPeriodType = mPeriodTypeSpinner.getSelectedItem().toString();
        String[] periodTypes = resources.getStringArray(R.array.dayMonthYear);
        final Date now = new Date();
        String selectedTypes = mRootActivity.buildInClauseFromSelectedTypes();
        // If DAY is selected
        if (selectedPeriodType.equals( periodTypes[0] )) {
            creditStatements = mDao.getDaysStatement(periodNumber, true, selectedTypes);
            expenseStatements = mDao.getDaysStatement(periodNumber, false, selectedTypes);
            xAxisValueFormatter = new IAxisValueFormatter() {
                private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.FRANCE);
                private Calendar cal = Calendar.getInstance();

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    cal.setTime(now);
                    cal.add(Calendar.DATE, (int) value);
                    return dateFormat.format(cal.getTime());
                }
            };
        }
        // If MONTH is selected
        else if (selectedPeriodType.equals( periodTypes[1] )) {
            creditStatements = mDao.getMonthsStatement(periodNumber, true, selectedTypes);
            expenseStatements = mDao.getMonthsStatement(periodNumber, false, selectedTypes);
            xAxisValueFormatter = new IAxisValueFormatter() {
                private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM", Locale.FRANCE);
                private Calendar cal = Calendar.getInstance();

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    cal.setTime(now);
                    cal.add(Calendar.MONTH, (int) value);
                    return dateFormat.format(cal.getTime());
                }
            };
        }
        // If YEAR is selected
        else if (selectedPeriodType.equals( periodTypes[2] )) {
            creditStatements = mDao.getYearsStatement(periodNumber, true, selectedTypes);
            expenseStatements = mDao.getYearsStatement(periodNumber, false, selectedTypes);
            xAxisValueFormatter = new IAxisValueFormatter() {
                private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy", Locale.FRANCE);
                private Calendar cal = Calendar.getInstance();

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    cal.setTime(now);
                    cal.add(Calendar.YEAR, (int) value);
                    return dateFormat.format(cal.getTime());
                }
            };
        }

        // Creates entries dataset and compute the final statement over the period
        mCreditEntries = new ArrayList<>();
        mExpenseEntries = new ArrayList<>();
        int cumulativeCredits = 0;
        int cumulativeExpenses = 0;
        for (int i = 0; i < periodNumber; ++i) {
            boolean isFirstOrLast = i == 0 || i == periodNumber - 1;
            // Add a new entry only if the statement is not zero for intermediate values
            cumulativeCredits += creditStatements[i];
            cumulativeExpenses += expenseStatements[i];
            if (isCumulative) {
                mCreditEntries.add(new Entry(i + 1 - periodNumber, cumulativeCredits / 100.0f));
                mExpenseEntries.add(new Entry(i + 1 - periodNumber, cumulativeExpenses / 100.0f));
            }
            else {
                mCreditEntries.add(new Entry(i + 1 - periodNumber, creditStatements[i] / 100.0f));
                mExpenseEntries.add(new Entry(i + 1 - periodNumber, expenseStatements[i] / 100.0f));
            }
        }
        // Update lines of the line chart
        LineDataSet creditDataset = new LineDataSet(mCreditEntries,
                resources.getString(isCumulative ? R.string.cumulative_credits : R.string.credits));
        creditDataset.setColor(ContextCompat.getColor(mRootActivity, R.color.green));
        creditDataset.setCircleColor(ContextCompat.getColor(mRootActivity, R.color.green));
        creditDataset.setDrawValues(false);
        LineDataSet expenseDataset = new LineDataSet(mExpenseEntries,
                resources.getString(isCumulative ? R.string.cumulative_expenses : R.string.expenses));
        expenseDataset.setColor(ContextCompat.getColor(mRootActivity, R.color.red));
        expenseDataset.setCircleColor(ContextCompat.getColor(mRootActivity, R.color.red));
        expenseDataset.setDrawValues(false);
        List<ILineDataSet> lines = new ArrayList<>();
        lines.add(creditDataset);
        lines.add(expenseDataset);
        mLineChart.setData(new LineData(lines));
        // Set up the x Axis
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setGranularity(1);
        xAxis.setValueFormatter(xAxisValueFormatter);
        mLineChart.invalidate();
        onChartValueSelectedListener.onNothingSelected();

        // Update the final statement over the selected period
        int finalStatement = (cumulativeCredits - cumulativeExpenses);
        mStatementValueView.setText(String.valueOf(finalStatement / 100.0f) + "€");
        if (finalStatement >= 0)
            mStatementValueView.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_green));
        else
            mStatementValueView.setTextColor(ContextCompat.getColor(mRootActivity, R.color.dark_soft_red));
        mStatementValueView.invalidate();

        // Update the mean expense and credit over the selected period without last day/month/year.
        cumulativeExpenses -= expenseStatements[periodNumber - 1];
        cumulativeCredits -= creditStatements[periodNumber - 1];
        mMeanExpenseView.setText(String.valueOf(cumulativeExpenses / (periodNumber - 1) / 100.0f) + "€");
        mMeanCreditView.setText(String.valueOf(cumulativeCredits / (periodNumber - 1) / 100.0f) + "€");

        // Update the last expense and credit over the selected period
        mLastExpenseView.setText(String.valueOf(expenseStatements[periodNumber - 1] / 100.0f) + "€");
        mLastCreditView.setText(String.valueOf(creditStatements[periodNumber - 1] / 100.0f) + "€");
    }

    /**
     * Listener on value selected on line chart.
     */
    private OnChartValueSelectedListener onChartValueSelectedListener = new OnChartValueSelectedListener() {
        @Override
        public void onValueSelected(Entry e, Highlight h) {
            mLineChartInfo.setVisibility(View.VISIBLE);
            int entryIndex = mCreditEntries.indexOf(e);
            if (entryIndex < 0)
                entryIndex = mExpenseEntries.indexOf(e);
            mSelectedCreditValueText.setText(String.valueOf(mCreditEntries.get(entryIndex).getY()) + "€");
            mSelectedExpenseValueText.setText(String.valueOf(mExpenseEntries.get(entryIndex).getY()) + "€");
            mSelectedDateText.setText(xAxisValueFormatter.getFormattedValue(e.getX(), null));
        }

        @Override
        public void onNothingSelected() {
            mLineChartInfo.setVisibility(View.INVISIBLE);
        }
    };
}
