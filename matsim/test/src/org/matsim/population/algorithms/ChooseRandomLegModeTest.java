/* *********************************************************************** *
 * project: org.matsim.*
 * ChooseRandomLegMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class ChooseRandomLegModeTest extends MatsimTestCase {

	public void testRandomChoice() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk}, MatsimRandom.random);
		Plan plan = new org.matsim.population.PlanImpl(null);
		plan.createAct("home", new CoordImpl(0, 0));
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("work", new CoordImpl(0, 0));
		boolean foundCarMode = false;
		boolean foundPtMode = false;
		boolean foundWalkMode = false;
		BasicLeg.Mode previousMode = leg.getMode();
		for (int i = 0; i < 5; i++) {
			algo.run(plan);
			assertNotSame("leg mode must have changed.", previousMode, leg.getMode());
			previousMode = leg.getMode();
			if (BasicLeg.Mode.car.equals(previousMode)) {
				foundCarMode = true;
			} else if (BasicLeg.Mode.pt.equals(previousMode)) {
				foundPtMode = true;
			} else if (BasicLeg.Mode.walk.equals(previousMode)) {
				foundWalkMode = true;
			} else {
				fail("unexpected mode: " + previousMode.toString());
			}
		}
		assertTrue("expected to find car-mode", foundCarMode);
		assertTrue("expected to find pt-mode", foundPtMode);
		assertTrue("expected to find walk-mode", foundWalkMode);
	}

	public void testHandleEmptyPlan() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk}, MatsimRandom.random);
		Plan plan = new org.matsim.population.PlanImpl(null);
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	public void testHandlePlanWithoutLeg() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk}, MatsimRandom.random);
		Plan plan = new org.matsim.population.PlanImpl(null);
		plan.createAct("home", new CoordImpl(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	/**
	 * Test that all the legs have the same, changed mode
	 */
	public void testMultipleLegs() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt}, MatsimRandom.random);
		Plan plan = new org.matsim.population.PlanImpl(null);
		plan.createAct("home", new CoordImpl(0, 0));
		plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("work", new CoordImpl(0, 0));
		plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("shop", new CoordImpl(0, 0));
		plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("home", new CoordImpl(0, 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", BasicLeg.Mode.pt, ((BasicLeg) plan.getActsLegs().get(1)).getMode());
		assertEquals("unexpected leg mode in leg 2.", BasicLeg.Mode.pt, ((BasicLeg) plan.getActsLegs().get(3)).getMode());
		assertEquals("unexpected leg mode in leg 3.", BasicLeg.Mode.pt, ((BasicLeg) plan.getActsLegs().get(5)).getMode());
	}

}
