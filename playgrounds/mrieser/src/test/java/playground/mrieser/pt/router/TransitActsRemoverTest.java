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

package playground.mrieser.pt.router;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;

import playground.mrieser.pt.router.TransitActsRemover;

public class TransitActsRemoverTest extends TestCase {

	
	public void testNormalTransitPlan() {
		PlanImpl plan = new PlanImpl();
		Coord dummyCoord = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("h", dummyCoord));

		plan.addLeg(new LegImpl(TransportMode.walk));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.pt));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.walk));
		
		plan.addActivity(new ActivityImpl("w", dummyCoord));
		
		plan.addLeg(new LegImpl(TransportMode.walk));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.pt));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		// direct connection without walking
		plan.addLeg(new LegImpl(TransportMode.pt));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.walk));

		plan.addActivity(new ActivityImpl("s", dummyCoord));
		
		plan.addLeg(new LegImpl(TransportMode.walk));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.pt));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.walk)); // connection with walking
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.pt));
		plan.addActivity(new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.walk));
		
		plan.addActivity(new ActivityImpl("h", dummyCoord));
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
	
	public void testEmptyPlan() {
		PlanImpl plan = new PlanImpl();
		new TransitActsRemover().run(plan);
		assertEquals(0, plan.getPlanElements().size());
		// this mostly checks that there is no exception
	}
	
	public void testPlanWithoutLegs() {
		PlanImpl plan = new PlanImpl();
		Coord dummyCoord = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("h", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(1, plan.getPlanElements().size());
		// this mostly checks that there is no exception
	}

	public void testWalkOnlyPlan() {
		PlanImpl plan = new PlanImpl();
		Coord dummyCoord = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("h", dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.walk));
		plan.addActivity(new ActivityImpl("w", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(3, plan.getPlanElements().size());
		assertEquals(TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
	}
	
	public void testNoTransitActPlan() {
		PlanImpl plan = new PlanImpl();
		Coord dummyCoord = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("h", dummyCoord));
		plan.addLeg(new LegImpl(TransportMode.car));
		plan.addActivity(new ActivityImpl("w", dummyCoord));
		new TransitActsRemover().run(plan);
		assertEquals(3, plan.getPlanElements().size());
		assertEquals(TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
	}
	
}
