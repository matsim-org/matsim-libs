/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package tutorial.programming.createNetworkExample;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author tthunig
 */
public class RunCreateNetworkExample {

	// capacity at the links that all agents have to use
	private static final long CAP_FIRST_LAST = 3600; // [veh/h]
	// capacity at all other links
	private static final long CAP_MAIN = 1800; // [veh/h]

	// link length for all other links
	private static final long LINK_LENGTH = 200; // [m]

	// travel time for the middle link
	private static final double LINK_TT_MID = 1 * 60;
	// travel time for the middle route links
	private static final double LINK_TT_SMALL = 1 * 60; // [s]
	// travel time for the two remaining outer route links (choose at least
	// 3*LINK_TT_SMALL!)
	private static final double LINK_TT_BIG = 10 * 60; // [s]
	// travel time for links that all agents have to use
	private static final double MINIMAL_LINK_TT = 1; // [s]

	
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network net = scenario.getNetwork();
		NetworkFactory fac = net.getFactory();

		// create nodes
		Node n0 = fac.createNode(Id.createNodeId(0), new Coord(-200, 200));
		net.addNode(n0);
		Node n1 = fac.createNode(Id.createNodeId(1), new Coord(0, 200));
		net.addNode(n1);
		Node n2 = fac.createNode(Id.createNodeId(2), new Coord(200, 200));
		net.addNode(n2);
		Node n3 = fac.createNode(Id.createNodeId(3), new Coord(400, 400));
		net.addNode(n3);
		Node n4 = fac.createNode(Id.createNodeId(4), new Coord(400, 0));
		net.addNode(n4);
		Node n5 = fac.createNode(Id.createNodeId(5), new Coord(600, 200));
		net.addNode(n5);
		Node n6 = fac.createNode(Id.createNodeId(6), new Coord(800, 200));
		net.addNode(n6);

		// create links
		Link l = fac.createLink(Id.createLinkId("0_1"), n0, n1);
		setLinkAttributes(l, CAP_FIRST_LAST, LINK_LENGTH, MINIMAL_LINK_TT);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("1_2"), n1, n2);
		setLinkAttributes(l, CAP_FIRST_LAST, LINK_LENGTH, MINIMAL_LINK_TT);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("2_3"), n2, n3);
		setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, LINK_TT_SMALL);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("2_4"), n2, n4);
		setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, LINK_TT_BIG);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("3_4"), n3, n4);
		setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, LINK_TT_MID);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("3_5"), n3, n5);
		setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, LINK_TT_BIG);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("4_5"), n4, n5);
		setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, LINK_TT_SMALL);
		net.addLink(l);
		l = fac.createLink(Id.createLinkId("5_6"), n5, n6);
		setLinkAttributes(l, CAP_FIRST_LAST, LINK_LENGTH, MINIMAL_LINK_TT);
		net.addLink(l);

		// write network
		new NetworkWriter(net).write("output/network.xml");
	}

	
	private static void setLinkAttributes(Link link, double capacity, double length, double travelTime) {
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(link.getLength() / (travelTime - 0.1));
	}

}
