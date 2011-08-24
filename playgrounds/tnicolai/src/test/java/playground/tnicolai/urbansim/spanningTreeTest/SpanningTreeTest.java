package playground.tnicolai.urbansim.spanningTreeTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.toronto.ttimematrix.SpanningTree;
import playground.toronto.ttimematrix.SpanningTree.NodeData;

public class SpanningTreeTest{
	
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	double dummyCostFactor = 0.003;
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		
		createNetwork();
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator());
		
		SpanningTree spTime = new SpanningTree(ttc, new TravelTimeCostCalculatorTest(ttc, scenario.getConfig().planCalcScore()));
		SpanningTree spDistance = new SpanningTree(ttc, new TravelDistanceCostCalculatorTest(ttc, scenario.getConfig().planCalcScore()));
	
		assert(this.scenario.getNetwork() != null);
		Node origin = this.scenario.getNetwork().getNodes().get(new IdImpl(1));
		assert(origin != null);
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(new IdImpl(4));
		assert( destination != null );

		spTime.setOrigin(origin);
		spTime.setDepartureTime(8*3600);
		spTime.run(this.scenario.getNetwork());
		double spTravelTime = spTime.getTree().get( destination.getId() ).getTime();
		double spTravelTimeCost = spTime.getTree().get( destination.getId() ).getCost(); // here cost = travel time
		List<Node> spTimeVisitedNodes = getVisitedNodes(spTime, destination, "Travel Time");
		printResults(spTravelTime, spTravelTimeCost); // travel time = 100s, cost = (50s+50s) * 0,003 = 0,3s
		Assert.assertTrue( containsNode(spTimeVisitedNodes, 2));
		
		spDistance.setOrigin(origin);
		spDistance.setDepartureTime(8*3600);
		spDistance.run(this.scenario.getNetwork());
		double spDistanceTime = spDistance.getTree().get( destination.getId() ).getTime();
		double spDistanceCost = spDistance.getTree().get( destination.getId() ).getCost(); // here cost = travel distance
		List<Node> spDistenceVisitedNodes = getVisitedNodes(spDistance, destination, "Travel Distance");
		printResults(spDistanceTime, spDistanceCost); // travel time = 1000s, cost = (50m+50m) * 0,003 = 0,3m
		Assert.assertTrue( containsNode(spDistenceVisitedNodes, 3));

	}
	
	private void printResults(double tt, double tc){
		System.out.println("Travel Times:" + tt + ", Travel Costs: " + tc);
	}
	
	private List<Node> getVisitedNodes(SpanningTree st, Node destination, String costType) {
		
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
	
	private boolean containsNode(List<Node> list, int nodeId){
		
		Iterator<Node> nodes = list.iterator();
		
		while(nodes.hasNext()){
			
			Node node = nodes.next();
			if(node.getId().compareTo( new IdImpl(nodeId)) == 0)
				return true;
			
		}
		return false;
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
	
	
	/**
	 * From here: dummy Cost Calculators ...
	 */
	
	
	class TravelTimeCostCalculatorTest implements TravelMinCost {

		protected final TravelTime timeCalculator;
		private final double marginalCostOfTime;

		public TravelTimeCostCalculatorTest(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.timeCalculator = timeCalculator;
			/* Usually, the travel-utility should be negative (it's a disutility)
			 * but the cost should be positive. Thus negate the utility.
			 */
			this.marginalCostOfTime = SpanningTreeTest.this.dummyCostFactor;
			// normally use cost of:
			// (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);	
		}

		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
			
			return this.marginalCostOfTime * travelTime;
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
		}
	} 
	
	class TravelDistanceCostCalculatorTest implements TravelMinCost {

		protected final TravelTime timeCalculator;
		private final double marginalCostOfDistance;

		public TravelDistanceCostCalculatorTest(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.timeCalculator = timeCalculator;
			/* Usually, the travel-utility should be negative (it's a disutility)
			 * but the cost should be positive. Thus negate the utility.
			 */
			this.marginalCostOfDistance = SpanningTreeTest.this.dummyCostFactor;
			// normally use cost of:
			// - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney();
		}

		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			
			return this.marginalCostOfDistance * link.getLength();
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			return this.marginalCostOfDistance * link.getLength();
		}
	} 

}
