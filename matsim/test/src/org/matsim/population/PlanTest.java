/* *********************************************************************** *
 * project: org.matsim.*
 * PlanTest.java
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

package org.matsim.population;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

public class PlanTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(PlanTest.class);

	/**
	 * @author mrieser
	 */
	public void testCreateLeg() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		try {
			plan.createLeg(Mode.car);
			fail("expected IllegalStateException when creating a leg in an empty plan.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAct("h", new CoordImpl(0, 0));
		plan.createLeg(Mode.car);
		try {
			plan.createLeg(Mode.bike);
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAct("w", new CoordImpl(100, 200));
		plan.createLeg(Mode.bike);
		plan.createAct("h", new CoordImpl(0, 0));
	}

	/**
	 * @author mrieser
	 */
	public void testCreateAct() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAct("h", new CoordImpl(0, 0));
		// don't allow a second act directly after the first
		try {
			plan.createAct("w", new CoordImpl(100, 200));
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createLeg(Mode.car);
		// but after a leg, it must be possible to add an additional act
		plan.createAct("w", new CoordImpl(100, 200));
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_Between() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		Act homeAct = plan.createAct("h", new CoordImpl(0, 0));
		Leg leg1 = plan.createLeg(Mode.car);
		Act workAct = plan.createAct("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getActsLegs().size());

		// modification
		Act a = new Act("l", new CoordImpl(200, 100));
		Leg l = new org.matsim.population.LegImpl(Mode.car);
		plan.insertLegAct(1, l, a);

		// test
		assertEquals(5, plan.getActsLegs().size());
		assertEquals(homeAct, plan.getActsLegs().get(0));
		assertEquals(l, plan.getActsLegs().get(1));
		assertEquals(a, plan.getActsLegs().get(2));
		assertEquals(leg1, plan.getActsLegs().get(3));
		assertEquals(workAct, plan.getActsLegs().get(4));
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_AtEnd() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		Act homeAct = plan.createAct("h", new CoordImpl(0, 0));
		Leg leg1 = plan.createLeg(Mode.car);
		Act workAct = plan.createAct("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getActsLegs().size());

		// modification
		Act a = new Act("l", new CoordImpl(200, 100));
		Leg l = new org.matsim.population.LegImpl(Mode.car);
		plan.insertLegAct(3, l, a);

		// test
		assertEquals(5, plan.getActsLegs().size());
		assertEquals(homeAct, plan.getActsLegs().get(0));
		assertEquals(leg1, plan.getActsLegs().get(1));
		assertEquals(workAct, plan.getActsLegs().get(2));
		assertEquals(l, plan.getActsLegs().get(3));
		assertEquals(a, plan.getActsLegs().get(4));
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_AtWrongPosition() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAct("h", new CoordImpl(0, 0));
		plan.createLeg(Mode.car);
		plan.createAct("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getActsLegs().size());

		// modification
		Act a = new Act("l", new CoordImpl(200, 100));
		Leg l = new org.matsim.population.LegImpl(Mode.car);
		try {
			plan.insertLegAct(2, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_AtStart() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAct("h", new CoordImpl(0, 0));
		plan.createLeg(Mode.car);
		plan.createAct("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getActsLegs().size());

		// modification
		Act a = new Act("l", new CoordImpl(200, 100));
		Leg l = new org.matsim.population.LegImpl(Mode.car);
		try {
			plan.insertLegAct(0, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}


	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_BehindEnd() {
		Plan plan = new org.matsim.population.PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAct("h", new CoordImpl(0, 0));
		plan.createLeg(Mode.car);
		plan.createAct("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getActsLegs().size());

		// modification
		Act a = new Act("l", new CoordImpl(200, 100));
		Leg l = new org.matsim.population.LegImpl(Mode.car);
		try {
			plan.insertLegAct(4, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

		try {
			plan.insertLegAct(5, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

	}

}
