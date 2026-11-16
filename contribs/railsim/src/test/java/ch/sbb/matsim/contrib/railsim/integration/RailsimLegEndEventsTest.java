package ch.sbb.matsim.contrib.railsim.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests that every leg is opened and closed with the same traffic events.
 */
public class RailsimLegEndEventsTest extends AbstractIntegrationTest {

	@Test
	void circulationClosesLegs() {

		// Trains run several trips in a row, with a reversal in between
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"));

		Map<Id<Person>, Integer> legs = assertLegEventsAreConsistent(result.getEvents());

		// 4 scheduled trips and 3 reversal legs in between
		Assertions.assertThat(legs).hasSize(4)
			.allSatisfy((driver, count) -> Assertions.assertThat(count).isEqualTo(7));
	}

	@Test
	void chainedDeparturesCloseLegs() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "chainFormations"),
			(s) -> s.getConfig().qsim().setEndTime(30 * 3600));

		Map<Id<Person>, Integer> legs = assertLegEventsAreConsistent(result.getEvents());

		Assertions.assertThat(legs)
			.as("scenario should contain trains that continue with another leg")
			.containsValue(2);
	}

	/**
	 * Each leg of a train has to start with a person and vehicle entering traffic and to end with the
	 * counterparts, before the arrival of the driver is processed. Vehicles may enter traffic multiple
	 * times, e.g. when a train continues with another departure.
	 *
	 * @return the number of legs of each driver
	 */
	private Map<Id<Person>, Integer> assertLegEventsAreConsistent(List<Event> events) {

		// The driver of a vehicle is known from the traffic events
		Map<Id<Vehicle>, Id<Person>> drivers = new LinkedHashMap<>();
		for (Event event : events) {
			if (event instanceof VehicleEntersTrafficEvent e) {
				drivers.put(e.getVehicleId(), e.getPersonId());
			} else if (event instanceof VehicleLeavesTrafficEvent e) {
				drivers.put(e.getVehicleId(), e.getPersonId());
			}
		}

		Assertions.assertThat(drivers).isNotEmpty();

		Map<Id<Person>, Map<Double, Integer>> arrivals = new HashMap<>();
		for (int i = 0; i < events.size(); i++) {
			if (events.get(i) instanceof PersonArrivalEvent e && drivers.containsValue(e.getPersonId())) {
				arrivals.computeIfAbsent(e.getPersonId(), k -> new HashMap<>()).put(e.getTime(), i);
			}
		}

		Map<Id<Person>, Integer> legs = new LinkedHashMap<>();

		for (Map.Entry<Id<Vehicle>, Id<Person>> entry : drivers.entrySet()) {

			Id<Vehicle> vehicleId = entry.getKey();
			Id<Person> driverId = entry.getValue();

			// All events of this vehicle and its driver, ignoring passengers
			List<Integer> indices = new ArrayList<>();
			for (int i = 0; i < events.size(); i++) {
				Event event = events.get(i);
				if (event instanceof VehicleEntersTrafficEvent e && e.getVehicleId().equals(vehicleId)) {
					indices.add(i);
				} else if (event instanceof VehicleLeavesTrafficEvent e && e.getVehicleId().equals(vehicleId)) {
					indices.add(i);
				} else if (event instanceof PersonEntersVehicleEvent e && e.getPersonId().equals(driverId)) {
					indices.add(i);
				} else if (event instanceof PersonLeavesVehicleEvent e && e.getPersonId().equals(driverId)) {
					indices.add(i);
				}
			}

			int enters = 0;
			int leaves = 0;
			boolean inTraffic = false;

			for (int i = 0; i < indices.size(); i++) {
				Event event = events.get(indices.get(i));

				if (event instanceof PersonEntersVehicleEvent) {
					Assertions.assertThat(inTraffic)
						.as("%s enters the vehicle again, but the previous leg was not closed with a vehicle leaves traffic event", vehicleId).isFalse();
					Assertions.assertThat(eventAt(events, indices, i + 1))
						.as("enter events of %s", vehicleId).isInstanceOf(VehicleEntersTrafficEvent.class);
				} else if (event instanceof VehicleEntersTrafficEvent) {
					Assertions.assertThat(eventAt(events, indices, i - 1))
						.as("enter events of %s", vehicleId).isInstanceOf(PersonEntersVehicleEvent.class);
					inTraffic = true;
					enters++;
				} else if (event instanceof VehicleLeavesTrafficEvent leave) {
					Assertions.assertThat(inTraffic)
						.as("%s leaves traffic without having entered it", vehicleId).isTrue();
					inTraffic = false;
					leaves++;

					// The driver leaves the vehicle right after the vehicle left traffic
					Event next = eventAt(events, indices, i + 1);
					Assertions.assertThat(next)
						.as("leave events of %s", vehicleId).isInstanceOf(PersonLeavesVehicleEvent.class);
					Assertions.assertThat(next.getTime()).isEqualTo(leave.getTime());

					// ... and the arrival of the driver follows afterwards
					Integer arrivalIdx = arrivals.getOrDefault(driverId, Map.of()).get(leave.getTime());
					Assertions.assertThat(arrivalIdx)
						.as("%s has an arrival at %.0f", vehicleId, leave.getTime()).isNotNull();

					PersonArrivalEvent arrival = (PersonArrivalEvent) events.get(arrivalIdx);
					Assertions.assertThat(arrival.getLinkId())
						.as("arrival link of %s", vehicleId).isEqualTo(leave.getLinkId());
					Assertions.assertThat(arrival.getLegMode())
						.as("arrival mode of %s", vehicleId).isEqualTo(leave.getNetworkMode());
					Assertions.assertThat(arrivalIdx)
						.as("arrival of %s is emitted after the end of the leg", vehicleId)
						.isGreaterThan(indices.get(i + 1));
				}
			}

			Assertions.assertThat(inTraffic)
				.as("%s is still in traffic at the end of the simulation", vehicleId).isFalse();
			Assertions.assertThat(enters)
				.as("number of legs of %s", vehicleId).isEqualTo(leaves);
			Assertions.assertThat(arrivals.getOrDefault(driverId, Map.of()))
				.as("number of arrivals of %s", vehicleId).hasSize(leaves);

			legs.put(driverId, leaves);
		}

		return legs;
	}

	private static Event eventAt(List<Event> events, List<Integer> indices, int idx) {
		return idx >= 0 && idx < indices.size() ? events.get(indices.get(idx)) : null;
	}
}
