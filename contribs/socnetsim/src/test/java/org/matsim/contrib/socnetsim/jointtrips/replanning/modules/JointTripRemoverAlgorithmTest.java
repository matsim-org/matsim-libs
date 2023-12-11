/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRemoverAlgorithmTest.java
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifierImpl;

import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTrip;

/**
 * @author thibautd
 */
public class JointTripRemoverAlgorithmTest {
	private static final Logger log =
		LogManager.getLogger(JointTripRemoverAlgorithmTest.class);

	private List<Fixture> fixtures;

	// /////////////////////////////////////////////////////////////////////////
	// init routines
	// /////////////////////////////////////////////////////////////////////////
	@BeforeEach
	public void initFixtures() {
		fixtures = new ArrayList<Fixture>();

		fixtures.add( createSimplisticFixture() );
		fixtures.add( createTwoPassengersFixture() );
		fixtures.add( createTwoPassengersFixtureWithInternOverlap() );
		fixtures.add( createTwoPassengersFixtureWithExternOverlap() );
		fixtures.add( createMultiPassengerStageFixture() );
		fixtures.add( createMultiDriverStageFixture() );
		fixtures.add( createTwoPassengersInDifferentTripsRemoveFirstFixture() );
		fixtures.add( createTwoPassengersInDifferentTripsRemoveSecondFixture() );
		fixtures.add( createTwoDriversFixture( true ) );
		fixtures.add( createTwoDriversFixture( false ) );
	}

