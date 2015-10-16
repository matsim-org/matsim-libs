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

import static floetteroed.utilities.math.linesearch.LineSearchUtils.fabs;
import static floetteroed.utilities.math.linesearch.LineSearchUtils.fmax;
import static floetteroed.utilities.math.linesearch.LineSearchUtils.sign;

/**
 * 
 * Java implementation of "Routine for Initially Bracketing a Minimum",
 * Numerical Recipes in C, Chapter 10.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class InitialBracket {

	private static final double GOLD = 1.618034;

	private static final double TINY = 1e-20;

	private static final double GLIMIT = 100.0;

	private double ax;

	private double bx;

	private double cx;

	private double fa;

	private double fb;

	private double fc;

	public InitialBracket() {
	}

	/**
	 * Given a function func, and given distinct initial points ax and bx, this
	 * routine searches in the downhill direction (defined by the function as
	 * evaluated at the initial points) and computes the new points ax, bx, cx
	 * that bracket a minimum of the function. Also computed are the function
	 * values at the three points.
	 * 
	 */
	public void run(final double axStart, final double bxStart,
			final double faStart, final double fbStart,
			final OneDimensionalFunction func) {

		double u, fu;

		this.ax = axStart;
		this.bx = bxStart;

		this.fa = faStart; // func.evaluate(this.ax);
		this.fb = fbStart; // func.evaluate(this.bx);

		// Ensure that going from a to b is a downhill direction.

		if (this.fb > this.fa) {

			double dum = this.ax;
			this.ax = this.bx;
			this.bx = dum;

			dum = this.fb;
			this.fb = this.fa;
			this.fa = dum;
		}

		// First guess for c.

		this.cx = this.bx + GOLD * (this.bx - this.ax);
		this.fc = func.evaluate(this.cx);

		// Keep repeating this until a bracket is found.

		while (this.fb > this.fc) {

			// Compute u by parabolic extrapolation from a, b, c.

			final double r = (this.bx - this.ax) * (this.fb - this.fc);
			final double q = (this.bx - this.cx) * (this.fb - this.fa);
			u = this.bx - ((this.bx - this.cx) * q - (this.bx - this.ax) * r)
					/ (2.0 * sign(fmax(fabs(q - r), TINY), q - r));

			// We won't go farther than this.

			final double ulim = this.bx + GLIMIT * (this.cx - this.bx);

			// Test various possibilities:

			if ((this.bx - u) * (u - this.cx) > 0.0) {

				// Parabolic u is between b and c.

				fu = func.evaluate(u);

				if (fu < this.fc) {

					// Got a minimum between b and c.

					this.ax = bx;
					this.bx = u;

					this.fa = fb;
					this.fb = fu;

					return;

				} else if (fu > this.fb) {

					// Got a minimum between a and u.

					this.cx = u;
					this.fc = fu;

					return;

				}

				// Parabolic fit was no use. Use default magnification.

				u = this.cx + GOLD * (this.cx - this.bx);
				fu = func.evaluate(u);

			} else if ((this.cx - u) * (u - ulim) > 0.0) {

				// Parabolic fit is between c and its allowed limit.

				fu = func.evaluate(u);

				if (fu < this.fc) {

					this.bx = this.cx;
					this.cx = u;
					u = this.cx + GOLD * (this.cx - this.bx);

					this.fb = this.fc;
					this.fc = fu;
					fu = func.evaluate(u);

				}

			} else if ((u - ulim) * (ulim - this.cx) >= 0.0) {

				// Limit parabolic u to maximum allowed value.

				u = ulim;
				fu = func.evaluate(u);

			} else {

				// Reject parabolic u, use default maginification.

				u = this.cx + GOLD * (this.cx - this.bx);
				fu = func.evaluate(u);

			}

			this.ax = this.bx;
			this.bx = this.cx;
			this.cx = u;

			this.fa = this.fb;
			this.fb = this.fc;
			this.fc = fu;
		}
	}

	// -------------------- GETTERS --------------------

	public double ax() {
		return this.ax;
	}

	public double bx() {
		return this.bx;
	}

	public double cx() {
		return this.cx;
	}

	public double fa() {
		return this.fa;
	}

	public double fb() {
		return this.fb;
	}

	public double fc() {
		return this.fc;
	}
}
