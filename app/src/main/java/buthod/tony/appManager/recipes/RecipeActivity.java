package buthod.tony.appManager.recipes;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.utils.Utils;
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
    private List<IngredientViewManagement> mIngredients = null;

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
                float numberPeople = Utils.parseFloatWithDefault(charSequence.toString(), 0);
                if (numberPeople > 0) {
                    updateIngredientQuantities(numberPeople);
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

        populateWithRecipe(mRecipeId);
    }

    /**
     * Populate the view with the recipe information.
     * @param recipeId The recipe id to populate with.
     */
    private void populateWithRecipe(long recipeId) {
        mRecipe = mDao.getRecipe(recipeId);
        mIngredients = new ArrayList<>();
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
        for (int i = 0; i < mRecipe.separations.size(); ++i) {
            addSeparationView(mRecipe.separations.get(i));
        }
        mPeople.setText(String.valueOf(mRecipe.people));
        // Update quantities with the number of people
        updateIngredientQuantities(Utils.parseFloatWithDefault(mPeople.getText().toString(), 0));
        // Load the image
        Bitmap bitmap = Utils.loadLocalImage(this, getExternalFilesDir(null),
                RecipesActivity.getImageNameFromId(mRecipe.id));
        mImageView.setImageBitmap(bitmap);
        // Add unit conversion
        enableUnitConversionsForIngredients();
    }

    /**
     * Load conversions in database and set it for each ingredient to enable the unit selection.
     */
    private void enableUnitConversionsForIngredients() {
        // First needed ingredient conversions
        long[] ingredientsId = new long[mRecipe.ingredients.size()];
        for (int i = 0; i < mRecipe.ingredients.size(); ++i)
            ingredientsId[i] = mRecipe.ingredients.get(i).idIngredient;
        List<RecipesDAO.IngredientConversions> ingredientConversions =
                mDao.getIngredientsWithConversions(ingredientsId);
        LongSparseArray<RecipesDAO.IngredientConversions> conversions = new LongSparseArray<>();
        for (int i = 0; i < ingredientConversions.size(); ++i)
            conversions.append(ingredientConversions.get(i).ingredientId, ingredientConversions.get(i));
        // Then for each ingredient with conversions, set the conversion list.
        RecipesDAO.IngredientConversions conversionsFound = null;
        for (int i = 0; i < mRecipe.ingredients.size(); ++i) {
            RecipesDAO.Ingredient ingredient = mRecipe.ingredients.get(i);
            if ((conversionsFound = conversions.get(ingredient.idIngredient, null)) != null
                    && conversionsFound.conversions.size() > 0) {
                mIngredients.get(i).setConversion(this, conversionsFound.conversions);
            }
        }
    }

    /**
     * Add or modify an ingredient.
     * @param ingredient The ingredient to add or modify.
     */
    private void addIngredientView(final RecipesDAO.Ingredient ingredient) {
        // Add the view
        IngredientViewManagement ingredientManagement = new IngredientViewManagement(this);
        ingredientManagement.setIngredient(ingredient);
        View ingredientView = ingredientManagement.getView();
        mIngredientsLayout.addView(ingredientView, mIngredientsLayout.getChildCount());
        mIngredients.add(ingredientManagement);
    }

    /**
     * Add or modify a step.
     * @param step The step to add or modify.
     */
    private void addStepView(final RecipesDAO.Step step) {
        int index = mStepsLayout.getChildCount();
        // Add the view
        final View stepView = getLayoutInflater().inflate(R.layout.step_view, null);
        mStepsLayout.addView(stepView, index);
        // Update text view information
        ((TextView) stepView.findViewById(R.id.number_view)).setText(String.valueOf(index + 1));
        ((TextView) stepView.findViewById(R.id.description_view)).setText(step.description);
        stepView.invalidate();
    }

    /**
     * Add or modify a separation.
     * @param separation The separation to add or modify.
     */
    private void addSeparationView(final RecipesDAO.Separation separation) {
        // Add the view
        final View separationView = getLayoutInflater().inflate(R.layout.separation_view, null);
        if (separation.type == RecipesDAO.Separation.INGREDIENT_TYPE)
            mIngredientsLayout.addView(separationView, separation.number);
        else
            mStepsLayout.addView(separationView, separation.number);
        // Update text view information
        ((TextView) separationView.findViewById(R.id.description_view)).setText(separation.description);
        separationView.invalidate();
    }

    /**
     * Update quantities with the new number of people set by the user.
     */
    private void updateIngredientQuantities(float people) {
        for (int i = 0; i < mIngredients.size(); ++i) {
            mIngredients.get(i).setQuantityFactor(people / mRecipe.people);
        }
        mIngredientsLayout.invalidate();
    }

    //region EDITION

    private static int EDIT_RECIPE_REQUEST = 1;

    /**
     * Listener to delete a recipe. Show a confirmation dialog before deletion.
     */
    private View.OnClickListener mOnDeleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Utils.showConfirmDeleteDialog(RecipeActivity.this, R.string.delete_confirmation,
                    new Runnable() {
                @Override
                public void run() {
                    mDao.deleteRecipe(mRecipeId);
                    mDao.deleteUnusedIngredients();
                    RecipeActivity.this.finish();
                }
            });
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
            startActivityForResult(intent, EDIT_RECIPE_REQUEST);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_RECIPE_REQUEST) {
                // The recipe might have been edited, than populate the activity with new data.
                populateWithRecipe(mRecipeId);
            }
        }
    }

    //endregion

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
