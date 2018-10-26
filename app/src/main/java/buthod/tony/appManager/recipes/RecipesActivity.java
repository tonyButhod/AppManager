package buthod.tony.appManager.recipes;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

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

    private LinearLayout mRecipesList = null;
    private ImageButton mBackButton = null;
    private ImageButton mAddRecipeButton = null;

    private LongSparseArray<ImageView> mRecipeImages = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipes);

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mRecipesList = (LinearLayout) findViewById(R.id.recipes_list);
        mAddRecipeButton = (ImageButton) findViewById(R.id.add_recipe_button);
        mRecipeImages = new LongSparseArray<>();

        // Finish the activity if back button is pressed
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Add recipe
        mAddRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AddEditRecipeActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateListWithRecipes();
    }

    private void populateListWithRecipes() {
        mRecipesList.removeAllViews();
        mRecipeImages.clear();
        // Params used for each recipe.
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                200, 200
        );
        Resources res = getResources();

        ArrayList<RecipesDAO.Recipe> recipes = mDao.getRecipes();
        for (int i = 0; i < recipes.size(); ++i) {
            final RecipesDAO.Recipe recipe = recipes.get(i);

            LinearLayout recipeLayout = new LinearLayout(this);
            recipeLayout.setOrientation(LinearLayout.HORIZONTAL);
            recipeLayout.setLayoutParams(layoutParams);
            // Initialize the image view
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(imageParams);
            imageView.setBackground(res.getDrawable(R.drawable.no_image));
            mRecipeImages.append(recipe.id, imageView);
            recipeLayout.addView(imageView);
            // Initialize the text view
            TextView textView = new TextView(this);
            textView.setText(String.format(Locale.getDefault(),
                    "%d : %s (%d)\nNote : %d   Difficulté : %d\nTemps de préparation : %d   Personnes : %d",
                    recipe.id, recipe.name, recipe.type, recipe.grade, recipe.difficulty, recipe.time, recipe.people));
            recipeLayout.setTag(recipe.id);
            recipeLayout.addView(textView);
            recipeLayout.setOnClickListener(mRecipeOnClick);
            recipeLayout.setOnLongClickListener(mRecipeOnLongClick);
            mRecipesList.addView(recipeLayout);
        }
        mRecipesList.invalidate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startImportingImages();
            }
        }).start();
    }

    /**
     * On click listener on recipes layouts.
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
     * On long click listener on recipes layouts.
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
                            // Delete the recipe image if exists
                            Utils.deleteLocalFile(RecipesActivity.this,
                                    getExternalFilesDir(null), getImageNameFromId(recipeId));
                            // Delete the recipe in database
                            mDao.deleteRecipe(recipeId);
                            mRecipesList.removeView(v);
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

    public void startImportingImages() {
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

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }

    //region STATIC_FUNCTIONS

    /**
     * Get the image name from the id of the recipe.
     */
    public static String getImageNameFromId(long recipeId) {
        return "Recipe_" + String.valueOf(recipeId) + ".jpg";
    }

    //endregion
}
