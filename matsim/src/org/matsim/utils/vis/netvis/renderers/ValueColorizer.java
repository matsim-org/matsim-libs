/* *********************************************************************** *
 * project: org.matsim.*
 * ValueColorizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.netvis.renderers;

import java.awt.Color;

/**
 * @author gunnar
 */
public class ValueColorizer {

	// MEMBER VARIABLES

	private double[] values;

	private Color[] colors;

	// CONSTRUCTION

	public ValueColorizer() {
		this(new double[] { -1.0, 0.0, 0.1, 0.3, 1.0 }, new Color[] { Color.BLUE, Color.WHITE, Color.GREEN, Color.YELLOW,
				Color.RED });
	}

	public ValueColorizer(double[] values, Color[] colors) {
		this.values = values;
		this.colors = colors;
	}

	// COLOR GENERATION

	private int upperIndex(double val) {
		int result = 0;
		while (values[result] < val && result < values.length - 1)
			result++;
		return result;

	}

	private int bound(double x) {
		return (int) Math.max(0, Math.min(Math.round(x), 255));
	}

	public Color getColor(double value) {
		final int u = upperIndex(value);
		if (u == 0) {
			return colors[0];
		}
		final double w = (value - values[u - 1]) / (values[u] - values[u - 1]);
		final int r = bound(w * colors[u].getRed() + (1 - w) * colors[u - 1].getRed());
		final int g = bound(w * colors[u].getGreen() + (1 - w) * colors[u - 1].getGreen());
		final int b = bound(w * colors[u].getBlue() + (1 - w) * colors[u - 1].getBlue());

		return new Color(r, g, b);
	}

}