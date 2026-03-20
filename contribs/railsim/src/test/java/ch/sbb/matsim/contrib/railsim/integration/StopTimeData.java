package ch.sbb.matsim.contrib.railsim.integration;

import java.util.Map;

/**
 * Data class to hold arrival and departure times for a train at a specific facility.
 */
final class StopTimeData {

	private final String facilityId;

	double arrivalTime = -1.0;
	double departureTime = -1.0;
	int stopCount = 0;

	/**
	 * Previous stop time data, if any. This can be used to chain stop times together.
	 */
	private final StopTimeData prev;

	public StopTimeData(String facilityId, Map.Entry<String, StopTimeData> prev) {
		this.facilityId = facilityId;
		this.prev = prev != null ? prev.getValue() : null;
	}

	/**
	 * @return the facility id
	 */
	public String getFacilityId() {
		return facilityId;
	}

	/**
	 * @return true if both arrival and departure times are set
	 */
	boolean isComplete() {
		return arrivalTime >= 0 && departureTime >= 0;
	}

	/**
	 * @return true if the train has arrived at the facility.
	 */
	boolean hasArrived() {
		return arrivalTime >= 0;
	}

	/**
	 * @return the stop duration in seconds, or -1 if incomplete
	 */
	double getStopDuration() {
		return isComplete() ? departureTime - arrivalTime : -1.0;
	}

	/**
	 * Calculates the travel time from the previous stop to this one.
	 */
	double getTravelTime() {
		return hasArrived() && prev != null ? arrivalTime - prev.departureTime : -1.0;
	}

	public int getStopCount() {
		return stopCount;
	}

	@Override
	public String toString() {
		return "StopTimeData{" +
			"arrivalTime=" + arrivalTime +
			", departureTime=" + departureTime +
			", travelTime=" + getTravelTime() +
			", stopDuration=" + getStopDuration() +
			", stopCount=" + stopCount +
			'}';
	}
}
