/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanUtils.java
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
package playground.thibautd.cliquessim.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.thibautd.cliquessim.population.Clique;
import playground.thibautd.cliquessim.population.DriverRoute;
import playground.thibautd.cliquessim.population.JointActingTypes;
import playground.thibautd.cliquessim.population.JointPlan;
import playground.thibautd.cliquessim.population.PassengerRoute;
import playground.thibautd.cliquessim.utils.JointPlanUtils.DriverTrip;
import playground.thibautd.cliquessim.utils.JointPlanUtils.JointTrip;
import playground.thibautd.cliquessim.utils.JointPlanUtils.JointTravelStructure;

/**
 * @author thibautd
 */
public class JointPlanUtilsTest {
	private List<Fixture> fixtures;

	@Before
	public void initFixtures() {
		fixtures = new ArrayList<Fixture>();

		Person driver = new PersonImpl( new IdImpl( "Alain Prost" ) );
		Person passenger1 = new PersonImpl( new IdImpl( "Tintin" ) );
		Person passenger2 = new PersonImpl( new IdImpl( "Milou" ) );

		Id link1 = new IdImpl( 1 );
		Id link2 = new IdImpl( 2 );
		Id link3 = new IdImpl( 3 );
		Id link4 = new IdImpl( 4 );

		Map<Id, Plan> plans = new HashMap<Id, Plan>();

		// plan 1
		// just one passenger
		PlanImpl plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		PassengerRoute passengerRoute = new PassengerRoute( link2 , link3 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger1.getId() , plan );

		DriverTrip driverTrip = new DriverTrip( driver.getId() );
		driverTrip.driverTrip.add( driverLeg );
		driverTrip.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip.passengerDestinations.put( passenger1.getId() , link3 );

		fixtures.add(
				new Fixture(
					"one passenger",
					Arrays.asList( driverTrip ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg ),
								passenger1.getId(),
								passengerLeg))),
					new JointPlan(
						new Clique( null ),
						plans )));

		// plan 2
		// two passenger, two ODs
		plans = new HashMap<Id, Plan>();
		List<JointTrip> jointTrips = new ArrayList<JointTrip>();
		plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));

		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		Leg driverLeg2 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link4 ),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerRoute = new PassengerRoute( link2 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger1.getId() , plan );

		plan = new PlanImpl( passenger2 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		Leg passengerLeg2 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerRoute = new PassengerRoute( link3 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger2.getId() , plan );

		driverTrip = new DriverTrip( driver.getId() );
		driverTrip.driverTrip.add( driverLeg );
		driverTrip.driverTrip.add( driverLeg2 );
		driverTrip.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip.passengerDestinations.put( passenger1.getId() , link4 );
		driverTrip.passengerOrigins.put( passenger2.getId() , link3 );
		driverTrip.passengerDestinations.put( passenger2.getId() , link4 );

		fixtures.add(
				new Fixture(
					"two passengers",
					Arrays.asList( driverTrip ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg , driverLeg2 ),
								passenger1.getId(),
								passengerLeg),
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg2 ),
								passenger2.getId(),
								passengerLeg2))),
					new JointPlan(
						new Clique( null ),
						plans )));
	}

	@Test
	public void testExtractJointTrips() throws Exception {
		for ( Fixture f : fixtures ) {
			JointTravelStructure struct = JointPlanUtils.analyseJointTravel( f.plan );
			Assert.assertEquals(
					"wrong structure for fixture "+f.name,
					f.structure,
					struct);
		}
	}

	@Test
	public void testParseDriverTrips() throws Exception {
		for ( Fixture f : fixtures ) {
			List<DriverTrip> trips = JointPlanUtils.parseDriverTrips( f.plan );

			Assert.assertEquals(
					"wrong number of driver trips: "+f.driverTrips+" is the target, got "+trips,
					f.driverTrips.size(),
					trips.size());

			Assert.assertTrue(
					"wrong driver trips: "+f.driverTrips+" is the target, got "+trips,
					trips.containsAll( f.driverTrips ));
		}
	}

	private static class Fixture {
		public final String name;
		public final JointTravelStructure structure;
		public final List<DriverTrip> driverTrips;
		public final JointPlan plan;

		public Fixture(
				final String name,
				final List<DriverTrip> driverTrips,
				final JointTravelStructure structure,
				final JointPlan plan) {
			this.name = name;
			this.driverTrips = driverTrips;
			this.structure = structure;
			this.plan = plan;
		}
	}
}

