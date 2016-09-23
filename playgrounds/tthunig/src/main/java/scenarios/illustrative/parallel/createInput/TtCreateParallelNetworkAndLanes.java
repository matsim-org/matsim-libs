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
package scenarios.illustrative.parallel.createInput;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to create network and lanes for the parallel scenario.
 *
 * Set the capacity and choose whether you want to have a third and fourth OD
 * pair (the vertically relation - in addition to the horizontally relation)
 * before calling the method createNetworkWithLanes().
 *
 * Network without second ODPair:
 *
 *                           (3)---------(4)
 *                          ´               `
 *                        ´                   `
 * (a)-------(1)-------(2)                     (5)-------(6)-------(b)
 *                        `                   ´
 *		                    `               ´
 *			                 (7)---------(8)
 *
 *
 * Network with second ODPair:
 *
 *                                 (c)
 *                                  |
 *                                  |
 *                                  |
 *                                 (9)
 *                                  |
 *                                  |
 *                                  |
 * 		      	                   (10)
 * 	                              ´   `
 *                              ´       `
 *                           (3)---------(4)
 *                          ´ |           | `
 *                        ´   |           |   `
 * (a)-------(1)-------(2)    |           |    (5)-------(6)-------(b)
 *                        `   |           |   ´
 *                          ` |           | ´
 *                           (7)---------(8)
 *                              `       ´
 *                                `   ´
 *                                 (11)
 * 					                |
 * 	                                |
 * 	                                |
 *                                 (12)
 *                                  |
 *                                  |
 *                                  |
 *                                 (d)
 *
 * @author gthunig
 * 
 */
public final class TtCreateParallelNetworkAndLanes {

	private static final Logger log = Logger.getLogger(TtCreateParallelNetworkAndLanes.class);

	private Scenario scenario;

	private static final double LINK_LENGTH = 300.0; // m
	private static final double FREESPEED = 10.0; // m/s

	private double capacity; // veh/h

	private boolean useSecondODPair = false;

	private Map<String, Id<Link>> links = new HashMap<>();

