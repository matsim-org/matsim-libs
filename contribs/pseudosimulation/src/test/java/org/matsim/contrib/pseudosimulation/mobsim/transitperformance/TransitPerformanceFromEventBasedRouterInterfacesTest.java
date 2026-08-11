package org.matsim.contrib.pseudosimulation.mobsim.transitperformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

class TransitPerformanceFromEventBasedRouterInterfacesTest {

	@Test
	void adjacentStopsRetainLegacyZeroTransitTime() {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = factory.createTransitSchedule();
		TransitStopFacility access = factory.createTransitStopFacility(
				Id.create("access", TransitStopFacility.class), new Coord(0, 0), false);
		TransitStopFacility egress = factory.createTransitStopFacility(
				Id.create("egress", TransitStopFacility.class), new Coord(1, 0), false);
		schedule.addStopFacility(access);
		schedule.addStopFacility(egress);
		TransitRouteStop accessRouteStop = factory.createTransitRouteStop(access, 0, 0);
		TransitRouteStop egressRouteStop = factory.createTransitRouteStop(egress, 10, 10);
		TransitRoute transitRoute = factory.createTransitRoute(Id.create("route", TransitRoute.class), null,
				List.of(accessRouteStop, egressRouteStop), "pt");
		TransitLine line = factory.createTransitLine(Id.create("line", TransitLine.class));
		line.addRoute(transitRoute);
		schedule.addTransitLine(line);
		Leg leg = PopulationUtils.createLeg("pt");
		leg.setRoute(new DefaultTransitPassengerRoute(access, line, transitRoute, egress));

		TransitEmulator.Trip trip = new TransitPerformanceFromEventBasedRouterInterfaces(schedule)
				.findTrip(leg, 100.0);

		assertNull(trip.vehicleId());
		assertEquals(100.0, trip.accessTime_s());
		assertEquals(100.0, trip.egressTime_s());
	}
}
