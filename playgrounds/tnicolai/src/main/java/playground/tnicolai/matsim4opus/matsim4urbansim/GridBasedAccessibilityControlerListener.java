package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.FixedSizeGrid;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.AccessibilityStorage;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AggregatedWorkplaceCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.GridBasedAccessibilityCSVWriter;

public class GridBasedAccessibilityControlerListener implements ShutdownListener{
	
	/**
	 * Code improvements since first version (deadline ersa paper):
	 * - aggregated workplaces: workplaces with same nearest_node are aggregated to a weighted job (see 
	 * 							JobClusterObject). This means much less destinations need to be visited and 
	 * 							results in much less iteration cycles.
	 * - less time consuming look-ups: all workplaces are assigned to their nearest node in an pre-proscess step
	 * 							(see addNearestNodeToJobClusterArray) instead to do nearest node look-ups in each 
	 * 							iteration cycle
	 * - distance based accessibility: is now also computed with with LeastCostPathTree 
	 * 							-> CHECK WITH KAI IF TRAVELDISTANCECOSTCALULATOR IS OK!
	 * tnicolai: sep'11
	 */
	
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListener.class);
	
	private JobClusterObject[] aggregatedWorkplaces;
	private int resolutionMeter;
	
	private Map<Id, AccessibilityStorage> resultMap;
	
	private Benchmark benchmark;
	
	/**
	 * constructor 
	 * @param aggregatedWorkplaces
	 * @param resolutionMeter
	 * @param benchmark
	 */
	public GridBasedAccessibilityControlerListener(JobClusterObject[] aggregatedWorkplaces, int resolutionMeter, Benchmark benchmark){
		
		log.info("Initializing GridBasedAccessibilityControlerListener ...");
		
		assert(aggregatedWorkplaces != null);
		this.aggregatedWorkplaces = aggregatedWorkplaces;
		assert(resolutionMeter > 0);
		this.resolutionMeter = resolutionMeter;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		// this stores all accessibility measures (feeding the grid matrix)
		this.resultMap = new HashMap<Id, AccessibilityStorage>();
		
		GridBasedAccessibilityCSVWriter.initAccessiblityWriter( Constants.MATSIM_4_OPUS_TEMP +
													   			Constants.GRID_DATA_FILE_CSV);
		
		log.info(".. done initializing GridBasedAccessibilityControlerListener!");
	}
	
	
	/**
	 * calculating accessibility indicators from each network node to each "aggregated workplace"
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		int benchmarkID = this.benchmark.addMeasure("grid-based accessibility computation");
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
//		tnicolai: testing travel time calculator
//		LeastCostPathTree lcptTravelTimeTest = new LeastCostPathTree(ttc, new TravelTimeCostCalculator(ttc));
		// this calculates a least cost path for (travelTime*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance)   with marginalCostOfDistance = 0
		LeastCostPathTree lcptTravelTimeDistance = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// this calculates a least cost path tree only based on link.getLength() (without marginalCostOfDistance since it's zero)
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( ttc, new TravelDistanceCostCalculator() ); // tnicolai: this is experimental, check with Kai, sep'2011
		
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		try{
			log.info("Computing and writing accessibility measures ..." );
			Iterator<? extends Node> startNodeIterator = network.getNodes().values().iterator();
			int numberOfStartNodes = network.getNodes().values().size();
			log.info("Calculating " + numberOfStartNodes + " starting points ...");
			
			ProgressBar bar = new ProgressBar( numberOfStartNodes );
			
			// iterating over all network nodes as starting points calculating their workplace accessibility
			while( startNodeIterator.hasNext() ){
				
				bar.update();
				
				Node originNode = startNodeIterator.next();
				assert(originNode != null);
				// run dijkstra on network
				lcptTravelTimeDistance.calculate(network, originNode, depatureTime);
				lcptTravelDistance.calculate(network, originNode, depatureTime);
//				tnicolai: testing travel time calculator
//				lcptTravelTimeTest.calculate(network, originNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("originNode")
				
				double accessibilityTravelTimes = 0.;
				double accessibilityTravelTimeCosts = 0.;
				double accessibilityTravelDistanceCosts = 0.;
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int i = 0; i < this.aggregatedWorkplaces.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedWorkplaces[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[i].getNumberOfJobs();

					double arrivalTime = lcptTravelTimeDistance.getTree().get( nodeID ).getTime();
					
					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
//					tnicolai: testing travel time cost calculator
//					double testTravelTime_min = lcptTravelTimeTest.getTree().get( nodeID ).getCost() / 60.;
					// travel costs in utils
					double travelCosts = lcptTravelTimeDistance.getTree().get( nodeID ).getCost();
					// travel distance by car in km (since distances in meter are very large values making the log sum -Infinity most of the time) 
					double travelDistance_km = lcptTravelDistance.getTree().get( nodeID ).getCost() / 1000.;
					
					// sum travel times
					accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobWeight;
					// sum travel costs  (mention the beta)
					accessibilityTravelTimeCosts += Math.exp( beta_per_min * travelCosts ) * jobWeight; // tnicolai: find another beta for travel costs
					// sum travel distances  (mention the beta)
					accessibilityTravelDistanceCosts += Math.exp( beta_per_min * travelDistance_km ) * jobWeight; // tnicolai: find another beta for travel distance
				}
				
				// assign accessibility 
				double travelTimeAccessibility = Math.log( accessibilityTravelTimes );
				double travelCostAccessibility = Math.log( accessibilityTravelTimeCosts );
				double travelDistanceAccessibility = Math.log( accessibilityTravelDistanceCosts );

				// assigning each storage object with its corresponding node id
				this.resultMap.put(originNode.getId(), 
						new AccessibilityStorage(travelTimeAccessibility, travelCostAccessibility, travelDistanceAccessibility));
				
				// writing accessibility measures of current node in csv format
				GridBasedAccessibilityCSVWriter.write(originNode, travelTimeAccessibility, travelCostAccessibility, travelDistanceAccessibility);
			}
			
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + numberOfStartNodes
					+ " starting points (origins) and "
					+ this.aggregatedWorkplaces.length
					+ " destinations (workplaces) took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID) / 60.
					+ " minutes).");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally{
			// writing accessibility measures stored in a hash map as matrix
			int coarseSteps = 8;
			FixedSizeGrid grid = new FixedSizeGrid(resolutionMeter, network, resultMap, coarseSteps);
			grid.writeGrid();
			
			// writing aggregated workplace data in csv format
			AggregatedWorkplaceCSVWriter.writeWorkplaceData2CSV( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv", this.aggregatedWorkplaces );
			
			// finalizing/closing csv file containing accessibility measures
			GridBasedAccessibilityCSVWriter.close();
		}
	}
	
//	/**
//	 * Experimental
//	 * initialize betas for logsum cost function
//	 * 
//	 * @param scenario
//	 */
//	private void initCostfunctionParameter(Scenario scenario){
//		
//		// beta per hr should be -12 (by default configuration)
//		double beta_per_hr = scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr() ;
//		this.beta_per_minute = beta_per_hr / 60.; // get utility per second
//		
//		try{
//			beta = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA) );
//			betaTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_TIMES) );
//			betaLnTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_TIMES) );
//			betaPowerTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_TIMES) );
//			betaTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_COSTS) );
//			betaLnTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_COSTS) );
//			betaPowerTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_COSTS) );
//			betaTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_DISTANCE) );
//			betaLnTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_DISTANCE) );
//			betaPowerTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_DISTANCE) );
//		}
//		catch(NumberFormatException e){
//			e.printStackTrace();
//			System.exit(-1);
//		}
//	}
	
}
