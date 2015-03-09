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
package playground.thibautd.socnetsim.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointPlanRouterTest {
	@Test
	public void testDriverIdIsKept() throws Exception {
        final PopulationFactory populationFactory =
                ScenarioUtils.createScenario(
		        ConfigUtils.createConfig()).getPopulation().getFactory();

		final JointPlanRouter testee =
			new JointPlanRouter(
					createTripRouter(
						populationFactory),
					null);

		final Id<Link> linkId = Id.create( "some_link" , Link.class );
		final Plan plan = populationFactory.createPlan();
		final Activity act1 =
			populationFactory.createActivityFromLinkId(
					"say hello",
					linkId);
		act1.setEndTime( 1035 );
		plan.addActivity( act1 );
		final Leg leg = populationFactory.createLeg( JointActingTypes.PASSENGER );
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
				"leg not replaced",
				leg,
				newLeg);

		final PassengerRoute newRoute = (PassengerRoute) leg.getRoute();
		assertNotNull(
				"new passenger route is null",
				newRoute);

		assertEquals(
				"driver id not kept correctly",
				driverId,
				newRoute.getDriverId());
	}

	@Test
	public void testPassengerIdIsKept() throws Exception {
        final PopulationFactory populationFactory =
                ScenarioUtils.createScenario(
		        ConfigUtils.createConfig()).getPopulation().getFactory();

		final JointPlanRouter testee =
			new JointPlanRouter(
					createTripRouter(
						populationFactory),
					null);

		final Id<Link> linkId = Id.create( "some_link" , Link.class );
		final Plan plan = populationFactory.createPlan();
		final Activity act1 =
			populationFactory.createActivityFromLinkId(
					"say hello",
					linkId);
		act1.setEndTime( 1035 );
		plan.addActivity( act1 );
		final Leg leg = populationFactory.createLeg( JointActingTypes.DRIVER );
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
				"leg not replaced",
				leg,
				newLeg);

		final DriverRoute newRoute = (DriverRoute) leg.getRoute();
		assertNotNull(
				"new driver route is null",
				newRoute);

		final Collection<Id<Person>> passengers = Arrays.asList( passengerId1 , passengerId2 );
		assertEquals(
				"not the right number of passenger ids in "+newRoute.getPassengersIds(),
				passengers.size(),
				newRoute.getPassengersIds().size());

		assertTrue(
				"not the right ids in "+newRoute.getPassengersIds(),
				passengers.containsAll(
					newRoute.getPassengersIds() ));
	}

	private static TripRouter createTripRouter(final PopulationFactory populationFactory) {
		final TripRouter instance = new TripRouter();

		instance.setRoutingModule(
				JointActingTypes.DRIVER,
				new DriverRoutingModule(
					JointActingTypes.DRIVER,
					populationFactory,
					new RoutingModule() {

						@Override
						public List<? extends PlanElement> calcRoute(
								final Facility fromFacility,
								final Facility toFacility,
								final double departureTime,
								final Person person) {
							return Arrays.asList( new LegImpl( TransportMode.car ) );
						}

						@Override
						public StageActivityTypes getStageActivityTypes() {
							return EmptyStageActivityTypes.INSTANCE;
						}
					}));

		instance.setRoutingModule(
				JointActingTypes.PASSENGER,
				new PassengerRoutingModule(
					JointActingTypes.PASSENGER,
					populationFactory));

		return instance;
	}
}

