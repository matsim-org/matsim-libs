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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class ChooseRandomSingleLegModeTest {

	@Test
	public void testRandomChoice() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		boolean foundCarMode = false;
		boolean foundPtMode = false;
		boolean foundWalkMode = false;
		for (int i = 0; i < 5; i++) {
			algo.run(plan);
			String mode = leg.getMode();
			if (TransportMode.car.equals(mode)) {
				foundCarMode = true;
			} else if (TransportMode.pt.equals(mode)) {
				foundPtMode = true;
			} else if (TransportMode.walk.equals(mode)) {
				foundWalkMode = true;
			} else {
				fail("unexpected mode: " + mode);
			}
		}
		assertTrue("expected to find car-mode", foundCarMode);
		assertTrue("expected to find pt-mode", foundPtMode);
		assertTrue("expected to find walk-mode", foundWalkMode);
	}

	@Test
	public void testHandleEmptyPlan() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	@Test
	public void testHandlePlanWithoutLeg() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	@Test
	public void testHandlePlan_DifferentThanLastMode() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		String oldMode = leg.getMode();
		for (int i = 0; i < 5; i++) {
			algo.run(plan);
			String mode = leg.getMode();
			assertTrue(!oldMode.equals(mode));
			oldMode = mode;
		}
	}

	@Test
	public void testHandlePlan_OnlySingleLegChanged() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		LegImpl leg1 = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		LegImpl leg2 = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		String oldMode1 = leg1.getMode();
		String oldMode2 = leg2.getMode();
		for (int i = 0; i < 5; i++) {
			algo.run(plan);
			int cntChanges = 0;
			String mode1 = leg1.getMode();
			String mode2 = leg2.getMode();
			if (!mode1.equals(oldMode1)) {
				cntChanges++;
				oldMode1 = mode1;
			}
			if (!mode2.equals(oldMode2)) {
				cntChanges++;
				oldMode2 = mode2;
			}
			assertEquals(1, cntChanges);
		}
	}

	@Test
	public void testIgnoreCarAvailability_Never() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.bike}, MatsimRandom.getRandom());
		algo.setIgnoreCarAvailability(false);
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "never");
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.pt);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

	@Test
	public void testIgnoreCarAvailability_Never_noChoice() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt}, MatsimRandom.getRandom());
		algo.setIgnoreCarAvailability(false);
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "never");
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.pt);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

	@Test
	public void testIgnoreCarAvailability_Always() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(new String[] {TransportMode.car, TransportMode.pt, TransportMode.bike}, new Random(1));
		algo.setIgnoreCarAvailability(false);
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		PersonUtils.setCarAvail(person, "always");
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.pt);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.car, ((Leg) plan.getPlanElements().get(1)).getMode());
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.bike, ((Leg) plan.getPlanElements().get(1)).getMode());
	}

}
