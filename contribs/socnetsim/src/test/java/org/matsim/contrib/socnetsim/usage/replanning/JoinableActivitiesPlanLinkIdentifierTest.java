/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableActivitiesPlanLinkIdentifierTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.replanning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.jointactivities.replanning.JoinableActivitiesPlanLinkIdentifier;

/**
 * @author thibautd
 */
public class JoinableActivitiesPlanLinkIdentifierTest {
	private static final PopulationFactory factory =
		ScenarioUtils.createScenario(
				ConfigUtils.createConfig() ).getPopulation().getFactory();

	@Test
	void testOpenPlansSamePlaceSameType() {
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class);

		final Plan plan1 = createOpenPlan( Id.create( 1 , Person.class ) , type , facility );
		final Plan plan2 = createOpenPlan( Id.create( 2 , Person.class ) , type , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertTrue(
				testee.areLinked( plan1 , plan2 ),
				"plans with activities of good type at the same place the whole day should be joint" );
	}

	@Test
	void testOpenPlansSamePlaceDifferentType() {
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );

		final Plan plan1 = createOpenPlan( Id.create( 1 , Person.class ) , type , facility );
		final Plan plan2 = createOpenPlan( Id.create( 2 , Person.class ) , "other type" , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with activities of different types should not be joined" );
	}

	@Test
	void testOpenPlansDifferentPlaceSameType() {
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );
		final Id<ActivityFacility> facility2 = Id.create( "fa2" , ActivityFacility.class );

		final Plan plan1 = createOpenPlan( Id.create( 1 , Person.class ) , type , facility );
		final Plan plan2 = createOpenPlan( Id.create( 2 , Person.class ) , type , facility2 );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with activities at different locations should not be joined" );
	}

	@Test
	void testOpenPlansSamePlaceSameWrongType() {
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class);

		final Plan plan1 = createOpenPlan( Id.create( 1 , Person.class ) , type , facility );
		final Plan plan2 = createOpenPlan( Id.create( 2 , Person.class ) , type , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( "other type" );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with activities of non joinable types should not be joined" );
	}

	private static Plan createOpenPlan(
			final Id<Person> personId,
			final String type,
			final Id<ActivityFacility> facility) {
		final Person pers1 = factory.createPerson( personId );
		final Plan plan1 = factory.createPlan();
		pers1.addPlan( plan1 );
		final Activity act1 = (Activity) factory.createActivityFromLinkId( type , Id.create( "link" , Link.class ) );
		act1.setFacilityId( facility );
		plan1.addActivity( act1 );

		return plan1;
	}

	@Test
	void testSingleTourOverlaping() {
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class);

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					10,
					30);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					20,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertTrue(
				testee.areLinked( plan1 , plan2 ),
				"plans with overlaping activities should be joint" );
	}

	@Test
	void testSingleTourPlansNonOverlaping() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					10,
					20);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					22,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with non overlaping activities should not be joint" );
	}

	@Test
	void testSingleTourPlansZeroDurationAct() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class);

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					30,
					30);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					22,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertTrue(
				testee.areLinked( plan1 , plan2 ),
				"plans with zero-length overlaping activities should be joint" );
	}

	@Test
	void testSingleTourPlansZeroDurationBegin() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class);

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					22,
					22);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					22,
					40);

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		// actual result irrelevant for this border case, as long as consistent
		//Assert.assertTrue(
		//		"plans with zero-length overlaping activities should be joint",
		//		testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	void testSingleTourPlansZeroDurationEnd() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					40,
					40);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					22,
					40);

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		// actual result irrelevant for this border case, as long as consistent
		//Assert.assertTrue(
		//		"plans with zero-length overlaping activities should be joint",
		//		testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	void testDoubleTourPlansZeroDurationEnd() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );
		final Id<ActivityFacility> wrongFacility = Id.create( "fac2" , ActivityFacility.class );

		final Plan plan1 =
			createDoubleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					30,
					wrongFacility,
					40,
					facility,
					40);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					22,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		// whether plans are linked or not on this border case is irrelevant,
		// but the result should be consistent.
		//Assert.assertTrue(
		//		"plans with zero-length overlaping activities should be joint",
		//		testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	void testSingleTourPlansInconsistentDurationAct() {
		//LogManager.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id<ActivityFacility> facility = Id.create( "fac" , ActivityFacility.class );

		final Plan plan1 =
			createSingleTripPlan(
					Id.create( 1 , Person.class ),
					type,
					facility,
					30,
					// this start has to be shifted
					20);
		final Plan plan2 =
			createSingleTripPlan(
					Id.create( 2 , Person.class ),
					type,
					facility,
					// test: activity in the middle does not last for ever
					32,
					33);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assertions.assertEquals(
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ),
				"inconsistency!" );
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with inconsistent duration not handled correctly" );
	}

	private static Plan createDoubleTripPlan(
			final Id<Person> personId,
			final String type,
			final double start1,
			final Id<ActivityFacility> facility1,
			final double end1,
			final Id<ActivityFacility> facility2,
			final double end2) {
		final Person pers = factory.createPerson( personId );
		final Plan plan = factory.createPlan();
		pers.addPlan( plan );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( "home" , Id.create( "link" , Link.class ) );
			act.setFacilityId( Id.create( "home" , ActivityFacility.class ) );
			act.setEndTime( start1 );
			plan.addActivity( act );
		}

		plan.addLeg( PopulationUtils.createLeg("car") );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( type , Id.create( "link" , Link.class ) );
			act.setFacilityId( facility1 );
			act.setEndTime( end1 );
			plan.addActivity( act );
		}

		plan.addLeg( PopulationUtils.createLeg("car") );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( type , Id.create( "link" , Link.class ) );
			act.setFacilityId( facility2 );
			act.setEndTime( end2 );
			plan.addActivity( act );
		}


		plan.addLeg( PopulationUtils.createLeg("car") );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( "home" , Id.create( "link" , Link.class ) );
			act.setFacilityId( Id.create( "home" , ActivityFacility.class ) );
			plan.addActivity( act );
		}

		return plan;
	}

	private static Plan createSingleTripPlan(
			final Id<Person> personId,
			final String type,
			final Id<ActivityFacility> facility,
			final double start,
			final double end) {
		final Person pers = factory.createPerson( personId );
		final Plan plan = factory.createPlan();
		pers.addPlan( plan );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( "home" , Id.create( "link" , Link.class ) );
			act.setFacilityId( Id.create( "home" , ActivityFacility.class ) );
			act.setEndTime( start );
			plan.addActivity( act );
		}

		plan.addLeg( PopulationUtils.createLeg("car") );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( type , Id.create( "link" , Link.class ) );
			act.setFacilityId( facility );
			act.setEndTime( end );
			plan.addActivity( act );
		}

		plan.addLeg( PopulationUtils.createLeg("car") );

		{
			final Activity act = (Activity) factory.createActivityFromLinkId( "home" , Id.create( "link" , Link.class ) );
			act.setFacilityId( Id.create( "home" , ActivityFacility.class ) );
			plan.addActivity( act );
		}

		return plan;
	}
}

