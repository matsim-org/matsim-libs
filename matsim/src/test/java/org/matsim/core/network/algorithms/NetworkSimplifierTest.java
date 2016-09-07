/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSimplifierThresholdTest.java
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

package org.matsim.core.network.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkSimplifierTest {

	@Test
	public void testBuildNetwork(){
		Network network = buildNetwork();
		assertEquals("Wrong number of nodes.",  6, network.getNodes().size());
		assertEquals("Wrong number of links.",  5, network.getLinks().size());
	}
	
	
	@Test
	public void testRun() {
		Network network = buildNetwork();

		NetworkSimplifier nst = new NetworkSimplifier();
		nst.run(network, 20.0);
		assertEquals("Wrong number of links", 3, network.getLinks().size());
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC")));
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CD")));
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("DE-EF")));
	}

	
	@Test
	public void testRunMergeLinkStats() {
		Network network = buildNetwork();
		
		NetworkSimplifier nst = new NetworkSimplifier();
		nst.setMergeLinkStats(true);
		nst.run(network, 20.0);
		assertEquals("Wrong number of links", 2, network.getLinks().size());
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC")));
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CD-DE-EF")));
		
		network = buildNetwork();
		nst.run(network, 40.0);
		assertEquals("Wrong number of links", 1, network.getLinks().size());
		assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")));
		
		network = buildNetwork();
		nst.run(network, 5.0);
		assertEquals("Wrong number of links", 5, network.getLinks().size());
	}
	
	
	/**
	 * Builds a test network like the following diagram.
	 * 
	 * A--->B--->C===>D--->E--->F
	 * 
	 * with each link having length 10m. Links AB, BC, DE, and EF have one 
	 * lanes each, while CD has two lanes. All free-flow speeds are 60km/h.
	 * 
	 * @return
	 */
	private Network buildNetwork(){
		Network network = NetworkUtils.createNetwork();
		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), CoordUtils.createCoord(0.0,  0.0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), CoordUtils.createCoord(10.0,  0.0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), CoordUtils.createCoord(20.0,  0.0));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), CoordUtils.createCoord(30.0,  0.0));
		Node e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), CoordUtils.createCoord(40.0,  0.0));
		Node f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), CoordUtils.createCoord(50.0,  0.0));
		
		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), a, b, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), b, c, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("CD"), c, d, 10.0, 60.0/3.6, 1000.0, 2);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("DE"), d, e, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("EF"), e, f, 10.0, 60.0/3.6, 1000.0, 1);
		
		return network;
	}

}
