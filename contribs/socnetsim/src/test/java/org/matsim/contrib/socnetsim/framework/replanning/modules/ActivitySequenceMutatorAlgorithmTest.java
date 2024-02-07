/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySequenceMutatorAlgorithmTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

import java.util.Random;

/**
 * @author thibautd
 */
public class ActivitySequenceMutatorAlgorithmTest {
	@Test
	void testTwoActivities() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("somebody", Person.class)));
		
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", Id.create( "w" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("l", Id.create( "l" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					StageActivityHandling.StagesAsNormalActivities );
		testee.run( plan );

		Assertions.assertEquals(
				plan.getPlanElements().size(),
				7,
				"unexpected size of plan "+plan.getPlanElements() );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"l",
				"unexpected type of first in-plan activity" );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 4 )).getType(),
				"w",
				"unexpected type of second in-plan activity" );
	}

	@Test
	void testOneActivities() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("somebody", Person.class)));
		
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", Id.create( "w" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					StageActivityHandling.StagesAsNormalActivities );
		testee.run( plan );

		Assertions.assertEquals(
				plan.getPlanElements().size(),
				5,
				"unexpected size of plan "+plan.getPlanElements() );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"w",
				"unexpected type of first in-plan activity" );
	}

	@Test
	void testZeroActivities() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("somebody", Person.class)));
		
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					StageActivityHandling.StagesAsNormalActivities );
		testee.run( plan );

		Assertions.assertEquals(
				plan.getPlanElements().size(),
				3,
				"unexpected size of plan "+plan.getPlanElements() );
	}

	@Test
	void testStage() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("somebody", Person.class)));
		
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("stage", Id.create( "s" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", Id.create( "w" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("l", Id.create( "l" , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode") );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", Id.create( "h" , Link.class )) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					StageActivityHandling.ExcludeStageActivities );
		testee.run( plan );

		Assertions.assertEquals(
				plan.getPlanElements().size(),
				9,
				"unexpected size of plan "+plan.getPlanElements() );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"stage",
				"unexpected type of first in-plan activity" );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 4 )).getType(),
				"l",
				"unexpected type of second in-plan activity" );
		Assertions.assertEquals(
				((Activity) plan.getPlanElements().get( 6 )).getType(),
				"w",
				"unexpected type of third in-plan activity" );
	}
}

