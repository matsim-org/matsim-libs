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
package org.matsim.contrib.socnetsim.jointtrips;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.DriverTrip;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTravelStructure;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTrip;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;

import java.util.*;

/**
 * @author thibautd
 */
public class JointTravelUtilsTest {
	private final List<Fixture> fixtures = new ArrayList<Fixture>();

	@AfterEach
	public void clearFixtures() {
		fixtures.clear();
	}

	@BeforeEach
	public void initOnePassengerFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));

		final Id<Link> link1 = Id.create( 1 , Link.class);
		final Id<Link> link2 = Id.create( 2 , Link.class);
		final Id<Link> link3 = Id.create( 3 , Link.class);

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		// plan 1
		// just one passenger
		Plan plan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		Leg driverLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( driver.getId() , plan );

		plan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		Leg passengerLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		PassengerRoute passengerRoute = new PassengerRoute( link2 , link3 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

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

	@BeforeEach
	public void initTwoPassengerTwoOdFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.create("Alain Prost", Person.class));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.create("Tintin", Person.class));
		final Person passenger2 = PopulationUtils.getFactory().createPerson(Id.create("Milou", Person.class));

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );
		final Id<Link> link4 = Id.create( 4, Link.class );

		// plan 2
		// two passenger, two ODs
		final Map<Id<Person>, Plan> plans = new HashMap< >();
		Plan plan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));

		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		Leg driverLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link3, link4),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( driver.getId() , plan );

		plan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( passenger1.getId() , plan );

		plan = PopulationUtils.createPlan(passenger2);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link4 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

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

	@BeforeEach
	public void initTwoPassengersTwoOdsTwoJtFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));
		final Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Milou"));

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );
		final Id<Link> link4 = Id.create( 4 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		// plan 3
		// two passenger, two ODs, two JT for one passenger
		Plan plan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));

		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link3, link4),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "take a nap", link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg driverLeg3 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg3.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link3, link1),
					Arrays.asList(
						passenger1.getId())));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( driver.getId() , plan );

		plan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link4 );
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "sunbath", link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg passengerLeg3 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link1 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg3.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( passenger1.getId() , plan );

		plan = PopulationUtils.createPlan(passenger2);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute3 = new PassengerRoute( link3 , link4 );
		passengerRoute3.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute3 );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

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

	@BeforeEach
	public void initTwoPassengersMiddleTripFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));
		final Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Milou"));

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );
		final Id<Link> link4 = Id.create( 4, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap<Id<Person>, Plan>();

		// plan 4
		// two passengers, "midle trip"
		Plan plan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));

		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link3, link4),
					Arrays.asList(
						passenger1.getId(),
						passenger2.getId())));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		final Leg driverLeg3 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
		driverLeg3.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link4, link1),
					Arrays.asList(
						passenger1.getId())));
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( driver.getId() , plan );

		plan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute = new PassengerRoute( link2 , link1);
		passengerRoute.setDriverId( driver.getId() );
		passengerLeg.setRoute( passengerRoute );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

		plans.put( passenger1.getId() , plan );

		plan = PopulationUtils.createPlan(passenger2);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link3);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link3 , link4 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, link4);
		PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", link1);

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

	@BeforeEach
	public void initOnePassengerTwoTripsFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final Plan dPlan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg1 = PopulationUtils.createAndAddLeg( dPlan, JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( dPlan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);

		plans.put( driver.getId() , dPlan );

		final Plan pPlan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg1 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);


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

	@BeforeEach
	public void initOnePassengerTwoTripsWithDifferentDriversFixture() {
		final Person driver1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person driver2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Michel Vaillant"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final Plan d1Plan = PopulationUtils.createPlan(driver1);
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, "home", link1);
		PopulationUtils.createAndAddLeg( d1Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg1 = PopulationUtils.createAndAddLeg( d1Plan, JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( d1Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, "home", link1);

		plans.put( driver1.getId() , d1Plan );

		final Plan d2Plan = PopulationUtils.createPlan(driver2);
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, "home", link1);
		PopulationUtils.createAndAddLeg( d2Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( d2Plan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( d2Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, "home", link1);

		plans.put( driver2.getId() , d2Plan );

		final Plan pPlan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg1 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver1.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver2.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);


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
	@BeforeEach
	public void initOnePassengerTwoTripsWithDifferentDriversSecondDriverFirstFixture() {
		final Person driver1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person driver2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Michel Vaillant"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final Plan d1Plan = PopulationUtils.createPlan(driver1);
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, "home", link1);
		PopulationUtils.createAndAddLeg( d1Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg1 = PopulationUtils.createAndAddLeg( d1Plan, JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( d1Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d1Plan, "home", link1);

		plans.put( driver1.getId() , d1Plan );

		final Plan d2Plan = PopulationUtils.createPlan(driver2);
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, "home", link1);
		PopulationUtils.createAndAddLeg( d2Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( d2Plan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( d2Plan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(d2Plan, "home", link1);

		plans.put( driver2.getId() , d2Plan );

		final Plan pPlan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg1 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link2 , link3 );
		passengerRoute1.setDriverId( driver2.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver1.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);


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
	@BeforeEach
	public void initOnePassengerTwoTripsInconsistentSequenceFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alain Prost"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Tintin"));

		final Id<Link> link1 = Id.create( 1, Link.class );
		final Id<Link> link2 = Id.create( 2, Link.class );
		final Id<Link> link3 = Id.create( 3, Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();

		final Plan dPlan = PopulationUtils.createPlan(driver);
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link2);
		final Leg driverLeg1 = PopulationUtils.createAndAddLeg( dPlan, JointActingTypes.DRIVER );
		driverLeg1.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link2, link3),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link3);
		final Leg driverLeg2 = PopulationUtils.createAndAddLeg( dPlan, JointActingTypes.DRIVER );
		driverLeg2.setRoute( new DriverRoute(
					RouteUtils.createLinkNetworkRouteImpl(link3, link2),
					Arrays.asList( passenger1.getId() )));
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, JointActingTypes.INTERACTION, link2);
		PopulationUtils.createAndAddLeg( dPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(dPlan, "home", link1);

		plans.put( driver.getId() , dPlan );

		final Plan pPlan = PopulationUtils.createPlan(passenger1);
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		final Leg passengerLeg1 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute1 = new PassengerRoute( link3 , link2 );
		passengerRoute1.setDriverId( driver.getId() );
		passengerLeg1.setRoute( passengerRoute1 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link2);
		final Leg passengerLeg2 = PopulationUtils.createAndAddLeg( pPlan, JointActingTypes.PASSENGER );
		final PassengerRoute passengerRoute2 = new PassengerRoute( link2 , link3 );
		passengerRoute2.setDriverId( driver.getId() );
		passengerLeg2.setRoute( passengerRoute2 );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( pPlan, TransportMode.walk );
		PopulationUtils.createAndAddActivityFromLinkId(pPlan, "home", link1);


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
	void testExtractJointTrips() throws Exception {
		for ( Fixture f : fixtures ) {
			JointTravelStructure struct = JointTravelUtils.analyseJointTravel(f.plan);

			Assertions.assertEquals(
					f.structure,
					struct,
					"wrong structure for fixture "+f.name+
					" of size "+f.structure.getJointTrips().size()+
					" compared to result of size "+struct.getJointTrips().size());
		}
	}

	@Test
	void testParseDriverTrips() throws Exception {
		for ( Fixture f : fixtures ) {
			List<DriverTrip> trips = JointTravelUtils.parseDriverTrips(f.plan);

			Assertions.assertEquals(
					f.driverTrips.size(),
					trips.size(),
					"wrong number of driver trips: "+f.driverTrips+" is the target, got "+trips);

			Assertions.assertTrue(
					trips.containsAll( f.driverTrips ),
					"wrong driver trips: "+f.driverTrips+" is the target, got "+trips);
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

