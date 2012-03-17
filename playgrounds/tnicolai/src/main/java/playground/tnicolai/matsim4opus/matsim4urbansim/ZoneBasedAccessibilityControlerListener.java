package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.EuclideanDistance;
import playground.tnicolai.matsim4opus.gis.ZoneMapper;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;
import playground.tnicolai.matsim4opus.utils.io.writer.ZoneBasedAccessibilityCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.ZoneCSVWriter;

/**
 *  improvements feb'12
 *  - distance between zone centroid and nearest node on road network is considered in the accessibility computation
 *  as walk time of the euclidian distance between both (centroid and nearest node). This walk time is added as an offset 
 *  to each measured travel times
 *  - using walk travel times instead of travel distances. This is because of the betas that are utils/time unit. The walk time
 *  corresponds to distances since this is also linear.
 * 
 * @author thomas
 *
 */
public class ZoneBasedAccessibilityControlerListener implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListener.class);
	
	private ActivityFacilitiesImpl zones; 
	private ClusterObject[] aggregatedWorkplaces;
	
	private double walkSpeedMeterPerMin = -1;
	
	private Benchmark benchmark;
	
	/**
	 * constructor
	 * @param zones (origin)
	 * @param aggregatedWorkplaces (destination)
	 * @param benchmark
	 */
	public ZoneBasedAccessibilityControlerListener(ActivityFacilitiesImpl zones, 
												   ClusterObject[] aggregatedWorkplaces, 
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
		// data set table. THIS PRODUCES URBANSIM INPUT
		ZoneBasedAccessibilityCSVWriter.initAccessiblityWriter(Constants.MATSIM_4_OPUS_TEMP +
															   Constants.ZONES_FILE_CSV);
		// in contrast to the file above this contains all information about zones but is not dedicated for URBASIM
		ZoneCSVWriter.initAccessiblityWriter(Constants.MATSIM_4_OPUS_TEMP + 
											 Constants.ZONES_COMPLETE_FILE_CSV);
		
		log.info(".. done initializing ZoneBasedAccessibilityControlerListener!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
		int benchmarkID = this.benchmark.addMeasure("zone-based accessibility computation");
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// calculates the workplace accessibility based on congested travel times:
		// (travelTime(sec)*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance) but marginalCostOfDistance = 0
		LeastCostPathTree lcptCongestedTravelTime = new LeastCostPathTree( ttc, new TravelTimeAndDistanceBasedTravelDisutility(ttc, controler.getConfig().planCalcScore()) );
		// calculates the workplace accessibility based on freespeed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// this calculates a least cost path tree only based on link.getLength() (without marginalCostOfDistance since it's zero)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator(sc.getConfig().plansCalcRoute().getWalkSpeed()) );
		
		this.walkSpeedMeterPerMin = sc.getConfig().plansCalcRoute().getWalkSpeed() * 60.;
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaBrain = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaCarHour = betaBrain * (sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaCarMin = betaCarHour / 60.; // get utility per minute
		double betaWalkHour = betaBrain * (sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			
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
			log.info("Walk speed (meter/min): " + this.walkSpeedMeterPerMin);
			
			// gather zone information like zone id, nearest node and coordinate (zone centroid)
			ZoneObject[] zones = ZoneMapper.mapZoneCentroid2NearestNode(this.zones, network);
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
				lcptCongestedTravelTime.calculate(network, fromNode, depatureTime);
				lcptFreespeedTravelTime.calculate(network, fromNode, depatureTime);
				lcptWalkTime.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the eulidean distance between a zone centroid and its nearest node
				LinkImpl nearestLink = network.getNearestLink( zones[fromIndex].getZoneCoordinate() );
				double distCentroid2Link = nearestLink.calcDistance( zones[fromIndex].getZoneCoordinate() );
				double walkTimeOffset_min = (distCentroid2Link / this.walkSpeedMeterPerMin); 
//				double walkTimeOffset_min = EuclideanDistance.getEuclideanDistanceAsWalkTimeInSeconds(zones[fromIndex].getZoneCoordinate(), fromNode.getCoord()) / 60.;
				double congestedTravelTimesCarSum = 0.;
				double freespeedTravelTimesCarSum = 0.;
				double travelTimesWalkSum 		  = 0.; // substitute for travel distance
				
				// iterate through all aggregated jobs (respectively their nearest network nodes) 
				// and calculate workplace accessibility for current start/origin node.
				for ( int toIndex = 0; toIndex < this.aggregatedWorkplaces.length; toIndex++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedWorkplaces[toIndex].getNearestNode();
					Id nodeID = destinationNode.getId();
					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[toIndex].getNumberOfObjects();

					double arrivalTime = lcptCongestedTravelTime.getTree().get( nodeID ).getTime();
					
					// congested car travel times in minutes
					double congestedTravelTime_min = (arrivalTime - depatureTime) / 60.;
					// freespeed car  travel times in minutes
					double freespeedTravelTime_min = lcptFreespeedTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// walk travel times in minutes
					double walkTravelTime_min = lcptWalkTime.getTree().get( nodeID ).getCost() / 60.;

					// sum congested travel times
					congestedTravelTimesCarSum += Math.exp( (betaCarMin * congestedTravelTime_min) + (betaWalkMin * walkTimeOffset_min) ) * jobWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( (betaCarMin * freespeedTravelTime_min) + (betaWalkMin * walkTimeOffset_min) ) * jobWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaWalkMin * (walkTravelTime_min + walkTimeOffset_min) ) * jobWeight;
				}
				
				// get log sum 
				double congestedTravelTimesCarLogSum = Math.log( congestedTravelTimesCarSum );
				double freespeedTravelTimesCarLogSum = Math.log( freespeedTravelTimesCarSum );
				double travelTimesWalkLogSum 		 = Math.log( travelTimesWalkSum );

				// writing accessibility measures of current node in csv format (UrbanSim input)
				ZoneBasedAccessibilityCSVWriter.write(originZoneID,
													  congestedTravelTimesCarLogSum, 
													  freespeedTravelTimesCarLogSum, 
													  travelTimesWalkLogSum);
				// writing complete zones information for further analysis
				ZoneCSVWriter.write(originZoneID, 
									zones[fromIndex].getZoneCoordinate(), 
									fromNode.getCoord(), 
									congestedTravelTimesCarLogSum, 
									freespeedTravelTimesCarLogSum, 
									travelTimesWalkLogSum);
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
			ZoneCSVWriter.close();
		}
	}
}
