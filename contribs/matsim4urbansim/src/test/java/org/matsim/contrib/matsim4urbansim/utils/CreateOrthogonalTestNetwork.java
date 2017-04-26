package org.matsim.contrib.matsim4urbansim.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
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
	private static Network createOrthogonalDistanceTestNetwork() {
		// yyyy not used anywhere.  kai, feb'17

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

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = (Network) scenario.getNetwork();

		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 500, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 0, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 0, (double) 375));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 500, (double) 375));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 1000, (double) 375));
		final Node fromNode = node1;
		final Node toNode = node2;
		final double freespeed1 = freespeed;
		final double capacity1 = capacity;
		final double numLanes1 = numLanes;

		// add links (bi-directional)
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 500, freespeed1, capacity1, numLanes1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node1;
		final double freespeed2 = freespeed;
		final double capacity2 = capacity;
		final double numLanes2 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, (double) 500, freespeed2, capacity2, numLanes2 );
		final Node fromNode2 = node1;
		final Node toNode2 = node3;
		final double freespeed3 = freespeed;
		final double capacity3 = capacity;
		final double numLanes3 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, (double) 625, freespeed3, capacity3, numLanes3 );
		final Node fromNode3 = node3;
		final Node toNode3 = node1;
		final double freespeed4 = freespeed;
		final double capacity4 = capacity;
		final double numLanes4 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, (double) 625, freespeed4, capacity4, numLanes4 );
		final Node fromNode4 = node1;
		final Node toNode4 = node4;
		final double freespeed5 = freespeed;
		final double capacity5 = capacity;
		final double numLanes5 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), fromNode4, toNode4, (double) 375, freespeed5, capacity5, numLanes5 );
		final Node fromNode5 = node4;
		final Node toNode5 = node1;
		final double freespeed6 = freespeed;
		final double capacity6 = capacity;
		final double numLanes6 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), fromNode5, toNode5, (double) 375, freespeed6, capacity6, numLanes6 );
		final Node fromNode6 = node4;
		final Node toNode6 = node5;
		final double freespeed7 = freespeed;
		final double capacity7 = capacity;
		final double numLanes7 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), fromNode6, toNode6, (double) 500, freespeed7, capacity7, numLanes7 );
		final Node fromNode7 = node5;
		final Node toNode7 = node4;
		final double freespeed8 = freespeed;
		final double capacity8 = capacity;
		final double numLanes8 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(8, Link.class), fromNode7, toNode7, (double) 500, freespeed8, capacity8, numLanes8 );
		
		return network;
	}

}
