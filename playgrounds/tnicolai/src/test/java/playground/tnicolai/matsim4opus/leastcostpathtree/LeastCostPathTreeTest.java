package playground.tnicolai.matsim4opus.leastcostpathtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.LeastCostPathTree;
import org.matsim.utils.LeastCostPathTree.NodeData;

import playground.tnicolai.matsim4opus.costcalculators.TravelDistanceCostCalculatorTest;
import playground.tnicolai.matsim4opus.costcalculators.TravelTimeCostCalculatorTest;

public class LeastCostPathTreeTest extends MatsimTestCase{
	
	ScenarioImpl scenario;
	double dummyCostFactor;
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.dummyCostFactor = 1.;//0.003;
		createNetwork();
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculatorTest(ttc, scenario.getConfig().planCalcScore(), dummyCostFactor));
		LeastCostPathTree lcptDistance = new LeastCostPathTree(ttc, new TravelDistanceCostCalculatorTest(ttc, scenario.getConfig().planCalcScore(), dummyCostFactor));
	
		assert(this.scenario.getNetwork() != null);
		Node origin = this.scenario.getNetwork().getNodes().get(new IdImpl(1));
		assert(origin != null);
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(new IdImpl(4));
		assert( destination != null );

		lcptTime.calculate(this.scenario.getNetwork(), origin, 8*3600);
		double spTravelTime = lcptTime.getTree().get( destination.getId() ).getTime();
		double spTravelTimeCost = lcptTime.getTree().get( destination.getId() ).getCost(); // here cost = travel time
		List<Id> spTimeVisitedNodes = getVisitedNodes(lcptTime, destination, "Travel Time");
		printResults(spTravelTime, spTravelTimeCost); // travel time = 100s, cost = (50s+50s) * 0,003 = 0,3s
		Assert.assertTrue( containsNode(spTimeVisitedNodes, this.scenario.createId("2")));
		
		lcptDistance.calculate(this.scenario.getNetwork(), origin, 8*3600);
		double spDistanceTime = lcptDistance.getTree().get( destination.getId() ).getTime();
		double spDistanceCost = lcptDistance.getTree().get( destination.getId() ).getCost(); // here cost = travel distance
		List<Id> spDistenceVisitedNodes = getVisitedNodes(lcptDistance, destination, "Travel Distance");
		printResults(spDistanceTime, spDistanceCost); // travel time = 1000s, cost = (50m+50m) * 0,003 = 0,3m
		Assert.assertTrue( containsNode(spDistenceVisitedNodes, this.scenario.createId("3")));

	}
	
	private void printResults(double tt, double tc){
		System.out.println("Travel Times:" + tt + ", Travel Costs: " + tc);
	}
	
	private List<Id> getVisitedNodes(LeastCostPathTree lcpt, Node destination, String costType) {
		
		Map<Id, NodeData> tree = lcpt.getTree();
		List<Id> nodeList = new ArrayList<Id>();
		
		// set destination node ...
		// ... from there we get the route to the origin node by the following iteration
		Id tmpNode = destination.getId();
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
	
	private boolean containsNode(List<Id> list, Id nodeId){
		
		Iterator<Id> nodes = list.iterator();
		
		while(nodes.hasNext()){
			
			Id node = nodes.next();
			if(node.compareTo(nodeId) == 0)
				return true;
			
		}
		return false;
	}
	
	/**
	 * creating a test network
	 * the path 1,2,4 has a total length of 1000m with a free speed travel time of 10m/s
	 * the second path 1,3,4 has a total length of 100m but only a free speed travel time of 0.1m/s
	 */
	private void createNetwork() {
		/*
		 * 
		 *          (2) 
		 *        /     \
		 *(10m/s)/       \(10m/s)
		 *(500m)/	        \(500m)
		 *     /           \
		 *    /             \
		 *	 /               \
		 *(1)-------(3)-------(4)
		 *(50m,0.1m/s)(50m,0.1m/s) 			
		 */
		
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl(1), this.scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), this.scenario.createCoord(50, 100));
		Node node3 = network.createAndAddNode(new IdImpl(3), this.scenario.createCoord(50, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), this.scenario.createCoord(100, 0));

		// add links
		network.createAndAddLink(new IdImpl(1), node1, node2, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl(2), node2, node4, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl(3), node1, node3, 50.0, 0.1, 3600.0, 1);
		network.createAndAddLink(new IdImpl(4), node3, node4, 50.0, 0.1, 3600.0, 1);
	}

}
