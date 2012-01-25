package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;
import playground.tnicolai.matsim4opus.utils.io.writer.ZoneBasedAccessibilityCSVWriter;

public class ZoneBasedAccessibilityControlerListener implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListener.class);
	
	private ActivityFacilitiesImpl zones; 
	private JobClusterObject[] aggregatedWorkplaces;
	
	private Benchmark benchmark;
	
	/**
	 * constructor
	 * @param zones (origin)
	 * @param aggregatedWorkplaces (destination)
	 * @param benchmark
	 */
	public ZoneBasedAccessibilityControlerListener(ActivityFacilitiesImpl zones, 
												   JobClusterObject[] aggregatedWorkplaces, 
												   Benchmark benchmark){
		
		log.info("Initializing ZoneBasedAccessibilityControlerListener ...");
		
		assert(zones != null);
		this.zones = zones;
		assert(aggregatedWorkplaces != null);
		this.aggregatedWorkplaces = aggregatedWorkplaces;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		// writing accessibility measures continuously into "zone.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table.
		ZoneBasedAccessibilityCSVWriter.initAccessiblityWriter(Constants.MATSIM_4_OPUS_TEMP +
															   Constants.ZONES_FILE_CSV);
		
		log.info(".. done initializing ZoneBasedAccessibilityControlerListener!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		int benchmarkID = this.benchmark.addMeasure("zone-based accessibility computation");
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// this calculates a least cost path for (travelTime*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance)   with marginalCostOfDistance = 0
		LeastCostPathTree lcptTravelTimeDistance = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// this calculates a least cost path tree only based on link.getLength() (without marginalCostOfDistance since it's zero)
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( ttc, new TravelDistanceCostCalculator() ); // tnicolai: this is experimental, check with Kai, sep'2011
		
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			
			// gather zone information like zone id, nearest node and coordinate (zone centroid)
			ZoneObject[] zones = UtilityCollection.assertZoneCentroid2NearestNode(this.zones, network);
			assert( zones != null );
			log.info("Calculating " + zones.length + " zones ...");
			
			ProgressBar bar = new ProgressBar( zones.length );
			
			// iterating over all zones as starting points calculating their workplace accessibility
			for(int fromIndex= 0; fromIndex < zones.length; fromIndex++){
				
				bar.update();
				
				// get nearest network node and zone id for origin zone
				Node fromNode = zones[fromIndex].getNearestNode();
				Id originZoneID = zones[fromIndex].getZoneID();
				// run dijkstra on network
				lcptTravelTimeDistance.calculate(network, fromNode, depatureTime);
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				double accessibilityTravelTimes = 0.;
				double accessibilityTravelTimeCosts = 0.;
				double accessibilityTravelDistanceCosts = 0.;
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int toIndex = 0; toIndex < this.aggregatedWorkplaces.length; toIndex++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedWorkplaces[toIndex].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[toIndex].getNumberOfJobs();

					double arrivalTime = lcptTravelTimeDistance.getTree().get( nodeID ).getTime();
					
					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
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
				
				// get log sum 
				double travelTimeAccessibility = Math.log( accessibilityTravelTimes );
				double travelCostAccessibility = Math.log( accessibilityTravelTimeCosts );
				double travelDistanceAccessibility = Math.log( accessibilityTravelDistanceCosts );

				// writing accessibility measures of current node in csv format
				ZoneBasedAccessibilityCSVWriter.write(originZoneID, travelTimeAccessibility, travelCostAccessibility, travelDistanceAccessibility);
			}
			
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + zones.length
					+ " zones (origins) and "
					+ this.aggregatedWorkplaces.length
					+ " destinations (workplaces) took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID) / 60.
					+ " minutes).");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally{
			// finalizing/closing csv file containing accessibility measures
			ZoneBasedAccessibilityCSVWriter.close();
		}
	}
}
