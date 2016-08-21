package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PlannedActivity {

	// -------------------- CONSTANTS --------------------

	/*
	 * For numerical reasons.
	 */
	static final double minActDur_s = 1.0;

	final Object location;

	final Object departureMode;

	/*
	 * Must be in [00:00:01, 24:00:00].
	 */
	final double desiredDur_s;

	/*
	 * Only for within-day activities, otherwise null. Must be in [00:00:00,
	 * 24:00:00]. Opening time must be strictly smaller than closing time.
	 */
	final Double openingTime_s;
	final Double closingTime_s;

	/*
	 * Must be in [00:00:00, 24:00:00]. For within-day activities, latest
	 * arrival time must be smaller than or equal to earliest departure time.
	 * For overnight activities, latest arrival time must be strictly larger
	 * than earliest departure time.
	 */
	final Double latestArrTime_s;
	final Double earliestDptTime_s;

	final boolean isOvernight;

	// -------------------- PRIVATE CONSTRUCTOR --------------------

	private PlannedActivity(final Object location, final Object departureMode, final double desiredDur_s,
			final Double openingTime_s, final Double closingTime_s, final Double latestArrTime_s,
			final Double earliestDptTime_s, final boolean isOvernight) {
		if (desiredDur_s < minActDur_s) {
			throw new RuntimeException(
					"Desired activity duration is " + desiredDur_s + "s but must be at least " + minActDur_s + "s.");
		}
		if (isOvernight) {
			if (openingTime_s != null) {
				throw new RuntimeException("Overnight activities must not have opening times.");
			}
			if (closingTime_s != null) {
				throw new RuntimeException("Overnight activities must not have closing times.");
			}
		}
		if ((openingTime_s != null) && (closingTime_s != null) && (openingTime_s >= closingTime_s)) {
			throw new RuntimeException("Opening time is " + openingTime_s + "s and closing time is " + closingTime_s
					+ "s but opening time must be strictly smaller than closing time.");
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

	// -------------------- FACTORIES --------------------

	public static PlannedActivity newWithinDayActivity(final Object location, final Object departureMode,
			final double desiredDuration_s, final Double openingTime_s, final Double closingTime_s,
			final Double latestArrTime_s, final Double earliestDptTime_s) {
		if ((latestArrTime_s != null) && (earliestDptTime_s != null) && (latestArrTime_s > earliestDptTime_s)) {
			throw new RuntimeException("Latest arrival time is " + latestArrTime_s + "s and earliest departure time is "
					+ earliestDptTime_s
					+ "s but for a _within-day_ activity, the latest arrival time must smaller than or equal to "
					+ "the earliest departure time.");
		}
		return new PlannedActivity(location, departureMode, desiredDuration_s, openingTime_s, closingTime_s,
				latestArrTime_s, earliestDptTime_s, false);
	}

	public static PlannedActivity newOvernightActivity(final Object location, final Object departureMode,
			final double desiredDuration_s, final Double latestArrTime_s, final Double earliestDptTime_s) {
		if ((latestArrTime_s != null) && (earliestDptTime_s != null) && (latestArrTime_s <= earliestDptTime_s)) {
			throw new RuntimeException("Latest arrival time is " + latestArrTime_s + "s and earliest departure time is "
					+ earliestDptTime_s
					+ "s but for an _overnight_ activity, the latest arrival time must be strictly larger than "
					+ "the earliest departure time.");
		}
		return new PlannedActivity(location, departureMode, desiredDuration_s, null, null, latestArrTime_s,
				earliestDptTime_s, true);
	}

	// -------------------- GETTERS --------------------

	/**
	 * @param time_s
	 *            must be in [00:00:00, 24:00:00], even for overnight activities
	 */
	boolean isLateArrival(final double time_s) {
		return (this.latestArrTime_s != null) && (time_s > this.latestArrTime_s);
	}

	/**
	 * @param time_s
	 *            must be in [00:00:00, 24:00:00], even for overnight activities
	 */
	boolean isEarlyDeparture(final double time_s) {
		return (this.earliestDptTime_s != null) && (time_s < this.earliestDptTime_s);
	}

	/**
	 * @param time_s
	 *            must be in [00:00:00, 24:00:00], even for overnight activities
	 *            (which are by definition never closed)
	 */
	boolean isClosed(final double time_s) {
		final boolean opensLater = (this.openingTime_s != null) && (time_s < this.openingTime_s);
		final boolean closesBefore = (this.closingTime_s != null) && (time_s > this.closingTime_s);
		return (opensLater || closesBefore);
	}
}
