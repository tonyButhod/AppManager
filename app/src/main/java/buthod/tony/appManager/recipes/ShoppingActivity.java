package buthod.tony.appManager.recipes;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.Utils;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 22/09/2018.
 */

public class ShoppingActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private ImageButton mBackButton = null;
    private LinearLayout mQuantitiesLayout = null;
    private LinearLayout mRecipesLayout = null;

    private long[] mRecipesId;
    private LongSparseArray<Float> mRecipesPeopleRatio;
    private ArrayList<RecipesDAO.Recipe> mRecipes;
    private LongSparseArray<ImageView> mRecipeImages = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shopping_activity);

        mRecipesId = getIntent().getLongArrayExtra(DatabaseHandler.RECIPES_KEY);
        float[] peopleRatio = getIntent().getFloatArrayExtra(DatabaseHandler.RECIPES_PEOPLE);
        mRecipesPeopleRatio = new LongSparseArray<>();
        for (int i = 0; i < peopleRatio.length; ++i)
            mRecipesPeopleRatio.append(mRecipesId[i], peopleRatio[i]);
        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mQuantitiesLayout = (LinearLayout) findViewById(R.id.quantities_list);
        mRecipesLayout = (LinearLayout) findViewById(R.id.recipes_list);
        mRecipeImages = new LongSparseArray<>();

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        populateWithQuantitiesList();
        populateWithRecipesList();
    }

    private void populateWithQuantitiesList() {
        ArrayList<RecipesDAO.Ingredient> quantities = mDao.getQuantitiesFromRecipes(mRecipesId, mRecipesPeopleRatio);
        LayoutInflater inflater = getLayoutInflater();
        String[] units = getResources().getStringArray(R.array.units_array);
        for (int i = 0; i < quantities.size(); ++i) {
            View v = inflater.inflate(R.layout.ingredient_view, null);
            ((TextView) v.findViewById(R.id.quantity_view)).setText(Utils.floatToString(quantities.get(i).quantity, 3));
            ((TextView) v.findViewById(R.id.unit_view)).setText(units[quantities.get(i).idUnit]);
            ((TextView) v.findViewById(R.id.ingredient_name_view)).setText(quantities.get(i).name);
            v.findViewById(R.id.optional_view).setVisibility(
                    quantities.get(i).type == RecipesDAO.Ingredient.OPTIONAL_TYPE ? View.VISIBLE : View.GONE);
            mQuantitiesLayout.addView(v);
        }
    }

    private void populateWithRecipesList() {
        mRecipes = mDao.getRecipes(mRecipesId);
        LayoutInflater inflater = getLayoutInflater();
        Resources res = getResources();
        for (int i = 0; i < mRecipes.size(); ++i) {
            View v = inflater.inflate(R.layout.recipe_view, null);
            mRecipeImages.append(mRecipes.get(i).id, (ImageView) v.findViewById(R.id.image_view));
            ((TextView) v.findViewById(R.id.title_view)).setText(mRecipes.get(i).name);
            ((TextView) v.findViewById(R.id.recipe_time)).setText(String.valueOf(mRecipes.get(i).time));
            int selectedPeople = Math.round(mRecipes.get(i).people * mRecipesPeopleRatio.get(mRecipes.get(i).id));
            ((TextView) v.findViewById(R.id.recipe_people)).setText(String.valueOf(selectedPeople));
            v.findViewById(R.id.recipe_people).setVisibility(View.VISIBLE);
            v.findViewById(R.id.recipe_people_label).setVisibility(View.VISIBLE);
            // Change time field and use it to display the number of people used
            // Update stars for grade and difficulty
            LinearLayout firstLineLayout = (LinearLayout) v.findViewById(R.id.first_line);
            for (int j = 0; j < mRecipes.get(i).grade; ++j)
                firstLineLayout.getChildAt(1 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
            for (int j = 0; j < mRecipes.get(i).difficulty; ++j)
                firstLineLayout.getChildAt(8 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
            mRecipesLayout.addView(v);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                RecipesActivity.startImportingImages(ShoppingActivity.this, mRecipeImages);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
