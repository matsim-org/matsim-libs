/* *********************************************************************** *
 * project: org.matsim.*
 * TripDetectionTest.java
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;

/**
 * @author thibautd
 */
public class TripDetectionTest {
	private static class Fixture {
		public final Plan plan;
		public final List<PlanElement> expectedPlanStructure;
		public final String name;

		public Fixture(
				final String name,
				final Plan plan,
				final List<PlanElement> structure) {
			this.name = name;
			this.plan = plan;
			this.expectedPlanStructure = structure;
		}
	}

	private static final String DUMMY_1 = "dummy_1";
	private static final String DUMMY_2 = "dummy_2";

	@Test
	public void testMonoLegPlan() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "id" ) ) );
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );

		Activity act = new ActivityImpl( "act_1" , id1 );
		plan.addActivity( act );
		structure.add( act );
		Leg leg = new LegImpl( "mode_1" );
		plan.addLeg( leg );
		structure.add( leg );
		act = new ActivityImpl( "act_2" , id2 );
		plan.addActivity( act );
		structure.add( act );
		leg = new LegImpl( "mode_2" );
		plan.addLeg( leg );
		structure.add( leg );
		act = new ActivityImpl( "act_3" , id1 );
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"mono leg trips",
					plan,
					structure));
	}

	@Test
	public void testMultiLegPlan() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "id" ) ) );
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );

		Activity act = new ActivityImpl( "act_1" , id1 );
		plan.addActivity( act );
		structure.add( act );

		Leg leg = new LegImpl( "mode_1" );
		plan.addLeg( leg );
		structure.add( leg );

		leg = new LegImpl( "mode_1bis" );
		plan.addLeg( leg );

		act = new ActivityImpl( "act_2" , id2 );
		plan.addActivity( act );
		structure.add( act );

		leg = new LegImpl( "mode_2" );
		plan.addLeg( leg );
		structure.add( leg );

		leg = new LegImpl( "mode_2bis" );
		plan.addLeg( leg );

		act = new ActivityImpl( "act_3" , id1 );
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"multi leg trips",
					plan,
					structure));	
	}

	@Test
	public void testDummyActsPlan() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "id" ) ) );
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );
		final Id id3 = new IdImpl( 3 );

		Activity act = new ActivityImpl( "act_1" , id1 );
		plan.addActivity( act );
		structure.add( act );

		Leg leg = new LegImpl( "mode_1" );
		plan.addLeg( leg );
		structure.add( leg );

		act = new ActivityImpl( DUMMY_1 , id3 );
		plan.addActivity( act );

		leg = new LegImpl( "mode_1bis" );
		plan.addLeg( leg );

		act = new ActivityImpl( "act_2" , id2 );
		plan.addActivity( act );
		structure.add( act );

		leg = new LegImpl( "mode_2" );
		plan.addLeg( leg );
		structure.add( leg );

		act = new ActivityImpl( DUMMY_2 , id3 );
		plan.addActivity( act );

		leg = new LegImpl( "mode_2bis" );
		plan.addLeg( leg );

		act = new ActivityImpl( "act_3" , id1 );
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"dummy act trips",
					plan,
					structure));		
	}

	private static void performTest(final Fixture fixture) {
		final StageActivityTypes types =
			new StageActivityTypesImpl(
					Arrays.asList(
						DUMMY_1,
						DUMMY_2 ));
		final TripRouter router = new TripRouter();

		final List<PlanElement> structure = router.tripsToLegs( fixture.plan , types );

		assertEquals(
				"wrong structure size for fixture <<"+fixture.name+">>",
				fixture.expectedPlanStructure.size(),
				structure.size());

		final Iterator<PlanElement> expIter = fixture.expectedPlanStructure.iterator();
		final Iterator<PlanElement> actualIter = structure.iterator();

		while ( expIter.hasNext() ) {
			final PlanElement expected = expIter.next();
			final PlanElement actual = actualIter.next();

			if ( actual instanceof Activity ) {
				assertTrue(
						"incompatible types sequence in fixture <<"+fixture.name+">>",
						expected instanceof Activity );

				assertEquals(
						"incompatible activity types in fixture <<"+fixture.name+">>",
						((Activity) expected).getType(),
						((Activity) actual).getType());
			}
			else if ( actual instanceof Leg ) {
				assertTrue(
						"incompatible types sequence in fixture <<"+fixture.name+">>",
						expected instanceof Leg );

				assertEquals(
						"incompatible leg modes in fixture <<"+fixture.name+">>",
						((Leg) expected).getMode(),
						((Leg) actual).getMode());
			}
			else {
				throw new RuntimeException( actual.getClass().getName() );
			}
		}
	}
}

