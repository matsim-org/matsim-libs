/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.toronto.ttimematrix;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree.NodeData;
import org.matsim.vehicles.Vehicle;


public class RouteChoiceCostcalculatorTest {
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		
		createNetwork();
		compareRouteChoices();
	}
	
	private void compareRouteChoices(){
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		
		// init different cost calculators
		TravelDistanceCostCalculator distanceCostCalculator = new TravelDistanceCostCalculator();
		TravelTimeCostCalculator timeCostCalculator = new TravelTimeCostCalculator(ttc);
		// init spanning trees
		LeastCostPathTree spDistance = new LeastCostPathTree(ttc, distanceCostCalculator);
		LeastCostPathTree spTime = new LeastCostPathTree(ttc, timeCostCalculator);
		
		assert(this.scenario.getNetwork() != null);
		Node origin = this.scenario.getNetwork().getNodes().get(Id.create(1, Node.class));
		assert(origin != null);
		
		// calculate spanning trees
		spDistance.calculate(this.scenario.getNetwork(), origin, 0);
		spTime.calculate(this.scenario.getNetwork(), origin, 0);
		
		// get destination node
		Node destination = this.scenario.getNetwork().getNodes().get(Id.create(4, Node.class));
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
	
	private void printResults(double tt, double tc, String costType, LeastCostPathTree st, Node destination){
		System.out.println("+++");
		System.out.println(costType);
		System.out.println("Travel Times:" + tt + ", Travel Costs: " + tc);
		
		Map<Id<Node>, NodeData> tree = st.getTree();
		
		// print route choice
		Id<Node> tmpNode = destination.getId();
		System.out.println("Choosen route :");
		while(true){
			System.out.println("Node " + tmpNode);
			
			NodeData nodeData = tree.get(tmpNode);
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNodeId();
			
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
		
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 50, (double) 100));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 50, (double) 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 100, (double) 0));

		// add links
		network.createAndAddLink(Id.create(1, Link.class), node1, node2, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(2, Link.class), node2, node4, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(3, Link.class), node1, node3, 50.0, 0.1, 3600.0, 1);
		network.createAndAddLink(Id.create(4, Link.class), node3, node4, 50.0, 0.1, 3600.0, 1);
	}

	
	public static class TravelDistanceCostCalculator implements TravelDisutility{
		private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			if(link != null)
				return link.getLength();
			log.warn("Link is null. Reurned 0 as link length.");
			return 0.;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			throw new UnsupportedOperationException();
		}
	}
	
	public static class TravelCostCostCalculator implements TravelDisutility{
		private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			if(link != null)
				return link.getLength();
			log.warn("Link is null. Reurned 0 as link length.");
			return 0.;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			throw new UnsupportedOperationException();
		}
	}
	
	public static class TravelTimeCostCalculator implements TravelDisutility{
		
		protected final TravelTime timeCalculator;
		
		public TravelTimeCostCalculator(final TravelTime timeCalculator) {
			this.timeCalculator = timeCalculator;
		}
		
		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			return this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			throw new UnsupportedOperationException();
		}
	}
}
