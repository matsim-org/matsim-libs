package playground.toronto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.toronto.ttimematrix.SpanningTree;
import playground.toronto.ttimematrix.SpanningTree.NodeData;

public class RouteChoiceCostcalculatorTest {
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		
		createNetwork();
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator());
		
		// init different cost calculators
		TravelDistanceCostCalculator distanceCostCalculator = new TravelDistanceCostCalculator();
		TravelTimeCostCalculator timeCostCalculator = new TravelTimeCostCalculator(ttc);
		// init spanning trees
		SpanningTree spDistance = new SpanningTree(ttc, distanceCostCalculator);
		SpanningTree spTime = new SpanningTree(ttc, timeCostCalculator);
		
		assert(this.scenario.getNetwork() != null);
		Node origin = this.scenario.getNetwork().getNodes().get(new IdImpl(1));
		assert(origin != null);
		
		// set start node for spanning trees
		spDistance.setOrigin( origin );
		spDistance.setDepartureTime(0);
		spTime.setOrigin( origin );
		spTime.setDepartureTime(0);
		
		// run spanning trees
		spDistance.run(this.scenario.getNetwork());
		spTime.run(this.scenario.getNetwork());
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(new IdImpl(4));
		assert( destination != null );
		
		// get results
		double distanceTravelTime = spDistance.getTree().get( destination.getId() ).getTime();
		double distanceTravelCost = spDistance.getTree().get( destination.getId() ).getCost();
		printResults(distanceTravelTime, distanceTravelCost, "Results based on Travel Distance", spDistance, destination );
		Assert.assertEquals(1000.,distanceTravelTime,1.e-8) ;
		Assert.assertEquals( 100.,distanceTravelCost,1.e-8) ;
		
		double timeTravelTime = spTime.getTree().get( destination.getId() ).getTime();
		double timeTravelCost = spTime.getTree().get( destination.getId() ).getCost();
		printResults(timeTravelTime, timeTravelCost, "Results based on Travel Times", spTime, destination);
		Assert.assertEquals(100.,timeTravelTime,1.e-8) ;
		Assert.assertEquals(100.,timeTravelCost,1.e-8) ;
	}
	
	private void printResults(double tt, double tc, String costType, SpanningTree st, Node destination){
		System.out.println("+++");
		System.out.println(costType);
		System.out.println("Travel Times:" + tt + ", Travel Costs: " + tc);
		
		HashMap<Id, NodeData> tree = st.getTree();
		List<Node> nodeList = new ArrayList<Node>();
		
		// print route choice
		Node tmpNode = destination;
		System.out.println("Choosen route :");
		while(true){
			System.out.println("Node " + tmpNode.getId());
			nodeList.add( tmpNode );
			
			NodeData nodeData = tree.get(tmpNode.getId());
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNode();
			
			if(tmpNode == null)
				break;
		}
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

	
	public static class TravelDistanceCostCalculator implements TravelCost{
		private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);

		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			if(link != null)
				return link.getLength();
			log.warn("Link is null. Reurned 0 as link length.");
			return 0.;
		}
	}
	
	public static class TravelCostCostCalculator implements TravelCost{
		private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);

		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			if(link != null)
				return link.getLength();
			log.warn("Link is null. Reurned 0 as link length.");
			return 0.;
		}
	}
	
	public static class TravelTimeCostCalculator implements TravelCost{
		private static final Logger log = Logger.getLogger(TravelTimeCostCalculator.class);
		
		protected final TravelTime timeCalculator;
		
		public TravelTimeCostCalculator(final TravelTime timeCalculator) {
			this.timeCalculator = timeCalculator;
		}
		
		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			return this.timeCalculator.getLinkTravelTime(link, time);
		}
	}
}
