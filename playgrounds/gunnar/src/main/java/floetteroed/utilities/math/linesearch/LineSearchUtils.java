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
package floetteroed.utilities.math.linesearch;

/**
 * Some translations of functions used in "Numerical Recipes in C".
 * 
 * @author Gunnar Flötteröd
 *
 */
class LineSearchUtils {

	private LineSearchUtils() {
	}

	static double sign(final double a, final double b) {
		return (b >= 0) ? Math.abs(a) : -Math.abs(a);
	}

	static double fabs(final double a) {
		return Math.abs(a);
	}

	static double fmax(final double a, final double b) {
		return Math.max(a, b);
	}

}
