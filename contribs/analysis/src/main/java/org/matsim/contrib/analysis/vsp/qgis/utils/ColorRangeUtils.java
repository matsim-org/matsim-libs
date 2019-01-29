package org.matsim.contrib.analysis.vsp.qgis.utils;

import org.apache.log4j.Logger;

import java.awt.*;

/**
 * @author gthunig on 22.05.2017.
 */
public class ColorRangeUtils {
    public static final Logger log = Logger.getLogger(ColorRangeUtils.class);

    public enum ColorRange {
        DEFAULT_RED_TO_BLUE, RED_TO_GREEN, GREEN_TO_RED, GREEN_TO_BLUE, BLUE_TO_GREEN, BLUE_TO_RED, RED_TO_BLUE,
        BLACK_TO_WHITE, WHITE_TO_BLACK, DENSITY
    }

    public static Color getColor(ColorRange colorRange, double power) {
        switch (colorRange) {
            case DEFAULT_RED_TO_BLUE:
                return getDefaultColorFromRedToBlue(power);

            case RED_TO_GREEN:
                return getColorFromRedToGreen(power);

            case GREEN_TO_BLUE:
                return getColorFromGreenToBlue(power);

            case BLUE_TO_RED:
                return getColorFromBlueToRed(power);

            case GREEN_TO_RED:
                return getColorFromRedToGreen(invert(power));

            case BLUE_TO_GREEN:
                return getColorFromGreenToBlue(invert(power));

            case RED_TO_BLUE:
                return getColorFromBlueToRed(invert(power));

            case BLACK_TO_WHITE:
                return getColorFromBlackToWhite(power);

            case WHITE_TO_BLACK:
                return getColorFromBlackToWhite(invert(power));

            case DENSITY:
                return getColorInDensityRange();

            default:
                log.error("No such color range");
                return new Color(0,0,0);
        }
    }



    private static Color getDefaultColorFromRedToBlue(double power) {
        DefaultColorRange colorRange = new DefaultColorRange();
        return colorRange.getColor(power);
    }

    private static Color getColorFromRedToGreen(double power) {
        double H = power * 0.35; // Hue
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        return Color.getHSBColor((float)H, (float)S, (float)B);
    }

    private static Color getColorFromGreenToBlue(double power) {
        double H = 0.35 + power * 0.33; // Hue
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        return Color.getHSBColor((float)H, (float)S, (float)B);
    }

    private static Color getColorFromBlueToRed(double power) {
        double H = 0.68 + power * 0.32; // Hue
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        return Color.getHSBColor((float)H, (float)S, (float)B);
    }

    private static Color getColorFromBlackToWhite(double power) {
        int rgbValue = (int)(255 * power);
        return new Color(rgbValue);
    }

    private static Color getColorInDensityRange() {
        return new Color(255, 255, 255, 255);
    }

    private static double invert(double power) {
        return 1-power;
    }
}
