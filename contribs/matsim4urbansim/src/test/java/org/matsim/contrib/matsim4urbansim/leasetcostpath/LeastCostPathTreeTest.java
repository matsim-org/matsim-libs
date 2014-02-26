package org.matsim.contrib.matsim4urbansim.leasetcostpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.LeastCostPathTree;
import org.matsim.utils.LeastCostPathTree.NodeData;

public class LeastCostPathTreeTest extends MatsimTestCase{
	
	ScenarioImpl scenario;
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// createNetwork();
		this.scenario.setNetwork( CreateTestNetwork.createTriangularNetwork() );
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		double departureTime = 8 * 3600.;
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculator(ttc));
		LeastCostPathTree lcptDistance = new LeastCostPathTree(ttc, new TravelDistanceCalculator());
	
		assert( this.scenario.getNetwork() != null );
		
		// get origin node
		Node origin = this.scenario.getNetwork().getNodes().get(new IdImpl(1));
		assert( origin != null );
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(new IdImpl(4));
		assert( destination != null );

		lcptTime.calculate(this.scenario.getNetwork(), origin, departureTime);
		double arrivalTime = lcptTime.getTree().get( destination.getId() ).getTime();
		double travelTime  = lcptTime.getTree().get( destination.getId() ).getCost(); // here cost = traveled time
		printResults(arrivalTime, travelTime); // travel time = cost = (50s+50s) = 100s
		// check route (visited nodes should be 1,2,4)
		List<Id> spTimeVisitedNodes = getVisitedNodes(lcptTime, destination, "Travel Time");
		Assert.assertTrue( containsNode(spTimeVisitedNodes, this.scenario.createId("2")));
		// check travel duration
		Assert.assertTrue( travelTime == 100 );
		// check travel time
		Assert.assertTrue( arrivalTime - departureTime == 100 );
		
		lcptDistance.calculate(this.scenario.getNetwork(), origin, departureTime);
		double arrivalTimeTD  = lcptDistance.getTree().get( destination.getId() ).getTime();
		double travelDistance = lcptDistance.getTree().get( destination.getId() ).getCost(); // here cost = traveled distance
		printResults(arrivalTimeTD, travelDistance); // travel time = 1000s, cost = (50m+50m) = 100m
		// check route ( visited nodes should be 1,3,4)
		List<Id> spDistenceVisitedNodes = getVisitedNodes(lcptDistance, destination, "Travel Distance");
		Assert.assertTrue( containsNode(spDistenceVisitedNodes, this.scenario.createId("3")));
		// check travel distance
		Assert.assertTrue( travelDistance == 100 );
		// check travel time
		Assert.assertTrue( arrivalTimeTD - departureTime == 1000 );
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
	
	/**
	 * true if given node id is part of the list of visited nodes
	 * @param list
	 * @param nodeId
	 * @return boolean
	 */
	private boolean containsNode(List<Id> list, Id nodeId){
		
		Iterator<Id> nodes = list.iterator();
		
		while(nodes.hasNext()){
			
			Id node = nodes.next();
			if(node.compareTo(nodeId) == 0)
				return true;
			
		}
		return false;
	}

}