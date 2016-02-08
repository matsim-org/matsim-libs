/* *********************************************************************** *
 * project: org.matsim.*
 * LongLinkSplitterTest.java
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

package playground.southafrica.utilities.network;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class LongLinkSplitterTest {

	@Test
	public void testBuildNetwork() {
		Network nw = buildNetwork();
		Assert.assertTrue("Wrong number of nodes.", nw.getNodes().size() == 4);
		Assert.assertTrue("Wrong number of links.", nw.getLinks().size() == 4);
	}
	
	@Test
	public void testSplitNetwork(){
		Network nw = buildNetwork();
		Network newNetwork = LongLinkSplitter.splitNetwork(nw, 90.0);
		
		Assert.assertEquals("Wrong number of nodes.", 10l, newNetwork.getNodes().size());
		
		/* Node AB_n0001 */
		Assert.assertTrue("Node AB_n0001 not found", newNetwork.getNodes().containsKey(Id.createNodeId("AB_n0001")));
		Assert.assertEquals("Node AB_n0001 has wrong X", 50.0, newNetwork.getNodes().get(Id.createNodeId("AB_n0001")).getCoord().getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Node AB_n0001 has wrong Y", 50.0, newNetwork.getNodes().get(Id.createNodeId("AB_n0001")).getCoord().getY(), MatsimTestUtils.EPSILON);

		/* Node BC_n0001 */
		Assert.assertTrue("Node BC_n0001 not found", newNetwork.getNodes().containsKey(Id.createNodeId("BC_n0001")));
		Assert.assertEquals("Node BC_n0001 has wrong X", 166.0 + 2.0/3.0, newNetwork.getNodes().get(Id.createNodeId("BC_n0001")).getCoord().getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Node BC_n0001 has wrong Y", 66.0 + 2.0/3.0, newNetwork.getNodes().get(Id.createNodeId("BC_n0001")).getCoord().getY(), MatsimTestUtils.EPSILON);
		
		/* Node BC_n0002 */
		Assert.assertTrue("Node BC_n0002 not found", newNetwork.getNodes().containsKey(Id.createNodeId("BC_n0002")));
		Assert.assertEquals("Node BC_n0002 has wrong X", 233.0 + 1.0/3.0, newNetwork.getNodes().get(Id.createNodeId("BC_n0002")).getCoord().getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Node BC_n0002 has wrong Y", 33.0 + 1.0/3.0, newNetwork.getNodes().get(Id.createNodeId("BC_n0002")).getCoord().getY(), MatsimTestUtils.EPSILON);

		/* Number of links. */
		Assert.assertEquals("Wrong number of links.", 10l, newNetwork.getLinks().size());
		
		/* Link AB */
		Assert.assertTrue("Link AB_l0001 not found", newNetwork.getLinks().containsKey(Id.createLinkId("AB_l0001")));
		Assert.assertTrue("Link AB_l0002 not found", newNetwork.getLinks().containsKey(Id.createLinkId("AB_l0002")));
		Assert.assertEquals("Wrong length for link AB_l0001", nw.getLinks().get(Id.createLinkId("AB")).getLength() / 2.0, 
				newNetwork.getLinks().get(Id.createLinkId("AB_l0001")).getLength(), MatsimTestUtils.EPSILON);
		
		/* Link BC */
		Assert.assertTrue("Link BC_l0001 not found", newNetwork.getLinks().containsKey(Id.createLinkId("BC_l0001")));
		Assert.assertTrue("Link BC_l0002 not found", newNetwork.getLinks().containsKey(Id.createLinkId("BC_l0002")));
		Assert.assertTrue("Link BC_l0003 not found", newNetwork.getLinks().containsKey(Id.createLinkId("BC_l0003")));
		Assert.assertEquals("Wrong length for link BC_l0001", nw.getLinks().get(Id.createLinkId("BC")).getLength() / 3.0, 
				newNetwork.getLinks().get(Id.createLinkId("BC_l0001")).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong length for link BC_l0002", nw.getLinks().get(Id.createLinkId("BC")).getLength() / 3.0, 
				newNetwork.getLinks().get(Id.createLinkId("BC_l0002")).getLength(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong length for link BC_l0003", nw.getLinks().get(Id.createLinkId("BC")).getLength() / 3.0, 
				newNetwork.getLinks().get(Id.createLinkId("BC_l0003")).getLength(), MatsimTestUtils.EPSILON);
	}
	
	@Test
	@Ignore
	public void writeNetworks(){
		Network nw1 = buildNetwork();
		new NetworkWriter(nw1).write("/Users/jwjoubert/Downloads/nw1.xml");
		
		Network nw2 = LongLinkSplitter.splitNetwork(nw1, 100.0);
		new NetworkWriter(nw2).write("/Users/jwjoubert/Downloads/nw2.xml");
	}
	
	
	/**
	 * Building a small network with four nodes and four links, forming a 
	 * loop: AB, BC, CD and DA.
	 * 
	 *            B (100,100)
	 *           /`~.
	 *          /    `~.
	 *         /        `~.
	 *        /            `~.
	 *      A \            .~' C
	 *  (0,0)  \        .~'    (300, 0)
	 *          \    .~'
	 *           \.~'
	 *            D (100, -100)
	 * 
	 * @return
	 */
	private Network buildNetwork(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network nw = NetworkUtils.createNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node a = nf.createNode(Id.createNodeId("A"), new Coord(0.0, 0.0)); nw.addNode(a);
		Node b = nf.createNode(Id.createNodeId("B"), new Coord(100.0, 100.0)); nw.addNode(b);
		Node c = nf.createNode(Id.createNodeId("C"), new Coord(300.0, 0.0)); nw.addNode(c);
		Node d = nf.createNode(Id.createNodeId("D"), new Coord(100.0, -100.0)); nw.addNode(d);
		
		double lengthShort = Math.sqrt(Math.pow(100.0, 2.0)+ Math.pow(100.0, 2.0)); 
		double lengthLong = Math.sqrt(Math.pow(200.0, 2.0)+ Math.pow(100.0, 2.0)); 
		Link ab = nf.createLink(Id.createLinkId("AB"), a, b); ab.setLength(lengthShort); nw.addLink(ab);
		Link bc = nf.createLink(Id.createLinkId("BC"), b, c); bc.setLength(lengthLong);	nw.addLink(bc);
		Link cd = nf.createLink(Id.createLinkId("CD"), c, d); cd.setLength(lengthLong);	nw.addLink(cd);
		Link da = nf.createLink(Id.createLinkId("DA"), d, a); da.setLength(lengthShort); nw.addLink(da);
		
		return nw;
	}

}
