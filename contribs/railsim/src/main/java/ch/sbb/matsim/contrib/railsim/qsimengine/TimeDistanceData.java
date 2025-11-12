package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Time-distance data for a specific transit line and route.
 */
final class TimeDistanceData {

	final Id<TransitLine> lineId;
	final Id<TransitRoute> routeId;

	/**
	 * Maps target arrival time to row.
	 */
	final List<Row> rows = new ArrayList<>();

	double[] distances;

	public TimeDistanceData(Id<TransitLine> lineId, Id<TransitRoute> routeId) {
		this.lineId = lineId;
		this.routeId = routeId;
	}

	void add(double targetArrivalTime, double cumulativeDistance, Id<Link> linkId, Id<TransitStopFacility> stopId) {
		rows.add(new Row(targetArrivalTime, cumulativeDistance, linkId, stopId));
	}

	void createIndex() {
		distances = rows.stream().mapToDouble(r -> r.distance).toArray();
	}

	/**
	 * Approximate the delay bases on distance and current timestamp.
	 *
	 * @return the delay in seconds, negative if ahead of schedule
	 */
	double calcDelay(double departureTime, double distance, double timestamp) {

		int idx = Arrays.binarySearch(distances, distance);

		int insertion = -idx - 1;

		// Check if distances are approximately equal
		if (insertion > 0 && FuzzyUtils.equals(distances[insertion - 1], distance)) {
			idx = insertion - 1;
		}
		else if (insertion >= 0 && insertion < distances.length && FuzzyUtils.equals(distances[insertion], distance)) {
			idx = insertion;
		}

		// The entry is contained in the array
		if (idx >= 0) {

			// Check multiple entries for the same distance
			double lower = departureTime + rows.get(idx).time;
			double upper = departureTime + rows.get(idx).time;

			if (idx - 1 >= 0 && FuzzyUtils.equals(distances[idx - 1], distance)) {
				lower = departureTime + rows.get(idx - 1).time;
			}

			if (idx + 1 < rows.size() && FuzzyUtils.equals(distances[idx + 1], distance)) {
				upper = departureTime + rows.get(idx + 1).time;
			}

			if (timestamp > upper)
				return timestamp - upper;

			if (timestamp < lower)
				return timestamp - lower;

			return 0;
		}

		// Handle case where distance is before all entries
		if (insertion == 0) {
			return timestamp - (departureTime + rows.getFirst().time);
		}

		// Handle case where distance is after all entries
		if (insertion >= rows.size()) {
			return timestamp - (departureTime + rows.getLast().time);
		}

		// Interpolate linearly between two entries
		Row before = rows.get(insertion - 1);
		Row after = rows.get(insertion);

		// Linear interpolation: expected_time = time1 + (time2 - time1) * (distance - distance1) / (distance2 - distance1)
		double distanceDiff = after.distance - before.distance;

		double timeDiff = after.time - before.time;
		double expectedTime = departureTime + before.time + timeDiff * (distance - before.distance) / distanceDiff;

		return timestamp - expectedTime;
	}

	record Row(double time, double distance, Id<Link> linkId, Id<TransitStopFacility> stopId) {
	}
}
