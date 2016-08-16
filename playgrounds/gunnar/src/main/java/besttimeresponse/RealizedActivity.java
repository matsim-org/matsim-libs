package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RealizedActivity {

	// -------------------- CONSTANTS --------------------

	public final PlannedActivity plannedActivity;

	public final double realizedArrivalTime_s;

	public final double realizedDepartureTime_s;

	public final InterpolatedTripTravelTime nextTripTravelTime;

	// -------------------- CONSTRUCTION --------------------

	public RealizedActivity(final PlannedActivity plannedActivity, final double realizedArrivalTime_s,
			final double realizedDepartureTime_s, final InterpolatedTripTravelTime nextTripTravelTime) {
		this.plannedActivity = plannedActivity;
		this.realizedArrivalTime_s = realizedArrivalTime_s;
		this.realizedDepartureTime_s = realizedDepartureTime_s;
		this.nextTripTravelTime = nextTripTravelTime;
	}

	// -------------------- GETTERS --------------------

	public boolean isLateArrival() {
		return (this.realizedArrivalTime_s > this.plannedActivity.latestArrivalTime_s);
	}

	public boolean isEarlyDeparture() {
		return (this.realizedDepartureTime_s < this.plannedActivity.earliestDepartureTime_s);
	}

	public boolean isClosedAtArrival() {
		return ((this.realizedArrivalTime_s < this.plannedActivity.openingTime_s)
				|| (this.realizedArrivalTime_s > this.plannedActivity.closingTime_s));
	}

	public boolean isClosedAtDeparture() {
		return ((this.realizedDepartureTime_s < this.plannedActivity.openingTime_s)
				|| (this.realizedDepartureTime_s > this.plannedActivity.closingTime_s));
	}

	public double getEffectiveDuration_s() {
		final double effectiveStartTime_s = max(this.plannedActivity.openingTime_s, this.realizedArrivalTime_s);
		final double effectiveEndTime_s = min(this.plannedActivity.closingTime_s, this.realizedDepartureTime_s);
		return Math.max(0.0, effectiveEndTime_s - effectiveStartTime_s);
	}

	public double getEffectiveTimePressure() {
		return this.getEffectiveDuration_s() / this.plannedActivity.desiredDuration_s;
	}
}
