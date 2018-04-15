package buthod.tony.appManager.account;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.AccountDAO;

/**
 * Created by Tony on 09/10/2017.
 */

public class AccountStatementActivity extends RootActivity {

    private AccountDAO mDao = null;

    private ImageButton mBackButton = null;

    private Button mPeriodNumberButton = null;
    private Spinner mPeriodTypeSpinner = null;
    private TextView mStatementValueView = null;
    private LineChart mLineChart = null;
    private TextView mMeanExpenseView = null;
    private TextView mMeanCreditView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_statement);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mPeriodNumberButton = (Button) findViewById(R.id.period_number_button);
        mPeriodTypeSpinner = (Spinner) findViewById(R.id.period_type_spinner);
        mStatementValueView = (TextView) findViewById(R.id.statement_value);
        mLineChart = (LineChart) findViewById(R.id.line_chart);
        mMeanExpenseView = (TextView) findViewById(R.id.mean_expense);
        mMeanCreditView = (TextView) findViewById(R.id.mean_credit);
        mDao = new AccountDAO(getBaseContext());

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Add listener in the activity
        mPeriodNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNumberPickerDialog();
            }
        });
        mPeriodTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                computeStatements();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set line chart parameters
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setOnClickListener(null);
        mLineChart.setOnChartGestureListener(null);
        mLineChart.setOnChartValueSelectedListener(null);
        mLineChart.setOnHoverListener(null);

        // Update views
        computeStatements();
    }

    private void showNumberPickerDialog() {
        Resources res = getResources();
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("");
        // Set the view of the alert dialog
        final NumberPicker numberPicker = new NumberPicker(getApplicationContext());
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
        IAxisValueFormatter xAxisValueFormatter = null;
        Resources resources = getResources();
        // Compute financial statement depending on the the selected information
        int periodNumber = Integer.valueOf(mPeriodNumberButton.getText().toString());
        String selectedPeriodType = mPeriodTypeSpinner.getSelectedItem().toString();
        String[] periodTypes = resources.getStringArray(R.array.dayMonthYear);
        mDao.open();
        final Date now = new Date();
        // If DAY is selected
        if (selectedPeriodType.equals( periodTypes[0] )) {
            creditStatements = mDao.getDaysStatement(periodNumber, true);
            expenseStatements = mDao.getDaysStatement(periodNumber, false);
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
            creditStatements = mDao.getMonthsStatement(periodNumber, true);
            expenseStatements = mDao.getMonthsStatement(periodNumber, false);
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
            creditStatements = mDao.getYearsStatement(periodNumber, true);
            expenseStatements = mDao.getYearsStatement(periodNumber, false);
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
        mDao.close();

        // Creates entries dataset and compute the final statement over the period
        List<Entry> creditEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        int cumulativeCredits = 0;
        int cumulativeExpenses = 0;
        for (int i = 0; i < periodNumber; ++i) {
            boolean isFirstOrLast = i == 0 || i == periodNumber - 1;
            // Add a new entry only if the statement is not zero for intermediate values
            if (creditStatements[i] != 0 || isFirstOrLast) {
                cumulativeCredits += creditStatements[i];
                creditEntries.add(new Entry(i + 1 - periodNumber, cumulativeCredits / 100.0f));
            }
            if (expenseStatements[i] != 0 || isFirstOrLast) {
                cumulativeExpenses += expenseStatements[i];
                expenseEntries.add(new Entry(i + 1 - periodNumber, cumulativeExpenses / 100.0f));
            }
        }
        // Update lines of the line chart
        LineDataSet creditDataset = new LineDataSet(creditEntries,
                resources.getString(R.string.cumulative_credits));
        creditDataset.setColor(ContextCompat.getColor(this, R.color.green));
        creditDataset.setCircleColor(ContextCompat.getColor(this, R.color.green));
        creditDataset.setDrawValues(false);
        LineDataSet expenseDataset = new LineDataSet(expenseEntries,
                resources.getString(R.string.cumulative_expenses));
        expenseDataset.setColor(ContextCompat.getColor(this, R.color.red));
        expenseDataset.setCircleColor(ContextCompat.getColor(this, R.color.red));
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

        // Update the final statement over the selected period
        int finalStatement = (cumulativeCredits - cumulativeExpenses);
        mStatementValueView.setText(String.valueOf(finalStatement / 100.0f) + "€");
        if (finalStatement >= 0)
            mStatementValueView.setTextColor(ContextCompat.getColor(this, R.color.dark_soft_green));
        else
            mStatementValueView.setTextColor(ContextCompat.getColor(this, R.color.dark_soft_red));
        mStatementValueView.invalidate();

        // Update the mean expense and credit over the selected period
        mMeanExpenseView.setText(String.valueOf(cumulativeExpenses / periodNumber / 100.0f) + "€");
        mMeanCreditView.setText(String.valueOf(cumulativeCredits / periodNumber / 100.0f) + "€");
    }
}