	private Fixture createSimplisticFixture() {
		Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Schumacher"));
		Person passenger = PopulationUtils.getFactory().createPerson(Id.createPersonId("Asterix"));

		Id<Link> link1 = Id.createLinkId( 1 );
		Id<Link> link2 = Id.createLinkId( 2 );
		Id<Link> link3 = Id.createLinkId( 3 );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );
		final Id<Link> linkId = link1;

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId);
		PopulationUtils.createAndAddLeg( driverPlan, "horse" );
		final Id<Link> linkId1 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId1);
		Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId2 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId2);
		PopulationUtils.createAndAddLeg( driverPlan, "unicycle" );
		final Id<Link> linkId3 = link1;
		Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId3);

		Plan passengerPlan = PopulationUtils.createPlan(passenger);
		plans.put( passenger.getId() , passengerPlan );
		final Id<Link> linkId4 = link1;

		Activity pAct1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", linkId4);
		PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		final Id<Link> linkId5 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, linkId5);
		Leg jointPassengerLeg = PopulationUtils.createAndAddLeg( passengerPlan, JointActingTypes.PASSENGER );
		final Id<Link> linkId6 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, linkId6);
		PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		final Id<Link> linkId7 = link1;
		Activity pAct2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", linkId7);

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , PopulationUtils.createLeg(TransportMode.pt) , pAct2 ));

		return new Fixture(
				"one passenger",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg)
				);
	}

	private Fixture createTwoPassengersFixture() {
		Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alonso"));
		Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Boule"));
		Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Bill"));

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );
		final Id<Link> linkId = link1;

		Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId);
		PopulationUtils.createAndAddLeg( driverPlan, "skateboard" );
		final Id<Link> linkId1 = link2;
		Activity dPu = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId1);
		Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId2 = link3;
		Activity dDo = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId2);
		PopulationUtils.createAndAddLeg( driverPlan, "elevator" );
		final Id<Link> linkId3 = link1;
		Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId3);

		Plan passengerPlan1 = PopulationUtils.createPlan(passenger1);
		plans.put( passenger1.getId() , passengerPlan1 );
		final Id<Link> linkId4 = link1;

		Activity p1Act1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId4);
		PopulationUtils.createAndAddLeg( passengerPlan1, "jetpack" );
		final Id<Link> linkId5 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId5);
		Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan1, JointActingTypes.PASSENGER );
		final Id<Link> linkId6 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId6);
		PopulationUtils.createAndAddLeg( passengerPlan1, "paraglider" );
		final Id<Link> linkId7 = link1;
		Activity p1Act2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId7);

		Plan passengerPlan2 = PopulationUtils.createPlan(passenger2);
		plans.put( passenger2.getId() , passengerPlan2 );
		final Id<Link> linkId8 = link1;

		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId8);
		PopulationUtils.createAndAddLeg( passengerPlan2, "jetpack" );
		final Id<Link> linkId9 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId9);
		Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan2, JointActingTypes.PASSENGER );
		final Id<Link> linkId10 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId10);
		PopulationUtils.createAndAddLeg( passengerPlan2, "paraglider" );
		final Id<Link> linkId11 = link1;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId11);

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger1.getId() );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );
		jointPassengerLeg2.setRoute( pRoute.clone()	);

		Leg expectedDriverLeg = PopulationUtils.createLeg(JointActingTypes.DRIVER);
		DriverRoute expDRoute = dRoute.clone();
		expDRoute.removePassenger( passenger1.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dPu ,
					expectedDriverLeg,
					dDo , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , PopulationUtils.createLeg(TransportMode.pt) , p1Act2 ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				new ArrayList<PlanElement>( passengerPlan2.getPlanElements() ));

		return new Fixture(
				"two passengers full overlap",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger1.getId(),
					jointPassengerLeg1)
				);
	}

	private Fixture createTwoPassengersFixtureWithInternOverlap() {
		Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Prost"));
		Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Joe"));
		Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Avrell"));

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );
		Id<Link> link4 = Id.create( 4 , Link.class );
		Id<Link> link5 = Id.create( 5 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );
		final Id<Link> linkId = link1;

		Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId);
		PopulationUtils.createAndAddLeg( driverPlan, "Rollerblade" );
		final Id<Link> linkId1 = link2;
		Activity dPu1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId1);
		Leg jointDriverLeg1 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId2 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId2);
		Leg jointDriverLeg2 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId3 = link4;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId3);
		Leg jointDriverLeg3 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId4 = link5;
		Activity dDo2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId4);
		PopulationUtils.createAndAddLeg( driverPlan, "iceskate" );
		final Id<Link> linkId5 = link1;
		Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId5);

		Plan passengerPlan1 = PopulationUtils.createPlan(passenger1);
		plans.put( passenger1.getId() , passengerPlan1 );
		final Id<Link> linkId6 = link1;

		Activity p1Act1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId6);
		PopulationUtils.createAndAddLeg( passengerPlan1, "kayak" );
		final Id<Link> linkId7 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId7);
		Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan1, JointActingTypes.PASSENGER );
		final Id<Link> linkId8 = link4;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId8);
		PopulationUtils.createAndAddLeg( passengerPlan1, "submarine" );
		final Id<Link> linkId9 = link1;
		Activity p1Act2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId9);

		Plan passengerPlan2 = PopulationUtils.createPlan(passenger2);
		plans.put( passenger2.getId() , passengerPlan2 );
		final Id<Link> linkId10 = link1;

		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId10);
		PopulationUtils.createAndAddLeg( passengerPlan2, "spitfire" );
		final Id<Link> linkId11 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId11);
		Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan2, JointActingTypes.PASSENGER );
		final Id<Link> linkId12 = link5;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId12);
		PopulationUtils.createAndAddLeg( passengerPlan2, "deltaplane" );
		final Id<Link> linkId13 = link1;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId13);

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg1.setRoute( dRoute );
		dRoute = new DriverRoute( link3 , link4 );
		dRoute.addPassenger( passenger1.getId() );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg2.setRoute( dRoute );
		dRoute = new DriverRoute( link4 , link5 );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg3.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link3 , link4 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );

		pRoute = new PassengerRoute( link2 , link5 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg2.setRoute( pRoute	);

		Leg expectedDriverLeg = PopulationUtils.createLeg(JointActingTypes.DRIVER);
		DriverRoute expDRoute = new DriverRoute( link2 , link5 );
		expDRoute.addPassenger( passenger2.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dPu1 ,
					expectedDriverLeg,
					dDo2 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , PopulationUtils.createLeg(TransportMode.pt) , p1Act2 ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				new ArrayList<PlanElement>( passengerPlan2.getPlanElements() ));

		return new Fixture(
				"two passengers intern overlap",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg2 ),
					passenger1.getId(),
					jointPassengerLeg1)
				);
	}

	private Fixture createTwoPassengersFixtureWithExternOverlap() {
		Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Kowalski"));
		Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Pif"));
		Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Paf"));

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );
		Id<Link> link4 = Id.create( 4 , Link.class );
		Id<Link> link5 = Id.create( 5 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );
		final Id<Link> linkId = link1;

		Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId);
		PopulationUtils.createAndAddLeg( driverPlan, "poney" );
		final Id<Link> linkId1 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId1);
		Leg jointDriverLeg1 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId2 = link3;
		Activity dPu2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId2);
		Leg jointDriverLeg2 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId3 = link4;
		Activity dDo1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId3);
		Leg jointDriverLeg3 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Id<Link> linkId4 = link5;
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, linkId4);
		PopulationUtils.createAndAddLeg( driverPlan, "donkey" );
		final Id<Link> linkId5 = link1;
		Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", linkId5);

		Plan passengerPlan1 = PopulationUtils.createPlan(passenger1);
		plans.put( passenger1.getId() , passengerPlan1 );
		final Id<Link> linkId6 = link1;

		Activity p1Act1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId6);
		PopulationUtils.createAndAddLeg( passengerPlan1, "cablecar" );
		final Id<Link> linkId7 = link2;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId7);
		Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan1, JointActingTypes.PASSENGER );
		final Id<Link> linkId8 = link5;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, linkId8);
		PopulationUtils.createAndAddLeg( passengerPlan1, "ski" );
		final Id<Link> linkId9 = link1;
		Activity p1Act2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", linkId9);

		Plan passengerPlan2 = PopulationUtils.createPlan(passenger2);
		plans.put( passenger2.getId() , passengerPlan2 );
		final Id<Link> linkId10 = link1;

		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId10);
		PopulationUtils.createAndAddLeg( passengerPlan2, "hand walking" );
		final Id<Link> linkId11 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId11);
		Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan2, JointActingTypes.PASSENGER );
		final Id<Link> linkId12 = link4;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, linkId12);
		PopulationUtils.createAndAddLeg( passengerPlan2, "jumps" );
		final Id<Link> linkId13 = link1;
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", linkId13);

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger1.getId() );
		jointDriverLeg1.setRoute( dRoute );
		dRoute = new DriverRoute( link3 , link4 );
		dRoute.addPassenger( passenger1.getId() );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg2.setRoute( dRoute );
		dRoute = new DriverRoute( link4 , link5 );
		dRoute.addPassenger( passenger1.getId() );
		jointDriverLeg3.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link2 , link5 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );

		pRoute = new PassengerRoute( link2 , link5 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg2.setRoute( pRoute	);

		Leg expectedDriverLeg = PopulationUtils.createLeg(JointActingTypes.DRIVER);
		DriverRoute expDRoute = new DriverRoute( link4 , link5 );
		expDRoute.addPassenger( passenger2.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dPu2 ,
					expectedDriverLeg,
					dDo1 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , PopulationUtils.createLeg(TransportMode.pt) , p1Act2 ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				new ArrayList<PlanElement>( passengerPlan2.getPlanElements() ));

		return new Fixture(
				"two passengers extern overlap",
				new JointPlanFactory().createJointPlan(plans),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg1 , jointDriverLeg2 , jointDriverLeg3 ),
					passenger1.getId(),
					jointPassengerLeg1)
				);
	}

	private Fixture createMultiDriverStageFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Schumacher"));
		final Person passenger = PopulationUtils.getFactory().createPerson(Id.createPersonId("Asterix"));
		final String stageType = "drinkACoffee interaction";

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		final Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);
		PopulationUtils.createAndAddLeg( driverPlan, "horse" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, stageType, link1);
		PopulationUtils.createAndAddLeg( driverPlan, "horse" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( driverPlan, "unicycle" );
		PopulationUtils.createAndAddLeg( driverPlan, "unicycle" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, stageType, link1);
		PopulationUtils.createAndAddLeg( driverPlan, "unicycle" );
		final Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);

		final Plan passengerPlan = PopulationUtils.createPlan(passenger);
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", link1);
		PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointPassengerLeg = PopulationUtils.createAndAddLeg( passengerPlan, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		final Activity pAct2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", link1);

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , PopulationUtils.createLeg(TransportMode.pt) , pAct2 ));

		return new Fixture(
				"complex access trip driver",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg)
				);
	}

	private Fixture createMultiPassengerStageFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Schumacher"));
		final Person passenger = PopulationUtils.getFactory().createPerson(Id.createPersonId("Asterix"));
		final String stageType = "drinkACoffee interaction";

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		final Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);
		PopulationUtils.createAndAddLeg( driverPlan, "horse" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( driverPlan, "unicycle" );
		final Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);

		final Plan passengerPlan = PopulationUtils.createPlan(passenger);
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", link1);
		PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, stageType, link1);
		PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointPassengerLeg = PopulationUtils.createAndAddLeg( passengerPlan, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, stageType, link1);
		PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		final Activity pAct2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", link1);

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , PopulationUtils.createLeg(TransportMode.pt) , pAct2 ));

		Set<String> stageActivityTypes = new HashSet<>();
		stageActivityTypes.add(stageType);

		return new Fixture(
				"complex access trip passenger",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg)
				);
	}

	private Fixture createTwoPassengersInDifferentTripsRemoveFirstFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alonso"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Boule"));
		final Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Bill"));

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );

		final Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);
		PopulationUtils.createAndAddLeg( driverPlan, "skateboard" );
		/*final Activity dPu =*/ PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		/*final Activity dDo =*/ PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( driverPlan, "elevator" );
		final Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);
		final Leg dAccess2 = PopulationUtils.createAndAddLeg( driverPlan, "skateboard" );
		final Activity dPu2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link2);
		final Leg jointDriverLeg2 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Activity dDo2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, link3);
		final Leg dEgress2 = PopulationUtils.createAndAddLeg( driverPlan, "elevator" );
		final Activity dAct3 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", link1);

		final Plan passengerPlan1 = PopulationUtils.createPlan(passenger1);
		plans.put( passenger1.getId() , passengerPlan1 );

		final Activity p1Act1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", link1);
		PopulationUtils.createAndAddLeg( passengerPlan1, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, link2);
		final Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan1, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( passengerPlan1, "paraglider" );
		final Activity p1Act2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", link1);

		final Plan passengerPlan2 = PopulationUtils.createPlan(passenger2);
		plans.put( passenger2.getId() , passengerPlan2 );

		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", link1);
		PopulationUtils.createAndAddLeg( passengerPlan2, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, link2);
		final Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan2, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, link3);
		PopulationUtils.createAndAddLeg( passengerPlan2, "paraglider" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", link1);

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger1.getId() );
		jointDriverLeg.setRoute( dRoute );

		final DriverRoute dRoute2 = new DriverRoute( link2 , link3 );
		dRoute2.addPassenger( passenger2.getId() );
		jointDriverLeg2.setRoute( dRoute2 );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );
		jointPassengerLeg2.setRoute( pRoute.clone()	);

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , PopulationUtils.createLeg(TransportMode.car) , dAct2 ,
					dAccess2 , dPu2 , jointDriverLeg2 , dDo2 , dEgress2 , dAct3 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , PopulationUtils.createLeg(TransportMode.pt) , p1Act2 ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				new ArrayList<PlanElement>( passengerPlan2.getPlanElements() ));

		return new Fixture(
				"two passengers different trips remove first",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger1.getId(),
					jointPassengerLeg1)
				);
	}

	private Fixture createTwoPassengersInDifferentTripsRemoveSecondFixture() {
		final Person driver = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alonso"));
		final Person passenger1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Boule"));
		final Person passenger2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Bill"));

		final Id link1 = Id.createLinkId( 1 );
		final Id link2 = Id.createLinkId( 2 );
		final Id link3 = Id.createLinkId( 3 );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final Plan driverPlan = PopulationUtils.createPlan(driver);
		plans.put( driver.getId() , driverPlan );

		final Activity dAct1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", (Id<Link>) link1);
		final Leg dAccess = PopulationUtils.createAndAddLeg( driverPlan, "skateboard" );
		final Activity dPu = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointDriverLeg = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		final Activity dDo = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, (Id<Link>) link3);
		final Leg dEgress = PopulationUtils.createAndAddLeg( driverPlan, "elevator" );
		final Activity dAct2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", (Id<Link>) link1);
		PopulationUtils.createAndAddLeg( driverPlan, "skateboard" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointDriverLeg2 = PopulationUtils.createAndAddLeg( driverPlan, JointActingTypes.DRIVER );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan, JointActingTypes.INTERACTION, (Id<Link>) link3);
		PopulationUtils.createAndAddLeg( driverPlan, "elevator" );
		final Activity dAct3 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan, "home", (Id<Link>) link1);

		final Plan passengerPlan1 = PopulationUtils.createPlan(passenger1);
		plans.put( passenger1.getId() , passengerPlan1 );

		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", (Id<Link>) link1);
		PopulationUtils.createAndAddLeg( passengerPlan1, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan1, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, JointActingTypes.INTERACTION, (Id<Link>) link3);
		PopulationUtils.createAndAddLeg( passengerPlan1, "paraglider" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan1, "home", (Id<Link>) link1);

		final Plan passengerPlan2 = PopulationUtils.createPlan(passenger2);
		plans.put( passenger2.getId() , passengerPlan2 );

		final Activity p2Act1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", (Id<Link>) link1);
		PopulationUtils.createAndAddLeg( passengerPlan2, "jetpack" );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan2, JointActingTypes.PASSENGER );
		PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, JointActingTypes.INTERACTION, (Id<Link>) link3);
		PopulationUtils.createAndAddLeg( passengerPlan2, "paraglider" );
		final Activity p2Act2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan2, "home", (Id<Link>) link1);

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger1.getId() );
		jointDriverLeg.setRoute( dRoute );

		final DriverRoute dRoute2 = new DriverRoute( link2 , link3 );
		dRoute2.addPassenger( passenger2.getId() );
		jointDriverLeg2.setRoute( dRoute2 );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );
		jointPassengerLeg2.setRoute( pRoute.clone()	);

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 ,
					dAccess , dPu , jointDriverLeg , dDo , dEgress , dAct2 ,
					PopulationUtils.createLeg(TransportMode.car) , dAct3 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				new ArrayList<PlanElement>( passengerPlan1.getPlanElements() ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				Arrays.asList( p2Act1 , PopulationUtils.createLeg(TransportMode.pt) , p2Act2 ));

		return new Fixture(
				"two passengers different trips remove second",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg2 ),
					passenger2.getId(),
					jointPassengerLeg2)
				);
	}

	private Fixture createTwoDriversFixture(final boolean removeFirst) {
		final Person driver1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Alonso"));
		final Person driver2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("Schumacher"));
		final Person passenger = PopulationUtils.getFactory().createPerson(Id.createPersonId("Rantanplan"));

		final Id link1 = Id.createLinkId( 1 );
		final Id link2 = Id.createLinkId( 2 );
		final Id link3 = Id.createLinkId( 3 );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final Plan driverPlan1 = PopulationUtils.createPlan(driver1);
		plans.put( driver1.getId() , driverPlan1 );

		final Activity d1Act1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan1, "home", (Id<Link>) link1);
		PopulationUtils.createAndAddLeg( driverPlan1, "skateboard" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan1, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointDriverLeg1 = PopulationUtils.createAndAddLeg( driverPlan1, JointActingTypes.DRIVER );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan1, JointActingTypes.INTERACTION, (Id<Link>) link3);
		PopulationUtils.createAndAddLeg( driverPlan1, "elevator" );
		final Activity d1Act2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan1, "home", (Id<Link>) link1);
		final Leg d1Leg = PopulationUtils.createAndAddLeg( driverPlan1, "skateboard" );
		final Activity d1Act3 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan1, "home", (Id<Link>) link1);

		final Plan driverPlan2 = PopulationUtils.createPlan(driver2);
		plans.put( driver2.getId() , driverPlan2 );

		final Activity d2Act1 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan2, "home", (Id<Link>) link1);
		final Leg d2Leg = PopulationUtils.createAndAddLeg( driverPlan2, "skateboard" );
		final Activity d2Act2 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan2, "home", (Id<Link>) link1);
		PopulationUtils.createAndAddLeg( driverPlan2, "skateboard" );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan2, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointDriverLeg2 = PopulationUtils.createAndAddLeg( driverPlan2, JointActingTypes.DRIVER );
		PopulationUtils.createAndAddActivityFromLinkId(driverPlan2, JointActingTypes.INTERACTION, (Id<Link>) link3);
		PopulationUtils.createAndAddLeg( driverPlan2, "elevator" );
		final Activity d2Act3 = PopulationUtils.createAndAddActivityFromLinkId(driverPlan2, "home", (Id<Link>) link1);

		final Plan passengerPlan = PopulationUtils.createPlan(passenger);
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", (Id<Link>) link1);
		final Leg pAccess1 = PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		final Activity pPu1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointPassengerLeg1 = PopulationUtils.createAndAddLeg( passengerPlan, JointActingTypes.PASSENGER );
		final Activity pDo1 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, (Id<Link>) link3);
		final Leg pEgress1 = PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		final Activity pAct2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", (Id<Link>) link1);
		final Leg pAccess2 = PopulationUtils.createAndAddLeg( passengerPlan, "jetpack" );
		final Activity pPu2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, (Id<Link>) link2);
		final Leg jointPassengerLeg2 = PopulationUtils.createAndAddLeg( passengerPlan, JointActingTypes.PASSENGER );
		final Activity pDo2 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, JointActingTypes.INTERACTION, (Id<Link>) link3);
		final Leg pEgress2 = PopulationUtils.createAndAddLeg( passengerPlan, "paraglider" );
		final Activity pAct3 = PopulationUtils.createAndAddActivityFromLinkId(passengerPlan, "home", (Id<Link>) link1);

		final DriverRoute dRoute1 = new DriverRoute( link2 , link3 );
		dRoute1.addPassenger( passenger.getId() );
		jointDriverLeg1.setRoute( dRoute1 );

		final DriverRoute dRoute2 = new DriverRoute( link2 , link3 );
		dRoute2.addPassenger( passenger.getId() );
		jointDriverLeg2.setRoute( dRoute2 );

		final PassengerRoute pRoute1 = new PassengerRoute( link2 , link3 );
		pRoute1.setDriverId( driver1.getId() );
		jointPassengerLeg1.setRoute( pRoute1 );

		final PassengerRoute pRoute2 = new PassengerRoute( link2 , link3 );
		pRoute2.setDriverId( driver2.getId() );
		jointPassengerLeg2.setRoute( pRoute2 );

		if ( removeFirst ) {
			expectedAfterRemoval.put(
					driver1.getId(),
					Arrays.asList( d1Act1 , PopulationUtils.createLeg(TransportMode.car) , d1Act2 ,
						d1Leg , d1Act3 ));

			expectedAfterRemoval.put(
					driver2.getId(),
					new ArrayList<PlanElement>( driverPlan2.getPlanElements() ) );

			expectedAfterRemoval.put(
					passenger.getId(),
					Arrays.asList( pAct1 , PopulationUtils.createLeg(TransportMode.pt) , pAct2 ,
						pAccess2 , pPu2 , jointPassengerLeg2 , pDo2 , pEgress2 , pAct3 ));

			return new Fixture(
					"two drivers remove first",
					new JointPlanFactory().createJointPlan( plans ),
					expectedAfterRemoval,
					new JointTrip(
						driver1.getId(),
						Arrays.asList( jointDriverLeg1 ),
						passenger.getId(),
						jointPassengerLeg1)
					);
		}

		assert !removeFirst;
		expectedAfterRemoval.put(
				driver1.getId(),
				new ArrayList<PlanElement>( driverPlan1.getPlanElements() ) );

		expectedAfterRemoval.put(
				driver2.getId(),
				Arrays.asList( d2Act1 , d2Leg , d2Act2 ,
					PopulationUtils.createLeg(TransportMode.car) , d2Act3 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , pAccess1 , pPu1 , jointPassengerLeg1 , pDo1 , pEgress1 ,
					pAct2 , PopulationUtils.createLeg(TransportMode.pt) , pAct3 ));

		return new Fixture(
				"two drivers remove second",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver2.getId(),
					Arrays.asList( jointDriverLeg2 ),
					passenger.getId(),
					jointPassengerLeg2)
				);

	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	void testRemoval() throws Exception {
		// TODO: test driver and passenger removal separately
		for ( Fixture f : fixtures ) {
			log.info( "testing removal on fixture "+f.name );
			final JointTripRemoverAlgorithm algo = new JointTripRemoverAlgorithm( null , new MainModeIdentifierImpl() );
			algo.removePassengerTrip( f.toRemove , f.jointPlan );
			algo.removeDriverTrip( f.toRemove , f.jointPlan );

			for ( Plan p : f.jointPlan.getIndividualPlans().values() ) {
				assertChainsMatch(
						f.name,
						f.expectedPlanAfterRemoval.get( p.getPerson().getId() ),
						p.getPlanElements() );
			}
		}
	}
	private static void print( JointTrip trips ) {
		{
			StringBuilder msg = new StringBuilder();
			msg.append( "driverLegs=" ) ;
			for( Leg leg : trips.getDriverLegs() ){
				msg.append( "| " ).append( leg.getMode() ).append( " |" );
			}
			log.info( msg.toString() );
		}
		{
			log.info(  "passengerLeg=" + trips.getPassengerLeg().getMode() );
		}
	}
	private static void print( JointPlan plans ){
		for( Plan plan : plans.getIndividualPlans().values() ){
			StringBuilder msg = new StringBuilder();
			for( PlanElement planElement : plan.getPlanElements() ){
				if ( planElement instanceof Activity ){
					msg.append( "| " ).append( ((Activity) planElement).getType() ).append( " |" );
				} else if ( planElement instanceof Leg ) {
					msg.append( "| " ).append( ((Leg) planElement).getMode() ).append( " |" );
				}
			}
			log.info( msg.toString() );
		}
	}

	private void assertChainsMatch(
			final String fixtureName,
			final List<PlanElement> expected,
			final List<PlanElement> actual) {
		assertEquals(
				expected.size(),
				actual.size(),
				fixtureName+": sizes do not match "+expected+" and "+actual);

		Iterator<PlanElement> expectedIter = expected.iterator();
		Iterator<PlanElement> actualIter = actual.iterator();

		while (expectedIter.hasNext()) {
			PlanElement expElement = expectedIter.next();
			PlanElement actElement = actualIter.next();

			if (expElement instanceof Activity) {
				Activity exp = (Activity) expElement;
				Activity act = null;
				try {
					act = (Activity) actElement;
				}
				catch (ClassCastException e) {
					fail( fixtureName+": expected activity, got leg: "+exp+", "+actElement );
				}
				assertActivitiesMatch( exp , act );
			}
			else {
				Leg exp = (Leg) expElement;
				Leg act = null;
				try {
					act = (Leg) actElement;
				}
				catch (ClassCastException e) {
					fail( fixtureName+": expected leg, got activity: "+exp+", "+actElement );
				}
				assertLegsMatch( exp , act );
			}
		}
	}

	private void assertLegsMatch(final Leg exp,final Leg act) {
		assertEquals(
				exp.getMode(),
				act.getMode(),
				"wrong mode");

		if ( exp.getMode().equals( JointActingTypes.DRIVER ) ) {
			Collection<Id<Person>> expIds = ((DriverRoute) exp.getRoute()).getPassengersIds();
			Collection<Id<Person>> actIds = ((DriverRoute) act.getRoute()).getPassengersIds();
			assertEquals(
					expIds.size(),
					actIds.size(),
					"wrong number of passengers");

			assertTrue(
					actIds.containsAll( expIds ),
					"wrong passenger ids");
		}
		else if ( exp.getMode().equals( JointActingTypes.PASSENGER ) ) {
			Id expId = ((PassengerRoute) exp.getRoute()).getDriverId();
			Id actId = ((PassengerRoute) act.getRoute()).getDriverId();

			assertEquals(
					expId,
					actId,
					"wrong driver Id");
		}
	}

	private void assertActivitiesMatch(final Activity exp, final Activity act) {
		assertEquals(
				exp.getType(),
				act.getType(),
				"wrong type");

		assertEquals(
				exp.getLinkId(),
				act.getLinkId(),
				"wrong link");
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class Fixture {
		public final String name;
		public final JointPlan jointPlan;
		public final Map<Id<Person>, List<PlanElement>> expectedPlanAfterRemoval;
		public final JointTrip toRemove;

		public Fixture(
				final String name,
				final JointPlan jointPlan,
				final Map<Id<Person>, List<PlanElement>> expectedPlanAfterRemoval,
				final JointTrip toRemove) {
			if ( expectedPlanAfterRemoval.size() != jointPlan.getIndividualPlans().size() ) {
				throw new IllegalArgumentException( expectedPlanAfterRemoval.size()+" != "+jointPlan.getIndividualPlans().size() );
			}
			this.name = name;
			this.jointPlan = jointPlan;
			this.expectedPlanAfterRemoval = expectedPlanAfterRemoval;
			this.toRemove = toRemove;
		}
	}
}

