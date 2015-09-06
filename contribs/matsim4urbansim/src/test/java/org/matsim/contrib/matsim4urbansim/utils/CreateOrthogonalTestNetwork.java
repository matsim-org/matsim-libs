package org.matsim.contrib.matsim4urbansim.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This class creates a simple test network to test the orthogonal distance calculation in MATSim.
 * 
 * @author tthunig
 */
public class CreateOrthogonalTestNetwork {
	
	/**
	 * This method creates a simple test network to test the orthogonal distance calculation between nodes and links or between nodes and nodes via links.
	 * It is used in TestOrthogonalDistance.java.
	 * The network has 5 nodes and 4 links (see the sketch below).
	 * 
	 * @return the network
	 */
	public static NetworkImpl createOrthogonalDistanceTestNetwork() {

		/*
		 * (3)	   (4)-----(5)
		 *    \     |
		 *     \    |
		 *      \   |
		 *	     \  |
		 * 	      \ |
		 * (2)-----(1)
		 */
		double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		// add nodes
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 500, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 0, (double) 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 0, (double) 375));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 500, (double) 375));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 1000, (double) 375));

		// add links (bi-directional)
		network.createAndAddLink(Id.create(1, Link.class), node1, node2, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(2, Link.class), node2, node1, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(3, Link.class), node1, node3, 625, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(4, Link.class), node3, node1, 625, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(5, Link.class), node1, node4, 375, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(6, Link.class), node4, node1, 375, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(7, Link.class), node4, node5, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(8, Link.class), node5, node4, 500, freespeed, capacity, numLanes);
		
		return network;
	}

}
