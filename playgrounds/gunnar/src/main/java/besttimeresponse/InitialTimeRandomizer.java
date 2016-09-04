package besttimeresponse;

import java.util.Arrays;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.Histogram;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 */
class InitialTimeRandomizer<L, M> {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	private final TripTravelTimes<L, M> times;

	// -------------------- CONSTRUCTION --------------------

	InitialTimeRandomizer(final TimeDiscretization timeDiscr, final TripTravelTimes<L, M> times) {
		this.times = times;
		this.timeDiscr = timeDiscr;
	}

	// -------------------- IMPLEMENTATION --------------------

	double[] newFeasibleRandomInitialPoints(final int size, final L lastTripOrigin, final L lastTripDestination,
			final M lastTripMode) {

		double[] initialDptTimes_s = new double[size];
		double arrivalBinEndTime_s;

		do {
			for (int i = 0; i < size; i++) {
				initialDptTimes_s[i] = MatsimRandom.getRandom().nextDouble() * Units.S_PER_D;
			}
			Arrays.sort(initialDptTimes_s);
			arrivalBinEndTime_s = arrivalBinEndTime_s(lastTripOrigin, lastTripDestination,
					initialDptTimes_s[initialDptTimes_s.length - 1], lastTripMode);
		} while (arrivalBinEndTime_s > Units.S_PER_D);
		return initialDptTimes_s;
	}

//	double[] newFeasibleInitialPoints(final double[] initialPoints, final L lastTripOrigin, final L lastTripDestination,
//			final M lastTripMode) {
//		double[] result = Arrays.copyOf(initialPoints, initialPoints.length);
//		boolean feasible = true;
//		do {
//
//			final double arrivalBinEndTime_s = arrivalBinEndTime_s(lastTripOrigin, lastTripDestination,
//					result[result.length - 1], lastTripMode);
//			feasible = (arrivalBinEndTime_s <= Units.S_PER_D);
//			if (!feasible) {
//				final double fact = 1.0 + (Units.S_PER_D - arrivalBinEndTime_s) / result[result.length - 1];
//				for (int i = 0; i < result.length; i++) {
//					result[i] *= fact;
//				}
//			}
//		} while (!feasible);
//		return result;
//	}

	double arrivalBinEndTime_s(final L origin, final L destination, final double dptTime_s, final M mode) {
		final double arrivalTime_s = dptTime_s + this.times.getTravelTime_s(origin, destination, dptTime_s, mode);
		final int arrivalBin = this.timeDiscr.getBin(arrivalTime_s);
		return this.timeDiscr.getBinEndTime_s(arrivalBin);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final TripTravelTimes tripTimes = new TripTravelTimes() {
			@Override
			public double getTravelTime_s(Object origin, Object destination, double dptTime_s, Object mode) {
				return 12 * 3600;
			}
		};

		final InitialTimeRandomizer itr = new InitialTimeRandomizer(timeDiscr, tripTimes);
		double[] initial = new double[] { 6 * 3600, 15 * 3600, 30 * 3600 };
//		double[] result = itr.newFeasibleInitialPoints(initial, null, null, null);
		System.out.println(Arrays.toString(initial));
//		System.out.println(Arrays.toString(result));
		System.out.println();

		final Random rnd = new Random();
		final Histogram hist = new Histogram(0, 1 * 3600, 2 * 3600, 3 * 3600, 4 * 3600, 5 * 3600, 6 * 3600, 7 * 3600,
				8 * 3600, 9 * 3600, 10 * 3600, 11 * 3600, 12 * 3600, 13 * 3600, 14 * 3600, 15 * 3600, 16 * 3600,
				17 * 3600, 18 * 3600, 19 * 3600, 20 * 3600, 21 * 3600, 22 * 3600, 23 * 3600, 24 * 3600);
		for (int r = 0; r < 24 * 1000; r++) {
			double[] dptTimes = itr.newFeasibleRandomInitialPoints(rnd.nextInt(10) + 1, null, null, null);
			for (double time : dptTimes) {
				hist.add(time);
			}
		}
		System.out.println(hist.toString());

	}
}
