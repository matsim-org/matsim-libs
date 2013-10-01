package playground.dhosse.frequencyBasedPt.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class CreateTestNetwork {
	
	public static String createTestNetwork(String dir){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		
		double speed50 = 50/3.6;
		
		// (1)----------(2)----------(3)----------(4)----------(5)
		//  |													|
		//  |													|
		//	|													|
		//	|													|
		// (6)-------------------------------------------------(7)
		
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0,150));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(150,150));
		Node node3 = network.createAndAddNode(new IdImpl(3), new CoordImpl(300,150));
		Node node4 = network.createAndAddNode(new IdImpl(4), new CoordImpl(450,150));
		Node node5 = network.createAndAddNode(new IdImpl(5), new CoordImpl(600,150));
		Node node6 = network.createAndAddNode(new IdImpl(6), new CoordImpl(0,0));
		Node node7 = network.createAndAddNode(new IdImpl(7), new CoordImpl(600,0));
		
		network.createAndAddLink(new IdImpl(1), node1, node2, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(2), node2, node1, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(3), node2, node3, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(4), node3, node2, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(5), node3, node4, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(6), node4, node3, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(7), node4, node5, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(8), node5, node4, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(9), node1, node6, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(10), node6, node1, 600., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(11), node6, node7, 600., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(12), node7, node6, 600., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(13), node7, node5, 150., speed50, 1800., 1.);
		network.createAndAddLink(new IdImpl(14), node5, node7, 150., speed50, 1800., 1.);
		
		new NetworkWriter(network).write(dir + "/network.xml");
		
		return dir+"/network.xml";
		
	}

}
