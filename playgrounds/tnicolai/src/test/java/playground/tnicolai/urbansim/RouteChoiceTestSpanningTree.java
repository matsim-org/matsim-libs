package playground.tnicolai.urbansim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.tnicolai.urbansim.utils.ExtendedSpanningTree;
import playground.tnicolai.urbansim.utils.ExtendedSpanningTree.NodeData;

public class RouteChoiceTestSpanningTree{
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		
		createNetwork();
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator());
		
		ExtendedSpanningTree estTime = new ExtendedSpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().planCalcScore()));
		ExtendedSpanningTree estDistance = new ExtendedSpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().planCalcScore()));
		ExtendedSpanningTree estCost = new ExtendedSpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().planCalcScore()));
		
		assert(this.scenario.getNetwork() != null);
		Node origin = this.scenario.getNetwork().getNodes().get(new IdImpl(1));
		assert(origin != null);
		
		// set start node for spanning tree Time
		estTime.setOrigin(origin);
		estTime.setDepartureTime(8*3600);
		
		// set start node for spanning tree Distance
		estDistance.setOrigin(origin);
		estDistance.setDepartureTime(8*3600);
		
		// set start node for spanning tree Cost
		estCost.setOrigin(origin);
		estCost.setDepartureTime(8*3600);
		
		// run all spanning trees
		estTime.run(this.scenario.getNetwork(), estTime.isTravelTime);	
		estDistance.run(this.scenario.getNetwork(), estTime.isTravelDistance);	
		estCost.run(this.scenario.getNetwork(), estTime.isTravelCost);	
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(new IdImpl(4));
		assert( destination != null );
		
		// set destination in both spanning trees
		double estTimeTravelTime = estTime.getTree().get( destination.getId() ).getTime();
		double estTimeTravelDistance = estTime.getTree().get( destination.getId() ).getDistance();
		double estTimeTravelCost = estTime.getTree().get( destination.getId() ).getCost();
		List<Node> estTimeNodeList = getVisitedNodes(estTime, destination, "Travel Time");
		printResults(estTimeTravelTime, estTimeTravelDistance, estTimeTravelCost);
		
		double estDistanceTravelTime = estDistance.getTree().get( destination.getId() ).getTime();
		double estDistanceTravelDistance = estDistance.getTree().get( destination.getId() ).getDistance();
		double estDistanceTravelCost = estDistance.getTree().get( destination.getId() ).getCost();
		List<Node> estDistanceNodeList = getVisitedNodes(estDistance, destination, "Travel Distance");
		printResults(estDistanceTravelTime, estDistanceTravelDistance, estDistanceTravelCost);
		
		double estCostTravelTime = estCost.getTree().get( destination.getId() ).getTime();
		double estCostTravelDistance = estCost.getTree().get( destination.getId() ).getDistance();
		double estCostTravelCost = estCost.getTree().get( destination.getId() ).getCost();
		List<Node> estCostNodeList = getVisitedNodes(estCost, destination, "Travel Cost");
		printResults(estCostTravelTime, estCostTravelDistance, estCostTravelCost);

	}
	
	private void printResults(double tt, double td, double tc){
		System.out.println("Travel Times:" + tt + ", Travel Distance: " + td + ", Travel Costs: " + tc);
	}
	
	private List<Node> getVisitedNodes(ExtendedSpanningTree st, Node destination, String costType) {
		HashMap<Id, NodeData> tree = st.getTree();
		List<Node> nodeList = new ArrayList<Node>();
		
		// set destination node ...
		// ... from there we get the route to the origin node by the following iteration
		Node tmpNode = destination;
		System.out.println("Choosen route based on " + costType + " :");
		while(true){
			System.out.println("Node " + tmpNode.getId());
			nodeList.add( tmpNode );
			
			NodeData nodeData = tree.get(tmpNode.getId());
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNode();
			
			if(tmpNode == null)
				break;
		}
		return nodeList;
	}
	
	private void createNetwork() {
		/*
		 * 
		 *          (2) 
		 *        /     \
		 *(10m/s)/       \(10m/s)
		 *(500m)/	      \(500m)
		 *     /           \
		 *    /             \
		 *	 /               \
		 *(1)-------(3)-------(4)
		 *(50m,0.1m/s)(50m,0.1m/s) 			
		 */
		
		NetworkImpl network = this.scenario.getNetwork();
		
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
