/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import java.awt.Color;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class ValueColorizer {

	// -------------------- CONSTANTS --------------------

	private double[] values;

	private Color[] colors;

	// -------------------- CONSTRUCTION --------------------

	ValueColorizer(final String colordef) {
		try {
			final String[] colordefs = colordef.trim().split("\\s");
			this.values = new double[colordefs.length / 2];
			this.colors = new Color[colordefs.length / 2];
			for (int i = 0; i < colordefs.length / 2; i++) {
				this.colors[i] = (Color) Color.class.getField(colordefs[2 * i])
						.get(null);
				this.values[i] = Double.parseDouble(colordefs[1 + 2 * i]);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	private int upperIndex(final double val) {
		int result = 0;
		while (this.values[result] < val && result < this.values.length - 1) {
			result++;
		}
		return result;

	}

	private int bound(final double x) {
		return (int) Math.max(0, Math.min(Math.round(x), 255));
	}

	Color getColor(final double value) {
		final int u = upperIndex(value);
		if (u == 0) {
			return this.colors[0];
		} else {
			final double w = (value - this.values[u - 1])
					/ (this.values[u] - this.values[u - 1]);
			final int r = bound(w * this.colors[u].getRed() + (1 - w)
					* this.colors[u - 1].getRed());
			final int g = bound(w * this.colors[u].getGreen() + (1 - w)
					* this.colors[u - 1].getGreen());
			final int b = bound(w * this.colors[u].getBlue() + (1 - w)
					* this.colors[u - 1].getBlue());
			return (new Color(r, g, b));
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < this.values.length; i++) {
			result.append(this.colors[i].toString());
			result.append(" ");
			result.append(this.values[i]);
			result.append(" ");
		}
		return result.toString();
	}
}
