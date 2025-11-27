package ch.sbb.matsim.contrib.railsim.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * Assertions for maps of stop times organized by train and station.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class SimulationResultAssert extends AbstractAssert<SimulationResultAssert, SimulationResult> {

	private SimulationResultAssert(SimulationResult actual) {
		super(actual, SimulationResultAssert.class);
	}

	/**
	 * Creates a new StopTimeMapAssert for the given stop time map.
	 *
	 * @param actual the stop time map to assert on
	 * @return a new StopTimeMapAssert instance
	 */
	public static SimulationResultAssert assertThat(SimulationResult actual) {
		return new SimulationResultAssert(actual);
	}

	/**
	 * Asserts that a specific train exists in the map.
	 *
	 * @param trainId the train ID to check
	 * @return this assert instance
	 */
	private SimulationResultAssert containsTrain(String trainId) {
		isNotNull();
		if (!actual.stopTimes.containsKey(trainId)) {
			failWithMessage("Expected train <%s> to be present but it was not found", trainId);
		}
		return this;
	}

	/**
	 * Asserts that a specific train has a stop at a specific station.
	 *
	 * @param trainId   the train ID
	 * @param stationId the station ID
	 * @return this assert instance
	 */
	public SimulationResultAssert trainHasStopAt(String trainId, String stationId) {
		isNotNull();
		containsTrain(trainId);
		SequencedMap<String, StopTimeData> trainStops = actual.stopTimes.get(trainId);
		if (!trainStops.containsKey(stationId)) {
			failWithMessage("Expected train <%s> to have a stop at station <%s> but it was not found", trainId, stationId);
		}
		return this;
	}

	/**
	 * Asserts that a specific train has a stop at a specific station.
	 *
	 * @param trainId   the train ID
	 * @param stationId the station ID
	 * @return this assert instance
	 */
	public SimulationResultAssert trainHasStopAt(String trainId, String stationId, int expectedStopCount) {
		isNotNull();
		containsTrain(trainId);
		trainHasStopAt(trainId, stationId);
		int actualCount = actual.stopTimes.get(trainId).get(stationId).getStopCount();
		if (actualCount != expectedStopCount) {
			failWithMessage("Expected train <%s> to have <%d> stops at station <%s> but found <%d>",
				trainId, expectedStopCount, stationId, actualCount);
		}
		return this;
	}

	/**
	 * Asserts that a specific train does not have a stop at a specific station.
	 *
	 * @param trainId   the train ID
	 * @param stationId the station ID
	 * @return this assert instance
	 */
	public SimulationResultAssert trainDoesNotHaveStopAt(String trainId, String stationId) {
		isNotNull();
		containsTrain(trainId);
		SequencedMap<String, StopTimeData> trainStops = actual.stopTimes.get(trainId);
		if (trainStops.containsKey(stationId)) {
			failWithMessage("Expected train <%s> to not have a stop at station <%s> but it was found", trainId, stationId);
		}
		return this;
	}

	/**
	 * Returns a StopTimeAssert for a specific train and station.
	 *
	 * @param trainId   the train ID
	 * @param stationId the station ID
	 * @return a StopTimeAssert for the specific stop
	 */
	public StopTimeAssert forTrainAtStation(String trainId, String stationId) {
		isNotNull();
		trainHasStopAt(trainId, stationId);
		return StopTimeAssert.assertThat(actual.stopTimes.get(trainId).get(stationId));
	}

	/**
	 * Asserts that the number of trains equals the expected count.
	 *
	 * @param expectedCount the expected number of trains
	 * @return this assert instance
	 */
	public SimulationResultAssert hasNumberOfTrains(int expectedCount) {
		isNotNull();
		int actualCount = actual.stopTimes.size();
		if (actualCount != expectedCount) {
			failWithMessage("Expected <%d> trains but found <%d>", expectedCount, actualCount);
		}
		return this;
	}

	/**
	 * Asserts that the number of stops for a specific train equals the expected count.
	 *
	 * @param trainId       the train ID
	 * @param expectedCount the expected number of stops
	 * @return this assert instance
	 */
	public SimulationResultAssert trainHasNumberOfStops(String trainId, int expectedCount) {
		isNotNull();
		containsTrain(trainId);
		int actualCount = actual.stopTimes.get(trainId).values().stream().mapToInt(StopTimeData::getStopCount).sum();
		if (actualCount != expectedCount) {
			failWithMessage("Expected train <%s> to have <%d> stops but found <%d>", trainId, expectedCount, actualCount);
		}
		return this;
	}

	/**
	 * Asserts that all trains have the same number of stops.
	 *
	 * @param expectedCount the expected number of stops for all trains
	 * @return this assert instance
	 */
	public SimulationResultAssert allTrainsHaveNumberOfStops(int expectedCount) {
		isNotNull();

		// Find trains that don't have the expected number of stops
		List<String> trainsWithWrongCount = actual.stopTimes.entrySet().stream()
			.filter(trainEntry -> trainEntry.getValue().values().stream().mapToInt(StopTimeData::getStopCount).sum() != expectedCount)
			.map(trainEntry -> String.format("train %s: expected %d stops, found %d",
				trainEntry.getKey(), expectedCount, trainEntry.getValue().values().stream().mapToInt(StopTimeData::getStopCount).sum()))
			.toList();

		if (!trainsWithWrongCount.isEmpty()) {
			failWithMessage("Expected all trains to have <%d> stops but the following did not:\n\t%s",
				expectedCount, String.join("\n\t", trainsWithWrongCount));
		}
		return this;
	}

	/**
	 * Asserts that all stop times satisfy the given predicate.
	 *
	 * @param predicate the predicate to test against all stop times
	 * @return this assert instance
	 */
	public SimulationResultAssert allStopTimesSatisfy(Predicate<StopTimeData> predicate) {
		isNotNull();

		// Find failing stop times with their train and station info
		var failingStops = actual.stopTimes.entrySet().stream()
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				return trainEntry.getValue().entrySet().stream()
					.filter(stopEntry -> !predicate.test(stopEntry.getValue()))
					.map(stopEntry -> String.format("train %s at station %s: %s",
						trainId, stopEntry.getKey(), stopEntry.getValue()));
			})
			.toList();

		if (!failingStops.isEmpty()) {
			failWithMessage("Expected all stop times to satisfy the predicate but the following failed:\n\t%s",
				String.join("\n\t", failingStops));
		}
		return this;
	}

	/**
	 * Asserts that all stop times except the last and first one of each train satisfy the given predicate.
	 * The exempt stops may or may not statisfy the predicate.
	 *
	 * @param predicate the predicate to test against stop times
	 * @return this assert instance
	 */
	public SimulationResultAssert allStopTimesExceptFirstAndLastSatisfy(Predicate<StopTimeData> predicate) {
		isNotNull();

		// Find failing intermediate stop times with their train and station info
		List<String> failingStops = actual.stopTimes.entrySet().stream()
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				var trainStops = trainEntry.getValue();

				// Skip trains with less than 3 stops (no intermediate stops)
				if (trainStops.size() < 3) {
					return Stream.<String>empty();
				}

				// Get all stops except first and last
				return trainStops.entrySet().stream()
					.skip(1) // Skip first stop
					.limit(trainStops.size() - 2) // Take all except first and last
					.filter(stopEntry -> !predicate.test(stopEntry.getValue()))
					.map(stopEntry -> String.format("train %s at station %s: %s",
						trainId, stopEntry.getKey(), stopEntry.getValue()));
			})
			.toList();

		if (!failingStops.isEmpty()) {
			failWithMessage("Expected all stop times except first and last of each train to satisfy the predicate but the following failed:\n\t%s",
				String.join("\n\t", failingStops));
		}
		return this;
	}

	/**
	 * Asserts that any stop time satisfies the given predicate.
	 *
	 * @param predicate the predicate to test against stop times
	 * @return this assert instance
	 */
	public SimulationResultAssert anyStopTimeSatisfies(Predicate<StopTimeData> predicate) {
		isNotNull();

		// Check if any stop time satisfies the predicate
		boolean anySatisfies = actual.stopTimes.values().stream()
			.flatMap(stops -> stops.values().stream())
			.anyMatch(predicate);

		if (!anySatisfies) {
			// Find a sample of stop times that don't satisfy the predicate for better error message
			List<String> sampleStops = actual.stopTimes.entrySet().stream()
				.flatMap(trainEntry -> {
					String trainId = trainEntry.getKey();
					return trainEntry.getValue().entrySet().stream()
						.filter(stopEntry -> !predicate.test(stopEntry.getValue()))
						.limit(3) // Limit to first 3 failing stops for readability
						.map(stopEntry -> String.format("train %s at station %s: %s",
							trainId, stopEntry.getKey(), stopEntry.getValue()));
				})
				.toList();

			String sampleMessage = sampleStops.isEmpty() ? "no stop times found" :
				String.format("sample of failing stops:\n\t%s", String.join("\n\t", sampleStops));

			failWithMessage("Expected at least one stop time to satisfy the predicate but none did. %s", sampleMessage);
		}
		return this;
	}

	/**
	 * Asserts that no stop time satisfies the given predicate.
	 *
	 * @param predicate the predicate to test against stop times
	 * @return this assert instance
	 */
	public SimulationResultAssert noStopTimeSatisfies(Predicate<StopTimeData> predicate) {
		isNotNull();

		// Find stop times that satisfy the predicate (should be none)
		List<String> satisfyingStops = actual.stopTimes.entrySet().stream()
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				return trainEntry.getValue().entrySet().stream()
					.filter(stopEntry -> predicate.test(stopEntry.getValue()))
					.limit(3) // Limit to first 3 satisfying stops for readability
					.map(stopEntry -> String.format("train %s at station %s: %s",
						trainId, stopEntry.getKey(), stopEntry.getValue()));
			})
			.toList();

		if (!satisfyingStops.isEmpty()) {
			failWithMessage("Expected no stop time to satisfy the predicate but the following did:\n\t%s",
				String.join("\n\t", satisfyingStops));
		}
		return this;
	}

	/**
	 * Asserts that all trains from the transit schedule have arrived at all their scheduled stops.
	 * This method checks that for every transit line, route, and departure in the schedule,
	 * there is corresponding arrival data in the simulation results.
	 *
	 * @return this assert instance
	 */
	public SimulationResultAssert allTrainsArrived() {
		isNotNull();

		// Get the transit schedule from the scenario
		var transitSchedule = actual.getScenario().getTransitSchedule();
		List<String> missingArrivals = new ArrayList<>();

		// Iterate through all transit lines
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			// Iterate through all routes in the line
			for (TransitRoute route : line.getRoutes().values()) {
				// Iterate through all departures in the route
				for (Departure departure : route.getDepartures().values()) {
					String trainId = departure.getVehicleId().toString();

					// Check if this train has any arrival data
					if (!actual.stopTimes.containsKey(trainId)) {
						missingArrivals.add(String.format("train %s (line %s, route %s, departure %s): no arrival data found",
							trainId, line.getId(), route.getId(), departure.getId()));
						continue;
					}

					// Check if the train arrived at all scheduled stops
					var trainStops = actual.stopTimes.get(trainId);
					for (TransitRouteStop routeStop : route.getStops()) {
						String stopId = routeStop.getStopFacility().getId().toString();

						if (!trainStops.containsKey(stopId)) {
							missingArrivals.add(String.format("train %s (line %s, route %s, departure %s): missing arrival at stop %s",
								trainId, line.getId(), route.getId(), departure.getId(), stopId));
						} else {
							// Check if the train actually arrived (has arrival time)
							StopTimeData stopData = trainStops.get(stopId);
							if (!stopData.hasArrived()) {
								missingArrivals.add(String.format("train %s (line %s, route %s, departure %s): no arrival time at stop %s",
									trainId, line.getId(), route.getId(), departure.getId(), stopId));
							}
						}
					}
				}
			}
		}

		if (!missingArrivals.isEmpty()) {
			failWithMessage("Expected all trains to arrive at all scheduled stops but the following arrivals were missing:\n%s",
				String.join("\n\t", missingArrivals));
		}
		return this;
	}

	/**
	 * Asserts that a specific train has arrived at its last stop with the expected arrival time.
	 * This method checks if the train has any stops and if the last stop has the specified arrival time.
	 *
	 * @param trainId the train ID to check
	 * @param expectedArrivalTime the expected arrival time in seconds
	 * @return this assert instance
	 */
	public SimulationResultAssert trainHasLastArrival(String trainId, double expectedArrivalTime) {
		isNotNull();
		containsTrain(trainId);

		var trainStops = actual.stopTimes.get(trainId);
		if (trainStops.isEmpty()) {
			failWithMessage("Expected train <%s> to have stops but found none", trainId);
		}

		// Get the last stop (latest stop in the sequence)
		var lastStopEntry = trainStops.lastEntry();
		String lastStopId = lastStopEntry.getKey();
		StopTimeData lastStopData = lastStopEntry.getValue();

		if (!lastStopData.hasArrived()) {
			failWithMessage("Expected train <%s> to have arrived at its last stop <%s> but no arrival time found. Stop data: %s",
				trainId, lastStopId, lastStopData);
		}

		if (lastStopData.arrivalTime != expectedArrivalTime) {
			String expectedTimeStr = Time.writeTime(expectedArrivalTime);
			String actualTimeStr = Time.writeTime(lastStopData.arrivalTime);
			failWithMessage("Expected train <%s> to arrive at its last stop <%s> at time <%s> (%s) but arrived at <%s> (%s). Stop data: %s",
				trainId, lastStopId, expectedArrivalTime, expectedTimeStr, lastStopData.arrivalTime, actualTimeStr, lastStopData);
		}

		return this;
	}

	/**
	 * Asserts that a specific train has arrived at its last stop with the expected arrival time.
	 * This method checks if the train has any stops and if the last stop has the specified arrival time.
	 *
	 * @param trainId the train ID to check
	 * @param expectedArrivalTime the expected arrival time as a string (e.g., "8:00", "08:30:15")
	 * @return this assert instance
	 */
	public SimulationResultAssert trainHasLastArrival(String trainId, String expectedArrivalTime) {
		return trainHasLastArrival(trainId, org.matsim.core.utils.misc.Time.parseTime(expectedArrivalTime));
	}

	/**
	 * Asserts that all delays satisfy the given predicate.
	 *
	 * @param predicate the predicate to test against delays
	 * @return this assert instance
	 */
	public SimulationResultAssert allDelaysSatisfy(DoublePredicate predicate) {
		isNotNull();

		List<String> failingDelays = actual.stateEvents.entrySet().stream()
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				return trainEntry.getValue().stream()
					.filter(e -> !predicate.test(e.getDelay()))
					.map(e -> String.format("train %s at link %s @ %s: %s", trainId, e.getHeadLink(), Time.writeTime(e.getTime()), e.getDelay()));
			})
			.toList();

		if (!failingDelays.isEmpty()) {
			failWithMessage("Expected all delays to satisfy the predicate but the following failed:\n\t%s",
				String.join("\n\t", failingDelays));
		}

		return this;
	}

	/**
	 * Asserts that all delays at the start of a link satisfy the given predicate.
	 *
	 * @param predicate the predicate to test against delays
	 * @return this assert instance
	 */
	public SimulationResultAssert allDelaysAtLinkStartSatisfy(DoublePredicate predicate) {
		isNotNull();

		List<String> failingDelays = actual.stateEvents.entrySet().stream()
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				return trainEntry.getValue().stream()
					.filter(e -> e.getHeadPosition() == 0)
					.filter(e -> !predicate.test(e.getDelay()))
					.map(e -> String.format("train %s at link %s @ %s: %s", trainId, e.getHeadLink(), Time.writeTime(e.getTime()), e.getDelay()));
			})
			.toList();

		if (!failingDelays.isEmpty()) {
			failWithMessage("Expected all delays at the start of a link to satisfy the predicate but the following failed:\n\t%s",
				String.join("\n\t", failingDelays));
		}

		return this;
	}

	/**
	 * Asserts that all delays at the arrival at a stop satisfy the given predicate.
	 * The delay is based on the time from the disposition, which might be earlier than the schedule time.
	 *
	 * @param predicate the predicate to test against delays
	 * @return this assert instance
	 */
	public SimulationResultAssert allDelaysAtStopsSatisfy(String train, DoublePredicate predicate) {
		isNotNull();

		if (train != null && !actual.stateEvents.containsKey(train)) {
			failWithMessage("Expected train <%s> to have state events but found none", train);
		}

		List<String> failingDelays = actual.stateEvents.entrySet().stream()
			.filter(trainEntry -> train == null || trainEntry.getKey().equals(train))
			.flatMap(trainEntry -> {
				String trainId = trainEntry.getKey();
				return trainEntry.getValue().stream()
					.filter(e -> e.getSpeed() == 0 && Math.abs(e.getHeadPosition() - actual.getScenario().getNetwork().getLinks().get(e.getHeadLink()).getLength()) < 1e-6)
					.filter(e -> actual.stopTimes.get(trainId).values().stream().anyMatch(s -> s.arrivalTime == e.getTime()))
					.filter(e -> !predicate.test(e.getDelay()))
					.map(e -> String.format("train %s at link %s @ %s: %s", trainId, e.getTailLink(), Time.writeTime(e.getTime()), e.getDelay()));
			})
			.toList();

		if (!failingDelays.isEmpty()) {
			failWithMessage("Expected all delays at the start of a link to satisfy the predicate but the following failed:\n\t%s",
				String.join("\n\t", failingDelays));
		}

		return this;
	}

	/**
	 *  Asserts that all delays at the arrival at a stop satisfy the given predicate for a specific train,
	 */
	public SimulationResultAssert allDelaysAtStopsSatisfy(DoublePredicate predicate) {
		return allDelaysAtStopsSatisfy(null, predicate);
	}

	/**
	 * Asserts that all trains arrive at the transit stops as scheduled by disposition.
	 */
	public SimulationResultAssert allStopDelaysAreZero() {
		return allDelaysAtStopsSatisfy(d -> d == 0);
	}
}
