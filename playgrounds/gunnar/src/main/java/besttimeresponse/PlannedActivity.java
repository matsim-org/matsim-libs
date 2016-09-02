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
	 * Must be at least one second.
	 */
	final double desiredDur_s;

	/*
	 * Must be in [00:00:00, 24:00:00].
	 */
	final Double openingTime_s;
	final Double closingTime_s;

	/*
	 * Must be in [00:00:00, 24:00:00].
	 */
	final Double latestArrTime_s;
	final Double earliestDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	public PlannedActivity(final Object location, final Object departureMode, final double desiredDur_s,
			final Double openingTime_s, final Double closingTime_s, final Double latestArrTime_s,
			final Double earliestDptTime_s) {
		if (desiredDur_s < minActDur_s) {
			throw new RuntimeException(
					"Desired activity duration is " + desiredDur_s + "s but must be at least " + minActDur_s + "s.");
		}
		this.location = location;
		this.departureMode = departureMode;
		this.desiredDur_s = desiredDur_s;
		this.openingTime_s = openingTime_s;
		this.closingTime_s = closingTime_s;
		this.latestArrTime_s = latestArrTime_s;
		this.earliestDptTime_s = earliestDptTime_s;
	}

	// -------------------- GETTERS --------------------

	boolean isLateArrival(double time_s) {
		time_s = BestTimeResponseUtils.withinDayTime_s(time_s);
		return (this.latestArrTime_s != null) && (time_s > this.latestArrTime_s);
	}

	boolean isEarlyDeparture(double time_s) {
		time_s = BestTimeResponseUtils.withinDayTime_s(time_s);
		return (this.earliestDptTime_s != null) && (time_s < this.earliestDptTime_s);
	}

	boolean isClosed(double time_s) {
		time_s = BestTimeResponseUtils.withinDayTime_s(time_s);
		final boolean opensLater = (this.openingTime_s != null) && (time_s < this.openingTime_s);
		final boolean closesBefore = (this.closingTime_s != null) && (time_s > this.closingTime_s);
		return (opensLater || closesBefore);
	}
}
