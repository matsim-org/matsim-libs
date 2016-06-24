package besttimeresponse;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class InterpolatedTravelTimes {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	// -------------------- CONSTRUCTION --------------------

	public InterpolatedTravelTimes(final TimeDiscretization timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	// -------------------- GETTERS --------------------

	public TimeDiscretization getTimeDiscretization() {
		return this.timeDiscretization;
	}

	public double get_dTravelTime_dDptTime(final Trip trip) {
		return this.get_dTravelTime_dDptTime(trip.origin, trip.destination, trip.departureTime_s, trip.destination);
	}

	public double getTravelTimeOffset_s(final Trip trip) {
		return this.getTravelTimeOffset_s(trip.origin, trip.destination, trip.departureTime_s, trip.destination);
	}

	public double getArrivalTime_s(final Trip trip) {
		return (trip.departureTime_s + this.getTravelTime_s(trip));
	}

	public double getTravelTime_s(final Trip trip) {
		return this.getTravelTime_s(trip.origin, trip.destination, trip.departureTime_s, trip.mode);
	}

	public double getTravelTime_s(final Object origin, final Object destination, final double departureTime_s,
			final Object mode) {
		return this.get_dTravelTime_dDptTime(origin, destination, departureTime_s, mode) * departureTime_s
				+ this.getTravelTimeOffset_s(origin, destination, departureTime_s, mode);
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract double get_dTravelTime_dDptTime(Object origin, Object destination, double dptTime_s, Object mode);

	public abstract double getTravelTimeOffset_s(Object origin, Object destination, double dptTime_s, Object mode);

}
