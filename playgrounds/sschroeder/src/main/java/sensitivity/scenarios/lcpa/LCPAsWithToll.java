package sensitivity.scenarios.lcpa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsprit.analysis.toolbox.SolutionPrinter;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.io.VrpXMLReader;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.problem.vehicle.VehicleTypeImpl.VehicleCostParams;
import jsprit.core.util.Solutions;

import org.apache.commons.lang.time.StopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.InternalLeastCostPathCalculatorListener;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import sensitivity.scenarios.StopAlgoCompTime;


public class LCPAsWithToll {
	
	static class StopRouting implements InternalLeastCostPathCalculatorListener {

		private Map<Long,StopWatch> routerStopWatches = new HashMap<Long, StopWatch>();
		
		@Override
		public void startCalculation(long routerId) {
			if(!routerStopWatches.containsKey(Long.valueOf(routerId))){
				routerStopWatches.put(Long.valueOf(routerId), new StopWatch());
				routerStopWatches.get(Long.valueOf(routerId)).start();
			}
			else{
				routerStopWatches.get(Long.valueOf(routerId)).resume();
			}
			
		}

		@Override
		public void endCalculation(long routerId) {
			routerStopWatches.get(Long.valueOf(routerId)).suspend();
		}

		/**
		 * @return the routerStopWatches
		 */
		public Map<Long, StopWatch> getRouterStopWatches() {
			return routerStopWatches;
		}
	
	}
	
	static class LCPAFactory {
		final String name;
		final LeastCostPathCalculatorFactory factory;
		
		public LCPAFactory(String name, LeastCostPathCalculatorFactory factory) {
			super();
			this.name = name;
			this.factory = factory;
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("sschroeder/sensitivity/network.xml");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("sschroeder/sensitivity/output/lcpas-with-distance-toll.txt")));
		writer.write("lcpa\trun\tcomptime\trelations\n");
		List<LCPAFactory> factories = new ArrayList<LCPAFactory>();
		factories.add(new LCPAFactory("dijstra", LCPAFactories.getFastDijkstraFactory()));
		factories.add(new LCPAFactory("astar-eucl", LCPAFactories.getFastAStarEuclideanFactory()));
		factories.add(new LCPAFactory("astar-landm", LCPAFactories.getFastAStarLandmarksFactory()));
		
		for(LCPAFactory f : factories){
			for(int run=0;run<10;run++){
				StopRouting stopRouting = new StopRouting();
				VehicleRoutingProblem.Builder builder = VehicleRoutingProblem.Builder.newInstance();
				new VrpXMLReader(builder).read("sschroeder/sensitivity/vrp_tight_tw.xml");
				NetworkBasedTransportCosts.Builder costBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork());
				addVehicleTypeSpecificCosts(costBuilder,builder.getAddedVehicles());
				costBuilder.setThreadSafeLeastCostPathCalculatorFactory(f.factory);

				//create roadPricing Calc
				VehicleTypeDependentRoadPricingCalculator roadPricing = new VehicleTypeDependentRoadPricingCalculator();
				//define and add a roadPricingSchema
				RoadPricingSchemeImpl cordonToll = new RoadPricingSchemeImpl(); 
				new RoadPricingReaderXMLv1(cordonToll).parse("sschroeder/sensitivity/distanceToll1.xml");
				roadPricing.addPricingScheme("type1", cordonToll);
				//finally add roadpricingcalc to netBasedTransportCosts
				costBuilder.setRoadPricingCalculator(roadPricing);
				
				NetworkBasedTransportCosts routingCosts = costBuilder.build();
				routingCosts.getInternalListeners().add(stopRouting);
				builder.setRoutingCost(routingCosts);

				VehicleRoutingProblem vrp = builder.build();

				VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "sschroeder/sensitivity/algorithm.xml");
				StopAlgoCompTime compTimeStopper = new StopAlgoCompTime();
				vra.getAlgorithmListeners().addListener(compTimeStopper,Priority.HIGH);
				Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

				SolutionPrinter.print(Solutions.bestOf(solutions));
				printLowerBoundOfNuVehicles(vrp,40);
				
				long time = 0;
				for(StopWatch w : stopRouting.getRouterStopWatches().values()){
					time+=w.getTime(); 
				}
				writer.write(f.name+"\t"+run+"\t"+time+"\t"+routingCosts.ttMemorizedCounter.getCounter()+"\n");
				System.out.println("routingTime: " + time);
				System.out.println("compTime: " + compTimeStopper.stopWatch.toString());
			}
		}
		writer.close();	
	}

	private static void printLowerBoundOfNuVehicles(VehicleRoutingProblem vrp, int vCap) {
		int demand=0;
		for(Job j : vrp.getJobs().values()){
			demand+=j.getSize().get(0);
		}
		System.out.println("lowerBound="+((double)demand/(double)vCap));
	}

	private static void addVehicleTypeSpecificCosts(Builder costBuilder, Collection<Vehicle> vehicles) {
		for(Vehicle v : vehicles){
			VehicleCostParams params = v.getType().getVehicleCostParams();
			costBuilder.addVehicleTypeSpecificCosts(v.getType().getTypeId(), params.fix, params.perTimeUnit, params.perDistanceUnit);
		}
	}
}
