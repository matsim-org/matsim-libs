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
package playground.johannes.socialnetworks.graph.social.util;

import java.awt.Color;

/**
 * @author illenberger
 *
 */
public class ColorUtils {

	public static Color getHeatmapColor(double value) {
		float red = 1;
		float green = 1;
		float blue = 1;
		
		if(value < 0) {
			red = 0;
			green = 0;
			blue = 0;
		} else if(value > 0 && value <= 0.25) {
			red = (float) (value/0.25);
			green = 1;
			blue = 0;
		} else if (value > 0.25 && value <= 0.5) {
			red = 1;
			green = (float) (1 - (value-0.25)/0.25);
			blue = 0;
		} else if (value > 0.5 && value <= 0.75) {
			red = 1;
			green = 0;
			blue = (float) ((value - 0.5)/0.25);
		} else if (value > 0.75 && value <= 1) {
			red = 0;
			green = 0;
			blue = 1;
		}
		
		return new Color(red, green, blue);
	}
}
