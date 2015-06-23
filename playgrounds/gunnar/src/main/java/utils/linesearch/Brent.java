package utils.linesearch;

import static utils.linesearch.LineSearchUtils.fabs;
import static utils.linesearch.LineSearchUtils.sign;

/**
 * 
 * Java implementation of Brent's Algorithm, Numerical Recipes in C, Chapter 10.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Brent {

	public static final int ITMAX = 100;

	public static final double CGOLD = 0.3819660;

	public static final double ZEPS = 1e-10;

	public static final double TOL = 3e-8;

	// TODO encapsulate >>>

	public double xmin;

	public double fmin;

	public Brent() {
	}

	public void run(final double ax, final double bx, final double cx,
			final OneDimensionalFunction f) {

		int iter;
		double a, b, d, etemp, fu, fv, fw, fx, p, q, r, tol1, tol2, u, v, w, x, xm;

		d = 0; // TODO

		double e = 0.0; // Distance moved on the step before the last.

		// a and b must be in ascending order, but input abscissas need not be:

		a = (ax < cx ? ax : cx);
		b = (ax > cx ? ax : cx);

		// Initializations.

		x = w = v = bx;
		fw = fv = fx = f.evaluate(x);

		// Main program loop.

		for (iter = 1; iter <= ITMAX; iter++) {

			xm = 0.5 * (a + b);
			tol2 = 2.0 * (tol1 = TOL * fabs(x) + ZEPS);

			// Test for being done:

			if (fabs(x - xm) <= (tol2 - 0.5 * (b - a))) {
				this.xmin = x;
				this.fmin = fx;
				return;
			}

			if (fabs(e) > tol1) {

				// Construct a trial parabolic fit.

				r = (x - w) * (fx - fv);
				q = (x - v) * (fx - fw);
				p = (x - v) * q - (x - w) * r;
				q = 2.0 * (q - r);
				if (q > 0.0) {
					p = -p;
				}
				q = fabs(q);
				etemp = e;
				e = d;

				// Determine the acceptability of the parabolic fit.

				if (fabs(p) >= fabs(0.5 * q * etemp) || p <= q * (a - x)
						|| p >= q * (b - x)) {

					// Take the golden section step into the larger of the two
					// segments.

					d = CGOLD * (e = (x >= xm ? a - x : b - x));

				} else {

					// Take the parabolic step.

					d = p / q;
					u = x + d;
					if (u - a < tol2 || b - u < tol2) {
						d = sign(tol1, xm - x);
					}

				}

				//

			} else {

				d = CGOLD * (e = (x >= xm ? a - x : b - x));

			}

			u = (fabs(d) >= tol1 ? x + d : x + sign(tol1, d));
			fu = f.evaluate(u);

			// Now decide what to do with our one function evaluation.

			if (fu <= fx) {

				if (u >= x) {
					a = x;
				} else {
					b = x;
				}

				v = w;
				w = x;
				x = u;

				fv = fw;
				fw = fx;
				fx = fu;

			} else {

				if (u < x) {
					a = u;
				} else {
					b = u;
				}

				if (fu < fw || w == x) {

					v = w;
					w = u;

					fv = fw;
					fw = fu;

				} else if (fu <= fv || v == x || v == w) {
					
					v = u;
					fv = fu;
				
				}
			}
		}

		System.err.println("too many iterations");
		this.xmin = x;
		this.fmin = fx;

	}
}
