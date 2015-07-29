/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.analysis.control.vis;

import java.awt.Color;

import org.matsim.contrib.evacuation.analysis.data.ColorationMode;

public class Coloration {

	public static Color getColor(double value, ColorationMode mode, float alpha) {
		Color color;
		int alphaInt = (int) (255 * alpha);

		// depending on the selected colorization, set red, green and blue values
		if (mode.equals(ColorationMode.GREEN_YELLOW_RED)) {
			int red, green, blue;

			if (value > .5) {
				red = 255;
				green = (int) (255 - 255 * (value - .5) * 2);
				blue = 0;
			} else {
				red = (int) (255 * value * 2);
				green = 255;
				blue = 0;

			}
			color = new Color(red, green, blue, alphaInt);
		} else if (mode.equals(ColorationMode.GREEN_RED)) {
			int red, green, blue;

			red = (int) (255 * value);
			green = (int) (255 - 255 * value);
			blue = 0;

			color = new Color(red, green, blue, alphaInt);
		} else
			color = new Color(0, 127, (int) (255 * value), alphaInt);

		return color;
	}

}
