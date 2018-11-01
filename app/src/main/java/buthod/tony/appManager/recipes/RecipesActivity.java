package buthod.tony.appManager.recipes;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

public class RecipesActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private LinearLayout mRecipesLayout = null;
    private ImageButton mBackButton = null;
    private ImageButton mAddRecipeButton = null;
    private EditText mSearchField = null;

    private ArrayList<RecipesDAO.Recipe> mRecipes = null;
    private LongSparseArray<ImageView> mRecipeImages = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipes_activity);

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mRecipesLayout = (LinearLayout) findViewById(R.id.recipes_list);
        mAddRecipeButton = (ImageButton) findViewById(R.id.add_recipe_button);
        mSearchField = (EditText) findViewById(R.id.search_field);

        mRecipeImages = new LongSparseArray<>();

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Add recipe_activity
        mAddRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AddEditRecipeActivity.class);
                startActivity(intent);
            }
        });
        // Update search elements
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchFieldChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSearchField.setText("");
        populateListWithRecipes();
    }

    private void populateListWithRecipes() {
        mRecipesLayout.removeAllViews();
        mRecipeImages.clear();
        // Params used for each recipe_activity.
        Resources res = getResources();
        LayoutInflater inflater = getLayoutInflater();

        mRecipes = mDao.getRecipes();
        for (int i = 0; i < mRecipes.size(); ++i) {
            final RecipesDAO.Recipe recipe = mRecipes.get(i);

            View recipeView = inflater.inflate(R.layout.recipe_view, null);
            // Add image view in images to load
            ImageView imageView = (ImageView) recipeView.findViewById(R.id.image_view);
            mRecipeImages.append(recipe.id, imageView);
            // Set the title of recipe
            TextView titleView = (TextView) recipeView.findViewById(R.id.title_view);
            titleView.setText(recipe.name);
            // Update stars for grade and difficulty
            LinearLayout firstLineLayout = (LinearLayout) recipeView.findViewById(R.id.first_line);
            for (int j = 0; j < recipe.grade; ++j)
                firstLineLayout.getChildAt(1 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
            for (int j = 0; j < recipe.difficulty; ++j)
                firstLineLayout.getChildAt(8 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
            // Set preparation time
            TextView timeView = (TextView) recipeView.findViewById(R.id.recipe_time);
            timeView.setText(String.valueOf(recipe.time));

            recipeView.setTag(recipe.id);
            recipeView.setOnClickListener(mRecipeOnClick);
            recipeView.setOnLongClickListener(mRecipeOnLongClick);
            mRecipesLayout.addView(recipeView);
        }
        mRecipesLayout.invalidate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startImportingImages();
            }
        }).start();
    }

    /**
     * On click listener on recipes_activity layouts.
     */
    private View.OnClickListener mRecipeOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long recipeId = (long) v.getTag();
            Intent intent = new Intent(getBaseContext(), RecipeActivity.class);
            intent.putExtra(DatabaseHandler.RECIPES_KEY, recipeId);
            startActivity(intent);
        }
    };

    /**
     * On long click listener on recipes_activity layouts.
     */
    private View.OnLongClickListener mRecipeOnLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            final long recipeId = (long) v.getTag();
            // Set the view as selected
            v.setSelected(true);
            // Show a popup menu
            final PopupMenu popup = new PopupMenu(RecipesActivity.this, v);
            popup.inflate(R.menu.transaction_menu);
            // Registering clicks on the popup menu
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    popup.dismiss();
                    switch(item.getItemId()) {
                        case R.id.modify:
                            Intent intent = new Intent(getBaseContext(), AddEditRecipeActivity.class);
                            intent.putExtra(DatabaseHandler.RECIPES_KEY, recipeId);
                            startActivity(intent);
                            break;
                        case R.id.delete:
                            // Delete the recipe_activity image if exists
                            Utils.deleteLocalFile(RecipesActivity.this,
                                    getExternalFilesDir(null), getImageNameFromId(recipeId));
                            // Delete the recipe_activity in database
                            mDao.deleteRecipe(recipeId);
                            mRecipesLayout.removeView(v);
                            break;
                    }
                    return true;
                }
            });
            popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                @Override
                public void onDismiss(PopupMenu menu) {
                    // Deselect the view
                    v.setSelected(false);
                }
            });
            popup.show();
            return false;
        }
    };

    /**
     * Loads images of recipes and updates image views.
     */
    private void startImportingImages() {
        for (int i = 0; i < mRecipeImages.size(); ++i) {
            long recipeId = mRecipeImages.keyAt(i);
            final Bitmap bitmap = Utils.loadLocalImage(this, getExternalFilesDir(null),
                    getImageNameFromId(recipeId));
            if (bitmap != null) {
                final ImageView imageView = mRecipeImages.get(recipeId);
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }

    //region SEARCH

    private Handler handler = new Handler();

    /**
     * Called when the user changes the string to search.
     * @param searchString The new string to search.
     */
    private void onSearchFieldChanged(final String searchString) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (searchString.compareTo(mSearchField.getText().toString()) == 0) {
                    for (int i = 0; i < mRecipes.size(); ++i) {
                        if (Utils.searchInString(searchString, mRecipes.get(i).name)) {
                            mRecipesLayout.getChildAt(i).setVisibility(View.VISIBLE);
                        }
                        else {
                            mRecipesLayout.getChildAt(i).setVisibility(View.GONE);
                        }
                    }
                }
            }
        }, 400);
    }

    //endregion

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }

    //region STATIC_FUNCTIONS

    /**
     * Get the image name from the id of the recipe_activity.
     */
    public static String getImageNameFromId(long recipeId) {
        return "Recipe_" + String.valueOf(recipeId) + ".jpg";
    }

    //endregion
}
