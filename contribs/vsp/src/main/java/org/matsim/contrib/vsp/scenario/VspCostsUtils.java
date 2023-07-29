package org.matsim.contrib.vsp.scenario;

/**
 * Utility class for pricing calculations. Only applicable for models in Euro.
 */
public final class VspCostsUtils {

	private VspCostsUtils() {
	}

	/**
	 * Adjust base costs for inflation.
	 *
	 * @param costs      price in euro
	 * @param costYear   the year of the given costs
	 * @param targetYear the adjusted costs in the target year
	 */
	public static double adjustPriceForInflation(double costs, int costYear, int targetYear) {
		// TODO:
		return 0;
	}

	public static double betaPerforming(int targetYear) {
		return 0;
	}

	public static double dailyMonetaryConstantCar(int targetYear) {
		return 0;
	}

	public static double monetaryDistanceRateCar(int targetYear) {

		return 0;
	}

	public static double monetaryDistanceRateRide(int targetYear) {
		return 0;
	}

}
