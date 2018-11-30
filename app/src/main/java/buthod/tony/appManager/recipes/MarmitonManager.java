package buthod.tony.appManager.recipes;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import buthod.tony.appManager.Utils;
import buthod.tony.appManager.database.DatabaseHandler;
import buthod.tony.appManager.database.RecipesDAO;

/**
 * Class used to parse data from Marmiton web page to get recipe's data.
 */
public class MarmitonManager {

    private static RecipesDAO.Recipe mRecipe = null;
    private static String mImageUrl = null;

    public static RecipesDAO.Recipe getParsedRecipe() {
        return mRecipe;
    }

    public static String getParsedImageUrl() {
        return mImageUrl;
    }

    public static void parseWebPageContent(String content) {
        mRecipe = null;
        mImageUrl = null;
        Pattern pattern = Pattern.compile(
                ".*<script type=\"application/ld\\+json\">\\s*" +
                        "//<!\\[CDATA\\[\\s*" +
                        "(\\{.*\"@type\":\"Recipe\".*\\})\\s*" +
                        "//\\]\\]>\\s*" +
                        "</script>.*"
        );
        Matcher matcher = pattern.matcher(content);
        RecipesDAO.Recipe recipe = new RecipesDAO.Recipe();
        if (matcher.matches() && matcher.groupCount() > 0) {
            // A match was found, we can start parsing the match and extract recipe information
            try {
                JSONObject recipeObject = new JSONObject(matcher.group(1));
                if (!recipeObject.isNull("name"))
                    recipe.name = Utils.truncate(recipeObject.getString("name"),
                            DatabaseHandler.RecipeNameLength);
                // Image's URL
                if (!recipeObject.isNull("image"))
                    mImageUrl = recipeObject.getString("image");
                if (!recipeObject.isNull("prepTime")) {
                    String time = recipeObject.getString("prepTime");
                    Pattern p = Pattern.compile("PT((?:[0-9]+H)?)((?:[0-9]+M)?)");
                    Matcher m = p.matcher(time);
                    if (m.matches()) {
                        String hour = m.group(1);
                        recipe.time = 0;
                        if (!hour.isEmpty())
                            recipe.time += 60 * Utils.parseIntWithDefault(
                                    hour.substring(0, hour.length()-1), 0);
                        String min = m.group(2);
                        if (!min.isEmpty())
                            recipe.time += Utils.parseIntWithDefault(min.substring(0, min.length()-1), 0);
                    }
                }
                if (!recipeObject.isNull("recipeYield")) {
                    String yield = recipeObject.getString("recipeYield");
                    Pattern p = Pattern.compile("([0-9]+) personnes");
                    Matcher m = p.matcher(yield);
                    if (m.matches())
                        recipe.people = Utils.parseIntWithDefault(m.group(1), -1);
                }
                if (!recipeObject.isNull("aggregateRating")) {
                    JSONObject aggregateObject = recipeObject.getJSONObject("aggregateRating");
                    if (!aggregateObject.isNull("ratingValue"))
                        recipe.grade = (int) Math.round(aggregateObject.getDouble("ratingValue"));
                }
                // Ingredients
                if (!recipeObject.isNull("recipeIngredient")) {
                    JSONArray ingredients = recipeObject.getJSONArray("recipeIngredient");
                    for (int i = 0; i < ingredients.length(); ++i) {
                        String ingredientString = ingredients.getString(i);
                        RecipesDAO.Ingredient ingredient = parseStringToIngredient(ingredientString);
                        recipe.ingredients.add(ingredient);
                    }
                }
                // Steps
                if (!recipeObject.isNull("recipeInstructions")) {
                    JSONArray steps = recipeObject.getJSONArray("recipeInstructions");
                    for (int i = 0; i < steps.length(); ++i) {
                        JSONObject stepObject = steps.getJSONObject(i);
                        if (!stepObject.isNull("text")) {
                            String text = stepObject.getString("text");
                            while (text != null && !text.isEmpty()) {
                                String description;
                                if (text.length() > DatabaseHandler.StepDescriptionLength) {
                                    String[] split = text.split("\\.\\s", 2);
                                    description = split[0] + ".";
                                    text = (split.length > 1 ? split[1] : null);
                                }
                                else {
                                    description = text;
                                    text = null;
                                }
                                RecipesDAO.Step step = new RecipesDAO.Step();
                                step.number = recipe.steps.size();
                                step.description = Utils.truncate(description,
                                        DatabaseHandler.StepDescriptionLength);
                                recipe.steps.add(step);
                            }
                        }
                    }
                }
                mRecipe = recipe;
            }
            catch (JSONException e) {
                // Match does not have a JSON format
                Log.d("Error", "Error parsing the recipe from internet : " + e.getMessage());
            }
        }
    }

    /**
     * Parse an ingredient string from Marmiton to ingredient in the application.
     */
    private static RecipesDAO.Ingredient parseStringToIngredient(String ingredientString) {
        RecipesDAO.Ingredient ingredient = new RecipesDAO.Ingredient();
        Pattern p = Pattern.compile(
                "(?:([0-9]+(?:\\.[0-9]+)?)\\s+)?" +
                "(?:(g|kg|l|cl|cuill\\u00e8re \\u00e0 caf\\u00e9|cuill\\u00e8re \\u00e0 soupe)\\s+)?" +
                "([^0-9]*)"
        );
        Matcher m = p.matcher(ingredientString);
        Log.d("Debug", "Result for : " + ingredientString + " -> " + m.matches() + "/" + m.groupCount());
        if (m.matches()) {
            String quantity = m.group(1);
            if (quantity != null && !quantity.isEmpty()) {
                ingredient.quantity = Utils.parseFloatWithDefault(quantity, 0);
            }
            String unit = m.group(2);
            if (unit == null)
                ingredient.idUnit = 0;
            else if (unit.equals("g"))
                ingredient.idUnit = 1;
            else if (unit.equals("kg"))
                ingredient.idUnit = 2;
            else if (unit.equals("l"))
                ingredient.idUnit = 3;
            else if (unit.equals("cl"))
                ingredient.idUnit = 4;
            else if (unit.equals("cuill\u00e8re \u00e0 caf\u00e9"))
                ingredient.idUnit = 5;
            else if (unit.equals("cuill\u00e8re \u00e0 soupe")) {
                ingredient.idUnit = 5;
                ingredient.quantity *= 2;
            }
            else
                ingredient.idUnit = 0;

            String name = m.group(3);
            if (name != null && !name.isEmpty())
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            ingredient.name = Utils.truncate(name, DatabaseHandler.IngredientNameLength);
        }
        return ingredient;
    }
}
