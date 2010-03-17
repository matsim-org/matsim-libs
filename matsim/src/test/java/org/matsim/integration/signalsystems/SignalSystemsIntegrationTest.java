/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemIntegrationTest
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
package org.matsim.integration.signalsystems;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class SignalSystemsIntegrationTest extends MatsimTestCase {
  
  private static final Logger log = Logger.getLogger(SignalSystemsIntegrationTest.class);
  
	private final static String CONFIG_FILE_NAME = "signalSystemsIntegrationConfig.xml";

	public void testSignalSystems() {
		Config config = this.loadConfig(this.getClassInputDirectory() + CONFIG_FILE_NAME);
		config.controler().setOutputDirectory(this.getOutputDirectory());
		config.setQSimConfigGroup(new QSimConfigGroup());
		Controler c = new Controler(config);
		c.setCreateGraphs(false);
		c.run();
		
		String iterationOutput = this.getOutputDirectory() + "ITERS/it.10/";
		
		String eventsFileIn = this.getInputDirectory() + "10.events.xml.gz";
		String eventsFileOut = iterationOutput + "10.events.xml.gz";
		
		log.info("comparing " + eventsFileIn + " " + eventsFileOut);
		
		assertEquals("different events files", 
				CRCChecksum.getCRCFromFile(eventsFileIn), 
				CRCChecksum.getCRCFromFile(eventsFileOut));

		assertEquals("different population files", 
				CRCChecksum.getCRCFromFile(this.getInputDirectory() + "10.plans.xml.gz"), 
				CRCChecksum.getCRCFromFile(iterationOutput + "10.plans.xml.gz"));
	}
	
	
}
