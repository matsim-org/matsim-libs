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
package playground.thibautd.socnetsim.replanning;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public class JoinableActivitiesPlanLinkIdentifierTest {
	private static final PopulationFactory factory =
		ScenarioUtils.createScenario(
				ConfigUtils.createConfig() ).getPopulation().getFactory();

	@Test
	public void testOpenPlansSamePlaceSameType() {
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 = createOpenPlan( new IdImpl( 1 ) , type , facility );
		final Plan plan2 = createOpenPlan( new IdImpl( 2 ) , type , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertTrue(
				"plans with activities of good type at the same place the whole day should be joint",
				testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	public void testOpenPlansSamePlaceDifferentType() {
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 = createOpenPlan( new IdImpl( 1 ) , type , facility );
		final Plan plan2 = createOpenPlan( new IdImpl( 2 ) , "other type" , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertFalse(
				"plans with activities of different types should not be joined",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	@Test
	public void testOpenPlansDifferentPlaceSameType() {
		final String type = "type";
		final Id facility = new IdImpl( "fac" );
		final Id facility2 = new IdImpl( "fa2" );

		final Plan plan1 = createOpenPlan( new IdImpl( 1 ) , type , facility );
		final Plan plan2 = createOpenPlan( new IdImpl( 2 ) , type , facility2 );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertFalse(
				"plans with activities at different locations should not be joined",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	@Test
	public void testOpenPlansSamePlaceSameWrongType() {
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 = createOpenPlan( new IdImpl( 1 ) , type , facility );
		final Plan plan2 = createOpenPlan( new IdImpl( 2 ) , type , facility );

		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( "other type" );
		Assert.assertFalse(
				"plans with activities of non joinable types should not be joined",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	private static Plan createOpenPlan(
			final Id personId,
			final String type,
			final Id facility) {
		final Person pers1 = factory.createPerson( personId );
		final Plan plan1 = factory.createPlan();
		pers1.addPlan( plan1 );
		final ActivityImpl act1 = (ActivityImpl) factory.createActivityFromLinkId( type , new IdImpl( "link" ) );
		act1.setFacilityId( facility );
		plan1.addActivity( act1 );

		return plan1;
	}

	@Test
	public void testSingleTourOverlaping() {
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 =
			createSingleTripPlan(
					new IdImpl( 1 ),
					type,
					facility,
					10,
					30);
		final Plan plan2 =
			createSingleTripPlan(
					new IdImpl( 2 ),
					type,
					facility,
					20,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertTrue(
				"plans with overlaping activities should be joint",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	@Test
	public void testSingleTourPlansNonOverlaping() {
		//Logger.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 =
			createSingleTripPlan(
					new IdImpl( 1 ),
					type,
					facility,
					10,
					20);
		final Plan plan2 =
			createSingleTripPlan(
					new IdImpl( 2 ),
					type,
					facility,
					22,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertFalse(
				"plans with non overlaping activities should not be joint",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	@Test
	public void testSingleTourPlansZeroDurationAct() {
		//Logger.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 =
			createSingleTripPlan(
					new IdImpl( 1 ),
					type,
					facility,
					30,
					30);
		final Plan plan2 =
			createSingleTripPlan(
					new IdImpl( 2 ),
					type,
					facility,
					22,
					40);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertTrue(
				"plans with zero-length overlaping activities should be joint",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	@Test
	public void testSingleTourPlansInconsistentDurationAct() {
		//Logger.getLogger( JoinableActivitiesPlanLinkIdentifier.class ).setLevel( Level.TRACE );
		final String type = "type";
		final Id facility = new IdImpl( "fac" );

		final Plan plan1 =
			createSingleTripPlan(
					new IdImpl( 1 ),
					type,
					facility,
					30,
					// this start has to be shifted
					20);
		final Plan plan2 =
			createSingleTripPlan(
					new IdImpl( 2 ),
					type,
					facility,
					// test: activity in the middle does not last for ever
					32,
					33);


		final PlanLinkIdentifier testee = new JoinableActivitiesPlanLinkIdentifier( type );
		Assert.assertFalse(
				"plans with inconsistent duration not handled correctly",
				testee.areLinked( plan1 , plan2 ) );
		Assert.assertEquals(
				"inconsistency!",
				testee.areLinked( plan1 , plan2 ),
				testee.areLinked( plan2 , plan1 ) );
	}

	private static Plan createSingleTripPlan(
			final Id personId,
			final String type,
			final Id facility,
			final double start,
			final double end) {
		final Person pers = factory.createPerson( personId );
		final Plan plan = factory.createPlan();
		pers.addPlan( plan );

		{
			final ActivityImpl act = (ActivityImpl) factory.createActivityFromLinkId( "home" , new IdImpl( "link" ) );
			act.setFacilityId( new IdImpl( "home" ) );
			act.setEndTime( start );
			plan.addActivity( act );
		}

		{
			final ActivityImpl act = (ActivityImpl) factory.createActivityFromLinkId( type , new IdImpl( "link" ) );
			act.setFacilityId( facility );
			act.setEndTime( end );
			plan.addActivity( act );
		}

		{
			final ActivityImpl act = (ActivityImpl) factory.createActivityFromLinkId( "home" , new IdImpl( "link" ) );
			act.setFacilityId( new IdImpl( "home" ) );
			plan.addActivity( act );
		}

		return plan;
	}
}

