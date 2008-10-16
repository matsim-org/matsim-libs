/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPlanTest.java
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

package org.matsim.basic.v01;

import org.apache.log4j.Logger;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class BasicPlanTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(BasicPlanTest.class);

	/**
	 * Tests the behavior of {@link BasicPlanImpl.ActLegIterator}.
	 *
	 * @throws Exception
	 * @author mrieser
	 */
	public void testActLegIterator() throws Exception {
		Person person = new Person(new IdImpl("1"));
		Plan plan = person.createPlan(true);

		// test with empty plan, should not produce an error/exception
		BasicPlanImpl.ActLegIterator iter = plan.getIterator();
		assertFalse(iter.hasNextLeg());

		// add one act, no legs yet, test again
		Act act1 = plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIterator();
		assertEquals(act1, iter.nextAct());
		assertFalse(iter.hasNextLeg());

		// add leg + act, test again
		Leg leg1 = plan.createLeg(BasicLeg.Mode.car);
		Act act2 = plan.createAct("w", new CoordImpl(100.0, 100.0));
		iter = plan.getIterator();
		assertEquals(act1, iter.nextAct());
		assertTrue(iter.hasNextLeg());
		assertEquals(leg1, iter.nextLeg());
		assertFalse(iter.hasNextLeg());
		assertEquals(act2, iter.nextAct());
		assertFalse(iter.hasNextLeg());

		// add leg + act, test again. Now we have a generic plan with real first act, real last act, and real middle acts.
		Leg leg2 = plan.createLeg(BasicLeg.Mode.car);
		Act act3 = plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIterator();
		assertEquals(act1, iter.nextAct());
		assertTrue(iter.hasNextLeg());
		assertEquals(leg1, iter.nextLeg());
		assertTrue(iter.hasNextLeg());
		iter.hasNextLeg(); // a call to hasNextLeg() should not modify the internal structure, e.g. advancing the internal index...
		iter.hasNextLeg(); // ...we will see that this is not the case in the next assert-statement:
		assertEquals(act2, iter.nextAct());
		assertTrue(iter.hasNextLeg());
		assertEquals(leg2, iter.nextLeg());
		assertFalse(iter.hasNextLeg());
		assertEquals(act3, iter.nextAct());
		assertFalse(iter.hasNextLeg());

		// now test how wrong access are handled, they usually should result in an Exception
		iter = plan.getIterator();
		iter.nextAct(); // act1
		try {
			iter.nextAct();
			fail("Expected Exception, but got none.");
		} catch (IndexOutOfBoundsException e) {
			log.info("Catched expected exception: " + e.getMessage());
		}
		assertEquals(leg1, iter.nextLeg()); // the internal index shouldn't have changed with our wrong access
		assertTrue(iter.hasNextLeg());
		try {
			iter.nextLeg();
			fail("Expected Exception, but got none.");
		} catch (IndexOutOfBoundsException e) {
			log.info("Catched expected exception: " + e.getMessage());
		}

		// now test that two iterators are independent of each other
		iter = plan.getIterator();
		BasicPlanImpl.ActLegIterator iter2 = plan.getIterator();
		assertEquals(act1, iter.nextAct());
		assertEquals(act1, iter2.nextAct()); // must be possible, it's iter2
		assertEquals(leg1, iter.nextLeg());
		assertEquals(act2, iter.nextAct());
		assertEquals(leg2, iter.nextLeg());
		assertEquals(leg1, iter2.nextLeg()); // iter2 is still at leg1
		assertFalse(iter.hasNextLeg());
		assertTrue(iter2.hasNextLeg());
	}

	/**
	 * Tests the behavior of {@link BasicPlanImpl.LegIterator}.
	 *
	 * @throws Exception
	 * @author mrieser
	 */
	public void testLegIterator() throws Exception {
		Person person = new Person(new IdImpl("1"));
		Plan plan = person.createPlan(true);

		// test with empty plan, should not produce an error/exception
		BasicPlanImpl.LegIterator iter = plan.getIteratorLeg();
		assertFalse(iter.hasNext());

		// add one act, no legs yet, test again
		plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIteratorLeg();
		assertFalse(iter.hasNext());

		// add leg + act, test again
		Leg leg1 = plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("w", new CoordImpl(100.0, 100.0));
		iter = plan.getIteratorLeg();
		assertTrue(iter.hasNext());
		assertEquals(leg1, iter.next());
		assertFalse(iter.hasNext());

		// add leg + act, test again. Now we have a generic plan with real first act, real last act, and real middle acts.
		Leg leg2 = plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIteratorLeg();
		assertTrue(iter.hasNext());
		assertEquals(leg1, iter.next());
		assertTrue(iter.hasNext());
		iter.hasNext(); // a call to hasNext() should not modify the internal structure, e.g. advancing the internal index...
		iter.hasNext(); // ...we will see that this is not the case in the next assert-statement:
		assertEquals(leg2, iter.next());
		assertFalse(iter.hasNext());

		// test that an additional iter.next() results in an exception
		try {
			iter.next();
			fail("Expected Exception, but got none.");
		} catch (IndexOutOfBoundsException e) {
			log.info("Catched expected exception: " + e.getMessage());
		}

		// now test that two iterators are independent of each other
		iter = plan.getIteratorLeg();
		BasicPlanImpl.LegIterator iter2 = plan.getIteratorLeg();
		assertEquals(leg1, iter.next());
		assertEquals(leg2, iter.next());
		assertEquals(leg1, iter2.next()); // iter2 is still at leg1
		assertFalse(iter.hasNext());
		assertTrue(iter2.hasNext());
	}

	/**
	 * Tests the behavior of {@link BasicPlanImpl.ActIterator}.
	 *
	 * @throws Exception
	 * @author mrieser
	 */
	public void testGetIteratorAct() throws Exception {
		Person person = new Person(new IdImpl("1"));
		Plan plan = person.createPlan(true);

		// test with empty plan, should not produce an error/exception
		BasicPlanImpl.ActIterator iter = plan.getIteratorAct();
		assertFalse(iter.hasNext());

		// add one act, no legs yet, test again
		Act act1 = plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIteratorAct();
		assertEquals(act1, iter.next());
		assertFalse(iter.hasNext());

		// add leg + act, test again
		plan.createLeg(BasicLeg.Mode.car);
		Act act2 = plan.createAct("w", new CoordImpl(100.0, 100.0));
		iter = plan.getIteratorAct();
		assertEquals(act1, iter.next());
		assertTrue(iter.hasNext());
		assertEquals(act2, iter.next());
		assertFalse(iter.hasNext());

		// add leg + act, test again. Now we have a generic plan with real first act, real last act, and real middle acts.
		plan.createLeg(BasicLeg.Mode.car);
		Act act3 = plan.createAct("h", new CoordImpl(0.0, 0.0));
		iter = plan.getIteratorAct();
		assertEquals(act1, iter.next());
		assertTrue(iter.hasNext());
		assertTrue(iter.hasNext());
		iter.hasNext(); // a call to hasNext() should not modify the internal structure, e.g. advancing the internal index...
		iter.hasNext(); // ...we will see that this is not the case in the next assert-statement:
		assertEquals(act2, iter.next());
		assertTrue(iter.hasNext());
		assertEquals(act3, iter.next());
		assertFalse(iter.hasNext());

		// test that an additional iter.next() results in an exception
		try {
			iter.next();
			fail("Expected Exception, but got none.");
		} catch (IndexOutOfBoundsException e) {
			log.info("Catched expected exception: " + e.getMessage());
		}

		// now test that two iterators are independent of each other
		iter = plan.getIteratorAct();
		BasicPlanImpl.ActIterator iter2 = plan.getIteratorAct();
		assertEquals(act1, iter.next());
		assertEquals(act2, iter.next());
		assertEquals(act1, iter2.next()); // iter2 is still at act1
		assertEquals(act3, iter.next());
		assertEquals(act2, iter2.next());
		assertFalse(iter.hasNext());
		assertTrue(iter2.hasNext());
	}

}
