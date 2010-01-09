/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLeastCostPathCalculatorTest.java
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

package org.matsim.core.router;

import java.io.IOException;
import java.util.EnumSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class AbstractLeastCostPathCalculatorTest extends MatsimTestCase {
	
	protected abstract LeastCostPathCalculator getLeastCostPathCalculator(final NetworkLayer network);

	private static final String MODE_RESTRICTION_NOT_SUPPORTED = "Router algo does not support mode restrictions. ";
	
	public void testCalcLeastCostPath_Normal() throws SAXException, ParserConfigurationException, IOException {
		loadConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse("test/scenarios/equil/network.xml");
		NodeImpl node12 = network.getNodes().get(new IdImpl("12"));
		NodeImpl node15 = network.getNodes().get(new IdImpl("15"));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(node12, node15, 8.0*3600);

		assertEquals("number of nodes wrong.", 4, path.nodes.size());
		assertEquals("number of links wrong.", 3, path.links.size());
		assertEquals(network.getNodes().get(new IdImpl("12")), path.nodes.get(0));
		assertEquals(network.getNodes().get(new IdImpl("13")), path.nodes.get(1));
		assertEquals(network.getNodes().get(new IdImpl("14")), path.nodes.get(2));
		assertEquals(network.getNodes().get(new IdImpl("15")), path.nodes.get(3));
		assertEquals(network.getLinks().get(new IdImpl("20")), path.links.get(0));
		assertEquals(network.getLinks().get(new IdImpl("21")), path.links.get(1));
		assertEquals(network.getLinks().get(new IdImpl("22")), path.links.get(2));
	}

	public void testCalcLeastCostPath_SameFromTo() throws SAXException, ParserConfigurationException, IOException {
		loadConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse("test/scenarios/equil/network.xml");
		NodeImpl node12 = network.getNodes().get(new IdImpl("12"));
		
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(node12, node12, 8.0*3600);
		
		assertEquals("number of nodes wrong.", 1, path.nodes.size());
		assertEquals("number of links wrong.", 0, path.links.size());
		assertEquals(network.getNodes().get(new IdImpl("12")), path.nodes.get(0));
	}
	
	/**
	 * Tests route finding with a mode restriction of exactly one mode.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_PossibleRoutes() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;

			d.setModeRestriction(EnumSet.of(TransportMode.car));
			Path p = d.calcLeastCostPath(f.nodes[0], f.nodes[1], 6.0*3600.0);
			assertEquals("wrong number of links.", 2, p.links.size());

			d.setModeRestriction(EnumSet.of(TransportMode.bus));
			p = d.calcLeastCostPath(f.nodes[4], f.nodes[6], 6.0*3600.0);
			assertEquals("wrong number of links.", 3, p.links.size());
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction of exactly one mode,
	 * looking for impossible routes within that mode.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_ImpossibleRoutes() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.car));
			Path p = d.calcLeastCostPath(f.nodes[0], f.nodes[7], 6.0*3600.0);
			assertNull("no path should be possible", p);

			d.setModeRestriction(EnumSet.of(TransportMode.bus));
			p = d.calcLeastCostPath(f.nodes[0], f.nodes[6], 6.0*3600.0);
			assertNull("no path should be possible", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction of two modes,
	 * where both modes are required to reach the target.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_TwoModes() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.car, TransportMode.bus));
			Path p = d.calcLeastCostPath(f.nodes[0], f.nodes[2], 6.0*3600.0);
			assertNotNull("path should be possible", p);
			assertEquals("wrong number of links", 2, p.links.size());
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction to no modes,
	 * resulting in never-satisfiable route-request.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_NoMode() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.noneOf(TransportMode.class));
			Path p = d.calcLeastCostPath(f.nodes[0], f.nodes[1], 6.0*3600.0);
			assertNull("no path should be possible", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with mode restrictions disabled, effectively
	 * allowing all modes.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_NullMode() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(null);
			Path p = d.calcLeastCostPath(f.nodes[0], f.nodes[1], 6.0*3600.0);
			assertNotNull("path should be possible", p);
			assertEquals("wrong number of links", 1, p.links.size());
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction, where the first possible
	 * link has a wrong allowed mode. Tests the (possible) special case of handling
	 * the first node to expand.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_BadStartLink() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.bus));
			Path p = d.calcLeastCostPath(f.nodes[1], f.nodes[6], 6.0*3600.0);
			assertNull("no path should be possible", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction, where the last possible
	 * link has a wrong allowed mode. Tests the (possible) special case of
	 * handling the arrival node.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_BadEndLink() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.car));
			Path p = d.calcLeastCostPath(f.nodes[1], f.nodes[6], 6.0*3600.0);
			assertNull("no path should be possible", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}

	/**
	 * Tests route finding with a mode restriction from one node to another
	 * node, directly connected with a link, but with wrong allowed mode.
	 * Tests the (possible) special case of no real node-expansion needed,
	 * e.g. in Dijkstra.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_BadLink() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.bus));
			Path p = d.calcLeastCostPath(f.nodes[1], f.nodes[2], 6.0*3600.0);
			assertNull("no path should be possible", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	/**
	 * Tests route finding with a mode restriction, ending in a dead end.
	 * Some optimizations try to recognize dead ends in the network and 
	 * handle them specially, so let's try they work as expected.
	 */
	public void testCalcLeastCostPath_MultiModeNetwork_OneMode_DeadEndLink() {
		loadConfig(null);
		MultiModeFixture f = new MultiModeFixture();
		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(f.network);
		if (routerAlgo instanceof Dijkstra) {
			Dijkstra d = (Dijkstra) routerAlgo;
			
			d.setModeRestriction(EnumSet.of(TransportMode.car));
			Path p = d.calcLeastCostPath(f.nodes[4], f.nodes[3], 6.0*3600.0);
			assertNotNull("path should be possible for car.", p);

			d.setModeRestriction(EnumSet.of(TransportMode.bus));
			p = d.calcLeastCostPath(f.nodes[4], f.nodes[3], 6.0*3600.0);
			assertNull("no path should be possible for bus.", p);
		} else {
			fail(MODE_RESTRICTION_NOT_SUPPORTED + routerAlgo.getClass().getName());
		}
	}
	
	
	
	/**
	 * Provides a simple, multi-modal network for tests.
	 * 
	 * <pre>
	 *         cb            c            b
	 *   (4)----9---->(5)---10---->(6)---11---->(7)      availableModes = car:
	 *    ^          ^ | \          ^          ^           links: 1, 2, 3, 4, 5, 9, 10   (c)
	 *    |         /  |  \         |         /
	 *    |        /   |   \        |        /            availableModes = bus:
	 *    |       /    |    \       |      /               links: 0, 6, 7, 8, 9, 11   (b)
	 *   3-c   4-c    5-c    6-b   7-b   8-b         
	 *    |   /        |        \   |   /                 node 3 is bus-only, node 5 is car-only
	 *    |  /         |         \  |  /
	 *    | /          |          \ | /                   all links:
	 *    |/    b      v     c     v|/    c                 length = 1000m, capacity = 3600.0veh/h
	 *   (0)----0---->(1)----1---->(2)----2---->(3)         freespeed = 100m/s (=10s travel time)
	 * </pre>
	 * 
	 * Some important characteristics of that network:
	 * <ul>
	 * <li>Node 7 can only be reached by bus, node 3 only by car</li>
	 * <li>car-alternative route from node 0 to node 5 (link 4, or link3 + link 9)</li>
	 * <li>bus-alternative route from node 2 to node 7</li>
	 * <li>bus-only link 0 not connect to other bus-links</li>
	 * <li>link 9 allows bus and car</li>
	 * <li>dead-end at node 3</li>
	 * <li>node 1 only allows departure by car, node 6 only by bus</li>
	 * </ul>
	 *
	 * @author mrieser
	 */
	private static class MultiModeFixture {
		/*package*/ final NetworkLayer network = new NetworkLayer();
		/*package*/ final Node[] nodes = new NodeImpl[8];
		/*package*/ final Link[] links = new LinkImpl[12];

		public MultiModeFixture() {
			this.network.setCapacityPeriod(3600.0);
			this.nodes[0] = network.createAndAddNode(new IdImpl(0), new CoordImpl(0, 0));
			this.nodes[1] = network.createAndAddNode(new IdImpl(1), new CoordImpl(1000, 0));
			this.nodes[2] = network.createAndAddNode(new IdImpl(2), new CoordImpl(2000, 0));
			this.nodes[3] = network.createAndAddNode(new IdImpl(3), new CoordImpl(3000, 0));
			this.nodes[4] = network.createAndAddNode(new IdImpl(4), new CoordImpl(0, 1000));
			this.nodes[5] = network.createAndAddNode(new IdImpl(5), new CoordImpl(1000, 1000));
			this.nodes[6] = network.createAndAddNode(new IdImpl(6), new CoordImpl(2000, 1000));
			this.nodes[7] = network.createAndAddNode(new IdImpl(7), new CoordImpl(3000, 1000));
			this.links[0] = network.createAndAddLink(new IdImpl(0), this.nodes[0], this.nodes[1], 1000.0, 100.0, 3600.0, 1.0);
			this.links[1] = network.createAndAddLink(new IdImpl(1), this.nodes[1], this.nodes[2], 1000.0, 100.0, 3600.0, 1.0);
			this.links[2] = network.createAndAddLink(new IdImpl(2), this.nodes[2], this.nodes[3], 1000.0, 100.0, 3600.0, 1.0);
			this.links[3] = network.createAndAddLink(new IdImpl(3), this.nodes[0], this.nodes[4], 1000.0, 100.0, 3600.0, 1.0);
			this.links[4] = network.createAndAddLink(new IdImpl(4), this.nodes[0], this.nodes[5], 1000.0, 100.0, 3600.0, 1.0);
			this.links[5] = network.createAndAddLink(new IdImpl(5), this.nodes[5], this.nodes[1], 1000.0, 100.0, 3600.0, 1.0);
			this.links[6] = network.createAndAddLink(new IdImpl(6), this.nodes[5], this.nodes[2], 1000.0, 100.0, 3600.0, 1.0);
			this.links[7] = network.createAndAddLink(new IdImpl(7), this.nodes[2], this.nodes[6], 1000.0, 100.0, 3600.0, 1.0);
			this.links[8] = network.createAndAddLink(new IdImpl(8), this.nodes[2], this.nodes[7], 1000.0, 100.0, 3600.0, 1.0);
			this.links[9] = network.createAndAddLink(new IdImpl(9), this.nodes[4], this.nodes[5], 1000.0, 100.0, 3600.0, 1.0);
			this.links[10] = network.createAndAddLink(new IdImpl(10), this.nodes[5], this.nodes[6], 1000.0, 100.0, 3600.0, 1.0);
			this.links[11] = network.createAndAddLink(new IdImpl(11), this.nodes[6], this.nodes[7], 1000.0, 100.0, 3600.0, 1.0);
			EnumSet<TransportMode> carOnly = EnumSet.of(TransportMode.car);
			EnumSet<TransportMode> busOnly = EnumSet.of(TransportMode.bus);
			EnumSet<TransportMode> carNBus = EnumSet.of(TransportMode.car, TransportMode.bus);
			this.links[0].setAllowedModes(busOnly);
			this.links[1].setAllowedModes(carOnly);
			this.links[2].setAllowedModes(carOnly);
			this.links[3].setAllowedModes(carOnly);
			this.links[4].setAllowedModes(carOnly);
			this.links[5].setAllowedModes(carOnly);
			this.links[6].setAllowedModes(busOnly);
			this.links[7].setAllowedModes(busOnly);
			this.links[8].setAllowedModes(busOnly);
			this.links[9].setAllowedModes(carNBus);
			this.links[10].setAllowedModes(carOnly);
			this.links[11].setAllowedModes(busOnly);
		}
	}
	
}
