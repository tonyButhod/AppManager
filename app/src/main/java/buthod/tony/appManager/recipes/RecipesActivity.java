package buthod.tony.appManager.recipes;

import android.os.Bundle;
import android.widget.LinearLayout;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;

/**
 * Created by Tony on 22/09/2018.
 */

public class RecipesActivity extends RootActivity {

    private LinearLayout mRecipesList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipes);

        mRecipesList = (LinearLayout) findViewById(R.id.recipes_list);
    }
}
