/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanRouterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.router;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;

/**
 * @author thibautd
 */
public class JointPlanRouterTest {
	@Test
	void testDriverIdIsKept() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final PopulationFactory populationFactory =
                ScenarioUtils.createScenario(
				    config).getPopulation().getFactory();

		final JointPlanRouter testee =
			new JointPlanRouter(
					createTripRouter(
						populationFactory, config),
					null, TimeInterpretation.create(config));

		final Id<Link> linkId = Id.create( "some_link" , Link.class );
		final Plan plan = populationFactory.createPlan();
		final Activity act1 =
			populationFactory.createActivityFromLinkId(
					"say hello",
					linkId);
		act1.setEndTime( 1035 );
		plan.addActivity( act1 );
		final Leg leg = populationFactory.createLeg( JointActingTypes.PASSENGER );
		TripStructureUtils.setRoutingMode( leg, JointActingTypes.PASSENGER );
		plan.addLeg( leg );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"say goodbye",
					linkId));

		final Id<Person> driverId = Id.create( "the_driver" , Person.class );
		final PassengerRoute route = new PassengerRoute( linkId , linkId );
		route.setDriverId( driverId );
		leg.setRoute( route );

		testee.run( plan );

		final Leg newLeg = (Leg) plan.getPlanElements().get( 1 );
		assertNotSame(
				leg,
				newLeg,
				"leg not replaced");

		final PassengerRoute newRoute = (PassengerRoute) leg.getRoute();
		assertNotNull(
				newRoute,


				"new passenger route is null");

		assertEquals(
				driverId,
				newRoute.getDriverId(),
				"driver id not kept correctly");
	}

	@Test
	void testPassengerIdIsKept() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final PopulationFactory populationFactory =
                ScenarioUtils.createScenario(
				    config).getPopulation().getFactory();

		final JointPlanRouter testee =
			new JointPlanRouter(
					createTripRouter(
						populationFactory, config),
					null, TimeInterpretation.create(config));

		final Id<Link> linkId = Id.create( "some_link" , Link.class );
		final Plan plan = populationFactory.createPlan();
		final Activity act1 =
			populationFactory.createActivityFromLinkId(
					"say hello",
					linkId);
		act1.setEndTime( 1035 );
		plan.addActivity( act1 );
		final Leg leg = populationFactory.createLeg( JointActingTypes.DRIVER );
		TripStructureUtils.setRoutingMode( leg, JointActingTypes.DRIVER );
		plan.addLeg( leg );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"say goodbye",
					linkId));

		final Id<Person> passengerId1 = Id.create( "the_passenger_1" , Person.class );
		final Id<Person> passengerId2 = Id.create( "the_passenger_2" , Person.class );
		final DriverRoute route = new DriverRoute( linkId , linkId );
		route.addPassenger( passengerId1 );
		route.addPassenger( passengerId2 );
		leg.setRoute( route );

		testee.run( plan );

		final Leg newLeg = (Leg) plan.getPlanElements().get( 1 );
		assertNotSame(
				leg,
				newLeg,
				"leg not replaced");

		final DriverRoute newRoute = (DriverRoute) leg.getRoute();
		assertNotNull(
				newRoute,
				"new driver route is null");

		final Collection<Id<Person>> passengers = Arrays.asList( passengerId1 , passengerId2 );
		assertEquals(
				passengers.size(),
				newRoute.getPassengersIds().size(),
				"not the right number of passenger ids in "+newRoute.getPassengersIds());

		assertTrue(
				passengers.containsAll(
					newRoute.getPassengersIds() ),
				"not the right ids in "+newRoute.getPassengersIds());
	}

	private static TripRouter createTripRouter(final PopulationFactory populationFactory, Config config) {
		final TripRouter.Builder builder = new TripRouter.Builder(config) ;
		
		builder.setRoutingModule(
				JointActingTypes.DRIVER,
				new DriverRoutingModule(
					JointActingTypes.DRIVER,
					populationFactory,
					new RoutingModule() {

						@Override
						public List<? extends PlanElement> calcRoute(RoutingRequest request) {
							final Facility fromFacility = request.getFromFacility();
							final Facility toFacility = request.getToFacility();
							
							NetworkRoute route = RouteUtils.createNetworkRoute(List.of(fromFacility.getLinkId(), toFacility.getLinkId()), null);
							route.setTravelTime(10);
							Leg leg =  PopulationUtils.createLeg(TransportMode.car);
							leg.setRoute(route);
							return List.of(leg);
						}

					}));
		
		builder.setRoutingModule(
				JointActingTypes.PASSENGER,
				new PassengerRoutingModule(
					JointActingTypes.PASSENGER,
					populationFactory));

		return builder.build() ;
	}
}