	public TtCreateParallelNetworkAndLanes(Scenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Creates the Network for the parallel scenario and the required lanes.
     */
	public void createNetworkWithLanes() {
        log.info("Create network and lanes ...");

		Network net = this.scenario.getNetwork();
		if (net.getCapacityPeriod() != 3600.0){
			throw new IllegalStateException();
		}
		((Network)net).setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();

		// create nodes

		double scale = LINK_LENGTH;
		Node na, nb, n1, n2, n3, n4, n5, n6, n7, n8;
        net.addNode(na = fac.createNode(Id.create("a", Node.class), new Coord(0.0, 0.0)));
        net.addNode(nb = fac.createNode(Id.create("b", Node.class), new Coord(7.0 * scale, 0.0)));
		net.addNode(n1 = fac.createNode(Id.create(1, Node.class), new Coord(1.0 * scale, 0.0)));
		net.addNode(n2 = fac.createNode(Id.create(2, Node.class), new Coord(2.0 * scale, 0.0)));
		net.addNode(n3 = fac.createNode(Id.create(3, Node.class), new Coord(3.0 * scale, 1.0 * scale)));
		net.addNode(n4 = fac.createNode(Id.create(4, Node.class), new Coord(4.0 * scale, 1.0 * scale)));
		net.addNode(n5 = fac.createNode(Id.create(5, Node.class), new Coord(5.0 * scale, 0.0)));
		net.addNode(n6 = fac.createNode(Id.create(6, Node.class), new Coord(6.0 * scale, 0.0)));
		net.addNode(n7 = fac.createNode(Id.create(7, Node.class), new Coord(3.0 * scale, -1.0 * scale)));
		net.addNode(n8 = fac.createNode(Id.create(8, Node.class), new Coord(4.0 * scale, -1.0 * scale)));
		Node nc = null, nd = null, n9 = null, n10 = null, n11 = null, n12 = null;
		if (useSecondODPair) {
            net.addNode(nc = fac.createNode(Id.create("c", Node.class), new Coord(3.5 * scale, 4.0 * scale)));
            net.addNode(nd = fac.createNode(Id.create("d", Node.class), new Coord(3.5 * scale, -4.0 * scale)));
			net.addNode(n9 = fac.createNode(Id.create(9, Node.class), new Coord(3.5 * scale, 3.0 * scale)));
			net.addNode(n10 = fac.createNode(Id.create(10, Node.class), new Coord(3.5 * scale, 2.0 * scale)));
			net.addNode(n11 = fac.createNode(Id.create(11, Node.class), new Coord(3.5 * scale, -2.0 * scale)));
			net.addNode(n12 = fac.createNode(Id.create(12, Node.class), new Coord(3.5 * scale, -3.0 * scale)));
		}
		
		// create links

		initLinkIds();

        Link l = fac.createLink(links.get("a_1"), na, n1);
        setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
        net.addLink(l);
        l = fac.createLink(links.get("1_a"), n1, na);
        setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
        net.addLink(l);
        l = fac.createLink(links.get("6_b"), n6, nb);
        setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
        net.addLink(l);
        l = fac.createLink(links.get("b_6"), nb, n6);
        setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
        net.addLink(l);
		l = fac.createLink(links.get("1_2"), n1, n2);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_1"), n2, n1);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_3"), n2, n3);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("3_2"), n3, n2);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("3_4"), n3, n4);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("4_3"), n4, n3);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("4_5"), n4, n5);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_4"), n5, n4);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_6"), n5, n6);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("6_5"), n6, n5);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_7"), n2, n7);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("7_2"), n7, n2);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("7_8"), n7, n8);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("8_7"), n8, n7);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("8_5"), n8, n5);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_8"), n5, n8);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);

		if (useSecondODPair) {
            l = fac.createLink(links.get("9_c"), n9, nc);
            setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
            net.addLink(l);
            l = fac.createLink(links.get("c_9"), nc, n9);
            setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
            net.addLink(l);
            l = fac.createLink(links.get("12_d"), n12, nd);
            setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
            net.addLink(l);
            l = fac.createLink(links.get("d_12"), nd, n12);
            setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
            net.addLink(l);
			l = fac.createLink(links.get("3_7"), n3, n7);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("7_3"), n7, n3);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("4_8"), n4, n8);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("8_4"), n8, n4);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("3_10"), n3, n10);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("10_3"), n10, n3);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("4_10"), n4, n10);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("10_4"), n10, n4);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("9_10"), n9, n10);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("10_9"), n10, n9);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("7_11"), n7, n11);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_7"), n11, n7);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("8_11"), n8, n11);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_8"), n11, n8);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_12"), n11, n12);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("12_11"), n12, n11);
			setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
			net.addLink(l);
		}
		createLanes();
	}

	private void initLinkIds() {
        links.put("a_1", Id.create("a_1", Link.class));
        links.put("1_a", Id.create("1_a", Link.class));
        links.put("6_b", Id.create("6_b", Link.class));
        links.put("b_6", Id.create("b_6", Link.class));
		links.put("1_2", Id.create("1_2", Link.class));
		links.put("2_1", Id.create("2_1", Link.class));
		links.put("2_3", Id.create("2_3", Link.class));
		links.put("3_2", Id.create("3_2", Link.class));
		links.put("3_4", Id.create("3_4", Link.class));
		links.put("4_3", Id.create("4_3", Link.class));
		links.put("4_5", Id.create("4_5", Link.class));
		links.put("5_4", Id.create("5_4", Link.class));
		links.put("5_6", Id.create("5_6", Link.class));
		links.put("6_5", Id.create("6_5", Link.class));
		links.put("2_7", Id.create("2_7", Link.class));
		links.put("7_2", Id.create("7_2", Link.class));
		links.put("7_8", Id.create("7_8", Link.class));
		links.put("8_7", Id.create("8_7", Link.class));
		links.put("5_8", Id.create("5_8", Link.class));
		links.put("8_5", Id.create("8_5", Link.class));
		if (useSecondODPair) {
            links.put("9_c", Id.create("9_c", Link.class));
            links.put("c_9", Id.create("c_9", Link.class));
            links.put("12_d", Id.create("12_d", Link.class));
            links.put("d_12", Id.create("d_12", Link.class));
            links.put("a_1", Id.create("a_1", Link.class));
			links.put("3_7", Id.create("3_7", Link.class));
			links.put("7_3", Id.create("7_3", Link.class));
			links.put("4_8", Id.create("4_8", Link.class));
			links.put("8_4", Id.create("8_4", Link.class));
			links.put("3_10", Id.create("3_10", Link.class));
			links.put("10_3", Id.create("10_3", Link.class));
			links.put("4_10", Id.create("4_10", Link.class));
			links.put("10_4", Id.create("10_4", Link.class));
			links.put("9_10", Id.create("9_10", Link.class));
			links.put("10_9", Id.create("10_9", Link.class));
			links.put("7_11", Id.create("7_11", Link.class));
			links.put("11_7", Id.create("11_7", Link.class));
			links.put("8_11", Id.create("8_11", Link.class));
			links.put("11_8", Id.create("11_8", Link.class));
			links.put("11_12", Id.create("11_12", Link.class));
			links.put("12_11", Id.create("12_11", Link.class));
		}
	}

	private static void setLinkAttributes(Link link, double capacity,
			double length, double freeSpeed) {
		
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(freeSpeed);
	}

	/**
	 * creates a lane for every turning direction
	 */
	private void createLanes() {
		
		Lanes laneDef20 = this.scenario.getLanes();
		LanesFactory fac = laneDef20.getFactory();

		// create link assignment of link 1_2
		LanesToLinkAssignment linkAssignment = fac.createLanesToLinkAssignment(links.get("1_2"));

		LanesUtils.createAndAddLane(linkAssignment, fac,
				Id.create("1_2.ol", Lane.class), capacity,
				LINK_LENGTH, 0, 1, null,
				Arrays.asList(Id.create("1_2.l", Lane.class),
				Id.create("1_2.r", Lane.class)));

			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("1_2.l", Lane.class), capacity,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("2_3")), null);
			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("1_2.r", Lane.class), capacity,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("2_7")), null);

		laneDef20.addLanesToLinkAssignment(linkAssignment);

		// create link assignment of link 6_5
		linkAssignment = fac.createLanesToLinkAssignment(links.get("6_5"));

		LanesUtils.createAndAddLane(linkAssignment, fac,
				Id.create("6_5.ol", Lane.class), capacity,
				LINK_LENGTH, 0, 1, null,
				Arrays.asList(Id.create("6_5.l", Lane.class),
						Id.create("6_5.r", Lane.class)));

		LanesUtils.createAndAddLane(linkAssignment, fac,
				Id.create("6_5.l", Lane.class), capacity,
				LINK_LENGTH / 2, -1, 1,
				Collections.singletonList(links.get("5_8")), null);
		LanesUtils.createAndAddLane(linkAssignment, fac,
				Id.create("6_5.r", Lane.class), capacity,
				LINK_LENGTH / 2, 1, 1,
				Collections.singletonList(links.get("5_4")), null);

		laneDef20.addLanesToLinkAssignment(linkAssignment);

		if (useSecondODPair) {
			// create link assignment of link 9_10
			linkAssignment = fac.createLanesToLinkAssignment(links.get("9_10"));

			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("9_10.ol", Lane.class), capacity,
					LINK_LENGTH, 0, 1, null,
					Arrays.asList(Id.create("9_10.l", Lane.class),
							Id.create("9_10.r", Lane.class)));

			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("9_10.l", Lane.class), capacity,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("10_4")), null);
			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("9_10.r", Lane.class), capacity,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("10_3")), null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);

			// create link assignment of link 12_11
			linkAssignment = fac.createLanesToLinkAssignment(links.get("12_11"));

			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("12_11.ol", Lane.class), capacity,
					LINK_LENGTH, 0, 1, null,
					Arrays.asList(Id.create("12_11.l", Lane.class),
							Id.create("12_11.r", Lane.class)));

			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("12_11.l", Lane.class), capacity,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("11_7")), null);
			LanesUtils.createAndAddLane(linkAssignment, fac,
					Id.create("12_11.r", Lane.class), capacity,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("11_8")), null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);
		}
	}

	public void writeNetworkAndLanes(String directory) {
		new NetworkWriter(scenario.getNetwork()).write(directory + "network.xml");
		new LanesWriter(scenario.getLanes()).write(directory + "lanes.xml");
	}

	/**
	 *
	 * @param useSecondODPair
	 * 			Setting this flag true will expand the
	 * 			{@link playground.dgrether.koehlerstrehlersignal.figure9scenario.DgFigure9ScenarioGenerator}
	 * 			with a second origin_destination pair.
     */
	public void setUseSecondODPair(boolean useSecondODPair) {
		this.useSecondODPair = useSecondODPair;
	}

    /**
     * Adapts the link capacity to the number of persons.
     *
     * @param numberOfPersons demand of each OD pair
     */
	public void setCapacity(double numberOfPersons) {
		this.capacity = numberOfPersons;
	}

	/**
	 * Checks whether the second ODPair is in use
	 */
	public static boolean checkNetworkForSecondODPair(Network network) {
		return (network.getNodes().containsKey(Id.createNodeId(9))	&&
				network.getNodes().containsKey(Id.createNodeId(10)) &&
				network.getNodes().containsKey(Id.createNodeId(11)) &&
				network.getNodes().containsKey(Id.createNodeId(12)));
	}

}
