package freight.vrp;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.matrices.Matrix;
import vrp.api.Costs;
import vrp.api.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TravelingSalesmanExample {
	
	static class GeneralizedCostFunction {
		public double getCosts(double time, double distance){
			return time/3600*20 + distance*1;
		}
	}
	
	static class SalesmanTravelCosts implements TravelCost,TravelMinCost, TravelTime {

		private GeneralizedCostFunction genCostFunction;
		
		public SalesmanTravelCosts(GeneralizedCostFunction genCostFunction) {
			super();
			this.genCostFunction = genCostFunction;
		}

		public double getLinkGeneralizedTravelCost(Link link, double time) {
			return genCostFunction.getCosts(time, link.getLength());
		}

		public double getLinkMinimumTravelCost(Link link) {
			double speed = link.getFreespeed();
			return link.getLength()/speed;
		}

		public double getLinkTravelTime(Link link, double time) {
			return link.getLength()/link.getFreespeed();
		}
	}
	
	static class C2CTransportCostsOverNetwork implements Costs {

		private Matrix costMatrix;
		
		private Matrix ttMatrix;
		
		private Network network;
		
		private GeneralizedCostFunction costFunction;
		
		private LeastCostPathCalculator leastCostPathCalculator;
		
		private Collection<Id> poiIds = new ArrayList<Id>();
		
		public C2CTransportCostsOverNetwork(Network network, Collection<Id> poiIds, GeneralizedCostFunction costFunction) {
			super();
			this.network = network;
			this.poiIds = poiIds;
			this.costFunction = costFunction; 
			costMatrix = new Matrix("cost", "cost matrix");
			ttMatrix = new Matrix("time", "time matrix");
		}

		public void run(){
			initPathCalculator();
			for(Id fromCustomer : poiIds){
				org.matsim.api.core.v01.network.Node fromNode = network.getLinks().get(fromCustomer).getFromNode();
				assertNotNull(fromNode);
				for(Id toCustomer : poiIds){
					if(fromCustomer == toCustomer){
						costMatrix.setEntry(fromCustomer, toCustomer, 0.0);
						ttMatrix.setEntry(fromCustomer, toCustomer, 0.0);
					}
					else{
						org.matsim.api.core.v01.network.Node toNode = network.getLinks().get(toCustomer).getToNode();
						assertNotNull(toNode);
						Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, 0.0);
						costMatrix.setEntry(fromCustomer, toCustomer, path.travelCost);
						ttMatrix.setEntry(fromCustomer, toCustomer, path.travelTime);
					}
				}
			}
		}
		
		private void assertNotNull(org.matsim.api.core.v01.network.Node node) {
			if(node == null){
				throw new IllegalStateException("customerLocation does not exist");
			}
			
		}

		private void initPathCalculator() {
			SalesmanTravelCosts travelCosts = new SalesmanTravelCosts(costFunction);
			PreProcessEuclidean preProcessData = new PreProcessEuclidean(travelCosts);
			preProcessData.run(network);
			leastCostPathCalculator = new AStarEuclidean(network, preProcessData,travelCosts);
		}

		@Override
		public Double getGeneralizedCost(Node from, Node to, double time) {
			return costMatrix.getEntry(makeId(from.getId()), makeId(to.getId())).getValue();
		}

		@Override
		public Double getDistance(Node from, Node to, double time) {
			return costMatrix.getEntry(makeId(from.getId()), makeId(to.getId())).getValue();
		}

		@Override
		public Double getTransportTime(Node from, Node to, double time) {
			return ttMatrix.getEntry(makeId(from.getId()), makeId(to.getId())).getValue();
		}
		
		public Double getTime(Node from, Node to, double starttime){
			return null;
		}
		
	}
	
	public static void main(String[] args) {
		/*
		 * start@i(1,0)
		 * sightseeing@i(5,0)
		 * refreshing@j(8,1)
		 * geochaching@i(4,8)
		 */
		Logger.getRootLogger().setLevel(Level.INFO);
		Config config = ConfigUtils.createConfig();
		Scenario scen = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scen).readFile("networks/grid.xml");
		
		TravelingSalesmanProblemBuilder travelingSalesmanProblemBuilder = new TravelingSalesmanProblemBuilder(scen.getNetwork());
		travelingSalesmanProblemBuilder.addActivity("sightseeing", makeId("i(5,0)"), 14*3600, 16*3600, 3600);
		travelingSalesmanProblemBuilder.addActivity("refreshing", makeId("j(8,1)"), 12*3600, 14*3600, 3600);
		travelingSalesmanProblemBuilder.addActivity("geocaching", makeId("i(4,8)"), 8*3600, 16*3600, 3*3600);
		travelingSalesmanProblemBuilder.addActivity("drinkingBeer", makeId("j(8,4)"), 13*3600, 22*3600, 1800);
		travelingSalesmanProblemBuilder.setStart(makeId("i(1,0)"), 7*3600, 18*3600);
		
		List<Id> poiIds = Arrays.asList(makeId("i(1,0)"),makeId("i(5,0)"),makeId("j(8,1)"),makeId("i(4,8)"),makeId("j(8,4)"));
		
		C2CTransportCostsOverNetwork salesmanCosts = new C2CTransportCostsOverNetwork(scen.getNetwork(), poiIds, new GeneralizedCostFunction());
		salesmanCosts.run();
		
		travelingSalesmanProblemBuilder.setCosts(salesmanCosts);
		
//		ManhattanCosts manhattanCosts = new ManhattanCosts(); 
//		manhattanCosts.speed = 3.0;
//		travelingSalesmanProblemBuilder.setCosts(manhattanCosts);
		
		TravelingSalesman travelingSalesman = new TravelingSalesman(travelingSalesmanProblemBuilder.buildTSP());
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("output/salesman.png");
		travelingSalesman.registerListener(chartListener);
		travelingSalesman.registerListener(new RuinAndRecreateReport());
		travelingSalesman.run();
		
		print(travelingSalesman.getSolution());
	}

	private static void print(Collection<Tour> solution) {
		for(Tour t : solution){
			boolean firstElement = true;
			for(TourElement e : t.getTourElements()){
				if(firstElement){
					System.out.println("activity="+e.getActivityType()+";earliestDeparture=" + getTime(e.getTimeWindow().getStart()) + ";latestDeparture=" + getTime(e.getTimeWindow().getEnd()));
					firstElement = false;
				}
				else{
					System.out.println("activity="+e.getActivityType()+";earliestArrival=" + getTime(e.getTimeWindow().getStart()) + ";latestArrival=" + getTime(e.getTimeWindow().getEnd()));
				}
				
			}
		}
		
	}

	private static String getTime(double start) {
		long hour = (long)Math.floor(start/3600);
		long minute = Math.round((start%3600)/3600*60);
		String hourS = null;
		String minuteS = null;
		if(hour<10){
			hourS = "0"+hour; 
		}
		else{
			hourS = "" + hour;
		}
		if(minute<10){
			minuteS = "0" + minute;
		}
		else{
			minuteS = "" + minute;
		}
		return hourS+":"+minuteS;
	}

	private static Id makeId(String string) {
		return new IdImpl(string);
	}

}
