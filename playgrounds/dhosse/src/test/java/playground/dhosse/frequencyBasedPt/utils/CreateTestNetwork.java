package playground.dhosse.frequencyBasedPt.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateTestNetwork {
	
	public static Network createTestNetwork(){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network network = (Network)scenario.getNetwork();
		
		double speed50 = 50/3.6;
		
		// (1)----------(2)----------(3)----------(4)----------(5)
		//  |													|
		//  |													|
		//	|													|
		//	|													|
		// (6)-------------------------------------------------(7)

		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 150));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 150, (double) 150));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 300, (double) 150));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 450, (double) 150));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 600, (double) 150));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 0, (double) 0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 600, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		final double freespeed = speed50;
		
		NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), fromNode, toNode, 150., freespeed, 1800., 1. );
		final Node fromNode1 = node2;
		final Node toNode1 = node1;
		final double freespeed1 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), fromNode1, toNode1, 150., freespeed1, 1800., 1. );
		final Node fromNode2 = node2;
		final Node toNode2 = node3;
		final double freespeed2 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 3, Link.class), fromNode2, toNode2, 150., freespeed2, 1800., 1. );
		final Node fromNode3 = node3;
		final Node toNode3 = node2;
		final double freespeed3 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 4, Link.class), fromNode3, toNode3, 150., freespeed3, 1800., 1. );
		final Node fromNode4 = node3;
		final Node toNode4 = node4;
		final double freespeed4 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 5, Link.class), fromNode4, toNode4, 150., freespeed4, 1800., 1. );
		final Node fromNode5 = node4;
		final Node toNode5 = node3;
		final double freespeed5 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 6, Link.class), fromNode5, toNode5, 150., freespeed5, 1800., 1. );
		final Node fromNode6 = node4;
		final Node toNode6 = node5;
		final double freespeed6 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 7, Link.class), fromNode6, toNode6, 150., freespeed6, 1800., 1. );
		final Node fromNode7 = node5;
		final Node toNode7 = node4;
		final double freespeed7 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 8, Link.class), fromNode7, toNode7, 150., freespeed7, 1800., 1. );
		final Node fromNode8 = node1;
		final Node toNode8 = node6;
		final double freespeed8 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create( 9, Link.class), fromNode8, toNode8, 150., freespeed8, 1800., 1. );
		final Node fromNode9 = node6;
		final Node toNode9 = node1;
		final double freespeed9 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), fromNode9, toNode9, 600., freespeed9, 1800., 1. );
		final Node fromNode10 = node6;
		final Node toNode10 = node7;
		final double freespeed10 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), fromNode10, toNode10, 600., freespeed10, 1800., 1. );
		final Node fromNode11 = node7;
		final Node toNode11 = node6;
		final double freespeed11 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), fromNode11, toNode11, 600., freespeed11, 1800., 1. );
		final Node fromNode12 = node7;
		final Node toNode12 = node5;
		final double freespeed12 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), fromNode12, toNode12, 150., freespeed12, 1800., 1. );
		final Node fromNode13 = node5;
		final Node toNode13 = node7;
		final double freespeed13 = speed50;
		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), fromNode13, toNode13, 150., freespeed13, 1800., 1. );
		
		return network;
		
	}

}
