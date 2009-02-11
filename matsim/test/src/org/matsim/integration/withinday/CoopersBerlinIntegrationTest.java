/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.integration.withinday;

import org.matsim.config.Config;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.withinday.coopers.CoopersControler;

/**
 * Several integration tests for the withinday replanning implementation used for the Coopers project.
 * This tests use the ControlInputSB and the ControlInputMB travel time prediction model on a 
 * real world large scale Berlin metropolitan area network. The test population consists of
 * a 10 percent sample of the kutter berlin population. From this sample all persons are
 * simulated that are affected by the guidance, i.e. their plans consist at least one link
 * of the two guided routes.
 * 
 * @see org.matsim.integration.withinday.CoopersIntegrationTest for more granulated tests.
 * @author dgrether
 */
public class CoopersBerlinIntegrationTest extends MatsimTestCase {

	public void testBerlinReducedSB() {
		String eventsFileName = getOutputDirectory() + "ITERS/it.0/0.events.txt.gz";
		String referenceFileName = getInputDirectory() + "0.events.txt.gz";
		
		Config config = this.loadConfig(this.getInputDirectory() + "configSBAllFeatures.xml");
		String netFileName = this.getClassInputDirectory() + "wip_net_manipulated_new.xml.gz";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "wip_net_manipulatedChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);
		
		config.plans().setInputFile(this.getClassInputDirectory() + "filteredPlansTegel.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getInputDirectory() + "SBAllFeatures.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
				
		controler.run();

		long checksum1 = CRCChecksum.getCRCFromGZFile(referenceFileName);
		long checksum2 = CRCChecksum.getCRCFromGZFile(eventsFileName);
		assertEquals("different events-files.", checksum1, checksum2);
	}

	public void testBerlinReducedMB() {
		String eventsFileName = getOutputDirectory() + "ITERS/it.0/0.events.txt.gz";
		String referenceFileName = getInputDirectory() + "0.events.txt.gz";

		Config config = this.loadConfig(this.getInputDirectory() + "configMB.xml");
		String netFileName = this.getClassInputDirectory() + "wip_net_manipulated_new.xml.gz";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "wip_net_manipulatedChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);
		
		config.plans().setInputFile(this.getClassInputDirectory() + "filteredPlansTegel.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getInputDirectory() + "MB.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
				
		controler.run();
		
		long checksum1 = CRCChecksum.getCRCFromGZFile(referenceFileName);
		long checksum2 = CRCChecksum.getCRCFromGZFile(eventsFileName);
		assertEquals("different events-files.", checksum1, checksum2);
	}
	
}
