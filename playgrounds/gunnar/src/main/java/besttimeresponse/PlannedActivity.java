package besttimeresponse;

import floetteroed.utilities.Time;
import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 */
public class PlannedActivity<L, M> {

	// -------------------- CONSTANTS --------------------

	// For numerical reasons, to avoid computing the log of a number near zero.
	static final double MINACTDUR_S = 1.0;

	final L location;

	final M departureMode;

	// Must be at least MINACTDUR_S.
	final double desiredDur_s;

	// TODO NEW
	// Must be at least MINACTDUR_S.
	final double minDur_s;

	// Must be in [00:00:00, 24:00:00].
	final Double openingTime_s;
	final Double closingTime_s;

	// Must be in [00:00:00, 24:00:00].
	final Double latestArrTime_s;
	final Double earliestDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	public PlannedActivity(final L location, final M departureMode, final double desiredDur_s, final double minDur_s,
			final Double openingTime_s, final Double closingTime_s, final Double latestArrTime_s,
			final Double earliestDptTime_s) {

		if (desiredDur_s < MINACTDUR_S) {
			throw new RuntimeException(
					"Desired activity duration is " + desiredDur_s + "s but must be at least " + MINACTDUR_S + "s.");
		}
		if (minDur_s < MINACTDUR_S) {
			throw new RuntimeException(
					"Minimal activity duration is " + minDur_s + "s but must be at least " + MINACTDUR_S + "s.");
		}
		if ((openingTime_s != null) && (closingTime_s != null) && (openingTime_s > closingTime_s)) {
			throw new RuntimeException("Opening time " + openingTime_s + "s is larger than closing time "
					+ closingTime_s + "s. This would only make sense for overnight activities, "
					+ "but these are must be always open.");
		}
		this.checkWithinDay(openingTime_s, "Opening time");
		this.checkWithinDay(closingTime_s, "Closing time");
		this.checkWithinDay(latestArrTime_s, "Latest arrival time");
		this.checkWithinDay(earliestDptTime_s, "Earliest departure time");

		this.location = location;
		this.departureMode = departureMode;
		this.desiredDur_s = desiredDur_s;
		this.minDur_s = minDur_s;
		this.openingTime_s = openingTime_s;
		this.closingTime_s = closingTime_s;
		this.latestArrTime_s = latestArrTime_s;
		this.earliestDptTime_s = earliestDptTime_s;
	}

	// -------------------- INTERNALS --------------------

	private void checkWithinDay(final Double time_s, final String name) {
		if ((time_s != null) && ((time_s < 0) || (time_s > Units.S_PER_D))) {
			throw new RuntimeException(name + " " + time_s + "s is not in interval [" + Time.strFromSec(0) + ", "
					+ Time.strFromSec((int) Units.S_PER_D) + "].");
		}
	}

	// -------------------- GETTERS --------------------

	boolean isLateArrival(final double time_s) {
		return (this.latestArrTime_s != null) && (time_s > this.latestArrTime_s);
	}

	boolean isEarlyDeparture(final double time_s) {
		return (this.earliestDptTime_s != null) && (time_s < this.earliestDptTime_s);
	}

	boolean isClosed(final double time_s) {
		// Recall: Overnight activities are always open.
		final boolean opensLater = (this.openingTime_s != null) && (time_s < this.openingTime_s);
		final boolean closesBefore = (this.closingTime_s != null) && (time_s > this.closingTime_s);
		return (opensLater || closesBefore);
	}
}
