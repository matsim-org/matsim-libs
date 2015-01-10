/* *********************************************************************** *
 * project: org.matsim.*
 * FixedTransitRouteModuleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author thibautd
 */
public class FixedTransitRouteModuleTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRoutes() throws Exception {
		Config config = utils.loadConfig( utils.getPackageInputDirectory()+"/config.xml" );
		Scenario scenario = ScenarioUtils.loadScenario( config );
		Provider<TripRouter> tripRouterFactory =  new TripRouterProviderImpl(
				scenario,
				new TravelTimeAndDistanceBasedTravelDisutilityFactory(),
				new FreespeedTravelTimeAndDisutility( config.planCalcScore() ),
				new DijkstraFactory(),
				new TransitRouterImplFactory(
					scenario.getTransitSchedule(),
					new TransitRouterConfig( config )));
		TripRouter tripRouter = tripRouterFactory.get();
		TransitRouterWrapper transitRouter = (TransitRouterWrapper) tripRouter.getRoutingModule( TransportMode.pt );

		PlanRouter router = new PlanRouter( tripRouter );
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan testPlan = p.getSelectedPlan();
			router.run( testPlan );

			TripRouter tripRouterWithFixedRoutes = tripRouterFactory.get();
			FixedTransitRouteRoutingModule testee =
				new FixedTransitRouteRoutingModule(
						testPlan,
						scenario.getTransitSchedule(),
						transitRouter);
			tripRouterWithFixedRoutes.setRoutingModule( TransportMode.pt , testee );
			PlanRouter routerFixed = new PlanRouter( tripRouterWithFixedRoutes );
			// using the following causes a test fail,
			// so we are sure the test actually tests something.
			// routerFixed = router;

			for (int i=0; i < 10; i++) {
				Activity first = (Activity) testPlan.getPlanElements().get( 0 );
				first.setEndTime( first.getEndTime() + 3600 );
				PtTripInfo initialPlanInfo = analyseTrips( testPlan );

				routerFixed.run( testPlan );
				assertEquals(
						"routes changed!",
						initialPlanInfo,
						analyseTrips( testPlan ));
			}
		}
	}

	private static PtTripInfo analyseTrips(final Plan testPlan) {
		// TODO: return one info per trip?
		PtTripInfo currentInfo = new PtTripInfo();
		for (PlanElement pe : testPlan.getPlanElements()) {
			if (pe instanceof Leg && ((Leg) pe).getMode().equals( TransportMode.pt )) {
				currentInfo.notifyLeg( (Leg) pe );
			}
		}

		return currentInfo;
	}

	private static class PtTripInfo {
		final List<Id> lines = new ArrayList<Id>();
		final List<Id> routes = new ArrayList<Id>();
		final List<Id> stops = new ArrayList<Id>();

		public void notifyLeg(final Leg leg) {
			ExperimentalTransitRoute r = (ExperimentalTransitRoute) leg.getRoute();
			lines.add( r.getLineId() );
			routes.add( r.getRouteId() );
			stops.add( r.getAccessStopId() );
			stops.add( r.getEgressStopId() );
		}

		@Override
		public boolean equals(final Object o) {
			if ( !(o instanceof PtTripInfo) ) return false;

			PtTripInfo other = (PtTripInfo) o;

			return other.lines.equals( lines ) &&
				other.routes.equals( routes ) &&
				other.stops.equals( stops );
		}

		@Override
		public int hashCode() {
			return lines.hashCode() + routes.hashCode() + stops.hashCode();
		}
	}
}

