package besttimeresponse;

import static java.lang.Math.max;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RealizedActivity {

	// -------------------- CONSTANTS --------------------

	final PlannedActivity plannedActivity;

	final TripTime nextTripTravelTime;

	final boolean isLateArrival;

	final boolean isEarlyDeparture;

	final boolean isClosedAtArrival;

	final boolean isClosedAtDeparture;

	final double effectiveDuration_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity plannedActivity, final TripTime nextTripTimes,
			final double realizedArrTime_s, final double realizedDptTime_s) {

		this.plannedActivity = plannedActivity;
		this.nextTripTravelTime = nextTripTimes;

		this.isLateArrival = this.plannedActivity.isLateArrival(realizedArrTime_s);
		this.isEarlyDeparture = this.plannedActivity.isEarlyDeparture(realizedDptTime_s);

		// TOOD CONTINUE HERE

		if (plannedActivity.isOvernight) {

			if (realizedArrTime_s <= realizedDptTime_s) {
				throw new RuntimeException("Realized arrival time is " + realizedArrTime_s
						+ "s and realized departure time is " + realizedDptTime_s
						+ "s but in an _overnight_ activity, the arrival time must be strictly greater than "
						+ "the departure time.");
			}

			// proper overnight activity
			this.isClosedAtArrival = false; // no opening times for
			this.isClosedAtDeparture = false; // overnight activity
			this.effectiveDuration_s = (Units.S_PER_D - realizedArrTime_s) + realizedDptTime_s;

		} else { // !plannedActivity.isOvernight

			if (realizedArrTime_s > realizedDptTime_s) {
				throw new RuntimeException("Realized arrival time is " + realizedArrTime_s
						+ "s and realized departure time is " + realizedDptTime_s
						+ "s but in a _within-day_ activity, the arrival time must not be greater than "
						+ "the departure time.");
			}

			// proper within-day activity
			this.isClosedAtArrival = this.plannedActivity.isClosed(realizedArrTime_s);
			this.isClosedAtDeparture = this.plannedActivity.isClosed(realizedDptTime_s);

			final double effectiveStartTime_s = (this.isClosedAtArrival ? this.plannedActivity.openingTime_s
					: realizedArrTime_s);
			final double effectiveEndTime_s = (this.isClosedAtDeparture ? this.plannedActivity.closingTime_s
					: realizedDptTime_s);
			this.effectiveDuration_s = max(0.0, effectiveEndTime_s - effectiveStartTime_s);
		}
	}

	// -------------------- GETTERS --------------------

	public double getDesiredOverEffectiveDuration() {
		return this.plannedActivity.desiredDur_s / this.effectiveDuration_s;
	}
}
