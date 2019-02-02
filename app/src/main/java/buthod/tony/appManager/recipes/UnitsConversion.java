package buthod.tony.appManager.recipes;

import android.util.Log;

import java.util.List;

import buthod.tony.appManager.database.RecipesDAO;

/**
 * Class used to convert a unit into another one.
 * Link between units can be of several type :
 *    - The link is independent of the ingredient : it is hard coded here.
 *      Example : link between kg and g.
 *    - The link depends on the ingredient : the user has to specify it.
 *      Example : link between g and cl for flavour.
 */
public class UnitsConversion {

    public static int g = 1, kg = 2, L = 3, cL = 4;

    /**
     * Convert a unit into another one.
     * @return The conversion factor between units, or 0 if no conversion found.
     */
    public static float convert(RecipesDAO dao, int unitFrom, int unitTo, long ingredientId) {
        if (isSameMeasurementType(unitFrom, unitTo)) {
            return convertSameMeasurementType(unitFrom, unitTo);
        }
        else {
            return convertUsingDatabase(dao, unitFrom, unitTo, ingredientId);
        }
    }

    public static float convert(List<RecipesDAO.Conversion> conversions, int unitFrom, int unitTo) {
        if (isSameMeasurementType(unitFrom, unitTo)) {
            return convertSameMeasurementType(unitFrom, unitTo);
        }
        else {
            return convertUsingDatabase(conversions, unitFrom, unitTo);
        }
    }

    /**
     * Check if 2 units have the same measurement type.
     * For example, it returns true between g and kg.
     */
    public static boolean isSameMeasurementType(int unitFrom, int unitTo) {
        return unitFrom == unitTo || (unitFrom == g && unitTo == kg) || (unitFrom == kg && unitTo == g)
                || (unitFrom == L && unitTo == cL) || (unitFrom == cL && unitTo == L);
    }

    /**
     * Convert one unit to another one of the same measurement type.
     */
    public static float convertSameMeasurementType(int unitFrom, int unitTo) {
        if (unitFrom == unitTo)
            return 1;
        else if (unitFrom == g && unitTo == kg)
            return 0.001f;
        else if (unitFrom == kg && unitTo == g)
            return 1000;
        else if (unitFrom == cL && unitTo == L)
            return 0.01f;
        else if (unitFrom == L && unitTo == cL)
            return 100;
        else
            return 0;
    }

    public static float convertUsingDatabase(RecipesDAO dao, int unitFrom, int unitTo, long ingredientId) {
        return convertUsingDatabase(dao.getConversions(ingredientId), unitFrom, unitTo);
    }

    public static float convertUsingDatabase(List<RecipesDAO.Conversion> conversions, int unitFrom, int unitTo) {
        for (int i = 0; i < conversions.size(); ++i) {
            RecipesDAO.Conversion c = conversions.get(i);
            if (isSameMeasurementType(c.unitFrom, unitFrom) && isSameMeasurementType(c.unitTo, unitTo)) {
                return convertSameMeasurementType(unitFrom, c.unitFrom) * c.factor *
                        convertSameMeasurementType(c.unitTo, unitTo);
            }
            else if (isSameMeasurementType(c.unitTo, unitFrom) && isSameMeasurementType(c.unitFrom, unitTo)) {
                return convertSameMeasurementType(unitFrom, c.unitTo)  / c.factor *
                        convertSameMeasurementType(c.unitFrom, unitTo);
            }
        }
        return 0;
    }
}
