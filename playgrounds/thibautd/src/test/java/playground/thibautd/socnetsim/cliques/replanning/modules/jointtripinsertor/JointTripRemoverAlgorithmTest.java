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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;

import playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor.JointTripRemoverAlgorithm;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.utils.JointPlanUtils.JointTrip;

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
	}

	private Fixture createSimplisticFixture() {
		Person driver = new PersonImpl( new IdImpl( "Schumacher" ) );
		Person passenger = new PersonImpl( new IdImpl( "Asterix" ) );

		Id link1 = new IdImpl( 1 );
		Id link2 = new IdImpl( 2 );
		Id link3 = new IdImpl( 3 );

		Map<Id, Plan> plans = new HashMap<Id, Plan>();
		Map<Id, List<PlanElement>> expectedAfterRemoval = new HashMap<Id, List<PlanElement>>();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		// the fantaisist modes are not (only) for fun: they allow to check from
		// where the mode of the replacement comes. This can be important due
		// to the possibility that access/egress legs constitute subtours and thus
		// break the mode chain.
		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "horse" );
		driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		driverPlan.createAndAddLeg( "unicycle" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan = new PlanImpl( passenger );
		plans.put( passenger.getId() , passengerPlan );

		Activity pAct1 = passengerPlan.createAndAddActivity( "home" , link1 );
		passengerPlan.createAndAddLeg( "jetpack" );
		passengerPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointPassengerLeg = passengerPlan.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
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
					jointPassengerLeg));
	}

	private Fixture createTwoPassengersFixture() {
		Person driver = new PersonImpl( new IdImpl( "Alonso" ) );
		Person passenger1 = new PersonImpl( new IdImpl( "Boule" ) );
		Person passenger2 = new PersonImpl( new IdImpl( "Bill" ) );

		Id link1 = new IdImpl( 1 );
		Id link2 = new IdImpl( 2 );
		Id link3 = new IdImpl( 3 );

		Map<Id, Plan> plans = new HashMap<Id, Plan>();
		Map<Id, List<PlanElement>> expectedAfterRemoval = new HashMap<Id, List<PlanElement>>();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "skateboard" );
		Activity dPu = driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointDriverLeg = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo = driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		driverPlan.createAndAddLeg( "elevator" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "jetpack" );
		passengerPlan1.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		passengerPlan1.createAndAddLeg( "paraglider" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "jetpack" );
		passengerPlan2.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
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
					jointPassengerLeg1));
	}

	private Fixture createTwoPassengersFixtureWithInternOverlap() {
		Person driver = new PersonImpl( new IdImpl( "Prost" ) );
		Person passenger1 = new PersonImpl( new IdImpl( "Joe" ) );
		Person passenger2 = new PersonImpl( new IdImpl( "Avrell" ) );

		Id link1 = new IdImpl( 1 );
		Id link2 = new IdImpl( 2 );
		Id link3 = new IdImpl( 3 );
		Id link4 = new IdImpl( 4 );
		Id link5 = new IdImpl( 5 );

		Map<Id, Plan> plans = new HashMap<Id, Plan>();
		Map<Id, List<PlanElement>> expectedAfterRemoval = new HashMap<Id, List<PlanElement>>();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "Rollerblade" );
		Activity dPu1 = driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointDriverLeg1 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		Leg jointDriverLeg3 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo2 = driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link5 );
		driverPlan.createAndAddLeg( "iceskate" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "kayak" );
		passengerPlan1.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		passengerPlan1.createAndAddLeg( "submarine" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "spitfire" );
		passengerPlan2.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.DROP_OFF , link5 );
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
					jointPassengerLeg1));
	}

	private Fixture createTwoPassengersFixtureWithExternOverlap() {
		Person driver = new PersonImpl( new IdImpl( "Kowalski" ) );
		Person passenger1 = new PersonImpl( new IdImpl( "Pif" ) );
		Person passenger2 = new PersonImpl( new IdImpl( "Paf" ) );

		Id link1 = new IdImpl( 1 );
		Id link2 = new IdImpl( 2 );
		Id link3 = new IdImpl( 3 );
		Id link4 = new IdImpl( 4 );
		Id link5 = new IdImpl( 5 );

		Map<Id, Plan> plans = new HashMap<Id, Plan>();
		Map<Id, List<PlanElement>> expectedAfterRemoval = new HashMap<Id, List<PlanElement>>();

		PlanImpl driverPlan = new PlanImpl( driver );
		plans.put( driver.getId() , driverPlan );

		Activity dAct1 = driverPlan.createAndAddActivity( "home" , link1 );
		driverPlan.createAndAddLeg( "poney" );
		driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointDriverLeg1 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dPu2 = driverPlan.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		Leg jointDriverLeg2 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		Activity dDo1 = driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
		Leg jointDriverLeg3 = driverPlan.createAndAddLeg( JointActingTypes.DRIVER );
		driverPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link5 );
		driverPlan.createAndAddLeg( "donkey" );
		Activity dAct2 = driverPlan.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan1 = new PlanImpl( passenger1 );
		plans.put( passenger1.getId() , passengerPlan1 );

		Activity p1Act1 = passengerPlan1.createAndAddActivity( "home" , link1 );
		passengerPlan1.createAndAddLeg( "cablecar" );
		passengerPlan1.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		Leg jointPassengerLeg1 = passengerPlan1.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan1.createAndAddActivity( JointActingTypes.DROP_OFF , link5 );
		passengerPlan1.createAndAddLeg( "ski" );
		Activity p1Act2 = passengerPlan1.createAndAddActivity( "home" , link1 );

		PlanImpl passengerPlan2 = new PlanImpl( passenger2 );
		plans.put( passenger2.getId() , passengerPlan2 );

		passengerPlan2.createAndAddActivity( "home" , link1 );
		passengerPlan2.createAndAddLeg( "hand walking" );
		passengerPlan2.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		Leg jointPassengerLeg2 = passengerPlan2.createAndAddLeg( JointActingTypes.PASSENGER );
		passengerPlan2.createAndAddActivity( JointActingTypes.DROP_OFF , link4 );
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
					jointPassengerLeg1));
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testRemoval() throws Exception {
		// TODO: test driver and passenger removal separately
		for ( Fixture f : fixtures ) {
			log.info( "testing removal on fixture "+f.name );
			JointTripRemoverAlgorithm.removePassengerTrip( f.toRemove , f.jointPlan , EmptyStageActivityTypes.INSTANCE );
			JointTripRemoverAlgorithm.removeDriverTrip( f.toRemove , f.jointPlan );

			for ( Plan p : f.jointPlan.getIndividualPlans().values() ) {
				assertChainsMatch(
						f.expectedPlanAfterRemoval.get( p.getPerson().getId() ),
						p.getPlanElements() );
			}
		}
	}

	private void assertChainsMatch(
			final List<PlanElement> expected,
			final List<PlanElement> actual) {
		assertEquals(
				"sizes do not match "+expected+" and "+actual,
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
					fail( "expected activity, got leg: "+exp+", "+actElement );
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
					fail( "expected leg, got activity: "+exp+", "+actElement );
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
			Collection<Id> expIds = ((DriverRoute) exp.getRoute()).getPassengersIds();
			Collection<Id> actIds = ((DriverRoute) act.getRoute()).getPassengersIds();
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
		public final Map<Id, List<PlanElement>> expectedPlanAfterRemoval;
		public final JointTrip toRemove;

		public Fixture(
				final String name,
				final JointPlan jointPlan,
				final Map<Id, List<PlanElement>> expectedPlanAfterRemoval,
				final JointTrip toRemove) {
			this.name = name;
			this.jointPlan = jointPlan;
			this.expectedPlanAfterRemoval = expectedPlanAfterRemoval;
			this.toRemove = toRemove;
		}
	}
}

