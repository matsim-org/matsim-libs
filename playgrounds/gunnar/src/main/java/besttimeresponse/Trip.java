package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Trip {

	// -------------------- CONSTANTS --------------------

	final Object origin;

	final Object destination;

	final int departureTime_s;

	final Object mode;

	final double originTimePressure;

	final double destinationTimePressure;

	final boolean originClosesBeforeDeparture;

	final boolean earlyDepartureFromOrigin;

	final boolean destinationOpensAfterArrival;

	final boolean lateArrivalToDestination;

	// -------------------- CONSTRUCTION --------------------

	public Trip(final Object origin, final Object destination, final int departureTime_s, final Object mode,
			final double originTimePressure, final double destinationTimePressure,
			final boolean originClosesBeforeDeparture, final boolean earlyDepartureFromOrigin,
			final boolean destinationOpensAfterArrival, final boolean lateArrivalToDestination) {
		this.origin = origin;
		this.destination = destination;
		this.departureTime_s = departureTime_s;
		this.mode = mode;
		this.originTimePressure = originTimePressure;
		this.destinationTimePressure = destinationTimePressure;
		this.originClosesBeforeDeparture = originClosesBeforeDeparture;
		this.earlyDepartureFromOrigin = earlyDepartureFromOrigin;
		this.destinationOpensAfterArrival = destinationOpensAfterArrival;
		this.lateArrivalToDestination = lateArrivalToDestination;
	}
}