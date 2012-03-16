package playground.tnicolai.matsim4opus.matsim4urbansim.archive;

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
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.AccessibilityStorage;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ClusterObject;
import playground.tnicolai.matsim4opus.utils.io.writer.CellBasedAccessibilityCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.WorkplaceCSVWriter;

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
	 * 	
	 * tnicolai: sep'11
	 */
	
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListener.class);
	
	private ClusterObject[] aggregatedWorkplaces;
	private double gridSizeInMeter;
	
	private Map<Id, AccessibilityStorage> resultMap;
	private CellBasedAccessibilityCSVWriter accCsvWriter;
	
	private Benchmark benchmark;
	
	/**
	 * constructor 
	 * @param aggregatedWorkplaces
	 * @param gridSizeInMeter
	 * @param benchmark
	 */
	public GridBasedAccessibilityControlerListener(ClusterObject[] aggregatedWorkplaces, double gridSizeInMeter, Benchmark benchmark){
		
		log.info("Initializing GridBasedAccessibilityControlerListener ...");
		
		assert(aggregatedWorkplaces != null);
		this.aggregatedWorkplaces = aggregatedWorkplaces;
		assert(gridSizeInMeter > 0);
		this.gridSizeInMeter = gridSizeInMeter;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		// this stores all accessibility measures (feeding the grid matrix)
		this.resultMap = new HashMap<Id, AccessibilityStorage>();
		
		accCsvWriter = new CellBasedAccessibilityCSVWriter("");
		
		log.info(".. done initializing GridBasedAccessibilityControlerListener!");
	}
	
	
	/**
	 * calculating accessibility indicators from each network node to each "aggregated workplace"
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation (without shape file)");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// calculates the workplace accessibility based on congested travel times:
				// (travelTime(sec)*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance) but marginalCostOfDistance = 0
		LeastCostPathTree lcptCongestedTravelTime = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// calculates the workplace accessibility based on freespeed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// calculates walk times in seconds as substitute for travel distances (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator(sc.getConfig().plansCalcRoute().getWalkSpeed()) );
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaBrain = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaCarHour = betaBrain * (sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaCarMin = betaCarHour / 60.; // get utility per minute. this is done for urbansim that e.g. takes travel times in minutes (tnicolai feb'12)
		double betaWalkHour = betaBrain * (sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.
		
		try{
			log.info("Computing and writing grid based accessibility measures with following settings:" );
			log.info("Depature time (in seconds): " + depatureTime);
			log.info("Beta car traveling utils/h: " + sc.getConfig().planCalcScore().getTraveling_utils_hr());
			log.info("Beta walk traveling utils/h: " + sc.getConfig().planCalcScore().getTravelingWalk_utils_hr());
			log.info("Beta performing utils/h: " + sc.getConfig().planCalcScore().getPerforming_utils_hr());
			log.info("Beta brain (scale factor): " + betaBrain);
			log.info("Beta car traveling per h: " + betaCarHour);
			log.info("Beta car traveling per min: " + betaCarMin);
			log.info("Beta walk traveling per h: " + betaWalkHour);
			log.info("Beta walk traveling per min: " + betaWalkMin);
			
			Iterator<? extends Node> startNodeIterator = network.getNodes().values().iterator();
			log.info("Calculating " + network.getNodes().values().size() + " starting points ...");
			
			ProgressBar bar = new ProgressBar( network.getNodes().values().size() ); // init progress bar with number of origins
			
			// iterating over all network nodes as starting points calculating their workplace accessibility
			while( startNodeIterator.hasNext() ){
				
				bar.update();
				
				Node originNode = startNodeIterator.next();
				assert(originNode != null);
				// run dijkstra on network
				lcptCongestedTravelTime.calculate(network, originNode, depatureTime);
				lcptCongestedTravelTime.calculate(network, originNode, depatureTime);
				lcptWalkTime.calculate(network, originNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("originNode")
				
				double congestedTravelTimesCarSum = 0.;
				double freespeedTravelTimesCarSum = 0.;
				double travelTimesWalkSum 		  = 0.; // substitute for travel distance
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int i = 0; i < this.aggregatedWorkplaces.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedWorkplaces[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[i].getNumberOfObjects();

					double arrivalTime = lcptCongestedTravelTime.getTree().get( nodeID ).getTime();
					
					// travel times by car in minutes
					double congestedTravelTime_min = (arrivalTime - depatureTime) / 60.;
					// freespeed travel times by car in minutes
					double freespeedTravelTime_min = lcptFreespeedTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// walk travel times in minutes 
					double walkTravelTime_min = lcptWalkTime.getTree().get( nodeID ).getCost() / 60.;
					
					// sum congested travel times
					congestedTravelTimesCarSum += Math.exp( betaCarMin * congestedTravelTime_min ) * jobWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( betaCarMin * freespeedTravelTime_min ) * jobWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaWalkMin * walkTravelTime_min ) * jobWeight;
				}
				
				// assign accessibility 
				double congestedTravelTimesCarLogSum = Math.log( congestedTravelTimesCarSum );
				double freespeedTravelTimesCarLogSum = Math.log( freespeedTravelTimesCarSum );
				double travelTimesWalkLogSum 		 = Math.log( travelTimesWalkSum );

				// assigning each storage object with its corresponding node id
				this.resultMap.put(originNode.getId(), 
								   new AccessibilityStorage(congestedTravelTimesCarLogSum, 
										   					freespeedTravelTimesCarLogSum, 
										   					travelTimesWalkLogSum));
				
				// writing accessibility measures of current node in csv format
//				accCsvWriter.write(startZone, 
//						   		   coordFromZone, 
//						   		   fromNode, 
//						   		   congestedTravelTimesCarLogSum, 
//						   		   freespeedTravelTimesCarLogSum, 
//						   		   travelTimesWalkLogSum);
			}
			
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + network.getNodes().values().size()
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
			int coarseSteps = 4;
			FixedSizeGrid grid = new FixedSizeGrid(gridSizeInMeter, network, resultMap, coarseSteps);
			grid.writeGrid();
			
			// writing aggregated workplace data in csv format
			WorkplaceCSVWriter.writeAggregatedWorkplaceData2CSV( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv", this.aggregatedWorkplaces );
			
			// finalizing/closing csv file containing accessibility measures
			accCsvWriter.close();
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
