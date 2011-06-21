/* *********************************************************************** *
 * project: org.matsim.*
 * TestFitnessesEquivalence.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.fitness;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests whether the fitness "on the fly" and the fitness with the
 * "full decoder" give the same result.
 *
 * This is very important, as the plan returned by an optimisation
 * with an on the fly fitness is created by the full decoder.
 *
 * @author thibautd
 */
public class TestFitnessesEquivalence {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	@Ignore
	public void testFitnesses() {
	}
}

