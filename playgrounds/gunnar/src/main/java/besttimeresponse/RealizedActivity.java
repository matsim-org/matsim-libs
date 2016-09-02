package besttimeresponse;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RealizedActivity {

	// -------------------- CONSTANTS --------------------

	final PlannedActivity plannedActivity;

	final TripTime nextTripTravelTime;

	final double realizedArrTime_s;

	final double realizedDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity plannedActivity, final TripTime nextTripTimes,
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
		double firstDayDuration_s = MathHelpers.overlap(this.realizedArrTime_s, this.realizedDptTime_s,
				(this.plannedActivity.openingTime_s != null) ? this.plannedActivity.openingTime_s : 0.0,
				(this.plannedActivity.closingTime_s != null) ? this.plannedActivity.closingTime_s : Units.S_PER_D);
		final double secondDayDuration_s = MathHelpers.overlap(this.realizedArrTime_s, this.realizedDptTime_s,
				(this.plannedActivity.openingTime_s != null) ? Units.S_PER_D + this.plannedActivity.openingTime_s
						: Units.S_PER_D,
				(this.plannedActivity.closingTime_s != null) ? Units.S_PER_D + this.plannedActivity.closingTime_s
						: Units.S_PER_D + Units.S_PER_D);
		return firstDayDuration_s + secondDayDuration_s;
	}
}
