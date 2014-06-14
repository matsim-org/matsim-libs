package sensitivity.scenarios.lcpa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jsprit.analysis.toolbox.AlgorithmSearchProgressChartListener;
import jsprit.analysis.toolbox.SolutionPrinter;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import jsprit.core.algorithm.termination.VariationCoefficientTermination;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.io.VrpXMLReader;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.problem.vehicle.VehicleTypeImpl.VehicleCostParams;
import jsprit.core.util.Solutions;

import org.apache.commons.lang.time.StopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.InternalLeastCostPathCalculatorListener;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import sensitivity.scenarios.StopAlgoCompTime;


public class VehicleTypes {
	
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
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("sensitivity/network.xml");
		
		StopRouting stopRouting = new StopRouting();
		
		VehicleRoutingProblem.Builder builder = VehicleRoutingProblem.Builder.newInstance();
		new VrpXMLReader(builder).read("sensitivity/vrp_tight_tw.xml");

		NetworkBasedTransportCosts.Builder costBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork());
		addVehicleTypeSpecificCosts(costBuilder,builder.getAddedVehicles());
		costBuilder.setThreadSafeLeastCostPathCalculatorFactory(LCPAFactories.getAStarEuclideanFactory());
		NetworkBasedTransportCosts routingCosts = costBuilder.build();
		routingCosts.getInternalListeners().add(stopRouting);
		builder.setRoutingCost(routingCosts);
		
		VehicleRoutingProblem vrp = builder.build();
		
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "sensitivity/algorithm.xml");
		vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener("output/progress.png"));
//		vra.getAlgorithmListeners().addListener(new StopWatch());
		StopAlgoCompTime compTimeStopper = new StopAlgoCompTime();
		vra.getAlgorithmListeners().addListener(compTimeStopper,Priority.HIGH);
		VariationCoefficientTermination prematureAlgorithmBreaker = new VariationCoefficientTermination(100, 0.001);
		vra.setPrematureAlgorithmTermination(prematureAlgorithmBreaker);
		vra.getAlgorithmListeners().addListener(prematureAlgorithmBreaker);
		
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
		
		Carriers carriers = new Carriers();
		Carrier carrier = MatsimJspritFactory.createCarrier("myCarrier", vrp);
		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, Solutions.bestOf(solutions));
		carrier.setSelectedPlan(plan);
		
		NetworkRouter.routePlan(plan, routingCosts);
		
		carriers.addCarrier(carrier);
		
//		new VrpXMLWriter(vrp, solutions).write("output/vrp-sol.xml");
		SolutionPrinter.print(Solutions.bestOf(solutions));
		printLowerBoundOfNuVehicles(vrp,40);
		
//		new Visualiser(config,scenario).visualizeLive(carriers);
//		SolutionPlotter.plotSolutionAsPNG(vrp, Solutions.getBest(solutions), "output/vrp-sol.png", "sol");
		long time = 0;
		for(StopWatch w : stopRouting.getRouterStopWatches().values()){
			time+=w.getTime(); 
		}
		System.out.println("routingTime: " + time);
		System.out.println("compTime: " + compTimeStopper.stopWatch.toString());
		
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
