/* *********************************************************************** *
 * project: org.matsim.*
 * JointLegTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

/**
 * @author thibautd
 */
public class JointLegTest {
	/**
	 * Tests the behaviour of equals with the different constructors.
	 */
	@Test
	public void testEquals() {
		JointLeg leg = new JointLeg("mode", new PersonImpl(new IdImpl(1)));
		JointLeg copy = new JointLeg(leg);

		Assert.assertTrue(
				"leg generated with copy constructor not equal to the copied one",
				leg.equals(copy));

		LegImpl legImpl = new LegImpl("mode2");
		copy = new JointLeg(legImpl, leg);

		Assert.assertTrue(
				"leg generated with 'almost copy' constructor not equal to the copied one",
				leg.equals(copy));

		JointLeg leg2 = new JointLeg("mode", new PersonImpl(new IdImpl(1)));

		Assert.assertFalse(
				"two different legs found equal",
				leg.equals(leg2));
	}
}

