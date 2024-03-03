/* *********************************************************************** *
 * project: org.matsim.*
 * TransitActsRemoverTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;

public class TransitActsRemoverTest {


	@Test
	void testNormalTransitPlan() {
		Plan plan = PopulationUtils.createPlan();
		Coord dummyCoord = new Coord((double) 0, (double) 0);
		plan.addActivity(PopulationUtils.createActivityFromCoord("h", dummyCoord));

		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));

		plan.addActivity(PopulationUtils.createActivityFromCoord("w", dummyCoord));

		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		// direct connection without walking
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));

		plan.addActivity(PopulationUtils.createActivityFromCoord("s", dummyCoord));

		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk)); // connection with walking
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(PopulationUtils.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));

		plan.addActivity(PopulationUtils.createActivityFromCoord("h", dummyCoord));
		new TransitActsRemover().run(plan);

		assertEquals(7, plan.getPlanElements().size());
		assertEquals("h", ((Activity) plan.getPlanElements().get(0)).getType());
		assertEquals(TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals("w", ((Activity) plan.getPlanElements().get(2)).getType());
		assertEquals(TransportMode.pt, ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals("s", ((Activity) plan.getPlanElements().get(4)).getType());
		assertEquals(TransportMode.pt, ((Leg) plan.getPlanElements().get(5)).getMode());
		assertNull(((Leg) plan.getPlanElements().get(5)).getRoute());
		assertEquals("h", ((Activity) plan.getPlanElements().get(6)).getType());
	}

	@Test
	void testEmptyPlan() {
		Plan plan = PopulationUtils.createPlan();
		new TransitActsRemover().run(plan);
		assertEquals(0, plan.getPlanElements().size());
		// this mostly checks that there is no exception
	}

	@Test
	void testPlanWithoutLegs() {
		Plan plan = PopulationUtils.createPlan();
		Coord dummyCoord = new Coord((double) 0, (double) 0);
		plan.addActivity(PopulationUtils.createActivityFromCoord("h", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(1, plan.getPlanElements().size());
		// this mostly checks that there is no exception
	}

	@Test
	void testWalkOnlyPlan() {
		Plan plan = PopulationUtils.createPlan();
		Coord dummyCoord = new Coord((double) 0, (double) 0);
		plan.addActivity(PopulationUtils.createActivityFromCoord("h", dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.transit_walk));
		plan.addActivity(PopulationUtils.createActivityFromCoord("w", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(3, plan.getPlanElements().size());
		assertEquals(TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

	@Test
	void testNoTransitActPlan() {
		Plan plan = PopulationUtils.createPlan();
		Coord dummyCoord = new Coord((double) 0, (double) 0);
		plan.addActivity(PopulationUtils.createActivityFromCoord("h", dummyCoord));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
		plan.addActivity(PopulationUtils.createActivityFromCoord("w", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(3, plan.getPlanElements().size());
		assertEquals(TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

}
