package buthod.tony.appManager.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

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
 * Created by Tony on 09/10/2017.
 */

public class AccountActivity extends RootActivity {

    private ImageButton mBackButton = null;

    private Button mHistoryButton = null;
    private Button mStatementButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mHistoryButton = (Button) findViewById(R.id.history);
        mStatementButton = (Button) findViewById(R.id.statement);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Add listener for all buttons in the activity
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent historyIntent = new Intent(v.getContext(), AccountHistoryActivity.class);
                startActivity(historyIntent);
            }
        });
        mStatementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent statementIntent = new Intent(v.getContext(), AccountStatementActivity.class);
                startActivity(statementIntent);
            }
        });
    }

    /**
     * Function used to save all data stored on the database in a external file.
     * @param context The context of the application.
     * @return The JSONArray containing all data stored on the database.
     */
    public static JSONArray saveDataPublicStorage(Context context) {
        AccountDAO dao = new AccountDAO(context);
        dao.open();
        ArrayList<AccountDAO.TransactionInfo> transactions = dao.getTransactions(Integer.MAX_VALUE);
        dao.close();
        JSONArray saveData = new JSONArray();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);
        for (int i = 0; i < transactions.size(); ++i) {
            AccountDAO.TransactionInfo info = transactions.get(i);
            JSONArray infoJson = new JSONArray();
            infoJson.put(info.id);
            infoJson.put(info.type);
            infoJson.put(info.price);
            infoJson.put(formatter.format(info.date));
            infoJson.put(info.comment);
            saveData.put(infoJson);
        }
        return saveData;
    }

    public static void loadDataPublicStorage(Context context, JSONArray data)
            throws JSONException, ParseException {
        AccountDAO dao = new AccountDAO(context);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);
        dao.open();
        for (int i = 0; i < data.length(); ++i) {
            JSONArray infoJson = data.getJSONArray(i);
            dao.addTransaction(infoJson.getInt(1), infoJson.getInt(2),
                    formatter.parse(infoJson.getString(3)), infoJson.getString(4));
        }
        dao.close();
    }
}
