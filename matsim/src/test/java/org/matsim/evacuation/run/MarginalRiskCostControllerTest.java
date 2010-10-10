/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.evacuation.run;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class MarginalRiskCostControllerTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(MarginalRiskCostControllerTest.class);
	
	public void testMarginalRiskCostController() {
		String config = getInputDirectory() + "config.xml";

		String refEventsFile = getInputDirectory() + "10.events.xml.gz";
		String testEventsFile = getOutputDirectory() +"ITERS/it.10/10.events.xml.gz";
			
		EvacuationQSimControllerII controler = new EvacuationQSimControllerII(new String [] {config});
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(10);
		controler.run();
		
		//it 10
		log.info("comparing events files: ");
		log.info(refEventsFile);
		log.info(testEventsFile);
		int i = EventsFileComparator.compare(refEventsFile, testEventsFile);
		assertEquals("different events-files in iteration 10.",0, i);
		
		

	}
}
