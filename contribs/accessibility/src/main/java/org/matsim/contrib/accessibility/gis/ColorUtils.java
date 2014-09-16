/* *********************************************************************** *
 * project: org.matsim.*
 * ColorUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.accessibility.gis;

import java.awt.Color;

/**
 * Utility class for colors.
 * 
 * @author illenberger
 * 
 */
public final class ColorUtils {

	/**
	 * Returns a color from the spectrum green-yellow-red-pink-blue with
	 * increasing values for <tt>value</tt> where <tt>value</tt> < 0 returns
	 * black and <tt>value</tt> = 0 return white.
	 * 
	 * @param value
	 *            a value 0 < x < 1, or < 0 for black and = 0 for white.
	 * @return a color for <tt>value</tt>.
	 */
	public static Color getGRBColor(double value) {
		if (value < 0)
			return Color.BLACK;
		else if (value == 0)
			return Color.WHITE;
		else {
			float red = 0;
			float green = 0;
			float blue = 0;

			int segment = (int) Math.ceil(value * 4);
			float val = (float) ((value - (segment*.25-0.25)) * 4);
			switch (segment) {
			case 1:
				red = val;
				green = 1;
				blue = 0;
				break;
			case 2:
				red = 1;
				green = 1 - val;
				blue = 0;
				break;
			case 3:
				red = 1;
				green = 0;
				blue = val;
				break;
			default:
				red = 1 - val;
				green = 0;
				blue = 1;
				break;
			}

			return new Color(red, green, blue);
		}
	}
}
