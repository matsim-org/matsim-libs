package ch.sbb.matsim.contrib.railsim.integration;

import org.assertj.core.api.AbstractAssert;
import org.matsim.core.utils.misc.Time;


/**
 * Assertions for stop time data.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class StopTimeAssert extends AbstractAssert<StopTimeAssert, StopTimeData> {

	private StopTimeAssert(StopTimeData stopTimeData) {
		super(stopTimeData, StopTimeAssert.class);
	}

	/**
	 * Creates a new StopTimeAssert for the given stop time data.
	 * @param actual the stop time data to assert on
	 * @return a new StopTimeAssert instance
	 */
	public static StopTimeAssert assertThat(StopTimeData actual) {
		return new StopTimeAssert(actual);
	}

	/**
	 * Asserts that the arrival time equals the expected time.
	 * @param expectedTime time in seconds
	 * @return this assert instance
	 */
	public StopTimeAssert hasArrivalTime(double expectedTime) {
		isNotNull();
		if (actual.arrivalTime != expectedTime) {
			failWithMessage("Expected arrival time to be <%s> but was <%s>", expectedTime, actual.arrivalTime);
		}
		return this;
	}

	/**
	 * Asserts that the arrival time equals the expected time.
	 * @param expectedTime time string (e.g., "8:00", "08:30:15")
	 * @return this assert instance
	 */
	public StopTimeAssert hasArrivalTime(String expectedTime) {
		return hasArrivalTime(Time.parseTime(expectedTime));
	}

	/**
	 * Asserts that the departure time equals the expected time.
	 * @param expectedTime time in seconds
	 * @return this assert instance
	 */
	public StopTimeAssert hasDepartureTime(double expectedTime) {
		isNotNull();
		if (actual.departureTime != expectedTime) {
			failWithMessage("Expected departure time to be <%s> but was <%s> (%s)",
				expectedTime, actual.departureTime, Time.writeTime(actual.departureTime));
		}
		return this;
	}

	/**
	 * Asserts that the departure time equals the expected time.
	 * @param expectedTime time string (e.g., "8:00", "08:30:15")
	 * @return this assert instance
	 */
	public StopTimeAssert hasDepartureTime(String expectedTime) {
		return hasDepartureTime(Time.parseTime(expectedTime));
	}

	/**
	 * Asserts that the stop duration equals the expected duration.
	 * @param expectedDuration duration in seconds
	 * @return this assert instance
	 */
	public StopTimeAssert hasStopDuration(double expectedDuration) {
		isNotNull();
		double actualDuration = actual.getStopDuration();
		if (actualDuration != expectedDuration) {
			failWithMessage("Expected stop duration to be <%s> but was <%s>", expectedDuration, actualDuration);
		}
		return this;
	}

	/**
	 * Asserts that the stop duration equals the expected duration.
	 * @param expectedDuration duration string (e.g., "00:05", "00:30:15")
	 * @return this assert instance
	 */
	public StopTimeAssert hasStopDuration(String expectedDuration) {
		return hasStopDuration(Time.parseTime(expectedDuration));
	}

	/**
	 * Asserts that the travel time equals the expected time.
	 * @param expectedTime travel time in seconds
	 * @return this assert instance
	 */
	public StopTimeAssert hasTravelTime(double expectedTime) {
		isNotNull();
		double actualTravelTime = actual.getTravelTime();
		if (actualTravelTime != expectedTime) {
			failWithMessage("Expected travel time to be <%s> but was <%s>", expectedTime, actualTravelTime);
		}
		return this;
	}

	/**
	 * Asserts that the travel time equals the expected time.
	 * @param expectedTime travel time string (e.g., "00:15", "01:30:45")
	 * @return this assert instance
	 */
	public StopTimeAssert hasTravelTime(String expectedTime) {
		return hasTravelTime(Time.parseTime(expectedTime));
	}

	/**
	 * Asserts that the train has arrived at the facility.
	 * @return this assert instance
	 */
	public StopTimeAssert hasArrived() {
		isNotNull();
		if (!actual.hasArrived()) {
			failWithMessage("Expected train to have arrived but it has not");
		}
		return this;
	}

	/**
	 * Asserts that the train has not arrived at the facility.
	 * @return this assert instance
	 */
	public StopTimeAssert hasNotArrived() {
		isNotNull();
		if (actual.hasArrived()) {
			failWithMessage("Expected train to have not arrived but it has");
		}
		return this;
	}

	/**
	 * Asserts that the stop time data is complete (both arrival and departure times are set).
	 * @return this assert instance
	 */
	public StopTimeAssert isComplete() {
		isNotNull();
		if (!actual.isComplete()) {
			failWithMessage("Expected stop time data to be complete but it is not");
		}
		return this;
	}

	/**
	 * Asserts that the stop time data is incomplete (either arrival or departure time is not set).
	 * @return this assert instance
	 */
	public StopTimeAssert isIncomplete() {
		isNotNull();
		if (actual.isComplete()) {
			failWithMessage("Expected stop time data to be incomplete but it is complete");
		}
		return this;
	}

}

