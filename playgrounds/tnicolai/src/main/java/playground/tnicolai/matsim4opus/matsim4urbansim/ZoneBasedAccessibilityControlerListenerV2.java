package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.gis.ZoneUtil;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.Distances;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisZoneCSVWriterV2;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimZoneCSVWriterV2;
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
 *  improvements april'12
 *  - accessibility calculation uses configurable betas (coming from UrbanSim) for car/walk travel times, -distances and -costs
 *  
 * improvements / changes july'12 
 * - fixed error: used pre-factor (1/beta scale) in deterrence function instead of beta scale (fixed now!)
 * 
 * @author thomas
 *
 */
public class ZoneBasedAccessibilityControlerListenerV2 extends AccessibilityControlerListenerImpl implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListenerV2.class);
	
	/**
	 * constructor
	 * @param zones (origin)
	 * @param aggregatedOpportunities (destination)
	 * @param benchmark
	 */
	public ZoneBasedAccessibilityControlerListenerV2(ActivityFacilitiesImpl zones, 
												   AggregateObject2NearestNode[] aggregatedOpportunities, 
												   Benchmark benchmark,
												   ScenarioImpl scenario){
		
		log.info("Initializing ZoneBasedAccessibilityControlerListenerV2 ...");
		
		assert(zones != null);
		this.zones = zones;
		assert(aggregatedOpportunities != null);
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		// writing accessibility measures continuously into "zone.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES URBANSIM INPUT
		UrbanSimZoneCSVWriterV2.initUrbanSimZoneWriter();
		// in contrast to the file above this contains all information about
		// zones but is not dedicated as input for UrbanSim, use for analysis
		AnalysisZoneCSVWriterV2.initAccessiblityWriter();
		
		initAccessibilityParameter(scenario);
		log.info(".. done initializing ZoneBasedAccessibilityControlerListener!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("zone-based accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double inverseOfLogitScaleParameter = 1/(logitScaleParameter);
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			printParameterSettings();
			
			// gather zone information like zone id, nearest node and coordinate (zone centroid)
			ZoneObject[] zones = ZoneUtil.mapZoneCentroid2NearestNode(this.zones, network);
			log.info(zones.length + "  measurement points are now processing ...");
			
			ProgressBar bar = new ProgressBar( zones.length );
			
			GeneralizedCostSum gcs = new GeneralizedCostSum();
			
			// iterating over all zones as starting points calculating their work place accessibility
			for(int fromIndex= 0; fromIndex < zones.length; fromIndex++){
				
				bar.update();
				
				// get coordinate from origin (start point)
				Coord coordFromZone = zones[fromIndex].getZoneCoordinate();
				assert( coordFromZone!=null );
				Id originZoneID = zones[fromIndex].getZoneID();
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the distance (as walk time) between a zone centroid and the road network
				Link nearestLink = network.getNearestLinkExactly(coordFromZone);

				// determine nearest network node (from- or toNode) based on the link 
				Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
				assert( fromNode != null );
				// run dijkstra on network
				lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				
				Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, coordFromZone, fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
				
				double offsetWalkTime2Node_h 				= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;
				double carTravelTime_meterpersec			= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
				double freeSpeedTravelTime_meterpersec 		= nearestLink.getFreespeed();
				
				double offsetFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTime_meterpersec * 3600);
				double offsetCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTime_meterpersec * 3600.);
				double offsetBikeTime_h						= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
				
				
				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				
				gcs.reset();
				
				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// add the avg. distance of all aggregated opportunities (euclidiean distance from nearest node to opportunity)
					double averageDistanceRoad2Opportunitiy_meter = this.aggregatedOpportunities[i].getAverageDistance();
					double offsetWalkTime2Opportunity_h = averageDistanceRoad2Opportunitiy_meter / this.walkSpeedMeterPerHour;
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					
					// using number of aggregated workplaces as weight for log sum measure
					int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

					// free speed car travel times in hours
					double freeSpeedTravelTime_h = (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + offsetFreeSpeedTime_h;
					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					// bike travel times in hours
					double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + offsetBikeTime_h; // using a constant speed of 15km/h
					// walk travel times in hours
					double walkTravelTime_h		= travelDistance_meter / this.walkSpeedMeterPerHour;
					// congested car travel times in hours
					double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
					double congestedCarTravelTime_h = (arrivalTime - depatureTime) / 3600. + offsetCongestedCarTime_h;

					sumGeneralizedCosts(gcs, 
							distanceMeasuringPoint2Road_meter + averageDistanceRoad2Opportunitiy_meter,
							distanceRoad2Node_meter, 
							offsetWalkTime2Node_h + offsetWalkTime2Opportunity_h,
							opportunityWeight, freeSpeedTravelTime_h,
							travelDistance_meter, bikeTravelTime_h,
							walkTravelTime_h, congestedCarTravelTime_h);
				}
				
				// aggregated value
				double freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility;
				if(!useRawSum){ 	// get log sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
					carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
					bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
					walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
				}
				else{ 				// get raw sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
					carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
					bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
					walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
				}

				// writing accessibility measures of current node in csv format (UrbanSim input)
				UrbanSimZoneCSVWriterV2.write(originZoneID,
												freeSpeedAccessibility,
												carAccessibility,
												bikeAccessibility,
												walkAccessibility);
				// writing complete zones information for further analysis
				AnalysisZoneCSVWriterV2.write(originZoneID, 
											zones[fromIndex].getZoneCoordinate(), 
											fromNode.getCoord(), 
											freeSpeedAccessibility,
											carAccessibility,
											bikeAccessibility,
											walkAccessibility);
			}
			System.out.println("");
			// finalizing/closing csv file containing accessibility measures
			UrbanSimZoneCSVWriterV2.close();
			AnalysisZoneCSVWriterV2.close();
			
			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " + zones.length
						+ " zones (origins) and "
						+ this.aggregatedOpportunities.length
						+ " destinations (workplaces) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
