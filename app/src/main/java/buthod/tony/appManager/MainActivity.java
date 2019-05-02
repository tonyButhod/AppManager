package buthod.tony.appManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import buthod.tony.appManager.account.AccountActivity;
import buthod.tony.appManager.checkList.CheckListGroupsActivity;
import buthod.tony.appManager.pedometer.PedometerActivity;
import buthod.tony.appManager.pedometer.PedometerManager;
import buthod.tony.appManager.recipes.RecipesActivity;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends RootActivity {

    private SharedPreferences mPreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPreferences = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        ////////////// Settings //////////////
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(v.getContext(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        ///////////// Pedometer ///////////////
        // Button to access pedometer section
        findViewById(R.id.pedometer_section_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pedometerIntent = new Intent(v.getContext(), PedometerActivity.class);
                startActivity(pedometerIntent);
            }
        });

        ///////////////// Expenses ///////////////
        findViewById(R.id.expenses_section_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent expensesIntent = new Intent(getBaseContext(), AccountActivity.class);
                startActivity(expensesIntent);
            }
        });

        //////////////// Recipes /////////////////
        findViewById(R.id.recipes_section_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent recipesIntent = new Intent(getBaseContext(), RecipesActivity.class);
                startActivity(recipesIntent);
            }
        });

        //////////////// Check list /////////////////
        findViewById(R.id.check_lists_section_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent checkListsIntent = new Intent(getBaseContext(), CheckListGroupsActivity.class);
                startActivity(checkListsIntent);
            }
        });
    }
}
