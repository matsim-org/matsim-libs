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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;


/**
 * Test of api based network creation
 * @author dgrether
 */
public class NetworkCreationTest extends MatsimTestCase {
	
	
	public void testCreateNetwork() {
		Scenario sc = new ScenarioImpl();

		Id id1 = sc.createId("1");
		Id id2 = sc.createId("2");
		Coord coord = sc.createCoord(0.0, 0.0);
		Network network = sc.getNetwork();
		//test default capacity period
		assertEquals(3600.0, network.getCapacityPeriod());
		//have to cast to NetworkFactory because coord is needed otherwise null pointer exception
		NetworkFactory nb = network.getFactory();
		Node n1 = nb.createNode(id1, coord);
		assertNotNull(n1);
		Node n2 = nb.createNode(id2, coord);
		assertNotNull(n2);
		//add before link creation really needed? I don't think so dg 09/09
		network.addNode(n1);
		network.addNode(n2);
		Link l1 = nb.createLink(id1, id1, id2);
		//test defaults
		assertEquals(1.0, l1.getLength());
		assertEquals(1.0, l1.getCapacity(Time.UNDEFINED_TIME));
		assertEquals(1.0, l1.getFreespeed(Time.UNDEFINED_TIME));
		assertEquals(1.0, l1.getNumberOfLanes(Time.UNDEFINED_TIME));
		LinkImpl ll1 = (LinkImpl) l1;
		assertEquals(1.0/3600.0, ll1.getFlowCapacity(Time.UNDEFINED_TIME), MatsimTestCase.EPSILON);
		//would expect this cause no add was invoked
//		assertNull(l1.getLayer());
		//the next lines are not obvious because only the references have been given to the builder
		assertEquals(n1, l1.getFromNode());
		assertEquals(n2, l1.getToNode());
		//change attributes
		l1.setLength(1000.0);
		assertEquals(1000.0, l1.getLength());
		l1.setFreespeed(100.0);
		assertEquals(100.0, l1.getFreespeed(Time.UNDEFINED_TIME));
		l1.setCapacity(3600.0);
		assertEquals(3600.0, l1.getCapacity(Time.UNDEFINED_TIME));
		//tests on LinkImpl
		assertEquals(10.0, ll1.getFreespeedTravelTime(Time.UNDEFINED_TIME));
		assertEquals(1.0, ll1.getFlowCapacity(Time.UNDEFINED_TIME), EPSILON);
		
		//add to network
		network.addLink(l1);
		assertNotNull(l1.getLayer());
		assertEquals(network, l1.getLayer());
		//test for no side effects by adding to network
		assertEquals(1000.0, l1.getLength());
		assertEquals(100.0, l1.getFreespeed(Time.UNDEFINED_TIME));
		assertEquals(3600.0, l1.getCapacity(Time.UNDEFINED_TIME));
		assertEquals(10.0, ll1.getFreespeedTravelTime(Time.UNDEFINED_TIME));
		assertEquals(1.0, ll1.getFlowCapacity(Time.UNDEFINED_TIME), EPSILON);
	}
}
