package buthod.tony.appManager.recipes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;
import buthod.tony.appManager.database.AccountDAO;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Created by Tony on 22/09/2018.
 */

public class RecipesActivity extends RootActivity {

    private RecipesDAO mDao = null;

    private LinearLayout mRecipesList = null;
    private ImageButton mBackButton = null;
    private Button mAddRecipeButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipes);

        mDao = new RecipesDAO(getBaseContext());
        mDao.open();
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mRecipesList = (LinearLayout) findViewById(R.id.recipes_list);
        mAddRecipeButton = (Button) findViewById(R.id.add_recipe_button);

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
        ArrayList<RecipesDAO.Recipe> recipes = mDao.getRecipes();
        for (int i = 0; i < recipes.size(); ++i) {
            final RecipesDAO.Recipe recipe = recipes.get(i);

            TextView textView = new TextView(getBaseContext());
            textView.setText(String.format(Locale.getDefault(),
                    "%d : %s (%d)\nNote : %d   Difficulté : %d\nTemps de préparation : %d",
                    recipe.id, recipe.name, recipe.type, recipe.grade, recipe.difficulty, recipe.time));
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getBaseContext(), RecipeActivity.class);
                    intent.putExtra(DatabaseHandler.RECIPES_KEY, recipe.id);
                    startActivity(intent);
                }
            });
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Set the view as selected
                    v.setSelected(true);

                    // Show a popup menu
                    final PopupMenu popup = new PopupMenu(RecipesActivity.this, v);
                    popup.inflate(R.menu.transaction_menu);
                    // Registering clicks on the popup menu
                    final TextView currentView = (TextView) v;
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            popup.dismiss();
                            switch(item.getItemId()) {
                                case R.id.modify:
                                    Intent intent = new Intent(getBaseContext(), AddEditRecipeActivity.class);
                                    intent.putExtra(DatabaseHandler.RECIPES_KEY, recipe.id);
                                    startActivity(intent);
                                    break;
                                case R.id.delete:
                                    mDao.deleteRecipe(recipe);
                                    mRecipesList.removeView(currentView);
                                    break;
                            }
                            return true;
                        }
                    });
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            // Deselect the view
                            currentView.setSelected(false);
                        }
                    });
                    popup.show();
                    return false;
                }
            });
            mRecipesList.addView(textView);
        }
        mRecipesList.invalidate();
    }

    @Override
    protected void onDestroy() {
        mDao.close();
        super.onDestroy();
    }
}
