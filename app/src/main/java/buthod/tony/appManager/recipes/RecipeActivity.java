package buthod.tony.appManager.recipes;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;
import java.util.ResourceBundle;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.Utils;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 22/09/2018.
 */

public class RecipeActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private ImageButton mBackButton = null;
    private TextView mRecipeView = null;
    private ImageView mImageView = null;

    private RecipesDAO.Recipe mRecipe = null;

    private String[] mRecipeTypes, mUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe);

        long recipeId = getIntent().getLongExtra(DatabaseHandler.RECIPES_KEY, -1);
        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mRecipeView = (TextView) findViewById(R.id.recipe_view);
        mImageView = (ImageView) findViewById(R.id.image_view);
        // Get resources
        Resources res = getResources();
        mRecipeTypes = res.getStringArray(R.array.recipe_types);
        mUnits = res.getStringArray(R.array.units_array);

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        populateWithRecipe(recipeId);
    }

    private void populateWithRecipe(long recipeId) {
        mRecipe = mDao.getRecipe(recipeId);
        // Set the text information
        String contentView = String.format(Locale.getDefault(),
                "%d : %s (%d : %s)\nNote : %d   Difficulté : %d\nTemps de préparation : %d   Personnes : %d\n",
                mRecipe.id, mRecipe.name, mRecipe.type, mRecipeTypes[mRecipe.type], mRecipe.grade,
                mRecipe.difficulty, mRecipe.time, mRecipe.people);
        contentView += "Ingrédiants : \n";
        for (int i = 0; i < mRecipe.ingredients.size(); ++i) {
            RecipesDAO.Ingredient ingredient = mRecipe.ingredients.get(i);
            contentView += String.format(Locale.getDefault(), "%d    %s %s %s (%d)\n", ingredient.idQuantity,
                    Utils.floatToString(ingredient.quantity), mUnits[ingredient.idUnit], ingredient.name, ingredient.idIngredient);
        }
        contentView += "Etapes : \n";
        for (int i = 0; i < mRecipe.steps.size(); ++i) {
            RecipesDAO.Step step = mRecipe.steps.get(i);
            contentView += String.format(Locale.getDefault(), "%d    %d : %s\n",
                    step.id, step.number, step.description);
        }
        mRecipeView.setText(contentView);
        // Set the image if exists
        Bitmap image = Utils.loadLocalImage(this, getExternalFilesDir(null),
                RecipesActivity.getImageNameFromId(mRecipe.id));
        if (image != null) {
            mImageView.setImageBitmap(image);
            mImageView.setVisibility(View.VISIBLE);
        }
        else {
            mImageView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
