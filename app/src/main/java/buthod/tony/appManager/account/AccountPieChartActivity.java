package buthod.tony.appManager.account;


import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.ParseException;
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
 * Created by Tony on 28/04/2018.
 */

public class AccountPieChartActivity extends RootActivity {

    private ImageButton mBackButton = null;
    private Button mStartDateButton = null;
    private Button mEndDateButton = null;
    private PieChart mExpensesPieChart = null;
    private PieChart mCreditsPieChart = null;

    private AccountDAO mDao = null;
    private static final SimpleDateFormat mDateFormatter =
            new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

    public static final int[] PIE_CHART_COLORS = {
            Color.rgb(255, 0, 0), Color.rgb(80, 80, 255), Color.rgb(50, 200, 0),
            Color.rgb(180, 120, 80), Color.rgb(220, 50, 220), Color.rgb(255, 125, 0),
            Color.rgb(0, 210, 210), Color.rgb(220, 180, 0), Color.rgb(150, 150, 150),
            Color.rgb(70, 150, 200), Color.rgb(0, 190, 120)
    };
    public static final float
            LEGEND_TEXT_SIZE = 14.0f,
            VALUE_TEXT_SIZE = 12.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_pie_chart);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mStartDateButton = (Button) findViewById(R.id.start_date);
        mEndDateButton = (Button) findViewById(R.id.end_date);
        mExpensesPieChart = (PieChart) findViewById(R.id.expenses_pie_chart);
        mCreditsPieChart = (PieChart) findViewById(R.id.credits_pie_chart);
        mDao = new AccountDAO(this);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        View.OnClickListener selectDateListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectDatePopup((TextView) v);
            }
        };
        mStartDateButton.setOnClickListener(selectDateListener);
        mEndDateButton.setOnClickListener(selectDateListener);
        // Set default values
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        mEndDateButton.setText(mDateFormatter.format(cal.getTime()));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.MONTH, -3);
        mStartDateButton.setText(mDateFormatter.format(cal.getTime()));

        // Set default style
        mExpensesPieChart.getDescription().setEnabled(false);
        mExpensesPieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.dark_soft_red));
        mExpensesPieChart.setDrawEntryLabels(false);
        mExpensesPieChart.getLegend().setWordWrapEnabled(true);
        mExpensesPieChart.getLegend().setTextSize(LEGEND_TEXT_SIZE);
        mCreditsPieChart.getDescription().setEnabled(false);
        mCreditsPieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.dark_soft_green));
        mCreditsPieChart.setDrawEntryLabels(false);
        mCreditsPieChart.getLegend().setWordWrapEnabled(true);
        mCreditsPieChart.getLegend().setTextSize(LEGEND_TEXT_SIZE);

        computeStatementByCategory();
    }

    /**
     * Show a popup to select a date using default DatePicker of Android.
     * This popup updates the text of the TextView in argument with the formatted date.
     * @param clickedView The clicked view to update text.
     */
    private void showSelectDatePopup(final TextView clickedView) {
        Resources res = getResources();
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("");
        // Set the view of the alert dialog
        final DatePicker datePicker = new DatePicker(this);
        builder.setView(datePicker);
        // Set the date of the date picker depending on the clicked element
        final Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(mDateFormatter.parse(clickedView.getText().toString()));
        }
        catch (ParseException e) {
            // Do nothing
        }
        datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
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
                        cal.set(Calendar.YEAR, datePicker.getYear());
                        cal.set(Calendar.MONTH, datePicker.getMonth());
                        cal.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                        clickedView.setText(mDateFormatter.format(cal.getTime()));
                        computeStatementByCategory();
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    /**
     * Update the 2 pie charts for expenses and credits.
     * Also update the final statement view.
     */
    private void computeStatementByCategory() {
        // Get the selected dates
        Date startDate = new Date(), endDate = new Date();
        try {
            startDate = mDateFormatter.parse(mStartDateButton.getText().toString());
            endDate = mDateFormatter.parse(mEndDateButton.getText().toString());
        }
        catch (ParseException e) {
            // Do nothing
        }

        mDao.open();
        SparseIntArray statementByCategory = mDao.getStatementByCategory(startDate, endDate);
        mDao.close();

        String[] expenseLabels = getResources().getStringArray(R.array.expense_types);
        String[] creditLabels = getResources().getStringArray(R.array.credit_types);
        // Populate the pie charts
        List<PieEntry> expenseEntries = new ArrayList<>(),
                creditEntries = new ArrayList<>();
        int expensesStatement = 0, creditsStatement = 0;
        for (int i = 0; i < statementByCategory.size(); ++i) {
            int type = statementByCategory.keyAt(i);
            int price = statementByCategory.valueAt(i);
            if (type >= 0) {
                expenseEntries.add(new PieEntry(price / 100.0f, expenseLabels[type]));
                expensesStatement += price;
            }
            else {
                creditEntries.add(new PieEntry(price / 100.0f, creditLabels[-type-1]));
                creditsStatement += price;
            }
        }
        String datasetLabel = getResources().getString(R.string.pie_chart_dataset_label);
        PieDataSet expensesDataset = new PieDataSet(expenseEntries, datasetLabel),
                creditsDataset = new PieDataSet(creditEntries, datasetLabel);
        expensesDataset.setValueTextColor(Color.WHITE);
        creditsDataset.setValueTextColor(Color.WHITE);
        expensesDataset.setValueTextSize(VALUE_TEXT_SIZE);
        creditsDataset.setValueTextSize(VALUE_TEXT_SIZE);
        expensesDataset.setColors(PIE_CHART_COLORS);
        creditsDataset.setColors(PIE_CHART_COLORS);
        mExpensesPieChart.setData(new PieData(expensesDataset));
        mCreditsPieChart.setData(new PieData(creditsDataset));
        // Update statements in the center of pie charts
        mExpensesPieChart.setCenterText(String.valueOf(expensesStatement / 100.0f) + "€");
        mCreditsPieChart.setCenterText(String.valueOf(creditsStatement / 100.0f) + "€");
        // Invalidate pie charts
        mExpensesPieChart.invalidate();
        mCreditsPieChart.invalidate();
    }
}
