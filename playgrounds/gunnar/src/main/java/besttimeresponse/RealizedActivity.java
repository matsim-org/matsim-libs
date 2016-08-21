package besttimeresponse;

import static java.lang.Math.max;

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

	/*
	 * Must be within [00:00:00, 24:00:00], even for overnight activities.
	 */
	final double realizedArrTime_s;
	final double realizedDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity plannedActivity, final TripTime nextTripTimes,
			final double realizedArrTime_s, final double realizedDptTime_s) {
		if (!plannedActivity.isOvernight && (realizedArrTime_s > realizedDptTime_s)) {
			throw new RuntimeException("The realized arrival time is " + realizedArrTime_s
					+ "s and the realized departure time is " + realizedDptTime_s
					+ "s, but for a _within-day_ activity, the arrival time must be smaller than "
					+ "or equal to the departure time.");
		}
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
		if (this.plannedActivity.isOvernight) {
			// an overnight activity
			if (this.realizedArrTime_s < this.realizedDptTime_s) {
				// arrival is after midnight, i.e. in the same day as departure
				return max(0.0, this.realizedDptTime_s - this.realizedArrTime_s);
			} else {
				// arrival is before midnight, i.e. the day before departure
				return (Units.S_PER_D - this.realizedArrTime_s) + this.realizedDptTime_s;
			}
		} else {
			// a within-day activity
			return MathHelpers.overlap(this.realizedArrTime_s, this.realizedDptTime_s,
					(this.plannedActivity.openingTime_s != null) ? this.plannedActivity.openingTime_s : 0.0,
					(this.plannedActivity.closingTime_s != null) ? this.plannedActivity.closingTime_s : Units.S_PER_D);
		}
	}

//	double getDesiredOverEffectiveDuration() {
//		return this.plannedActivity.desiredDur_s / this.effectiveDuration_s();
//	}
}
