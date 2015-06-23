package utils.linesearch;

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
