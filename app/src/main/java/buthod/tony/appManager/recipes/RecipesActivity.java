package buthod.tony.appManager.recipes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.utils.Utils;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 22/09/2018.
 */

public class RecipesActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private LinearLayout mRecipesLayout = null;
    private ImageButton mAddRecipeButton = null;
    private EditText mSearchField = null;
    private ImageButton mTopRightButton = null, mValidateShoppingButton = null;
    private RelativeLayout mDropDownMenu = null;
    private Drawable mThreeDotsDrawable, mCrossCloseDrawable;

    private ArrayList<RecipesDAO.Recipe> mRecipes = null;
    private LongSparseArray<ImageView> mRecipeImages = null;
    // Shopping part
    private ArrayList<Integer> mSelectedIndices = null;
    private boolean mIsShopping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipes_activity);
        Resources res = getResources();

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mRecipesLayout = (LinearLayout) findViewById(R.id.recipes_list);
        mAddRecipeButton = (ImageButton) findViewById(R.id.add_recipe_button);
        mSearchField = (EditText) findViewById(R.id.search_field);
        mTopRightButton = (ImageButton) findViewById(R.id.drop_down_button);
        mDropDownMenu = (RelativeLayout) findViewById(R.id.drop_down_menu);
        mValidateShoppingButton = (ImageButton) findViewById(R.id.validate_shopping_button);
        mThreeDotsDrawable = res.getDrawable(R.drawable.three_dots);
        mCrossCloseDrawable = res.getDrawable(R.drawable.cross_close);

        mRecipeImages = new LongSparseArray<>();
        mSelectedIndices = new ArrayList<>();

        // Finish the activity if back button is pressed
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
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
        // Drop down menu part part
        mTopRightButton.setOnClickListener(mOnDropDownClickListener);
        findViewById(R.id.shopping_button).setOnClickListener(mOnShoppingClickListener);
        mValidateShoppingButton.setOnClickListener(mOnValidateShoppingListener);
        findViewById(R.id.ingredients_management_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnDropDownClickListener.onClick(mTopRightButton);
                Intent intent = new Intent(getBaseContext(), IngredientsManagementActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateListWithRecipes();
    }

    //region DROP_DOWN

    private boolean mIsMenuDown = false;
    private static int DROP_DOWN_MENU_HEIGHT = 250;
    private static int DROP_DOWN_DURATION = 200;

    private View.OnClickListener mOnDropDownClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mIsMenuDown) {
                mTopRightButton.setImageDrawable(mCrossCloseDrawable);
                ValueAnimator anim = ValueAnimator.ofInt(0, DROP_DOWN_MENU_HEIGHT);
                anim.addUpdateListener(mDropDownMenuAnimatorListener);
                anim.setDuration(DROP_DOWN_DURATION).start();
            }
            else {
                mTopRightButton.setImageDrawable(mThreeDotsDrawable);
                ValueAnimator anim = ValueAnimator.ofInt(DROP_DOWN_MENU_HEIGHT, 0);
                anim.addUpdateListener(mDropDownMenuAnimatorListener);
                anim.setDuration(DROP_DOWN_DURATION).start();
            }
            mIsMenuDown = !mIsMenuDown;
        }
    };

    private ValueAnimator.AnimatorUpdateListener mDropDownMenuAnimatorListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int val = (int) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = mDropDownMenu.getLayoutParams();
            params.height = val;
            mDropDownMenu.setLayoutParams(params);
        }
    };

    //endregion

    //region RECIPES

    private void populateListWithRecipes() {
        // Params used for each recipe_activity.
        Resources res = getResources();
        LayoutInflater inflater = getLayoutInflater();
        mRecipes = mDao.getRecipes();
        // First remove deleted recipes
        for (int i = 0; i < mRecipes.size(); ++i) {
            final RecipesDAO.Recipe recipe = mRecipes.get(i);

            // Try to recover the view with tag
            View recipeView = mRecipesLayout.findViewWithTag(recipe.id);
            if (recipeView == null) {
                // No view previously created, then inflate a new one
                recipeView = inflater.inflate(R.layout.recipe_view, null);
                recipeView.setTag(recipe.id);
                recipeView.setOnClickListener(mRecipeOnClick);
                recipeView.setOnLongClickListener(mRecipeOnLongClick);
                // Add image view in images to load
                ImageView imageView = (ImageView) recipeView.findViewById(R.id.image_view);
                mRecipeImages.append(recipe.id, imageView);
            }
            else {
                mRecipesLayout.removeView(recipeView);
            }
            // Add it to the layout
            mRecipesLayout.addView(recipeView, i);
            // Update recipe's information
            ((TextView) recipeView.findViewById(R.id.title_view)).setText(recipe.name);
            ((TextView) recipeView.findViewById(R.id.recipe_time)).setText(String.valueOf(recipe.time));
            // Update stars for grade and difficulty
            LinearLayout firstLineLayout = (LinearLayout) recipeView.findViewById(R.id.first_line);
            for (int j = 0; j < recipe.grade; ++j)
                firstLineLayout.getChildAt(1 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
            for (int j = 0; j < recipe.difficulty; ++j)
                firstLineLayout.getChildAt(8 + j)
                        .setBackground(res.getDrawable(R.drawable.star_filled));
        }
        // Delete recipes that are not existing anymore
        for (int i = mRecipes.size(); i < mRecipesLayout.getChildCount(); ++i)
            mRecipesLayout.removeViewAt(i);
        // Finally invalidate the view and load back images for update
        mRecipesLayout.invalidate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startImportingImages(RecipesActivity.this, mRecipeImages);
            }
        }).start();
    }

    /**
     * On click listener on recipes_activity layouts.
     */
    private View.OnClickListener mRecipeOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mIsShopping) {
                int index = mRecipesLayout.indexOfChild(v);
                // Select/deselect this recipe for shopping
                int notSelectedVisibility, selectedVisibility;
                if (mSelectedIndices.contains(index)) {
                    mSelectedIndices.remove((Integer)index);
                    notSelectedVisibility = View.VISIBLE;
                    selectedVisibility = View.GONE;
                }
                else {
                    mSelectedIndices.add(index);
                    notSelectedVisibility = View.GONE;
                    selectedVisibility = View.VISIBLE;
                }
                v.findViewById(R.id.not_checked_image).setVisibility(notSelectedVisibility);
                v.findViewById(R.id.checked_image).setVisibility(selectedVisibility);
                v.findViewById(R.id.first_line).setVisibility(notSelectedVisibility);
                v.findViewById(R.id.second_line).setVisibility(notSelectedVisibility);
                v.findViewById(R.id.people_shopping_line).setVisibility(selectedVisibility);
            }
            else {
                long recipeId = (long) v.getTag();
                // Increment the number of the recipe clicked to change the order
                mDao.incrementRecipeNumber(recipeId);
                // Start the recipe activity
                Intent intent = new Intent(getBaseContext(), RecipeActivity.class);
                intent.putExtra(DatabaseHandler.RECIPES_KEY, recipeId);
                startActivity(intent);
            }
        }
    };

    /**
     * On long click listener on recipes_activity layouts.
     */
    private View.OnLongClickListener mRecipeOnLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            if (mIsShopping)
                return false;

            final long recipeId = (long) v.getTag();
            // Set the view as selected
            v.setSelected(true);
            // Show a popup menu
            final PopupMenu popup = new PopupMenu(RecipesActivity.this, v);
            popup.inflate(R.menu.recipe_menu);
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
                            Utils.showConfirmDeleteDialog(RecipesActivity.this,
                                    R.string.delete_confirmation, new Runnable() {
                                @Override
                                public void run() {
                                    // Delete the recipe_activity image if exists
                                    Utils.deleteLocalFile(RecipesActivity.this,
                                            getExternalFilesDir(null), getImageNameFromId(recipeId));
                                    // Delete the recipe_activity in database
                                    mDao.deleteRecipe(recipeId);
                                    mDao.deleteUnusedIngredients();
                                    mRecipesLayout.removeView(v);
                                }
                            });
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
    public static void startImportingImages(Context context, LongSparseArray<ImageView> recipeImages) {
        for (int i = 0; i < recipeImages.size(); ++i) {
            long recipeId = recipeImages.keyAt(i);
            final Bitmap bitmap = Utils.loadLocalImage(context, context.getExternalFilesDir(null),
                    getImageNameFromId(recipeId));
            if (bitmap != null) {
                final ImageView imageView = recipeImages.get(recipeId);
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }

    //endregion

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

    //region SHOPPING

    private View.OnClickListener mOnShoppingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Hide the drop down menu
            if (mIsMenuDown)
                mOnDropDownClickListener.onClick(mTopRightButton);

            mIsShopping = !mIsShopping;
            mSelectedIndices.clear();
            // Show specific button to validate or cancel shopping.
            if (mIsShopping) {
                mAddRecipeButton.setVisibility(View.GONE);
                mValidateShoppingButton.setVisibility(View.VISIBLE);
                mTopRightButton.setImageDrawable(mCrossCloseDrawable);
                mTopRightButton.setOnClickListener(mOnShoppingClickListener);
            }
            else {
                mAddRecipeButton.setVisibility(View.VISIBLE);
                mValidateShoppingButton.setVisibility(View.GONE);
                mTopRightButton.setImageDrawable(mThreeDotsDrawable);
                mTopRightButton.setOnClickListener(mOnDropDownClickListener);
            }
            // Change the visual of layout
            int visibility = (mIsShopping ? View.VISIBLE : View.GONE);
            for (int i = 0; i < mRecipes.size(); ++i) {
                View recipeView = mRecipesLayout.getChildAt(i);
                recipeView.findViewById(R.id.not_checked_image).setVisibility(visibility);
                recipeView.findViewById(R.id.checked_image).setVisibility(View.GONE);
                recipeView.findViewById(R.id.people_shopping_line).setVisibility(View.GONE);
                recipeView.findViewById(R.id.first_line).setVisibility(View.VISIBLE);
                recipeView.findViewById(R.id.second_line).setVisibility(View.VISIBLE);
                ((TextView) recipeView.findViewById(R.id.people_edit))
                        .setText(String.valueOf(mRecipes.get(i).people));
            }
        }
    };

    private View.OnClickListener mOnValidateShoppingListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getBaseContext(), ShoppingActivity.class);
            long[] recipesId = new long[mSelectedIndices.size()];
            float[] recipesPeopleRatio = new float[mSelectedIndices.size()];
            for (int i = 0; i < mSelectedIndices.size(); ++i) {
                View recipeView = mRecipesLayout.getChildAt(mSelectedIndices.get(i));
                RecipesDAO.Recipe recipe = mRecipes.get(mSelectedIndices.get(i));
                recipesId[i] = recipe.id;
                int people = Utils.parseIntWithDefault(
                        ((EditText) recipeView.findViewById(R.id.people_edit)).getText().toString(), 0);
                recipesPeopleRatio[i] = people / (float)recipe.people;
            }
            intent.putExtra(DatabaseHandler.RECIPES_KEY, recipesId);
            intent.putExtra(DatabaseHandler.RECIPES_PEOPLE, recipesPeopleRatio);
            startActivity(intent);
        }
    };

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
