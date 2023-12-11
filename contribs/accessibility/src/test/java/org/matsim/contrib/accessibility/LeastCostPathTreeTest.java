package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.TravelDistanceCalculator;
import org.matsim.contrib.accessibility.TravelTimeCostCalculator;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree.NodeData;


public class LeastCostPathTreeTest {
	
	Scenario scenario;

	@Test
	void testRouteChoiceTestSpanningTree(){
		this.scenario = new ScenarioBuilder(ConfigUtils.createConfig()).setNetwork(CreateTestNetwork.createTriangularNetwork()).build() ;
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		double departureTime = 8 * 3600.;
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculator(ttc));
		LeastCostPathTree lcptDistance = new LeastCostPathTree(ttc, new TravelDistanceCalculator());
	
		assert( this.scenario.getNetwork() != null );
		
		// get origin node
		Node origin = this.scenario.getNetwork().getNodes().get(Id.create(1, Node.class));
		assert( origin != null );
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(Id.create(4, Node.class));
		assert( destination != null );

		lcptTime.calculate(this.scenario.getNetwork(), origin, departureTime);
		double arrivalTime = lcptTime.getTree().get( destination.getId() ).getTime();
		double travelTime  = lcptTime.getTree().get( destination.getId() ).getCost(); // here cost = traveled time
		printResults(arrivalTime, travelTime); // travel time = cost = (50s+50s) = 100s
		// check route (visited nodes should be 1,2,4)
		List<Id<Node>> spTimeVisitedNodes = getVisitedNodes(lcptTime, destination, "Travel Time");
		Assertions.assertTrue( containsNode(spTimeVisitedNodes, Id.create("2", Node.class)));
		// check travel duration
		Assertions.assertTrue( travelTime == 100 );
		// check travel time
		Assertions.assertTrue( arrivalTime - departureTime == 100 );
		
		lcptDistance.calculate(this.scenario.getNetwork(), origin, departureTime);
		double arrivalTimeTD  = lcptDistance.getTree().get( destination.getId() ).getTime();
		double travelDistance = lcptDistance.getTree().get( destination.getId() ).getCost(); // here cost = traveled distance
		printResults(arrivalTimeTD, travelDistance); // travel time = 1000s, cost = (50m+50m) = 100m
		// check route ( visited nodes should be 1,3,4)
		List<Id<Node>> spDistenceVisitedNodes = getVisitedNodes(lcptDistance, destination, "Travel Distance");
		Assertions.assertTrue( containsNode(spDistenceVisitedNodes, Id.create("3", Node.class)));
		// check travel distance
		Assertions.assertTrue( travelDistance == 100 );
		// check travel time
		Assertions.assertTrue( arrivalTimeTD - departureTime == 1000 );
	}
	
	private void printResults(double tt, double tc){
		System.out.println("Travel Times:" + tt + ", Travel Costs: " + tc);
	}
	
	private List<Id<Node>> getVisitedNodes(LeastCostPathTree lcpt, Node destination, String costType) {
		
		Map<Id<Node>, NodeData> tree = lcpt.getTree();
		List<Id<Node>> nodeList = new ArrayList<>();
		
		// set destination node ...
		// ... from there we get the route to the origin node by the following iteration
		Id<Node> tmpNode = destination.getId();
		System.out.println("Choosen route based on " + costType + " :");
		while(true){
			System.out.println("Node " + tmpNode);
			nodeList.add( tmpNode );
			
			NodeData nodeData = tree.get(tmpNode);
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNodeId();
			
			if(tmpNode == null)
				break;
		}
		return nodeList;
	}
	
	/**
	 * true if given node id is part of the list of visited nodes
	 * @param list
	 * @param nodeId
	 * @return boolean
	 */
	private boolean containsNode(List<Id<Node>> list, Id<Node> nodeId){
		
		Iterator<Id<Node>> nodes = list.iterator();
		
		while(nodes.hasNext()){
			
			Id<Node> node = nodes.next();
			if(node.compareTo(nodeId) == 0)
				return true;
			
		}
		return false;
	}

}
