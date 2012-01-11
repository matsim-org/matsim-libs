/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesUtilsTest.java
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
package playground.thibautd.jointtrips.population.jointtrippossibilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointActivity;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;

/**
 * Tests the correct behaviour of the class {@link JointTripPossibilitiesUtils}
 * @author thibautd
 */
public class JointTripPossibilitiesUtilsTest {
	private final IdFactory ids = new IdFactory();

	// /////////////////////////////////////////////////////////////////////////
	// test methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Tests whether the construction of the JoinableTrips
	 * data structure leads to consistent results.
	 */
	@Test
	public void testExtractJointTrips() {
		JointPlan plan = getPlanWithJointTrips();
		JointTripPossibilities possibilities =
			JointTripPossibilitiesUtils.extractJointTripPossibilities( plan );

		Assert.assertEquals(
				"incorrect number of joint trip possibilities",
				countPassengerLegs( plan ),
				possibilities.getJointTripPossibilities().size());

		//TODO: test ids of activities and agents
	}

	/**
	 * Extract the joinable trip information, and re-includes the
	 * trips in the plan, checking the resulting plan for consistency.
	 *
	 * Note that the fact that getting joint trips and reincluding them
	 * gives possibly different plans is not tested, as it is part of
	 * the expected behaviour of the method.
	 */
	@Test
	public void testIncludeJointTripsFromExtractedJointTrips() {
		JointPlan plan = getPlanWithJointTrips();
		JointTripPossibilities possibilities =
			JointTripPossibilitiesUtils.extractJointTripPossibilities( plan );

		plan.setJointTripPossibilities( possibilities );

		Map<JointTripPossibility, Boolean> trips =
			JointTripPossibilitiesUtils.getPerformedJointTrips( plan );

		JointTripPossibilitiesUtils.includeJointTrips( trips , plan );

		Assert.assertEquals(
				"incorrect number of joint trip possibilities",
				countPassengerLegs( plan ),
				possibilities.getJointTripPossibilities().size());

		//TODO: test for activity and agent ids
		// test for structure as well?
	}

	/**
	 * Given a plan without joint trips, creates a PossibleJointTrips
	 * structure corresponding to no pre-existing joint trips, and tests
	 * wheter inclusion leads to expected results.
	 */
	@Test
	public void testIncludeJointTripsFromExNihiloTrips() {
		JointPlan plan = getPlanWithoutJointTrips();
		JointTripPossibilities possibilities = getExNihiloPossibilities( plan );

		plan.setJointTripPossibilities( possibilities );

		Map<JointTripPossibility, Boolean> participation =
			JointTripPossibilitiesUtils.getPerformedJointTrips( plan );

		for (Map.Entry<JointTripPossibility, Boolean> entry : participation.entrySet()) {
			entry.setValue( true );
		}

		JointTripPossibilitiesUtils.includeJointTrips( participation , plan );

		Assert.assertEquals(
				"incorrect number of joint trip possibilities",
				countPassengerLegs( plan ),
				possibilities.getJointTripPossibilities().size());
	}

