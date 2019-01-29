package org.matsim.contrib.analysis.vsp.qgis.utils;

import java.awt.*;

/**
 * @author gthunig on 15.06.2017.
 */
class DefaultColorRange {

    private Color[] colorMarks = new Color[9];

    DefaultColorRange() {
        initColorMarks();
    }

    private void initColorMarks() {
        colorMarks[0] = new Color(215,25,28,255);
        colorMarks[1] = new Color(234,99,62,255);
        colorMarks[2] = new Color(253,174,97,255);
        colorMarks[3] = new Color(254,214,144,255);
        colorMarks[4] = new Color(255,255,191,255);
        colorMarks[5] = new Color(213,238,177,255);
        colorMarks[6] = new Color(171,221,164,255);
        colorMarks[7] = new Color(107,176,175,255);
        colorMarks[8] = new Color(43,131,186,255);
    }

    Color getColor(double power) {
        double scaledPower = power * 8;
        if ((scaledPower == Math.floor(scaledPower)) && !Double.isInfinite(scaledPower))
            return colorMarks[(int)scaledPower];
        else {
            Color lowerColor = getLowerColor(scaledPower);
            Color upperColor = getUpperColor(scaledPower);
            double stepPower = scaledPower - (int)scaledPower;
            return getColorBetween(lowerColor, upperColor, stepPower);
        }
    }

    private Color getLowerColor(double power) {
        return colorMarks[(int)power];
    }

    private Color getUpperColor(double power) {
        return colorMarks[((int)power)+1];
    }

    private Color getColorBetween(Color lowerColor, Color upperColor, double power) {
        float[] lowerHSB = Color.RGBtoHSB(lowerColor.getRed(), lowerColor.getGreen(), lowerColor.getBlue(), null);
        float[] upperHSB = Color.RGBtoHSB(upperColor.getRed(), upperColor.getGreen(), upperColor.getBlue(), null);

        float[] newHSB = new float[3];
        for (int i = 0; i<newHSB.length; i++) {
            newHSB[i] = (float) (lowerHSB[i] - ((lowerHSB[i] - upperHSB[i]) * power));
        }
        return Color.getHSBColor(newHSB[0], newHSB[1], newHSB[2]);
    }
}
