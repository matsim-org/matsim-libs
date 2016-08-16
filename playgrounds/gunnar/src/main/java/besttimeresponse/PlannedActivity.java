package besttimeresponse;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PlannedActivity {

	// -------------------- CONSTANTS --------------------

	public final Object location;

	public final Object departureMode;

	public final double desiredDuration_s;

	public final double openingTime_s;

	public final double closingTime_s;

	public final double latestArrivalTime_s;

	public final double earliestDepartureTime_s;

	// -------------------- CONSTRUCTION --------------------

	private PlannedActivity(final Object location, final Object departureMode, final double desiredDuration_s,
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

	public static PlannedActivity newOvernightActivity(final Object location, final Object departureMode,
			final double desiredDuration_s) {
		return new PlannedActivity(location, departureMode, desiredDuration_s, NEGATIVE_INFINITY, POSITIVE_INFINITY,
				POSITIVE_INFINITY, NEGATIVE_INFINITY);
	}

	public static PlannedActivity newWithinDayActivity(final Object location, final Object departureMode,
			final double desiredDuration_s, final double openingTime_s, final double closingTime_s,
			final double latestArrivalTime_s, final double earliestDepartureTime_s) {
		return new PlannedActivity(location, departureMode, desiredDuration_s, openingTime_s, closingTime_s,
				latestArrivalTime_s, earliestDepartureTime_s);
	}
}
