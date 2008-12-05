/* *********************************************************************** *
 * project: org.matsim.*
 * TryOut.java
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

package playground.marcel.pt;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Population;
import org.matsim.utils.geometry.CoordImpl;

public class TryOut {


	private void simulatePtVehicle() {
		// create network
		final NetworkLayer network = createNetwork();

		// create needed facilities
		final Facilities facilities = createFacilities();

		// create 1 Person Population
		final Population population = new Population(Population.NO_STREAMING);

		// create Bus with 1 schedule


		// prepare events handling
		final Events events = new Events();


		// run/simulate the things

	}

	/**
	 * Creates a simple test network:
	 *
	 * <pre>
	 * (1)                                             (8)
	 *   \                                             /
	 *    1                                           7
	 *     \                                         /
	 *     (3)---3---(4)---4---(5)---5---(6)---6---(7)
	 *     /                                         \
	 *    2                                           8
	 *   /                                             \
	 * (2)                                             (9)
	 * </pre>
	 *
	 * All links have length 1000.0, freespeed 10.0, capacity 3600.0 veh/h, and 1 lane.
	 *
	 * @return a test network
	 */
	private NetworkLayer createNetwork() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 1000));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(0, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(500, 500));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(1500, 500));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(2500, 500));
		Node node6 = network.createNode(new IdImpl("6"), new CoordImpl(3500, 500));
		Node node7 = network.createNode(new IdImpl("7"), new CoordImpl(4500, 500));
		Node node8 = network.createNode(new IdImpl("8"), new CoordImpl(5000, 1000));
		Node node9 = network.createNode(new IdImpl("9"), new CoordImpl(5000, 0));
		network.createLink(new IdImpl("1"), node1, node3, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("2"), node2, node3, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("3"), node3, node4, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("4"), node4, node5, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("5"), node5, node6, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("6"), node6, node7, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("7"), node7, node8, 1000, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("8"), node7, node9, 1000, 10.0, 3600.0, 1);
		return network;
	}

	private Facilities createFacilities() {
		final Facilities facilities = new Facilities();
		final Facility home = facilities.createFacility(new IdImpl("home"), new CoordImpl(0, 900));
		home.createActivity("home");

		// TODO [MR]
		return facilities;
	}

	public static void main(final String[] args) {
		final TryOut tryOut = new TryOut();

		tryOut.simulatePtVehicle();
	}

}
