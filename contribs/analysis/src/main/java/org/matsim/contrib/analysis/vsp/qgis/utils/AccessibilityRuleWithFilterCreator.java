package org.matsim.contrib.analysis.vsp.qgis.utils;

import org.matsim.contrib.analysis.vsp.qgis.Rule;

/**
 * @author gthunig on 22.05.2017.
 */
public class AccessibilityRuleWithFilterCreator {

    public static Rule createRule(String nameOfAccessibilityLayer, double lowerBound, double upperBound,
                                  String nameOfDensityLayer, double minimumDensity,
                                  int symbol, String label) {
        String filter = "&quot;" + nameOfAccessibilityLayer + "&quot; > " + lowerBound
                + " AND &quot;" + nameOfAccessibilityLayer + "&quot; &lt;= " + upperBound
                + " AND &quot;" + nameOfDensityLayer + "&quot; > " + minimumDensity;
        return new Rule(filter, symbol, label);
    }

    public static Rule createRuleWithoutLowerBound(String nameOfAccessibilityLayer, double upperBound,
                                  String nameOfDensityLayer, double minimumDensity,
                                  int symbol, String label) {
        String filter = "&quot;" + nameOfAccessibilityLayer + "&quot; &lt;= " + upperBound
                + " AND &quot;" + nameOfDensityLayer + "&quot; > " + minimumDensity;
        return new Rule(filter, symbol, label);
    }

    public static Rule createRuleWithoutUpperBound(String nameOfAccessibilityLayer, double lowerBound,
                                  String nameOfDensityLayer, double minimumDensity,
                                  int symbol, String label) {
        String filter = "&quot;" + nameOfAccessibilityLayer + "&quot; > " + lowerBound
                + " AND &quot;" + nameOfDensityLayer + "&quot; > " + minimumDensity;
        return new Rule(filter, symbol, label);
    }
}
