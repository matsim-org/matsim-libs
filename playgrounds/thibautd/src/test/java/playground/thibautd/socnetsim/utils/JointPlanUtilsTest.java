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
package playground.thibautd.socnetsim.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.utils.JointPlanUtils.DriverTrip;
import playground.thibautd.socnetsim.utils.JointPlanUtils.JointTravelStructure;
import playground.thibautd.socnetsim.utils.JointPlanUtils.JointTrip;

/**
 * @author thibautd
 */
public class JointPlanUtilsTest {
	private final List<Fixture> fixtures = new ArrayList<Fixture>();

	@After
	public void clearFixtures() {
		fixtures.clear();
	}

	@Before
	public void initOnePassengerFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alain Prost") );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin") );

		final Id<Link> link1 = Id.create( 1 , Link.class);
		final Id<Link> link2 = Id.create( 2 , Link.class);
		final Id<Link> link3 = Id.create( 3 , Link.class);

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		// plan 1
		// just one passenger
		PlanImpl plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		PassengerRoute passengerRoute = new PassengerRoute( link2 , link3 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
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
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Before
	public void initTwoPassengerTwoOdFixture() {
		final Person driver = new PersonImpl( Id.create( "Alain Prost" , Person.class ) );
		final Person passenger1 = new PersonImpl( Id.create( "Tintin" , Person.class ) );
		final Person passenger2 = new PersonImpl( Id.create( "Milou" , Person.class ) );

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );
		final Id<Link> link4 = Id.create( 4, Link.class );

		// plan 2
		// two passenger, two ODs
		final Map<Id<Person>, Plan> plans = new HashMap< >();
		PlanImpl plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));

		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		Leg driverLeg2 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link4 ),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger1.getId() , plan );

		plan = new PlanImpl( passenger2 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg passengerLeg2 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link4 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger2.getId() , plan );

		final DriverTrip driverTrip = new DriverTrip( driver.getId() );
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
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Before
	public void initTwoPassengersTwoOdsTwoJtFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );
		final Person passenger2 = new PersonImpl( Id.createPersonId( "Milou" ) );

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );
		final Id<Link> link4 = Id.create( 4 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		// plan 3
		// two passenger, two ODs, two JT for one passenger
		PlanImpl plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));

		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg driverLeg2 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link4 ),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "take a nap" , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg driverLeg3 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg3.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link1 ),
					Arrays.asList(
						passenger1.getId())));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "sunbath" , link4);
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg passengerLeg3 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link1 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg3.setRoute( passengerRoute2 );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger1.getId() , plan );

		plan = new PlanImpl( passenger2 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg passengerLeg2 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute3 = new PassengerRoute( link3 , link4 );
		passengerRoute3.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute3 );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger2.getId() , plan );

		final DriverTrip driverTrip = new DriverTrip( driver.getId() );
		driverTrip.driverTrip.add( driverLeg );
		driverTrip.driverTrip.add( driverLeg2 );
		driverTrip.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip.passengerDestinations.put( passenger1.getId() , link4 );
		driverTrip.passengerOrigins.put( passenger2.getId() , link3 );
		driverTrip.passengerDestinations.put( passenger2.getId() , link4 );

		final DriverTrip driverTrip2 = new DriverTrip( driver.getId() );
		driverTrip2.driverTrip.add( driverLeg3 );
		driverTrip2.passengerOrigins.put( passenger1.getId() , link3 );
		driverTrip2.passengerDestinations.put( passenger1.getId() , link1 );

		fixtures.add(
				new Fixture(
					"two passengers two trips",
					Arrays.asList( driverTrip , driverTrip2 ),
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
								passengerLeg2),
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg3 ),
								passenger1.getId(),
								passengerLeg3))),
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Before
	public void initTwoPassengersMiddleTripFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );
		final Person passenger2 = new PersonImpl( Id.createPersonId( "Milou" ) );

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );
		final Id<Link> link4 = Id.create( 4, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap<Id<Person>, Plan>();

		// plan 4
		// two passengers, "midle trip"
		PlanImpl plan = new PlanImpl( driver );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));

		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg driverLeg2 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link4 ),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		final Leg driverLeg3 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg3.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link4 , link1 ),
					Arrays.asList(
						passenger1.getId())));
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , plan );

		plan = new PlanImpl( passenger1 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link1);
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger1.getId() , plan );

		plan = new PlanImpl( passenger2 );
		plan.createAndAddActivity( "home" , link1 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg passengerLeg2 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link4 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "home" , link1);

		plans.put( passenger2.getId() , plan );

		final DriverTrip driverTrip = new DriverTrip( driver.getId() );
		driverTrip.driverTrip.add( driverLeg );
		driverTrip.driverTrip.add( driverLeg2 );
		driverTrip.driverTrip.add( driverLeg3 );
		driverTrip.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip.passengerDestinations.put( passenger1.getId() , link1 );
		driverTrip.passengerOrigins.put( passenger2.getId() , link3 );
		driverTrip.passengerDestinations.put( passenger2.getId() , link4 );

		fixtures.add(
				new Fixture(
					"two passengers middle trip",
					Arrays.asList( driverTrip ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg , driverLeg2 , driverLeg3 ),
								passenger1.getId(),
								passengerLeg),
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg2 ),
								passenger2.getId(),
								passengerLeg2))),
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Before
	public void initOnePassengerTwoTripsFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final PlanImpl dPlan = new PlanImpl( driver );
		dPlan.createAndAddActivity( "home" , link1 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg1 = dPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( "home" , link1 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg2 = dPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , dPlan );

		final PlanImpl pPlan = new PlanImpl( passenger1 );
		pPlan.createAndAddActivity( "home" , link1 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg1 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg2 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);


		plans.put( passenger1.getId() , pPlan );

		final DriverTrip driverTrip1 = new DriverTrip( driver.getId() );
		driverTrip1.driverTrip.add( driverLeg1 );
		driverTrip1.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip1.passengerDestinations.put( passenger1.getId() , link3 );

		final DriverTrip driverTrip2 = new DriverTrip( driver.getId() );
		driverTrip2.driverTrip.add( driverLeg2 );
		driverTrip2.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip2.passengerDestinations.put( passenger1.getId() , link3 );


		fixtures.add(
				new Fixture(
					"one passenger, two trips with same OD",
					Arrays.asList( driverTrip1 , driverTrip2 ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg1 ),
								passenger1.getId(),
								passengerLeg1),
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg2 ),
								passenger1.getId(),
								passengerLeg2)
							)),
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Before
	public void initOnePassengerTwoTripsWithDifferentDriversFixture() {
		final Person driver1 = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person driver2 = new PersonImpl( Id.createPersonId( "Michel Vaillant" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final PlanImpl d1Plan = new PlanImpl( driver1 );
		d1Plan.createAndAddActivity( "home" , link1 );
		d1Plan.createAndAddLeg( TransportMode.walk );
		d1Plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg1 = d1Plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		d1Plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		d1Plan.createAndAddLeg( TransportMode.walk );
		d1Plan.createAndAddActivity( "home" , link1 );

		plans.put( driver1.getId() , d1Plan );

		final PlanImpl d2Plan = new PlanImpl( driver2 );
		d2Plan.createAndAddActivity( "home" , link1 );
		d2Plan.createAndAddLeg( TransportMode.walk );
		d2Plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg2 = d2Plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		d2Plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		d2Plan.createAndAddLeg( TransportMode.walk );
		d2Plan.createAndAddActivity( "home" , link1 );

		plans.put( driver2.getId() , d2Plan );

		final PlanImpl pPlan = new PlanImpl( passenger1 );
		pPlan.createAndAddActivity( "home" , link1 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg1 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver1.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg2 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver2.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);


		plans.put( passenger1.getId() , pPlan );

		final DriverTrip driverTrip1 = new DriverTrip( driver1.getId() );
		driverTrip1.driverTrip.add( driverLeg1 );
		driverTrip1.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip1.passengerDestinations.put( passenger1.getId() , link3 );

		final DriverTrip driverTrip2 = new DriverTrip( driver2.getId() );
		driverTrip2.driverTrip.add( driverLeg2 );
		driverTrip2.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip2.passengerDestinations.put( passenger1.getId() , link3 );


		fixtures.add(
				new Fixture(
					"one passenger, two trips with same OD and different drivers",
					Arrays.asList( driverTrip1 , driverTrip2 ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver1.getId(),
								Arrays.asList( driverLeg1 ),
								passenger1.getId(),
								passengerLeg1),
							new JointTrip(
								driver2.getId(),
								Arrays.asList( driverLeg2 ),
								passenger1.getId(),
								passengerLeg2)
							)),
					new JointPlanFactory().createJointPlan( plans )));
	}

	// bugs may depend on iteration order...
	@Before
	public void initOnePassengerTwoTripsWithDifferentDriversSecondDriverFirstFixture() {
		final Person driver1 = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person driver2 = new PersonImpl( Id.createPersonId( "Michel Vaillant" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final PlanImpl d1Plan = new PlanImpl( driver1 );
		d1Plan.createAndAddActivity( "home" , link1 );
		d1Plan.createAndAddLeg( TransportMode.walk );
		d1Plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg1 = d1Plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		d1Plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		d1Plan.createAndAddLeg( TransportMode.walk );
		d1Plan.createAndAddActivity( "home" , link1 );

		plans.put( driver1.getId() , d1Plan );

		final PlanImpl d2Plan = new PlanImpl( driver2 );
		d2Plan.createAndAddActivity( "home" , link1 );
		d2Plan.createAndAddLeg( TransportMode.walk );
		d2Plan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg2 = d2Plan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		d2Plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		d2Plan.createAndAddLeg( TransportMode.walk );
		d2Plan.createAndAddActivity( "home" , link1 );

		plans.put( driver2.getId() , d2Plan );

		final PlanImpl pPlan = new PlanImpl( passenger1 );
		pPlan.createAndAddActivity( "home" , link1 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg1 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver2.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg2 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver1.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);


		plans.put( passenger1.getId() , pPlan );

		final DriverTrip driverTrip1 = new DriverTrip( driver1.getId() );
		driverTrip1.driverTrip.add( driverLeg1 );
		driverTrip1.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip1.passengerDestinations.put( passenger1.getId() , link3 );

		final DriverTrip driverTrip2 = new DriverTrip( driver2.getId() );
		driverTrip2.driverTrip.add( driverLeg2 );
		driverTrip2.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip2.passengerDestinations.put( passenger1.getId() , link3 );


		fixtures.add(
				new Fixture(
					"one passenger, two trips with same OD and different drivers, second driver first",
					Arrays.asList( driverTrip1 , driverTrip2 ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver2.getId(),
								Arrays.asList( driverLeg2 ),
								passenger1.getId(),
								passengerLeg1),
							new JointTrip(
								driver1.getId(),
								Arrays.asList( driverLeg1 ),
								passenger1.getId(),
								passengerLeg2)
							)),
					new JointPlanFactory().createJointPlan( plans )));
	}

	/**
	 * Link the first trip of the driver with the second of the passenger,
	 * and vice-versa. This is obbviously infeasible, but may be considered as valid,
	 * the same way going to the bus station when no bus is running is a valid, though stupid,
	 * plan.
	 */
	@Before
	public void initOnePassengerTwoTripsInconsistentSequenceFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alain Prost" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Tintin" ) );

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final PlanImpl dPlan = new PlanImpl( driver );
		dPlan.createAndAddActivity( "home" , link1 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg driverLeg1 = dPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link2 , link3 ),
					Arrays.asList( passenger1.getId() )));
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( "home" , link1 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg driverLeg2 = dPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					new LinkNetworkRouteImpl( link3 , link2 ),
					Arrays.asList( passenger1.getId() )));
		dPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		dPlan.createAndAddLeg( TransportMode.walk );
		dPlan.createAndAddActivity( "home" , link1 );

		plans.put( driver.getId() , dPlan );

		final PlanImpl pPlan = new PlanImpl( passenger1 );
		pPlan.createAndAddActivity( "home" , link1 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg passengerLeg1 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link3 , link2 );
		passengerRoute1.setDriverId( driver.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg passengerLeg2 = pPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		pPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		pPlan.createAndAddLeg( TransportMode.walk );
		pPlan.createAndAddActivity( "home" , link1);


		plans.put( passenger1.getId() , pPlan );

		final DriverTrip driverTrip1 = new DriverTrip( driver.getId() );
		driverTrip1.driverTrip.add( driverLeg1 );
		driverTrip1.passengerOrigins.put( passenger1.getId() , link2 );
		driverTrip1.passengerDestinations.put( passenger1.getId() , link3 );

		final DriverTrip driverTrip2 = new DriverTrip( driver.getId() );
		driverTrip2.driverTrip.add( driverLeg2 );
		driverTrip2.passengerOrigins.put( passenger1.getId() , link3 );
		driverTrip2.passengerDestinations.put( passenger1.getId() , link2 );


		fixtures.add(
				new Fixture(
					"one passenger, two trips with incorrect sequence",
					Arrays.asList( driverTrip1 , driverTrip2 ),
					new JointTravelStructure(
						Arrays.asList(
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg1 ),
								passenger1.getId(),
								passengerLeg2),
							new JointTrip(
								driver.getId(),
								Arrays.asList( driverLeg2 ),
								passenger1.getId(),
								passengerLeg1)
							)),
					new JointPlanFactory().createJointPlan( plans )));
	}

	@Test
	public void testExtractJointTrips() throws Exception {
		for ( Fixture f : fixtures ) {
			JointTravelStructure struct = JointPlanUtils.analyseJointTravel( f.plan );

			Assert.assertEquals(
					"wrong structure for fixture "+f.name+
					" of size "+f.structure.getJointTrips().size()+
					" compared to result of size "+struct.getJointTrips().size(),
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

