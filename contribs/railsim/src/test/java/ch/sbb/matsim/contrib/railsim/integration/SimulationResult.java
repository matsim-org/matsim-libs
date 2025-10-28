package ch.sbb.matsim.contrib.railsim.integration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.testcases.utils.EventsCollector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Data class holding the results of a simulation run and the scenario.
 */
final class SimulationResult {

	private final Scenario scenario;
	private final EventsCollector collector;

	final Map<String, SequencedMap<String, StopTimeData>> stopTimes;

	public SimulationResult(Scenario scenario, EventsCollector collector) {
		this.scenario = scenario;
		this.collector = collector;
		this.stopTimes = processStopTimes(collector.getEvents());
	}

	/**
	 * Processes events to extract stop time information for each train and facility combination.
	 *
	 * @param events List of all simulation events
	 * @return Map of train ID -> facility ID -> stop time data
	 */
	private Map<String, SequencedMap<String, StopTimeData>> processStopTimes(List<Event> events) {
		Map<String, SequencedMap<String, StopTimeData>> stopTimesByTrainAndFacility = new LinkedHashMap<>();

		for (Event event : events) {
			if (event instanceof VehicleArrivesAtFacilityEvent arrivalEvent) {
				String trainId = arrivalEvent.getVehicleId().toString();
				String facilityId = arrivalEvent.getFacilityId().toString();

				SequencedMap<String, StopTimeData> stops = stopTimesByTrainAndFacility
					.computeIfAbsent(trainId, k -> new LinkedHashMap<>());

				StopTimeData stop = stops.computeIfAbsent(facilityId, k -> new StopTimeData(stops.lastEntry()));

				stop.arrivalTime = arrivalEvent.getTime();
				stop.stopCount++;

			} else if (event instanceof VehicleDepartsAtFacilityEvent departureEvent) {
				String trainId = departureEvent.getVehicleId().toString();
				String facilityId = departureEvent.getFacilityId().toString();

				SequencedMap<String, StopTimeData> stops = stopTimesByTrainAndFacility
					.computeIfAbsent(trainId, k -> new LinkedHashMap<>());

				stops.computeIfAbsent(facilityId, k -> new StopTimeData(stops.lastEntry()))
					.departureTime = departureEvent.getTime();
			}
		}

		return stopTimesByTrainAndFacility;
	}

	Map<String, SequencedMap<String, StopTimeData>> getStopTimes() {
		return stopTimes;
	}

	Scenario getScenario() {
		return scenario;
	}

	List<Event> getEvents() {
		return collector.getEvents();
	}
}
