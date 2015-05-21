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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;

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
		Logger.getLogger(JointTripRemoverAlgorithmTest.class);

	private List<Fixture> fixtures;

	// /////////////////////////////////////////////////////////////////////////
	// init routines
	// /////////////////////////////////////////////////////////////////////////
	@Before
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
		Person driver = new PersonImpl( Id.createPersonId( "Schumacher" ) );
		Person passenger = new PersonImpl( Id.createPersonId( "Asterix" ) );

		Id<Link> link1 = Id.createLinkId( 1 );
		Id<Link> link2 = Id.createLinkId( 2 );
		Id<Link> link3 = Id.createLinkId( 3 );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "horse" );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "unicycle" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan = new PlanImpl( passenger );
		plans.put( passenger.getId() , passengerPlan );

		Activity pAct1 = passengerPlan.createAndAddActivity( "home" , link1 );
		passengerPlan.createAndAddLeg( "jetpack" );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointPassengerLeg = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan.createAndAddLeg( "paraglider" );
		Activity pAct2 = passengerPlan.createAndAddActivity( "home" , link1 );

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , new LegImpl( TransportMode.pt ) , pAct2 ));

		return new Fixture(
				"one passenger",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createTwoPassengersFixture() {
		Person driver = new PersonImpl( Id.createPersonId( "Alonso" ) );
		Person passenger1 = new PersonImpl( Id.createPersonId( "Boule" ) );
		Person passenger2 = new PersonImpl( Id.createPersonId( "Bill" ) );

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "skateboard" );
		Activity dPu = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "elevator" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "jetpack" );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan1.createAndAddLeg( "paraglider" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "jetpack" );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan2.createAndAddLeg( "paraglider" );
		passengerPlan2.createAndAddActivity( "home" , link1 );

		DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger1.getId() );
		dRoute.addPassenger( passenger2.getId() );
		jointDriverLeg.setRoute( dRoute );

		PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg1.setRoute( pRoute );
		jointPassengerLeg2.setRoute( pRoute.clone()	);

		Leg expectedDriverLeg = new LegImpl( JointActingTypes.DRIVER );
		DriverRoute expDRoute = dRoute.clone();
		expDRoute.removePassenger( passenger1.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dPu ,
					expectedDriverLeg,
					dDo , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , new LegImpl( TransportMode.pt ) , p1Act2 ));

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
					jointPassengerLeg1),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createTwoPassengersFixtureWithInternOverlap() {
		Person driver = new PersonImpl( Id.createPersonId( "Prost" ) );
		Person passenger1 = new PersonImpl( Id.createPersonId( "Joe" ) );
		Person passenger2 = new PersonImpl( Id.createPersonId( "Avrell" ) );

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );
		Id<Link> link4 = Id.create( 4 , Link.class );
		Id<Link> link5 = Id.create( 5 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "Rollerblade" );
		Activity dPu1 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointDriverLeg1 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		Leg jointDriverLeg3 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo2 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link5 );
		driverPlan.createAndAddLeg( "iceskate" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "kayak" );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		passengerPlan1.createAndAddLeg( "submarine" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "spitfire" );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link5 );
		passengerPlan2.createAndAddLeg( "deltaplane" );
		passengerPlan2.createAndAddActivity( "home" , link1 );

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

		Leg expectedDriverLeg = new LegImpl( JointActingTypes.DRIVER );
		DriverRoute expDRoute = new DriverRoute( link2 , link5 );
		expDRoute.addPassenger( passenger2.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dPu1 ,
					expectedDriverLeg,
					dDo2 , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , new LegImpl( TransportMode.pt ) , p1Act2 ));

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
					jointPassengerLeg1),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createTwoPassengersFixtureWithExternOverlap() {
		Person driver = new PersonImpl( Id.createPersonId( "Kowalski" ) );
		Person passenger1 = new PersonImpl( Id.createPersonId( "Pif" ) );
		Person passenger2 = new PersonImpl( Id.createPersonId( "Paf" ) );

		Id<Link> link1 = Id.create( 1 , Link.class );
		Id<Link> link2 = Id.create( 2 , Link.class );
		Id<Link> link3 = Id.create( 3 , Link.class );
		Id<Link> link4 = Id.create( 4 , Link.class );
		Id<Link> link5 = Id.create( 5 , Link.class );

		Map<Id<Person>, Plan> plans = new HashMap< >();
		Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "poney" );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointDriverLeg1 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dPu2 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo1 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		Leg jointDriverLeg3 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link5 );
		driverPlan.createAndAddLeg( "donkey" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "cablecar" );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link5 );
		passengerPlan1.createAndAddLeg( "ski" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "hand walking" );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link4 );
		passengerPlan2.createAndAddLeg( "jumps" );
		passengerPlan2.createAndAddActivity( "home" , link1 );

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

		Leg expectedDriverLeg = new LegImpl( JointActingTypes.DRIVER );
		DriverRoute expDRoute = new DriverRoute( link4 , link5 );
		expDRoute.addPassenger( passenger2.getId() );
		expectedDriverLeg.setRoute( expDRoute );
		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dPu2 ,
					expectedDriverLeg,
					dDo1 , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , new LegImpl( TransportMode.pt ) , p1Act2 ));

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
					jointPassengerLeg1),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createMultiDriverStageFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Schumacher" ) );
		final Person passenger = new PersonImpl( Id.createPersonId( "Asterix" ) );
		final String stageType = "drinkACoffee";

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		final Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "horse" );
		driverPlan.createAndAddActivity( stageType , link1 );
		driverPlan.createAndAddLeg( "horse" );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "unicycle" );
		driverPlan.createAndAddLeg( "unicycle" );
		driverPlan.createAndAddActivity( stageType , link1 );
		driverPlan.createAndAddLeg( "unicycle" );
		final Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan = new PlanImpl( passenger );
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = passengerPlan.createAndAddActivity( "home" , link1 );
		passengerPlan.createAndAddLeg( "jetpack" );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan.createAndAddLeg( "paraglider" );
		final Activity pAct2 = passengerPlan.createAndAddActivity( "home" , link1 );

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , new LegImpl( TransportMode.pt ) , pAct2 ));

		return new Fixture(
				"complex access trip driver",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg),
				new StageActivityTypesImpl( stageType ));
	}

	private Fixture createMultiPassengerStageFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Schumacher" ) );
		final Person passenger = new PersonImpl( Id.createPersonId( "Asterix" ) );
		final String stageType = "drinkACoffee";

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		final Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "horse" );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "unicycle" );
		final Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan = new PlanImpl( passenger );
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = passengerPlan.createAndAddActivity( "home" , link1 );
		passengerPlan.createAndAddLeg( "jetpack" );
		passengerPlan.createAndAddActivity( stageType , link1 );
		passengerPlan.createAndAddLeg( "jetpack" );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan.createAndAddLeg( "paraglider" );
		passengerPlan.createAndAddActivity( stageType , link1 );
		passengerPlan.createAndAddLeg( "paraglider" );
		passengerPlan.createAndAddLeg( "paraglider" );
		final Activity pAct2 = passengerPlan.createAndAddActivity( "home" , link1 );

		final DriverRoute dRoute = new DriverRoute( link2 , link3 );
		dRoute.addPassenger( passenger.getId() );
		jointDriverLeg.setRoute( dRoute );

		final PassengerRoute pRoute = new PassengerRoute( link2 , link3 );
		pRoute.setDriverId( driver.getId() );
		jointPassengerLeg.setRoute( pRoute );

		expectedAfterRemoval.put(
				driver.getId(),
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dAct2 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , new LegImpl( TransportMode.pt ) , pAct2 ));

		return new Fixture(
				"complex access trip passenger",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg ),
					passenger.getId(),
					jointPassengerLeg),
				new StageActivityTypesImpl( stageType ));
	}

	private Fixture createTwoPassengersInDifferentTripsRemoveFirstFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alonso" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Boule" ) );
		final Person passenger2 = new PersonImpl( Id.createPersonId( "Bill" ) );

		final Id<Link> link1 = Id.create( 1 , Link.class );
		final Id<Link> link2 = Id.create( 2 , Link.class );
		final Id<Link> link3 = Id.create( 3 , Link.class );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		final Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "skateboard" );
		/*final Activity dPu =*/ driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		/*final Activity dDo =*/ driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "elevator" );
		final Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );
		final Leg dAccess2 = driverPlan.createAndAddLeg( "skateboard" );
		final Activity dPu2 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		final Activity dDo2 = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg dEgress2 = driverPlan.createAndAddLeg( "elevator" );
		final Activity dAct3 = driverPlan.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		final Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "jetpack" );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan1.createAndAddLeg( "paraglider" );
		final Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "jetpack" );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan2.createAndAddLeg( "paraglider" );
		passengerPlan2.createAndAddActivity( "home" , link1 );

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
				Arrays.asList( dAct1 , new LegImpl( TransportMode.car ) , dAct2 ,
					dAccess2 , dPu2 , jointDriverLeg2 , dDo2 , dEgress2 , dAct3 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				Arrays.asList( p1Act1 , new LegImpl( TransportMode.pt ) , p1Act2 ));

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
					jointPassengerLeg1),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createTwoPassengersInDifferentTripsRemoveSecondFixture() {
		final Person driver = new PersonImpl( Id.createPersonId( "Alonso" ) );
		final Person passenger1 = new PersonImpl( Id.createPersonId( "Boule" ) );
		final Person passenger2 = new PersonImpl( Id.createPersonId( "Bill" ) );

		final Id link1 = Id.createLinkId( 1 );
		final Id link2 = Id.createLinkId( 2 );
		final Id link3 = Id.createLinkId( 3 );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		final Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		final Leg dAccess = driverPlan.createAndAddLeg( "skateboard" );
		final Activity dPu = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		final Activity dDo = driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg dEgress = driverPlan.createAndAddLeg( "elevator" );
		final Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "skateboard" );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan.createAndAddLeg( "elevator" );
		final Activity dAct3 = driverPlan.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "jetpack" );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan1.createAndAddLeg( "paraglider" );
		passengerPlan1.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		final Activity p2Act1 = passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "jetpack" );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		passengerPlan2.createAndAddLeg( "paraglider" );
		final Activity p2Act2 = passengerPlan2.createAndAddActivity( "home" , link1 );

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
					new LegImpl( TransportMode.car ) , dAct3 ));

		expectedAfterRemoval.put(
				passenger1.getId(),
				new ArrayList<PlanElement>( passengerPlan1.getPlanElements() ));

		expectedAfterRemoval.put(
				passenger2.getId(),
				Arrays.asList( p2Act1 , new LegImpl( TransportMode.pt ) , p2Act2 ));

		return new Fixture(
				"two passengers different trips remove second",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver.getId(),
					Arrays.asList( jointDriverLeg2 ),
					passenger2.getId(),
					jointPassengerLeg2),
				EmptyStageActivityTypes.INSTANCE);
	}

	private Fixture createTwoDriversFixture(final boolean removeFirst) {
		final Person driver1 = new PersonImpl( Id.createPersonId( "Alonso" ) );
		final Person driver2 = new PersonImpl( Id.createPersonId( "Schumacher" ) );
		final Person passenger = new PersonImpl( Id.createPersonId( "Rantanplan" ) );

		final Id link1 = Id.createLinkId( 1 );
		final Id link2 = Id.createLinkId( 2 );
		final Id link3 = Id.createLinkId( 3 );

		final Map<Id<Person>, Plan> plans = new HashMap< >();
		final Map<Id<Person>, List<PlanElement>> expectedAfterRemoval = new HashMap< >();

		final PlanImpl driverPlan1 = new PlanImpl( driver1 );
		plans.put( driver1.getId() , driverPlan1 );

		final Activity d1Act1 = driverPlan1.createAndAddActivity( "home" , link1 );
		driverPlan1.createAndAddLeg( "skateboard" );
		driverPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg1 = driverPlan1.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan1.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan1.createAndAddLeg( "elevator" );
		final Activity d1Act2 = driverPlan1.createAndAddActivity( "home" , link1 );
		final Leg d1Leg = driverPlan1.createAndAddLeg( "skateboard" );
		final Activity d1Act3 = driverPlan1.createAndAddActivity( "home" , link1 );

		final PlanImpl driverPlan2 = new PlanImpl( driver2 );
		plans.put( driver2.getId() , driverPlan2 );

		final Activity d2Act1 = driverPlan2.createAndAddActivity( "home" , link1 );
		final Leg d2Leg = driverPlan2.createAndAddLeg( "skateboard" );
		final Activity d2Act2 = driverPlan2.createAndAddActivity( "home" , link1 );
		driverPlan2.createAndAddLeg( "skateboard" );
		driverPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointDriverLeg2 = driverPlan2.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan2.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		driverPlan2.createAndAddLeg( "elevator" );
		final Activity d2Act3 = driverPlan2.createAndAddActivity( "home" , link1 );

		final PlanImpl passengerPlan = new PlanImpl( passenger );
		plans.put( passenger.getId() , passengerPlan );

		final Activity pAct1 = passengerPlan.createAndAddActivity( "home" , link1 );
		final Leg pAccess1 = passengerPlan.createAndAddLeg( "jetpack" );
		final Activity pPu1 = passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg1 = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final Activity pDo1 = passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg pEgress1 = passengerPlan.createAndAddLeg( "paraglider" );
		final Activity pAct2 = passengerPlan.createAndAddActivity( "home" , link1 );
		final Leg pAccess2 = passengerPlan.createAndAddLeg( "jetpack" );
		final Activity pPu2 = passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link2 );
		final Leg jointPassengerLeg2 = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		final Activity pDo2 = passengerPlan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		final Leg pEgress2 = passengerPlan.createAndAddLeg( "paraglider" );
		final Activity pAct3 = passengerPlan.createAndAddActivity( "home" , link1 );

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
					Arrays.asList( d1Act1 , new LegImpl( TransportMode.car ) , d1Act2 ,
						d1Leg , d1Act3 ));

			expectedAfterRemoval.put(
					driver2.getId(),
					new ArrayList<PlanElement>( driverPlan2.getPlanElements() ) );

			expectedAfterRemoval.put(
					passenger.getId(),
					Arrays.asList( pAct1 , new LegImpl( TransportMode.pt ) , pAct2 ,
						pAccess2 , pPu2 , jointPassengerLeg2 , pDo2 , pEgress2 , pAct3 ));

			return new Fixture(
					"two drivers remove first",
					new JointPlanFactory().createJointPlan( plans ),
					expectedAfterRemoval,
					new JointTrip(
						driver1.getId(),
						Arrays.asList( jointDriverLeg1 ),
						passenger.getId(),
						jointPassengerLeg1),
					EmptyStageActivityTypes.INSTANCE);
		}

		assert !removeFirst;
		expectedAfterRemoval.put(
				driver1.getId(),
				new ArrayList<PlanElement>( driverPlan1.getPlanElements() ) );

		expectedAfterRemoval.put(
				driver2.getId(),
				Arrays.asList( d2Act1 , d2Leg , d2Act2 ,
					new LegImpl( TransportMode.car ) , d2Act3 ));

		expectedAfterRemoval.put(
				passenger.getId(),
				Arrays.asList( pAct1 , pAccess1 , pPu1 , jointPassengerLeg1 , pDo1 , pEgress1 ,
					pAct2 , new LegImpl( TransportMode.pt ) , pAct3 ));

		return new Fixture(
				"two drivers remove second",
				new JointPlanFactory().createJointPlan( plans ),
				expectedAfterRemoval,
				new JointTrip(
					driver2.getId(),
					Arrays.asList( jointDriverLeg2 ),
					passenger.getId(),
					jointPassengerLeg2),
				EmptyStageActivityTypes.INSTANCE);

	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testRemoval() throws Exception {
		// TODO: test driver and passenger removal separately
		for ( Fixture f : fixtures ) {
			log.info( "testing removal on fixture "+f.name );
			final JointTripRemoverAlgorithm algo = new JointTripRemoverAlgorithm( null ,  f.stageActivities , new MainModeIdentifierImpl() );
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

	private void assertChainsMatch(
			final String fixtureName,
			final List<PlanElement> expected,
			final List<PlanElement> actual) {
		assertEquals(
				fixtureName+": sizes do not match "+expected+" and "+actual,
				expected.size(),
				actual.size());

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
				"wrong mode",
				exp.getMode(),
				act.getMode());

		if ( exp.getMode().equals( JointActingTypes.DRIVER ) ) {
			Collection<Id<Person>> expIds = ((DriverRoute) exp.getRoute()).getPassengersIds();
			Collection<Id<Person>> actIds = ((DriverRoute) act.getRoute()).getPassengersIds();
			assertEquals(
					"wrong number of passengers",
					expIds.size(),
					actIds.size());

			assertTrue(
					"wrong passenger ids",
					actIds.containsAll( expIds ));
		}
		else if ( exp.getMode().equals( JointActingTypes.PASSENGER ) ) {
			Id expId = ((PassengerRoute) exp.getRoute()).getDriverId();
			Id actId = ((PassengerRoute) act.getRoute()).getDriverId();

			assertEquals(
					"wrong driver Id",
					expId,
					actId);
		}
	}

	private void assertActivitiesMatch(final Activity exp, final Activity act) {
		assertEquals(
				"wrong type",
				exp.getType(),
				act.getType());

		assertEquals(
				"wrong link",
				exp.getLinkId(),
				act.getLinkId());
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class Fixture {
		public final String name;
		public final JointPlan jointPlan;
		public final Map<Id<Person>, List<PlanElement>> expectedPlanAfterRemoval;
		public final JointTrip toRemove;
		public final StageActivityTypes stageActivities;

		public Fixture(
				final String name,
				final JointPlan jointPlan,
				final Map<Id<Person>, List<PlanElement>> expectedPlanAfterRemoval,
				final JointTrip toRemove,
				final StageActivityTypes stageActivities) {
			if ( expectedPlanAfterRemoval.size() != jointPlan.getIndividualPlans().size() ) {
				throw new IllegalArgumentException( expectedPlanAfterRemoval.size()+" != "+jointPlan.getIndividualPlans().size() );
			}
			this.name = name;
			this.jointPlan = jointPlan;
			this.expectedPlanAfterRemoval = expectedPlanAfterRemoval;
			this.toRemove = toRemove;
			this.stageActivities = stageActivities;
		}
	}
}

