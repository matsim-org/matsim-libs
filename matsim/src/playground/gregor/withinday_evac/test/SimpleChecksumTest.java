/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleChecksumTest.java
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

package playground.gregor.withinday_evac.test;

import org.matsim.gbl.MatsimRandom;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

import playground.gregor.withinday_evac.controler.WithindayControler;

public class SimpleChecksumTest extends MatsimTestCase {

	public void testSimpleChecksum() {
		String config = "./src/playground/gregor/withinday_evac/test/config.xml";
		String ref = "./src/playground/gregor/withinday_evac/test/events.txt.gz";
		String compare = "./withinday_evac/ITERS/it.10/10.events.txt.gz";
		new WithindayControler(new String [] {config}).run();
		
		MatsimRandom.reset();
		assertEquals(CRCChecksum.getCRCFromGZFile(ref),	CRCChecksum.getCRCFromGZFile(compare));
		
	}
}
