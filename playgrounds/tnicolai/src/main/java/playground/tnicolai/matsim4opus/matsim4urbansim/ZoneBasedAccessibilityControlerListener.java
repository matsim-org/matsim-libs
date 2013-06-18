package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.ZoneUtil;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisZoneCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimZoneCSVWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

/**
 *  improvements feb'12
 *  - distance between zone centroid and nearest node on road network is considered in the accessibility computation
 *  as walk time of the euclidian distance between both (centroid and nearest node). This walk time is added as an offset 
 *  to each measured travel times
 *  - using walk travel times instead of travel distances. This is because of the betas that are utils/time unit. The walk time
 *  corresponds to distances since this is also linear.
 * 
 * This works for UrbanSim Zone and Parcel Applications !!! (march'12)
 * 
 * @author thomas
 *
 */
public class ZoneBasedAccessibilityControlerListener implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListener.class);
	
	private ActivityFacilitiesImpl zones; 
	private AggregateObject2NearestNode[] aggregatedOpportunities;
	
	private double walkSpeedMeterPerMin = -1;
	
	private Benchmark benchmark;
	
	/**
	 * constructor
	 * @param zones (origin)
	 * @param aggregatedOpportunities (destination)
	 * @param benchmark
	 */
	public ZoneBasedAccessibilityControlerListener(ActivityFacilitiesImpl zones, 
												   AggregateObject2NearestNode[] aggregatedOpportunities, 
												   Benchmark benchmark){
		
		log.info("Initializing ZoneBasedAccessibilityControlerListener ...");
		
		assert(zones != null);
		this.zones = zones;
		assert(aggregatedOpportunities != null);
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		// writing accessibility measures continuously into "zone.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES URBANSIM INPUT
		UrbanSimZoneCSVWriter.initUrbanSimZoneWriter(InternalConstants.MATSIM_4_OPUS_TEMP +
													 UrbanSimZoneCSVWriter.FILE_NAME);
		// in contrast to the file above this contains all information about
		// zones but is not dedicated as input for UrbanSim, use for analysis
		AnalysisZoneCSVWriter.initAccessiblityWriter(InternalConstants.MATSIM_4_OPUS_TEMP + 
											 		 InternalConstants.ZONES_COMPLETE_FILE_CSV);
		
		log.info(".. done initializing ZoneBasedAccessibilityControlerListener!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("zone-based accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
		double walkSpeedMeterPerSec = sc.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
		this.walkSpeedMeterPerMin = walkSpeedMeterPerSec * 60.;
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getLinkTravelTimes();
		// calculates the workplace accessibility based on congested travel times:
		// (travelTime(sec)*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance) but marginalCostOfDistance = 0
		LeastCostPathTree lcptCongestedTravelTime = new LeastCostPathTree( ttc, new TravelTimeAndDistanceBasedTravelDisutility(ttc, controler.getConfig().planCalcScore()) );
		// calculates the workplace accessibility based on free-speed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// calculates walk times in seconds as substitute for travel distances (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator( walkSpeedMeterPerSec ) );
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaScale = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaScalePreFactor = 1/betaScale;
		double betaCarHour = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double betaCarMin = betaCarHour / 60.; // get utility per minute. this is done for urbansim that e.g. takes travel times in minutes (tnicolai feb'12)
		double betaWalkHour = sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			
			log.info("Computing and writing grid based accessibility measures with following settings:" );
			log.info("Departure time (in seconds): " + depatureTime);
			log.info("Beta car traveling utils/h: " + sc.getConfig().planCalcScore().getTraveling_utils_hr());
			log.info("Beta walk traveling utils/h: " + sc.getConfig().planCalcScore().getTravelingWalk_utils_hr());
			log.info("Beta performing utils/h: " + sc.getConfig().planCalcScore().getPerforming_utils_hr());
			log.info("Beta scale: " + betaScale);
			log.info("Beta car traveling per h: " + betaCarHour);
			log.info("Beta car traveling per min: " + betaCarMin);
			log.info("Beta walk traveling per h: " + betaWalkHour);
			log.info("Beta walk traveling per min: " + betaWalkMin);
			log.info("Walk speed (meter/min): " + this.walkSpeedMeterPerMin);
			
			// gather zone information like zone id, nearest node and coordinate (zone centroid)
			ZoneObject[] zones = ZoneUtil.mapZoneCentroid2NearestNode(this.zones, network);
			assert( zones != null );
			log.info("Calculating " + zones.length + " zones ...");
			
			ProgressBar bar = new ProgressBar( zones.length );
			
			// iterating over all zones as starting points calculating their workplace accessibility
			for(int fromIndex= 0; fromIndex < zones.length; fromIndex++){
				
				bar.update();
				// get coordinate from origin (start point)
				Coord coordFromZone = zones[fromIndex].getZoneCoordinate();
				// get nearest network node and zone id for origin zone
				Node fromNode = zones[fromIndex].getNearestNode();
				assert( fromNode != null );
				Id originZoneID = zones[fromIndex].getZoneID();
				// run dijkstra on network
				lcptCongestedTravelTime.calculate(network, fromNode, depatureTime);
				lcptFreespeedTravelTime.calculate(network, fromNode, depatureTime);
				lcptWalkTime.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				double walkTimeOffset_min = NetworkUtil.getDistance2Node(network.getNearestLink(coordFromZone), 
																		 coordFromZone, 
						 												 fromNode)  / this.walkSpeedMeterPerMin;
				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(zones[fromIndex].getZoneCoordinate(), fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( zones[fromIndex].getZoneCoordinate() );
				// double walkTimeOffset_min = (nearestLink.calcDistance( zones[fromIndex].getZoneCoordinate() ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				double congestedTravelTimesCarSum = 0.;
				double freespeedTravelTimesCarSum = 0.;
				double travelTimesWalkSum 		  = 0.; // substitute for travel distance
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int toIndex = 0; toIndex < this.aggregatedOpportunities.length; toIndex++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[toIndex].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedOpportunities[toIndex].getNumberOfObjects();

					double arrivalTime = lcptCongestedTravelTime.getTree().get( nodeID ).getTime();
					
					// congested car travel times in minutes
					double congestedTravelTime_min = (arrivalTime - depatureTime) / 60.;
					// freespeed car  travel times in minutes
					double freespeedTravelTime_min = lcptFreespeedTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// walk travel times in minutes
					double walkTravelTime_min = lcptWalkTime.getTree().get( nodeID ).getCost() / 60.;

					// sum congested travel times
					congestedTravelTimesCarSum += Math.exp( betaScale * ((betaCarMin * congestedTravelTime_min) + (betaWalkMin * walkTimeOffset_min)) ) * jobWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( betaScale * ((betaCarMin * freespeedTravelTime_min) + (betaWalkMin * walkTimeOffset_min)) ) * jobWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaScale * (betaWalkMin * (walkTravelTime_min + walkTimeOffset_min)) ) * jobWeight;
				}
				
				// get log sum 
				double congestedTravelTimesCarLogSum = betaScalePreFactor * Math.log( congestedTravelTimesCarSum );
				double freespeedTravelTimesCarLogSum = betaScalePreFactor * Math.log( freespeedTravelTimesCarSum );
				double travelTimesWalkLogSum 		 = betaScalePreFactor * Math.log( travelTimesWalkSum );

				// writing accessibility measures of current node in csv format (UrbanSim input)
				UrbanSimZoneCSVWriter.write(originZoneID,
											congestedTravelTimesCarLogSum, 
											freespeedTravelTimesCarLogSum, 
											travelTimesWalkLogSum);
				// writing complete zones information for further analysis
				AnalysisZoneCSVWriter.write(originZoneID, 
											zones[fromIndex].getZoneCoordinate(), 
											fromNode.getCoord(), 
											congestedTravelTimesCarLogSum, 
											freespeedTravelTimesCarLogSum, 
											travelTimesWalkLogSum);
			}
			
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + zones.length
					+ " zones (origins) and "
					+ this.aggregatedOpportunities.length
					+ " destinations (workplaces) took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID) / 60.
					+ " minutes).");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally{
			// finalizing/closing csv file containing accessibility measures
			UrbanSimZoneCSVWriter.close();
			AnalysisZoneCSVWriter.close();
		}
	}
}
