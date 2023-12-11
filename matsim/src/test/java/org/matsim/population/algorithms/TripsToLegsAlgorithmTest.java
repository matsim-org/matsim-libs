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
package org.matsim.population.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;

/**
 * @author thibautd
 */
public class TripsToLegsAlgorithmTest {
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

	private static final String DUMMY_1 = "dummy_1 interaction";
	private static final String DUMMY_2 = "dummy_2 interaction";

	@Test
	void testMonoLegPlan() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("id", Person.class)));
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id<Link> id1 = Id.create( 1, Link.class );
		final Id<Link> id2 = Id.create( 2, Link.class );

		Activity act = PopulationUtils.createActivityFromLinkId("act_1", id1);
		plan.addActivity( act );
		structure.add( act );
		Leg leg = PopulationUtils.createLeg("mode_1");
		plan.addLeg( leg );
		structure.add( leg );
		act = PopulationUtils.createActivityFromLinkId("act_2", id2);
		plan.addActivity( act );
		structure.add( act );
		leg = PopulationUtils.createLeg("mode_2");
		plan.addLeg( leg );
		structure.add( leg );
		act = PopulationUtils.createActivityFromLinkId("act_3", id1);
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"mono leg trips",
					plan,
					structure));
	}

	@Test
	void testMultiLegPlan() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("id", Person.class)));
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id<Link> id1 = Id.create( 1, Link.class );
		final Id<Link> id2 = Id.create( 2, Link.class );

		Activity act = PopulationUtils.createActivityFromLinkId("act_1", id1);
		plan.addActivity( act );
		structure.add( act );

		Leg leg = PopulationUtils.createLeg("mode_1");
		plan.addLeg( leg );
		structure.add( leg );

		leg = PopulationUtils.createLeg("mode_1bis");
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId("act_2", id2);
		plan.addActivity( act );
		structure.add( act );

		leg = PopulationUtils.createLeg("mode_2");
		plan.addLeg( leg );
		structure.add( leg );

		leg = PopulationUtils.createLeg("mode_2bis");
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId("act_3", id1);
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"multi leg trips",
					plan,
					structure));	
	}

	@Test
	void testDummyActsPlan() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("id", Person.class)));
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id<Link> id1 = Id.create( 1, Link.class );
		final Id<Link> id2 = Id.create( 2, Link.class );
		final Id<Link> id3 = Id.create( 3, Link.class );

		Activity act = PopulationUtils.createActivityFromLinkId("act_1", id1);
		plan.addActivity( act );
		structure.add( act );

		Leg leg = PopulationUtils.createLeg("mode_1");
		plan.addLeg( leg );
		structure.add( leg );

		act = PopulationUtils.createActivityFromLinkId(DUMMY_1, id3);
		plan.addActivity( act );

		leg = PopulationUtils.createLeg("mode_1bis");
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId("act_2", id2);
		plan.addActivity( act );
		structure.add( act );

		leg = PopulationUtils.createLeg("mode_2");
		plan.addLeg( leg );
		structure.add( leg );

		act = PopulationUtils.createActivityFromLinkId(DUMMY_2, id3);
		plan.addActivity( act );

		leg = PopulationUtils.createLeg("mode_2bis");
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId("act_3", id1);
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"dummy act trips",
					plan,
					structure));		
	}

	@Test
	void testPtPlan() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("id", Person.class)));
		final List<PlanElement> structure = new ArrayList<PlanElement>();

		final Id<Link> id1 = Id.create( 1, Link.class );
		final Id<Link> id2 = Id.create( 2, Link.class );
		final Id<Link> id3 = Id.create( 3, Link.class );

		Activity act = PopulationUtils.createActivityFromLinkId("act_1", id1);
		plan.addActivity( act );
		structure.add( act );

		Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId(DUMMY_1, id3);
		plan.addActivity( act );

		leg = PopulationUtils.createLeg(TransportMode.pt);
		plan.addLeg( leg );
		structure.add( leg );

		act = PopulationUtils.createActivityFromLinkId("act_2", id2);
		plan.addActivity( act );
		structure.add( act );

		act = PopulationUtils.createActivityFromLinkId("act_2bis", id2);
		plan.addActivity( act );
		structure.add( act );

		leg = PopulationUtils.createLeg(TransportMode.transit_walk);
		plan.addLeg( leg );
		structure.add( PopulationUtils.createLeg(TransportMode.pt) );

		act = PopulationUtils.createActivityFromLinkId(DUMMY_2, id3);
		plan.addActivity( act );

		leg = PopulationUtils.createLeg("mode_2bis");
		plan.addLeg( leg );

		act = PopulationUtils.createActivityFromLinkId("act_3", id1);
		plan.addActivity( act );
		structure.add( act );

		performTest(
				new Fixture(
					"dummy act trips",
					plan,
					structure));		
	}

	private static void performTest(final Fixture fixture) {

		final TripsToLegsAlgorithm algorithm = new TripsToLegsAlgorithm( new MainModeIdentifierImpl() );
		algorithm.run( fixture.plan );

		assertEquals(
				fixture.expectedPlanStructure.size(),
				fixture.plan.getPlanElements().size(),
				"wrong structure size for fixture <<"+fixture.name+">>");

		final Iterator<PlanElement> expIter = fixture.expectedPlanStructure.iterator();
		final Iterator<PlanElement> actualIter = fixture.plan.getPlanElements().iterator();

		while ( expIter.hasNext() ) {
			final PlanElement expected = expIter.next();
			final PlanElement actual = actualIter.next();

			if ( actual instanceof Activity ) {
				assertTrue(
						expected instanceof Activity,
						"incompatible Activity/Leg sequence in fixture <<"+fixture.name+">>" );

				assertEquals(
						((Activity) expected).getType(),
						((Activity) actual).getType(),
						"incompatible activity types in fixture <<"+fixture.name+">>");
			}
			else if ( actual instanceof Leg ) {
				assertTrue(
						expected instanceof Leg,
						"incompatible types sequence in fixture <<"+fixture.name+">>" );

				assertEquals(
						((Leg) expected).getMode(),
						((Leg) actual).getMode(),
						"incompatible leg modes in fixture <<"+fixture.name+">>");
			}
			else {
				throw new RuntimeException( actual.getClass().getName() );
			}
		}
	}
}

