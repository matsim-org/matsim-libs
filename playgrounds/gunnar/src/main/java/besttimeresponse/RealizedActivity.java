package besttimeresponse;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;

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
class RealizedActivity<L, M> {

	// -------------------- CONSTANTS --------------------

	final PlannedActivity<L, M> plannedActivity;

	final TripTime nextTripTravelTime;

	final double realizedArrTime_s;

	final double realizedDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity<L, M> plannedActivity, final TripTime nextTripTimes,
			final double realizedArrTime_s, final double realizedDptTime_s) {
		this.plannedActivity = plannedActivity;
		this.nextTripTravelTime = nextTripTimes;
		this.realizedArrTime_s = realizedArrTime_s;
		this.realizedDptTime_s = realizedDptTime_s;
	}

	// -------------------- GETTERS --------------------

	boolean isLateArrival() {
		return this.plannedActivity.isLateArrival(this.realizedArrTime_s);
	}

	boolean isEarlyDeparture() {
		return this.plannedActivity.isEarlyDeparture(this.realizedDptTime_s);
	}

	boolean isClosedAtArrival() {
		return this.plannedActivity.isClosed(this.realizedArrTime_s);
	}

	boolean isClosedAtDeparture() {
		return this.plannedActivity.isClosed(this.realizedDptTime_s);
	}

	double effectiveDuration_s() {
		final double result;
		if (this.realizedArrTime_s > this.realizedDptTime_s) {
			// An overnight activity, always open.
			result = this.realizedDptTime_s + (Units.S_PER_D - this.realizedArrTime_s);
		} else {
			// A within-day activity, possibly closed.
			result = MathHelpers.overlap(this.realizedArrTime_s, this.realizedDptTime_s,
					(this.plannedActivity.openingTime_s != null) ? this.plannedActivity.openingTime_s
							: NEGATIVE_INFINITY,
					(this.plannedActivity.closingTime_s != null) ? this.plannedActivity.closingTime_s
							: POSITIVE_INFINITY);
		}
		return result;
	}
}
