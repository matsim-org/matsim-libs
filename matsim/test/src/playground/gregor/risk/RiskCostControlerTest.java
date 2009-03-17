/* *********************************************************************** *
 * project: org.matsim.*
 * RiskCostControlerTest.java
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

package playground.gregor.risk;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;


public class RiskCostControlerTest extends MatsimTestCase{
	
	public void testSimpleChecksum() {
		
		String config = getInputDirectory() + "config.xml";
		String ref = getInputDirectory() + "events.txt.gz";
		String compare = getOutputDirectory() + "ITERS/it.10/10.events.txt.gz";
		new RiskCostControler(new String [] {config}).run();
		assertEquals("different events-files.", CRCChecksum.getCRCFromFile(ref),	CRCChecksum.getCRCFromFile(compare));
	}
}
