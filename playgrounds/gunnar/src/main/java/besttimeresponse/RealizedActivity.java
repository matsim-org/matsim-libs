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

	final double realizedArrTime_s;

	final double realizedDptTime_s;

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
		this.realizedArrTime_s = realizedArrTime_s;
		this.realizedDptTime_s = realizedDptTime_s;

		this.isLateArrival = this.plannedActivity.isLateArrival(realizedArrTime_s);
		this.isEarlyDeparture = this.plannedActivity.isEarlyDeparture(realizedDptTime_s);

		if (plannedActivity.isOvernight) {

			// proper overnight activity
			this.isClosedAtArrival = false; // no overnight opening times
			this.isClosedAtDeparture = false; // no overnight closing times

			if (realizedArrTime_s < realizedDptTime_s) {
				// arrival is beyond midnight, i.e. just as departure in the following day
				this.effectiveDuration_s = max(0.0, realizedArrTime_s - realizedDptTime_s);
			} else {
				// arrival is before midnight, departure is after midnight the following day
				this.effectiveDuration_s = (Units.S_PER_D - realizedArrTime_s) + realizedDptTime_s;
			}

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
