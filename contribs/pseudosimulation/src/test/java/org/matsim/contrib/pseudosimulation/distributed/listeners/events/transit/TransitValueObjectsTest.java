package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitValueObjectsTest {

	@Test
	void ignoringOccupancyAlwaysAllowsBoarding() {
		BoardingModel model = new BoardingModelIgnoringOccupancy();

		assertTrue(model.canBoard(Double.NEGATIVE_INFINITY));
		assertTrue(model.canBoard(2.0));
		assertTrue(model.canBoard(Double.NaN));
	}

	@Test
	void stochasticModelHasDeterministicOutcomesOutsideProbabilityRange() {
		BoardingModel model = new BoardingModelStochasticLinear();

		assertTrue(model.canBoard(-1.0));
		assertFalse(model.canBoard(1.0));
		assertFalse(model.canBoard(Double.NaN));
	}

	@Test
	void dwellEventDefaultsAndMutatorsArePreserved() {
		DwellEvent event = new DwellEvent(12.5, "stop", null, 3);

		assertEquals(12.5, event.getArrivalTime());
		assertEquals("stop", event.getStopId());
		assertNull(event.getVehicle());
		assertEquals(3, event.getIndexInVehicleDwellEventList());
		assertEquals(Double.POSITIVE_INFINITY, event.getDepartureTime());
		assertEquals(0.0, event.getOccupancyAtDeparture());

		event.setDepartureTime(14.0);
		event.setOccupancyAtDeparture(0.75);
		assertEquals(14.0, event.getDepartureTime());
		assertEquals(0.75, event.getOccupancyAtDeparture());
	}

	@Test
	void dwellComparisonTruncatesSubSecondDifferencesAndRejectsOtherTypes() {
		DwellEvent earlier = new DwellEvent(10.1, "a", null, 0);
		DwellEvent later = new DwellEvent(10.9, "b", null, 1);

		assertEquals(0, earlier.compareTo(later));
		assertEquals(-2, earlier.compareTo(new DwellEvent(12.9, "c", null, 2)));
		assertThrows(ClassCastException.class, () -> earlier.compareTo("not a dwell event"));
	}

	@Test
	void fullDepartureBuildsCompositeIdentifierAndRetainsParts() {
		Id<TransitLine> line = Id.create("line", TransitLine.class);
		Id<TransitRoute> route = Id.create("route", TransitRoute.class);
		Id<Vehicle> vehicle = Id.createVehicleId("vehicle");
		Id<Departure> departure = Id.create("departure", Departure.class);

		FullDeparture full = new FullDeparture(line, route, vehicle, departure);

		assertEquals("line_route_vehicle_departure", full.getFullDepartureId().toString());
		assertEquals(line, full.getLineId());
		assertEquals(route, full.getRouteId());
		assertEquals(vehicle, full.getVehicleId());
		assertEquals(departure, full.getDepartureId());
	}
}
