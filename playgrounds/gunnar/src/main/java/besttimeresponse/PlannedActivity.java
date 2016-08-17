package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PlannedActivity {

	// -------------------- CONSTANTS --------------------

	public final Object location;

	public final Object departureMode;

	public final double desiredDur_s;

	public final Double openingTime_s;

	public final Double closingTime_s;

	public final Double latestArrTime_s;

	public final Double earliestDptTime_s;

	public final boolean isOvernight;

	// -------------------- CONSTRUCTION/FACTORIES --------------------

	private PlannedActivity(final Object location, final Object departureMode, final double desiredDur_s,
			final Double openingTime_s, final Double closingTime_s, final Double latestArrTime_s,
			final Double earliestDptTime_s, final boolean isOvernight) {
		if (desiredDur_s <= 0) {
			throw new RuntimeException(
					"Desired activity duration is " + desiredDur_s + "s but must be strictly positive.");
		}
		if ((openingTime_s != null) && (closingTime_s != null) && (openingTime_s >= closingTime_s)) {
			throw new RuntimeException("Opening time is " + openingTime_s + "s and closing time is " + closingTime_s
					+ "s but closing time must be strictly greater than opening time.");
		}
		this.location = location;
		this.departureMode = departureMode;
		this.desiredDur_s = desiredDur_s;
		this.openingTime_s = openingTime_s;
		this.closingTime_s = closingTime_s;
		this.latestArrTime_s = latestArrTime_s;
		this.earliestDptTime_s = earliestDptTime_s;
		this.isOvernight = isOvernight;
	}

	public static PlannedActivity newWithinDayActivity(final Object location, final Object departureMode,
			final double desiredDuration_s, final Double openingTime_s, final Double closingTime_s,
			final Double latestArrTime_s, final Double earliestDptTime_s) {
		if ((latestArrTime_s != null) && (earliestDptTime_s != null) && (latestArrTime_s > earliestDptTime_s)) {
			throw new RuntimeException("Latest arrival time is " + latestArrTime_s + "s and earliest departure time is "
					+ earliestDptTime_s
					+ "s but for a _within-day_ activity, the latest arrival time must not be _larger_ than "
					+ "the earliest departure time.");
		}
		return new PlannedActivity(location, departureMode, desiredDuration_s, openingTime_s, closingTime_s,
				latestArrTime_s, earliestDptTime_s, false);
	}

	public static PlannedActivity newOvernightActivity(final Object location, final Object departureMode,
			final double desiredDuration_s, final Double latestArrTime_s, final Double earliestDptTime_s) {
		if ((latestArrTime_s != null) && (earliestDptTime_s != null) && (latestArrTime_s < earliestDptTime_s)) {
			throw new RuntimeException("Latest arrival time is " + latestArrTime_s + "s and earliest departure time is "
					+ earliestDptTime_s
					+ "s but for an _overnight_ activity, the latest arrival time must not be _smaller_ than "
					+ "the earliest departure time.");
		}
		return new PlannedActivity(location, departureMode, desiredDuration_s, null, null, latestArrTime_s,
				earliestDptTime_s, true);
	}

	public static PlannedActivity newOvernightActivity(final Object location, final Object departureMode,
			final double desiredDuration_s) {
		return newOvernightActivity(location, departureMode, desiredDuration_s, null, null);
	}

	// -------------------- GETTERS --------------------

	boolean isLateArrival(final double time_s) {
		return (this.latestArrTime_s != null) && (time_s > this.latestArrTime_s);
	}

	boolean isEarlyDeparture(final double time_s) {
		return (this.earliestDptTime_s != null) && (time_s < this.earliestDptTime_s);
	}

	boolean isClosed(final double time_s) {
		final boolean opensLater = (this.openingTime_s != null) && (time_s < this.openingTime_s);
		final boolean closesBefore = (this.closingTime_s != null) && (time_s > this.closingTime_s);
		return (opensLater || closesBefore);
	}
}
