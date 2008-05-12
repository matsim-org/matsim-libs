/* *********************************************************************** *
 * project: org.matsim.*
 * RouterVisTest.java
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

package org.matsim.utils.vis.routervis;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

/**
 * @author glaemmel
 */
public class RouterVisTest extends MatsimTestCase {
	
	private static final Logger log = Logger.getLogger(RouterVisTest.class);
	
	public void testRouterVis(){
		Config config = loadConfig(getInputDirectory() + "config.xml");
		// read network
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());

		// calculate reference checksums
		String visConfigFile = getInputDirectory() + "SnapshotCONFIG.vis";
		long referenceChecksumConfig = CRCChecksum.getCRCFromFile(visConfigFile);
		log.info("Reference checksum config = " + referenceChecksumConfig + " file: " + visConfigFile);

		String visSnapshotFile = getInputDirectory() + "Snapshot00-00-00.vis";
		long referenceChecksumSnapshot = CRCChecksum.getCRCFromFile(visSnapshotFile);
		log.info("Reference checksum snapshot = " + referenceChecksumSnapshot + " file: " + visSnapshotFile);

		// run test
		Node fromNode = network.getNode("13");
		Node toNode = network.getNode("7");

		TravelTimeI costCalc = new FreespeedTravelTimeCost();
		RouterVis routerVis = new RouterVis(network, (TravelCostI) costCalc, costCalc);

		routerVis.runRouter(fromNode, toNode, 0.0);

		// check results
		String outDir = getOutputDirectory();
		String outConfig = outDir + "SnapshotCONFIG.vis";
		long checksumConfig = CRCChecksum.getCRCFromFile(outConfig);

		String outSnapshot = outDir + "Snapshot00-00-00.vis";
		long checksumSnapshot = CRCChecksum.getCRCFromFile(outSnapshot);

		assertEquals("different config files", referenceChecksumConfig, checksumConfig);
		assertEquals("different snapshot files", referenceChecksumSnapshot, checksumSnapshot);
	}
}
