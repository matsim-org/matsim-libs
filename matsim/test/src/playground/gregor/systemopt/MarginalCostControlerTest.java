/* *********************************************************************** *
 * project: org.matsim.*
 * MarginalCostControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.systemopt;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.sims.socialcost.MarginalCostControler;

public class MarginalCostControlerTest extends MatsimTestCase {
	public void testSimpleChecksum() {
		
		String config = getInputDirectory() + "config.xml";
		String ref = getInputDirectory() + "events.txt.gz";
		String compare = getOutputDirectory() + "ITERS/it.10/10.events.txt.gz";
		new MarginalCostControler(new String [] {config}).run();
		assertEquals("different events-files.", CRCChecksum.getCRCFromFile(ref),	CRCChecksum.getCRCFromFile(compare));
		MatsimRandom.reset();
	}
}
