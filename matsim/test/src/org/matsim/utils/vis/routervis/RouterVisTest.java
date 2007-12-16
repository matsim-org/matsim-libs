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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.Node;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.identifiers.IdI;

public class RouterVisTest extends MatsimTestCase{
	private NetworkLayer network;
	private long referenceChecksumConfig;
	private long referenceChecksumSnapshot;

	public void testRouterVis(){
		init("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		
	
		Node fromNode = network.getNode("13");
		Node toNode = network.getNode("7");
		
		
		TravelTimeI costCalc = new FreespeedTravelTimeCost();
		RouterVis routerVis = new RouterVis(network,(TravelCostI) costCalc,costCalc);
		
		routerVis.runRouter(fromNode, toNode, 0.0);
		
		String outDir = getOutputDirectory();
		
		String outConfig = outDir + "/SnapshotCONFIG.vis";
		long checksumConfig = CRCChecksum.getCRCFromFile(outConfig);
		
		
		String outSnapshot = outDir + "/Snapshot00-00-00.vis";
		long checksumSnapshot = CRCChecksum.getCRCFromFile(outSnapshot);
		
		assertEquals(this.referenceChecksumConfig,checksumConfig);
		assertEquals(this.referenceChecksumSnapshot,checksumSnapshot);

	}

	private void init(String configFile) {
		loadConfig(configFile);
		this.network = readNetwork();
		String visConfigFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/SnapshotCONFIG.vis";
		this.referenceChecksumConfig = CRCChecksum.getCRCFromFile(visConfigFile);
		System.out.println("Reference checksum config = " + this.referenceChecksumConfig + " file: " + visConfigFile);
		
		String visSnapshotFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/Snapshot00-00-00.vis";
		this.referenceChecksumSnapshot = CRCChecksum.getCRCFromFile(visSnapshotFile);
		System.out.println("Reference checksum snapshot = " + this.referenceChecksumSnapshot + " file: " + visSnapshotFile);
		System.out.println();
	}
	
	private NetworkLayer readNetwork() {
		System.out.println("  reading the network...");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		return network;
	}
}
