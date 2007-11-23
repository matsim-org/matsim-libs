/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGeneratorTest.java
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

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.evacuation.EvacuationAreaFileReader;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.evacuation.EvacuationPlansGeneratorAndNetworkTrimmer;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;
import org.xml.sax.SAXException;

/**
 * @author glaemmel
 */
public class EvacuationPlansGeneratorTest extends MatsimTestCase {

	public final void testEvacuationPlansGenerator(){
//		final String configFile = getInputDirectory() + "evacuationConf.xml";
		final String evacuationAreaLinksFile = getInputDirectory() + "evacuationarea.xml.gz";
		final String referencePlans = getInputDirectory() + "final_evacuation_plans.xml.gz";
		final String positionInfo = getInputDirectory() + "positionInfoPlansFile09-00-00.xml.gz";
		final String networkFile = "test/scenarios/berlin/network.xml";
		final String testPlans = getOutputDirectory() + "evacuation_plans.xml";

		loadConfig(null);
		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(positionInfo);

		HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
		EvacuationAreaFileReader enfr = new EvacuationAreaFileReader(evacuationAreaLinks);
		try {
			enfr.readFile(evacuationAreaLinksFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EvacuationPlansGeneratorAndNetworkTrimmer epg = new EvacuationPlansGeneratorAndNetworkTrimmer();
		epg.generatePlans(population, network, evacuationAreaLinks);

		PlansWriter plansWriter = new PlansWriter(population, testPlans, "v4");
		plansWriter.write();

		// now compare the two files
		long cksum1 = CRCChecksum.getCRCFromGZFile(referencePlans);
		long cksum2 = CRCChecksum.getCRCFromFile(testPlans);

		assertEquals(cksum1, cksum2);
	}
}
