package org.matsim.contrib.matsim4urbansim.utils;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
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
		Node node1 = network.createAndAddNode(new IdImpl(1), scenario.createCoord(500, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), scenario.createCoord(0, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), scenario.createCoord(0, 375));
		Node node4 = network.createAndAddNode(new IdImpl(4), scenario.createCoord(500, 375));
		Node node5 = network.createAndAddNode(new IdImpl(5), scenario.createCoord(1000, 375));

		// add links (bi-directional)
		network.createAndAddLink(new IdImpl(1), node1, node2, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(2), node2, node1, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(3), node1, node3, 625, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(4), node3, node1, 625, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(5), node1, node4, 375, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(6), node4, node1, 375, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(7), node4, node5, 500, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(8), node5, node4, 500, freespeed, capacity, numLanes);
		
		return network;
	}

}
