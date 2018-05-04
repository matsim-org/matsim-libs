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

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegMode;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ChooseRandomLegModeTest extends MatsimTestCase {

	public void testRandomChoice() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), false);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		boolean foundCarMode = false;
		boolean foundPtMode = false;
		boolean foundWalkMode = false;
		String previousMode = leg.getMode();
		for (int i = 0; i < 10; i++) {
			algo.run(plan);
			assertNotSame("leg mode must have changed.", previousMode, leg.getMode());
			previousMode = leg.getMode();
			if (TransportMode.car.equals(previousMode)) {
				foundCarMode = true;
			} else if (TransportMode.pt.equals(previousMode)) {
				foundPtMode = true;
			} else if (TransportMode.walk.equals(previousMode)) {
				foundWalkMode = true;
			} else {
				fail("unexpected mode: " + previousMode);
			}
		}
		assertTrue("expected to find car-mode", foundCarMode);
		assertTrue("expected to find pt-mode", foundPtMode);
		assertTrue("expected to find walk-mode", foundWalkMode);
	}

	public void testRandomChoiceWithinListedModesOnly() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), true);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		boolean foundCarMode = false;
		boolean foundPtMode = false;
		boolean foundWalkMode = false;
		String previousMode = leg.getMode();
		for (int i = 0; i < 10; i++) {
			algo.run(plan);
			assertNotSame("leg mode must have changed.", previousMode, leg.getMode());
			previousMode = leg.getMode();
			if (TransportMode.car.equals(previousMode)) {
				foundCarMode = true;
			} else if (TransportMode.pt.equals(previousMode)) {
				foundPtMode = true;
			} else if (TransportMode.walk.equals(previousMode)) {
				foundWalkMode = true;
			} else {
				fail("unexpected mode: " + previousMode);
			}
		}
		assertTrue("expected to find car-mode", foundCarMode);
		assertTrue("expected to find pt-mode", foundPtMode);
		assertTrue("expected to find walk-mode", foundWalkMode);
	}

	public void testRandomChoiceWithinListedModesOnlyWorks() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), true);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		String previousMode = leg.getMode();
		algo.run(plan);
		assertSame("leg mode must have changed.", previousMode, leg.getMode());
	}



	public void testHandleEmptyPlan() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), false);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	public void testHandlePlanWithoutLeg() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), false);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	/**
	 * Test that all the legs have the same, changed mode
	 */
	public void testMultipleLegs() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt}, MatsimRandom.getRandom(), false);
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "shop", new Coord((double) 0, (double) 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord((double) 0, (double) 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals("unexpected leg mode in leg 2.", TransportMode.pt, ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals("unexpected leg mode in leg 3.", TransportMode.pt, ((Leg) plan.getPlanElements().get(5)).getMode());
	}

	public void testIgnoreCarAvailability_Never() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.bike}, MatsimRandom.getRandom(), false);
		algo.setIgnoreCarAvailability(false);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "never");
		Plan plan = PopulationUtils.createPlan(person);
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord((double) 0, (double) 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

	public void testIgnoreCarAvailability_Never_noChoice() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt}, MatsimRandom.getRandom(), false);
		algo.setIgnoreCarAvailability(false);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "never");
		Plan plan = PopulationUtils.createPlan(person);
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord((double) 0, (double) 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

	public void testIgnoreCarAvailability_Always() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.bike}, new Random(1), false);
		algo.setIgnoreCarAvailability(false);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "always");
		Plan plan = PopulationUtils.createPlan(person);
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord((double) 0, (double) 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

}
