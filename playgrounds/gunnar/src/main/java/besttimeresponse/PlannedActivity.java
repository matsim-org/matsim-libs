package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PlannedActivity {

	final Object location;

	final Object departureMode;

	final double desiredDuration_s;

	final double openingTime_s;

	final double closingTime_s;

	final double latestArrivalTime_s;

	final double earliestDepartureTime_s;

	public PlannedActivity(final Object location, final Object departureMode, final double desiredDuration_s,
			final double openingTime_s, final double closingTime_s, final double latestArrivalTime_s,
			final double earliestDepartureTime_s) {
		this.location = location;
		this.departureMode = departureMode;
		this.desiredDuration_s = desiredDuration_s;
		this.openingTime_s = openingTime_s;
		this.closingTime_s = closingTime_s;
		this.latestArrivalTime_s = latestArrivalTime_s;
		this.earliestDepartureTime_s = earliestDepartureTime_s;
	}
}
