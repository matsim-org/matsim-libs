package org.matsim.contrib.pseudosimulation.mobsim.transitperformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class TransitPerformanceFromPSimSpecificImplementationTest {

	private static final Id<TransitLine> LINE = Id.create("line", TransitLine.class);
	private static final Id<TransitRoute> ROUTE = Id.create("route", TransitRoute.class);
	private static final Id<TransitStopFacility> ACCESS_STOP = Id.create("access", TransitStopFacility.class);
	private static final Id<TransitStopFacility> EGRESS_STOP = Id.create("egress", TransitStopFacility.class);

	@Test
	void forwardsRouteAndDepartureAndCalculatesTripTimes() {
		RecordingTransitPerformance performance = new RecordingTransitPerformance();
		TransitPerformanceFromPSimSpecificImplementation emulator =
				new TransitPerformanceFromPSimSpecificImplementation(performance,
						new TransitScheduleFactoryImpl().createTransitSchedule());
		Leg leg = PopulationUtils.createLeg("pt");
		leg.setRoute(new DefaultTransitPassengerRoute(null, null, ACCESS_STOP, EGRESS_STOP, LINE, ROUTE));

		TransitEmulator.Trip trip = emulator.findTrip(leg, 100.0);

		assertEquals(LINE, performance.line);
		assertEquals(ROUTE, performance.route);
		assertEquals(ACCESS_STOP, performance.originStop);
		assertEquals(EGRESS_STOP, performance.destinationStop);
		assertEquals(100.0, performance.time);
		assertNull(trip.vehicleId());
		assertEquals(112.5, trip.accessTime_s());
		assertEquals(146.5, trip.egressTime_s());
	}

	private static final class RecordingTransitPerformance extends TransitPerformance {
		private Id<TransitLine> line;
		private Id<TransitRoute> route;
		private Id<TransitStopFacility> originStop;
		private Id<TransitStopFacility> destinationStop;
		private double time;

		@Override
		public Tuple<Double, Double> getRouteTravelTime(Id<TransitLine> line, Id<TransitRoute> route,
				Id<TransitStopFacility> originStop, Id<TransitStopFacility> destinationStop, double time) {
			this.line = line;
			this.route = route;
			this.originStop = originStop;
			this.destinationStop = destinationStop;
			this.time = time;
			return new Tuple<>(12.5, 34.0);
		}
	}
}
