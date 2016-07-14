package playground.dhosse.frequencyBasedPt.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateTestNetwork {
	
	public static NetworkImpl createTestNetwork(){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		
		double speed50 = 50/3.6;
		
		// (1)----------(2)----------(3)----------(4)----------(5)
		//  |													|
		//  |													|
		//	|													|
		//	|													|
		// (6)-------------------------------------------------(7)

		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 150));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 150, (double) 150));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 300, (double) 150));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 450, (double) 150));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 600, (double) 150));
		Node node6 = network.createAndAddNode(Id.create(6, Node.class), new Coord((double) 0, (double) 0));
		Node node7 = network.createAndAddNode(Id.create(7, Node.class), new Coord((double) 600, (double) 0));
		
		network.createAndAddLink(Id.create( 1, Link.class), node1, node2, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 2, Link.class), node2, node1, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 3, Link.class), node2, node3, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 4, Link.class), node3, node2, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 5, Link.class), node3, node4, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 6, Link.class), node4, node3, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 7, Link.class), node4, node5, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 8, Link.class), node5, node4, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create( 9, Link.class), node1, node6, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create(10, Link.class), node6, node1, 600., speed50, 1800., 1.);
		network.createAndAddLink(Id.create(11, Link.class), node6, node7, 600., speed50, 1800., 1.);
		network.createAndAddLink(Id.create(12, Link.class), node7, node6, 600., speed50, 1800., 1.);
		network.createAndAddLink(Id.create(13, Link.class), node7, node5, 150., speed50, 1800., 1.);
		network.createAndAddLink(Id.create(14, Link.class), node5, node7, 150., speed50, 1800., 1.);
		
		return network;
		
	}

}
