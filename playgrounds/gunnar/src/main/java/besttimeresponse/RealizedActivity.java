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

}
