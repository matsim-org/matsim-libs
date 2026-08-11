package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransitPerformanceTest {

	private static final Id<TransitLine> LINE = Id.create("line", TransitLine.class);
	private static final Id<TransitRoute> ROUTE = Id.create("route", TransitRoute.class);
	private static final Id<TransitStopFacility> ORIGIN = Id.create("origin", TransitStopFacility.class);
	private static final Id<TransitStopFacility> DESTINATION = Id.create("destination", TransitStopFacility.class);

	@Test
	void defaultAndReplacementBoardingModelsRemainObservable() {
		TransitPerformance performance = new TransitPerformance();
		assertInstanceOf(BoardingModelStochasticLinear.class, performance.getBoardingModel());

		BoardingModel replacement = occupancy -> true;
		performance.setBoardingModel(replacement);
		assertSame(replacement, performance.getBoardingModel());
	}

	@Test
	void missingLineRouteOrOriginReturnsInfinityTuple() {
		TransitPerformance performance = new TransitPerformance(new BoardingModelIgnoringOccupancy());
		assertInfinity(performance.getRouteTravelTime(LINE, ROUTE, ORIGIN, DESTINATION, 0));

		performance.addVehicleDwellEventAtStop(LINE, ROUTE, ORIGIN, dwellEvent(100, 20, 0));
		assertInfinity(performance.getRouteTravelTime(LINE,
				Id.create("missing-route", TransitRoute.class), ORIGIN, DESTINATION, 0));
		assertInfinity(performance.getRouteTravelTime(LINE, ROUTE,
				Id.create("missing-origin", TransitStopFacility.class), DESTINATION, 0));
	}

	@Test
	void singleBoardableEventReturnsWaitAndInVehicleTime() {
		TransitPerformance performance = new TransitPerformance(new BoardingModelIgnoringOccupancy());
		performance.addVehicleDwellEventAtStop(LINE, ROUTE, ORIGIN, dwellEvent(100, 23, 0.75));

		Tuple<Double, Double> result = performance.getRouteTravelTime(LINE, ROUTE, ORIGIN, DESTINATION, 90);

		assertEquals(10, result.getFirst());
		assertEquals(23, result.getSecond());
	}

	@Test
	void rejectedBoardingReturnsInfinityTuple() {
		TransitPerformance performance = new TransitPerformance(occupancy -> false);
		performance.addVehicleDwellEventAtStop(LINE, ROUTE, ORIGIN, dwellEvent(100, 23, 0.75));

		assertInfinity(performance.getRouteTravelTime(LINE, ROUTE, ORIGIN, DESTINATION, 90));
	}

	@Test
	void eventBeforeRequestedTimeIsNotBoarded() {
		TransitPerformance performance = new TransitPerformance(new BoardingModelIgnoringOccupancy());
		performance.addVehicleDwellEventAtStop(LINE, ROUTE, ORIGIN, dwellEvent(100, 23, 0));

		assertInfinity(performance.getRouteTravelTime(LINE, ROUTE, ORIGIN, DESTINATION, 101));
	}

	private static DwellEvent dwellEvent(double arrivalTime, double inVehicleTime, double occupancy) {
		VehicleTracker vehicle = new VehicleTracker(null, null, 1) {
			@Override
			public double getInVehicleTime(DwellEvent dwellEvent, Id<TransitStopFacility> destinationStop) {
				assertEquals(DESTINATION, destinationStop);
				return inVehicleTime;
			}
		};
		DwellEvent event = new DwellEvent(arrivalTime, ORIGIN.toString(), vehicle, 0);
		event.setOccupancyAtDeparture(occupancy);
		return event;
	}

	private static void assertInfinity(Tuple<Double, Double> result) {
		assertEquals(Double.POSITIVE_INFINITY, result.getFirst());
		assertEquals(Double.POSITIVE_INFINITY, result.getSecond());
	}
}
