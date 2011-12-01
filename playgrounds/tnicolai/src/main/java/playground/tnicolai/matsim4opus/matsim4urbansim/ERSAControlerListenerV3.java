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
import playground.tnicolai.matsim4opus.utils.io.writer.AccessibilityCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.AggregatedWorkplaceCSVWriter;

public class ERSAControlerListenerV3 implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ERSAControlerListenerV3.class);
	
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
	public ERSAControlerListenerV3(JobClusterObject[] aggregatedWorkplaces, int resolutionMeter, Benchmark benchmark){
		
		log.info("Initializing ERSAControlerListenerV3 ...");
		
		assert(aggregatedWorkplaces != null);
		this.aggregatedWorkplaces = aggregatedWorkplaces;
		assert(resolutionMeter > 0);
		this.resolutionMeter = resolutionMeter;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		this.resultMap = new HashMap<Id, AccessibilityStorage>();
		
		AccessibilityCSVWriter.initAccessiblityWriter( Constants.MATSIM_4_OPUS_TEMP +
													   "accessibility_indicators_v2" + 
													   Constants.FILE_TYPE_CSV);
		
		log.info(".. done initializing ERSAControlerListenerV3!");
	}
	
	
	/**
	 * calculating accessibility indicators
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		NetworkImpl network = controler.getNetwork();
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// this calculates the workplace accessibility travel times
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// this calculates the workplace accessibility distances
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( ttc, new TravelDistanceCostCalculator() ); // tnicolai: this is experimental, check with Kai, sep'2011
		
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		try{
			log.info("Computing and writing accessibility measures ..." );
			Iterator<Node> startNodeIterator = network.getNodes().values().iterator();
			int numberOfStartNodes = network.getNodes().values().size();
			log.info("Calculating " + numberOfStartNodes + " starting points ...");
			
			ProgressBar bar = new ProgressBar( numberOfStartNodes );
			
			// iterating over all network nodes as starting points calculating their workplace accessibility
			while( startNodeIterator.hasNext() ){
				
				bar.update();
				
				Node originNode = startNodeIterator.next();
				assert(originNode != null);
				// run dijkstra on network
				lcptTravelTime.calculate(network, originNode, depatureTime);
				lcptTravelDistance.calculate(network, originNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("originNode")
				
				double accessibilityTravelTimes = 0.;
				double accessibilityTravelTimeCosts = 0.;
				double accessibilityTravelDistanceCosts = 0.;
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int i = 0; i < this.aggregatedWorkplaces.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggegated workplace)
					Node destinationNode = this.aggregatedWorkplaces[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggegated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[i].getNumberOfJobs();

					double arrivalTime = lcptTravelTime.getTree().get( nodeID ).getTime();
					
					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
					// travel costs in utils
					double travelCosts = lcptTravelTime.getTree().get( nodeID ).getCost();
					// travel distance by car in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					
					// sum travel times
					accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobWeight;
					// sum travel costs
					accessibilityTravelTimeCosts += Math.exp( beta_per_min * travelCosts ) * jobWeight; // tnicolai: find another beta for travel costs
					// sum travel distances
					accessibilityTravelDistanceCosts += Math.exp( beta_per_min * travelDistance_meter ) * jobWeight; // tnicolai: find another beta for travel distance
				}
				
				// assign accessibility 
				double tTimeAccessibility = Math.log( accessibilityTravelTimes );
				double tCostAccessibility = Math.log( accessibilityTravelTimeCosts );
				double tDistanceAccessibility = Math.log( accessibilityTravelDistanceCosts );
				
				// storing accessibility measures of different cost functions (log sums) in a storage
				AccessibilityStorage as = new AccessibilityStorage(tTimeAccessibility, tCostAccessibility, tDistanceAccessibility);
				
				// assigning each storage object with its corresponding node id
				this.resultMap.put(originNode.getId(), as);
				
				// writing accessibility measures of current node in csv format
				AccessibilityCSVWriter.write(originNode, tTimeAccessibility, tCostAccessibility, tDistanceAccessibility);
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
			int coarseSteps = 10;
			FixedSizeGrid grid = new FixedSizeGrid(resolutionMeter, network, resultMap, coarseSteps);
			grid.writeGrid();
			
			// writing aggregated workplace data in csv format
			AggregatedWorkplaceCSVWriter.writeWorkplaceData2CSV( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv", this.aggregatedWorkplaces );
			
			// finalizing/closing csv file containing accessibility measures
			AccessibilityCSVWriter.close();
		}
	}
}
