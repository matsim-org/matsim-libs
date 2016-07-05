package besttimeresponse;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class InterpolatedTravelTimes {

	public class Entry {

		public double dTravelTime_dDptTime;

		public double travelTimeOffset_s;

		public double minDptTime_s;

		public double maxDptTime_s;

		private Entry(final double dTravelTime_dDptTime, final double travelTimeOffset_s, final double minDptTime_s,
				final double maxDptTime_s) {
			this.dTravelTime_dDptTime = dTravelTime_dDptTime;
			this.travelTimeOffset_s = travelTimeOffset_s;
			this.minDptTime_s = minDptTime_s;
			this.maxDptTime_s = maxDptTime_s;
		}

		public double getTravelTime_s(final double dptTime_s) {
			return this.dTravelTime_dDptTime * dptTime_s + travelTimeOffset_s;
		}

	}

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

	// public double get_dTravelTime_dDptTime(final Trip trip) {
	// return this.get_dTravelTime_dDptTime(trip.origin, trip.destination,
	// trip.departureTime_s, trip.destination);
	// }
	//
	// public double getTravelTimeOffset_s(final Trip trip) {
	// return this.getTravelTimeOffset_s(trip.origin, trip.destination,
	// trip.departureTime_s, trip.destination);
	// }
	//
	// public double getArrivalTime_s(final Trip trip) {
	// return (trip.departureTime_s + this.getTravelTime_s(trip));
	// }
	//
	// public double getTravelTime_s(final Trip trip) {
	// return this.getTravelTime_s(trip.origin, trip.destination,
	// trip.departureTime_s, trip.mode);
	// }
	//

	public double getTravelTime_s(final PlannedActivity originAct, final PlannedActivity destinationAct,
			final double departureTime_s) {
		return this.getEntry(originAct, destinationAct, departureTime_s).getTravelTime_s(departureTime_s);
	}

	public Entry getEntry(final PlannedActivity originAct, final PlannedActivity destinationAct,
			final double departureTime_s) {
		final Object origin = originAct.location;
		final Object destination = destinationAct.location;
		final Object mode = originAct.departureMode;
		return this.getEntry(origin, destination, departureTime_s, mode);
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract Entry getEntry(Object origin, Object destination, double dptTime_s, Object mode);

}