	/**
	 * tests whether the linkage encoded by the pick-up "inital type" values
	 * is correct.
	 */
	@Test
	public void testInitialTypeMatching() {
		JointPlan plan = getPlanWithoutJointTrips();
		JointTripPossibilities possibilities = getExNihiloPossibilities( plan );

		plan.setJointTripPossibilities( possibilities );

		Map<JointTripPossibility, Boolean> participation =
			JointTripPossibilitiesUtils.getPerformedJointTrips( plan );

		for (Map.Entry<JointTripPossibility, Boolean> entry : participation.entrySet()) {
			entry.setValue( true );
		}

		JointTripPossibilitiesUtils.includeJointTrips( participation , plan );

		// check that type and inital type are as expected
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof JointActivity) {
				JointActivity act = (JointActivity) pe;

				if (act.getType().equals( JointActingTypes.PICK_UP )) {
					Assert.assertTrue(
							"unexpected initial type for pick up: "+act.getInitialType(),
							act.getInitialType().matches( JointActingTypes.PICK_UP_REGEXP ));
				}
				else {
					Assert.assertFalse(
							"unexpected type for activity: "+act.getType(),
							act.getType().matches( JointActingTypes.PICK_UP_REGEXP ));
				}
			}
			else if ( !(pe instanceof JointLeg) ) {
				Assert.fail( "unexpected plan element type "+pe.getClass().getName()+" inserted at trip creation" );
			}
		}

		// now, check that all pick-ups before a given joint leg have the same
		// initial type
		Map<Id, String> sharedLeg2Type = new HashMap<Id, String>();

		String lastInitialType = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof JointActivity) {
				lastInitialType = ((JointActivity) pe).getInitialType();
			}
			else if ( ((JointLeg) pe).getJoint() ) {
				for (Id id : ((JointLeg) pe).getLinkedElementsIds()) {
					// associate the previous activity type at the joint legs
					String oldType = sharedLeg2Type.put( id , lastInitialType );

					if (oldType != null) {
						// if a type already attached, check that it is the same
						Assert.assertEquals(
								"pick up types do not match",
								oldType,
								lastInitialType);
					}
				}
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// TODO: put the plan generating classes in a "Utils" class, to be used from
	// any test class?
	// /////////////////////////////////////////////////////////////////////////
	private JointPlan getPlanWithJointTrips() {
		Map<Id, Person> persons = new HashMap<Id, Person>();
		Map<Id, Plan> plans = new HashMap<Id, Plan>();

		Person person1 = new PersonImpl( ids.get( 1 ) );
		PlanImpl plan1 = new PlanImpl();
		persons.put( ids.get( 1 ) , person1 );
		plans.put( ids.get( 1 ) , plan1 );

		Person person2 = new PersonImpl( ids.get( 2 ) );
		PlanImpl plan2 = new PlanImpl();
		persons.put( ids.get( 2 ) , person2 );
		plans.put( ids.get( 2 ) , plan2 );

		Person person3 = new PersonImpl( ids.get( 3 ) );
		PlanImpl plan3 = new PlanImpl();
		persons.put( ids.get( 3 ) , person3 );
		plans.put( ids.get( 3 ) , plan3 );

		Person person4 = new PersonImpl( ids.get( 4 ) );
		PlanImpl plan4 = new PlanImpl();
		persons.put( ids.get( 4 ) , person4 );
		plans.put( ids.get( 4 ) , plan4 );


		// create a "shuttle" plan. This will be changed at inclusion.
		plan1.createAndAddActivity( "h" );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( getPuName( 1 ) );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( getPuName( 2 ) );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( getPuName( 3 ) );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( "h" );

		plan2.createAndAddActivity( "h" );
		plan2.createAndAddLeg( TransportMode.car );
		plan2.createAndAddActivity( getPuName( 1 ) );
		plan2.createAndAddLeg( JointActingTypes.PASSENGER );
		plan2.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan2.createAndAddLeg( TransportMode.car );
		plan2.createAndAddActivity( "h" );

		plan3.createAndAddActivity( "h" );
		plan3.createAndAddLeg( TransportMode.car );
		plan3.createAndAddActivity( getPuName( 2 ) );
		plan3.createAndAddLeg( JointActingTypes.PASSENGER );
		plan3.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan3.createAndAddLeg( TransportMode.car );
		plan3.createAndAddActivity( "h" );

		plan4.createAndAddActivity( "h" );
		plan4.createAndAddLeg( TransportMode.car );
		plan4.createAndAddActivity( getPuName( 3 ) );
		plan4.createAndAddLeg( JointActingTypes.PASSENGER );
		plan4.createAndAddActivity( JointActingTypes.DROP_OFF );
		plan4.createAndAddLeg( TransportMode.car );
		plan4.createAndAddActivity( "h" );

		Clique clique = new Clique( ids.get( "clique" ) , persons );
		JointPlan plan = new JointPlan( clique , plans );
		return plan;
	}

	private JointPlan getPlanWithoutJointTrips() {
		Map<Id, Person> persons = new HashMap<Id, Person>();
		Map<Id, Plan> plans = new HashMap<Id, Plan>();

		Person person1 = new PersonImpl( ids.get( 1 ) );
		PlanImpl plan1 = new PlanImpl();
		persons.put( ids.get( 1 ) , person1 );
		plans.put( ids.get( 1 ) , plan1 );

		Person person2 = new PersonImpl( ids.get( 2 ) );
		PlanImpl plan2 = new PlanImpl();
		persons.put( ids.get( 2 ) , person2 );
		plans.put( ids.get( 2 ) , plan2 );

		Person person3 = new PersonImpl( ids.get( 3 ) );
		PlanImpl plan3 = new PlanImpl();
		persons.put( ids.get( 3 ) , person3 );
		plans.put( ids.get( 3 ) , plan3 );

		Person person4 = new PersonImpl( ids.get( 4 ) );
		PlanImpl plan4 = new PlanImpl();
		persons.put( ids.get( 4 ) , person4 );
		plans.put( ids.get( 4 ) , plan4 );

		// TODO: make structure more complex
		plan1.createAndAddActivity( "h" );
		plan1.createAndAddLeg( TransportMode.car );
		plan1.createAndAddActivity( "h" );

		plan2.createAndAddActivity( "h" );
		plan2.createAndAddLeg( TransportMode.car );
		plan2.createAndAddActivity( "h" );

		plan3.createAndAddActivity( "h" );
		plan3.createAndAddLeg( TransportMode.car );
		plan3.createAndAddActivity( "h" );

		plan4.createAndAddActivity( "h" );
		plan4.createAndAddLeg( TransportMode.car );
		plan4.createAndAddActivity( "h" );

		Clique clique = new Clique( ids.get( "clique" ) , persons );
		JointPlan plan = new JointPlan( clique , plans );
		return plan;
	}

	private JointTripPossibilities getExNihiloPossibilities(final JointPlan plan) {
		// the first leg of the first plan is a driver trip, for all other first legs.
		List<JointTripParticipation> trips = new ArrayList<JointTripParticipation>();

		JointTripPossibilitiesFactory factory = new JointTripPossibilitiesFactoryImpl();

		for (Map.Entry<Id , List<PlanElement>> entry :
				plan.getIndividualPlanElements().entrySet()) {
			trips.add( factory.createJointTripParticipation(
						entry.getKey(),
						((JointActivity) entry.getValue().get( 0 )).getId(),
						((JointActivity) entry.getValue().get( 2 )).getId()));
		}

		List<JointTripPossibility> possibilities = new ArrayList<JointTripPossibility>();

		for (JointTripParticipation trip : trips.subList(1, trips.size())) {
			possibilities.add( factory.createJointTripPossibility(
						trips.get( 0 ),
						trip) );
		}

		return factory.createJointTripPossibilities( possibilities );
	}

	private static int countPassengerLegs(final JointPlan plan) {
		int count = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			if ( pe instanceof Leg &&
					((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
				count++;
			}
		}

		return count;
	}

	private static String getPuName( final int n ) {
		return JointActingTypes.PICK_UP_BEGIN + JointActingTypes.PICK_UP_SPLIT_EXPR + n;
	}
}

class IdFactory {
	public Id get(final String value) {
		return new IdImpl( value );
	}

	public Id get(final long value) {
		return get( ""+value );
	}
}
