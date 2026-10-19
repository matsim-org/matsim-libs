package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

class VehicleTrackerCollectionTest {

	private static final String VEHICLE = "bus";
	private static final Id<Vehicle> VEHICLE_ID = Id.createVehicleId(VEHICLE);
	private static final Id<TransitStopFacility> FIRST_STOP = Id.create("first", TransitStopFacility.class);
	private static final Id<TransitStopFacility> SECOND_STOP = Id.create("second", TransitStopFacility.class);

	private static VehicleTracker tracker(String departureName) {
		FullDeparture departure = new FullDeparture(Id.create("line", TransitLine.class),
				Id.create("route", TransitRoute.class), VEHICLE_ID,
				Id.create(departureName, Departure.class));
		return new VehicleTracker(departure, Id.create("driver", Person.class), 10);
	}

	@Test
	void returnsTrackerOfMostRecentDepartureAtOrBeforeTime() {
		VehicleTrackerCollection collection = new VehicleTrackerCollection(1);
		VehicleTracker morning = tracker("morning");
		VehicleTracker evening = tracker("evening");
		collection.put(VEHICLE, 100.0, morning);
		collection.put(VEHICLE, 1000.0, evening);

		assertSame(morning, collection.get(VEHICLE, 100.0));
		assertSame(morning, collection.get(VEHICLE, 999.0));
		assertSame(evening, collection.get(VEHICLE, 1000.0));
		assertSame(evening, collection.get(VEHICLE, 5000.0));
	}

	@Test
	void returnsNullBeforeTheFirstDepartureAndForUnknownVehicles() {
		VehicleTrackerCollection collection = new VehicleTrackerCollection(1);
		collection.put(VEHICLE, 100.0, tracker("morning"));

		assertNull(collection.get(VEHICLE, 99.0));
		assertNull(collection.get("tram", 100.0));
	}

	@Test
	void inVehicleTimeIsMeasuredBetweenArrivalsOfTheSameRun() {
		VehicleTracker tracker = tracker("morning");
		DwellEvent boarding = tracker.registerArrival(new VehicleArrivesAtFacilityEvent(100.0, VEHICLE_ID,
				FIRST_STOP, 0.0));
		tracker.registerDeparture(new VehicleDepartsAtFacilityEvent(130.0, VEHICLE_ID, FIRST_STOP, 0.0));
		tracker.registerArrival(new VehicleArrivesAtFacilityEvent(400.0, VEHICLE_ID, SECOND_STOP, 0.0));

		assertEquals(300.0, tracker.getInVehicleTime(boarding, SECOND_STOP));
		assertEquals(Double.POSITIVE_INFINITY,
				tracker.getInVehicleTime(boarding, Id.create("never", TransitStopFacility.class)));
		assertEquals(130.0, boarding.getDepartureTime());
	}

	@Test
	void departureWithoutAPrecedingArrivalIsIgnored() {
		VehicleTracker tracker = tracker("morning");

		tracker.registerDeparture(new VehicleDepartsAtFacilityEvent(130.0, VEHICLE_ID, FIRST_STOP, 0.0));

		DwellEvent boarding = tracker.registerArrival(new VehicleArrivesAtFacilityEvent(200.0, VEHICLE_ID,
				FIRST_STOP, 0.0));
		assertEquals(Double.POSITIVE_INFINITY, boarding.getDepartureTime());
	}

	@Test
	void occupancyIsRelativeToCapacityAndSkipsTheDriver() {
		VehicleTracker tracker = tracker("morning");

		tracker.ridershipIncrement(new org.matsim.api.core.v01.events.PersonEntersVehicleEvent(10.0,
				Id.create("driver", Person.class), VEHICLE_ID));
		assertEquals(0.0, tracker.getOccupancy());

		tracker.ridershipIncrement(new org.matsim.api.core.v01.events.PersonEntersVehicleEvent(11.0,
				Id.create("rider", Person.class), VEHICLE_ID));
		assertEquals(0.1, tracker.getOccupancy());

		tracker.ridershipDecrement(new org.matsim.api.core.v01.events.PersonLeavesVehicleEvent(12.0,
				Id.create("rider", Person.class), VEHICLE_ID));
		assertEquals(0.0, tracker.getOccupancy());
	}
}
