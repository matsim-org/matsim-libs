package ch.sbb.matsim.contrib.railsim.qsimengine;

/**
 * Util class for fuzzy comparisons.
 */
class FuzzyUtils {

	private FuzzyUtils() {
	}

	private final static double EPSILON = 1E-5;


	/**
	 * Returns true if two doubles are approximately equal.
	 */
	public static boolean equals(double a, double b) {
		return a == b || Math.abs(a - b) < EPSILON;
	}


	/**
	 * Returns true if the first double is approximately greater than the second.
	 */
	public static boolean greaterEqualThan(double a, double b) {
		return equals(a, b) || a - b > EPSILON;
	}

	/**
	 * Returns true if the first double is certainly greater than the second.
	 */
	public static boolean greaterThan(double a, double b) {
		return a - b > EPSILON;
	}


	/**
	 * Returns true if the first double is approximately less than the second.
	 */
	public static boolean lessEqualThan(double a, double b) {
		return equals(a, b) || b - a > EPSILON;
	}

	/**
	 * Returns true if the first double is approximately less than the second.
	 */
	public static boolean lessThan(double a, double b) {
		return b - a > EPSILON;
	}

}
