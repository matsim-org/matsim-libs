package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RealizedActivity {

	final PlannedActivity plannedActivity;

	final double realizedArrivalTime_s;

	final double realizedDepartureTime_s;

	final InterpolatedTravelTimes.Entry nextTripTravelTime;

	RealizedActivity(final PlannedActivity plannedActivity, final double realizedArrivalTime_s,
			final double realizedDepartureTime_s, final InterpolatedTravelTimes.Entry nextTripTravelTime) {
		this.plannedActivity = plannedActivity;
		this.realizedArrivalTime_s = realizedArrivalTime_s;
		this.realizedDepartureTime_s = realizedDepartureTime_s;
		this.nextTripTravelTime = nextTripTravelTime;
	}

	boolean getLateArrival() {
		return (this.realizedArrivalTime_s > this.plannedActivity.latestArrivalTime_s);
	}

	boolean getEarlyDeparture() {
		return (this.realizedDepartureTime_s < this.plannedActivity.earliestDepartureTime_s);
	}

	boolean getClosedAtArrival() {
		return (this.realizedArrivalTime_s < this.plannedActivity.openingTime_s);
	}

	boolean getClosedAtDeparture() {
		return (this.realizedDepartureTime_s > this.plannedActivity.closingTime_s);
	}

	double getEffectiveDuration_s() {
		return Math.min(this.plannedActivity.closingTime_s, this.realizedDepartureTime_s)
				- Math.max(this.plannedActivity.openingTime_s, this.realizedArrivalTime_s);
	}

	double getEffectiveTimePressure() {
		return this.getEffectiveDuration_s() / this.plannedActivity.desiredDuration_s;
	}

}
