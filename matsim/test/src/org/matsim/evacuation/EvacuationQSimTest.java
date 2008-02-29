/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimTest.java
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

package org.matsim.evacuation;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class EvacuationQSimTest extends MatsimTestCase {

	public final void testEvacuationQSim(){
		String config = getInputDirectory() + "/config.xml";
		String referenceNetFile = getInputDirectory() + "/evacuation_net.xml";
		String referenceEventsFile = getInputDirectory() + "/events.txt.gz";
		String referencePlansFile = getInputDirectory() + "/plans.xml.gz";

		String netFile = getOutputDirectory() + "/evacuation_net.xml";
		String eventsFile = getOutputDirectory() + "/ITERS/it.10/10.events.txt.gz";
		String plansFile = getOutputDirectory() + "/output_plans.xml.gz";

		EvacuationQSimControler controler = new EvacuationQSimControler(new String[] {config});
		controler.setCreateGraphs(false);
		controler.run();

		System.out.println("checking network file ...");
		long checksum1 = CRCChecksum.getCRCFromFile(referenceNetFile);
		long checksum2 = CRCChecksum.getCRCFromFile(netFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different network files", checksum1, checksum2);

		System.out.println("checking events file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referenceEventsFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(eventsFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different events files", checksum1, checksum2);

		System.out.println("checking plans file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referencePlansFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(plansFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different plans files", checksum1, checksum2);
	}

}
