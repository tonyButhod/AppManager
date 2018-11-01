package buthod.tony.appManager.recipes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.ParseException;

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
    private ImageView mImageView = null;
    private TextView mRecipeName;
    private LinearLayout mFirstLineLayout;
    private ImageView mFirstGradeStar;
    private ImageView mFirstDifficultyStar;
    private TextView mRecipeTime;
    private EditText mPeople;
    private LinearLayout mIngredientsLayout;
    private LinearLayout mStepsLayout;
    private Button mDeleteRecipe, mEditRecipe;

    private long mRecipeId = -1;
    private RecipesDAO.Recipe mRecipe = null;

    private String[] mRecipeTypes, mUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe_activity);

        mRecipeId = getIntent().getLongExtra(DatabaseHandler.RECIPES_KEY, -1);
        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mRecipeName = (TextView) findViewById(R.id.recipe_name);
        mFirstLineLayout = (LinearLayout) findViewById(R.id.first_line);
        mFirstGradeStar = (ImageView) findViewById(R.id.grade_star_1);
        mFirstDifficultyStar = (ImageView) findViewById(R.id.difficulty_star_1);
        mRecipeTime = (TextView) findViewById(R.id.recipe_time);
        mPeople = (EditText) findViewById(R.id.recipe_people);
        mIngredientsLayout = (LinearLayout) findViewById(R.id.ingredients_list);
        mStepsLayout = (LinearLayout) findViewById(R.id.steps_list);
        mDeleteRecipe = (Button) findViewById(R.id.delete_recipe);
        mEditRecipe = (Button) findViewById(R.id.edit_recipe);
        // Get resources
        Resources res = getResources();
        mRecipeTypes = res.getStringArray(R.array.recipe_types);
        mUnits = res.getStringArray(R.array.units_array);
        // Listener on people number change
        mPeople.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    float numberPeople = Float.parseFloat(charSequence.toString());
                    if (numberPeople > 0) {
                        updateIngredientQuantities(numberPeople);
                    }
                }
                catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Delete and edit buttons listeners
        mDeleteRecipe.setOnClickListener(mOnDeleteListener);
        mEditRecipe.setOnClickListener(mOnEditListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateWithRecipe(mRecipeId);
    }

    /**
     * Populate the view with the recipe information.
     * @param recipeId The recipe id to populate with.
     */
    private void populateWithRecipe(long recipeId) {
        mRecipe = mDao.getRecipe(recipeId);
        Resources res = getResources();
        // Set the recipe information
        mRecipeName.setText(mRecipe.name);
        int offset = mFirstLineLayout.indexOfChild(mFirstGradeStar);
        for (int i = 0; i < mRecipe.grade; i++)
            mFirstLineLayout.getChildAt(offset + i).setBackground(res.getDrawable(R.drawable.star_filled));
        for (int i = mRecipe.grade; i < 5; i++)
            mFirstLineLayout.getChildAt(offset + i).setBackground(res.getDrawable(R.drawable.star_empty));
        offset = mFirstLineLayout.indexOfChild(mFirstDifficultyStar);
        for (int i = 0; i < mRecipe.difficulty; i++)
            mFirstLineLayout.getChildAt(offset + i).setBackground(res.getDrawable(R.drawable.star_filled));
        for (int i = mRecipe.difficulty; i < 5; i++)
            mFirstLineLayout.getChildAt(offset + i).setBackground(res.getDrawable(R.drawable.star_empty));
        mRecipeTime.setText(String.valueOf(mRecipe.time));
        mIngredientsLayout.removeAllViews();
        for (int i = 0; i < mRecipe.ingredients.size(); ++i) {
            addIngredientView(mRecipe.ingredients.get(i));
        }
        mStepsLayout.removeAllViews();
        for (int i = 0; i < mRecipe.steps.size(); ++i) {
            addStepView(mRecipe.steps.get(i));
        }
        mPeople.setText(String.valueOf(mRecipe.people));
        // Load the image
        Bitmap bitmap = Utils.loadLocalImage(this, getExternalFilesDir(null),
                RecipesActivity.getImageNameFromId(mRecipe.id));
        mImageView.setImageBitmap(bitmap);
    }

    /**
     * Add or modify an ingredient.
     * @param ingredient The ingredient to add or modify.
     */
    private void addIngredientView(final RecipesDAO.Ingredient ingredient) {
        // Add the view
        View ingredientView = getLayoutInflater().inflate(R.layout.ingredient_view, null);
        mIngredientsLayout.addView(ingredientView, mIngredientsLayout.getChildCount());
        // Update text view information
        ((TextView) ingredientView.findViewById(R.id.quantity_view)).setText(String.valueOf(ingredient.quantity));
        ((TextView) ingredientView.findViewById(R.id.unit_view)).setText(mUnits[ingredient.idUnit]);
        ((TextView) ingredientView.findViewById(R.id.ingredient_name_view)).setText(ingredient.name);
        ingredientView.invalidate();
    }

    /**
     * Add or modify a step.
     * @param step The step to add or modify.
     */
    private void addStepView(final RecipesDAO.Step step) {
        // Add the view
        final View stepView = getLayoutInflater().inflate(R.layout.step_view, null);
        mStepsLayout.addView(stepView, mStepsLayout.getChildCount());
        // Update text view information
        ((TextView) stepView.findViewById(R.id.number_view)).setText(String.valueOf(step.number));
        ((TextView) stepView.findViewById(R.id.description_view)).setText(step.description);
        stepView.invalidate();
    }

    /**
     * Update quantities with the new number of people set by the user.
     */
    private void updateIngredientQuantities(float people) {
        for (int i = 0; i < mRecipe.ingredients.size(); ++i) {
            RecipesDAO.Ingredient ingredient = mRecipe.ingredients.get(i);
            View ingredientView = mIngredientsLayout.getChildAt(i);
            ((TextView) ingredientView.findViewById(R.id.quantity_view)).setText(
                    String.valueOf(ingredient.quantity * people / mRecipe.people)
            );
        }
        mIngredientsLayout.invalidate();
    }

    /**
     * Listener to delete a recipe. Show a confirmation dialog before deletion.
     */
    private View.OnClickListener mOnDeleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Initialize an alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(RecipeActivity.this);
            builder.setTitle(R.string.delete_confirmation);
            // Set up dialog buttons
            Resources res = getResources();
            builder.setNegativeButton(res.getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.setPositiveButton(res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            mDao.deleteRecipe(mRecipeId);
                            RecipeActivity.this.finish();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    };

    /**
     * Listener to edit a recipe.
     */
    private View.OnClickListener mOnEditListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(RecipeActivity.this, AddEditRecipeActivity.class);
            intent.putExtra(DatabaseHandler.RECIPES_KEY, mRecipe.id);
            startActivity(intent);
        }
    };

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
