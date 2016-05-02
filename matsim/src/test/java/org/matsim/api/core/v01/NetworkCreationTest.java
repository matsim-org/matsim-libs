/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCreationTest
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
package org.matsim.api.core.v01;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;


/**
 * Test of api based network creation
 * @author dgrether
 */
public class NetworkCreationTest extends MatsimTestCase {


	public void testCreateNetwork() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Id<Node> nodeId1 = Id.create("1", Node.class);
		Id<Node> nodeId2 = Id.create("2", Node.class);
		Coord coord1 = new Coord(0.0, 0.0);
		Coord coord2 = new Coord(300.0, 400.0);
		Network network = sc.getNetwork();
		//test default capacity period
		assertEquals(3600.0, network.getCapacityPeriod());
		//have to cast to NetworkFactory because coord is needed otherwise null pointer exception
		NetworkFactory nb = network.getFactory();
		Node n1 = nb.createNode(nodeId1, coord1);
		assertNotNull(n1);
		Node n2 = nb.createNode(nodeId2, coord2);
		assertNotNull(n2);
		//add before link creation really needed? I don't think so dg 09/09
		network.addNode(n1);
		network.addNode(n2);
		Link l1 = nb.createLink(Id.create(1, Link.class), n1, n2);
		//test defaults
		assertEquals(500.0, l1.getLength()); // euclidean link length
		assertEquals(1.0, l1.getCapacity());
		assertEquals(1.0, l1.getFreespeed());
		assertEquals(1.0, l1.getNumberOfLanes());
		assertEquals(1.0/3600.0, l1.getCapacity()/network.getCapacityPeriod(), MatsimTestCase.EPSILON);
		//the next lines are not obvious because only the references have been given to the builder
		assertEquals(n1, l1.getFromNode());
		assertEquals(n2, l1.getToNode());
		//change attributes
		l1.setLength(1000.0);
		assertEquals(1000.0, l1.getLength());
		l1.setFreespeed(100.0);
		assertEquals(100.0, l1.getFreespeed());
		l1.setCapacity(3600.0);
		assertEquals(3600.0, l1.getCapacity());
		//tests on LinkImpl
		assertEquals(1.0, l1.getCapacity()/network.getCapacityPeriod(), EPSILON);

		//add to network
		network.addLink(l1);
		//test for no side effects by adding to network
		assertEquals(1000.0, l1.getLength());
		assertEquals(100.0, l1.getFreespeed());
		assertEquals(3600.0, l1.getCapacity());
		assertEquals(1.0, l1.getCapacity()/network.getCapacityPeriod(), EPSILON);
	}
}
