package playground.tnicolai.urbansim.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.toronto.ttimematrix.SpanningTree;
import playground.toronto.ttimematrix.SpanningTree.NodeData;

public class TestSpanningTree {
	
	public static void main(String[] args) {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("/Users/thomas/Development/opus_home/data/psrc/network/psrc.xml.gz");
		NetworkImpl network = scenario.getNetwork();
		
		// init spanning tree here
		TravelTime ttc = new TravelTimeCalculator(network,60,30*3600, scenario.getConfig().travelTimeCalculator());
		SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().planCalcScore()));
		
		// than set the start node
		Node origin = network.getNodes().get(new IdImpl(4224));
		st.setOrigin(origin);
		st.setDepartureTime(8*3600);
		st.run(network);

		HashMap<Id, NodeData> tree = st.getTree();
		
		// now set the destination node
		Node destination = network.getNodes().get(new IdImpl(2176));
		
		List<Node> nodeList = new ArrayList<Node>();
								// set destination node ...
								// ... from there we get the route to the origin node by the following iteration
		Node tmpNode = destination;
		while(true){
			
			nodeList.add( tmpNode );
			
			NodeData nodeData = tree.get(tmpNode.getId());
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNode();
			
			if(tmpNode == null)
				break;
		}
		System.out.println();
		for(Node node: nodeList)
			System.out.println(node.getId());
		
		// create a link list out of that ...
		List<Link> linkList = RouteUtils.getLinksFromNodes(nodeList);
		List<Id> linkIdList = new ArrayList<Id>();
		
		System.out.println();
		for(Link link: linkList){
			System.out.println(link.getId());
			linkIdList.add(link.getId());
		}
		
		// ... to calculate the distance	
		
		NetworkRoute nwr= RouteUtils.createNetworkRoute(linkIdList, network);
		double distance1 = RouteUtils.calcDistance(nwr, network);
		
		// or alternately
		double distance2 = 0.;
		for(Link link: linkList)
			distance2 += link.getLength();
		double distanceLink1 = linkList.get(0).getLength();
		double distanceLink2 = linkList.get(linkList.size()-1).getLength();
		
		
		System.out.println("Distance1 is : " + distance1);
		System.out.println("Distance2 is : " + distance2 + " without origin and end link distance this is " + (distance2 - (distanceLink1+distanceLink2)));
		
		
//		System.out.println("Node ID\t TravelTime \t TravelCost\t PreviousNode ID");
//		for (Id id : tree.keySet()) {
//			NodeData d = tree.get(id);
//			
//			
//			
//			if (d.getPrevNode() != null) {
//				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+d.getPrevNode().getId());
//			}
//			else {
//				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+"0");
//			}
//		}
	}
	

}
