package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RealizedActivity {

	// -------------------- CONSTANTS --------------------

	final PlannedActivity plannedActivity;

	final TripTimes nextTripTravelTime;

	final boolean isLateArrival;

	final boolean isEarlyDeparture;

	final boolean isClosedAtArrival;

	final boolean isClosedAtDeparture;

	final double effectiveDuration_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity plannedActivity, final TripTimes nextTripTimes,
			final double realizedArrivalTime_s, final double realizedDepartureTime_s) {

		this.plannedActivity = plannedActivity;
		this.nextTripTravelTime = nextTripTimes;

		this.isLateArrival = (realizedArrivalTime_s > this.plannedActivity.latestArrivalTime_s);
		this.isEarlyDeparture = (realizedDepartureTime_s < this.plannedActivity.earliestDepartureTime_s);

		this.isClosedAtArrival = ((realizedArrivalTime_s < this.plannedActivity.openingTime_s)
				|| (realizedArrivalTime_s > this.plannedActivity.closingTime_s));
		this.isClosedAtDeparture = ((realizedDepartureTime_s < this.plannedActivity.openingTime_s)
				|| (realizedDepartureTime_s > this.plannedActivity.closingTime_s));
		
		if (realizedArrivalTime_s <= realizedDepartureTime_s) {
			// within-day activity
			final double effectiveStartTime_s = max(this.plannedActivity.openingTime_s, realizedArrivalTime_s);
			final double effectiveEndTime_s = min(this.plannedActivity.closingTime_s, realizedDepartureTime_s);
			this.effectiveDuration_s = max(0.0, effectiveEndTime_s - effectiveStartTime_s);
		} else {
			// overnight activity (compute durations before and after midnight separately)
			this.effectiveDuration_s = (Units.S_PER_D - realizedArrivalTime_s) + realizedDepartureTime_s;
		}
	}

	// -------------------- GETTERS --------------------

	public double getEffectiveTimePressure() {
		return this.effectiveDuration_s / this.plannedActivity.desiredDuration_s;
	}
}
