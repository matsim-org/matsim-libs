package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.gis.ZoneMapper;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
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
 * @author thomas
 *
 */
public class ZoneBasedAccessibilityControlerListenerV2 extends AccessibilityControlerListenerTemplate implements ShutdownListener{
	
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
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double logitScaleParameterPreFactor = 1/(logitScaleParameter);
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			printParameterSettings();
			
			// gather zone information like zone id, nearest node and coordinate (zone centroid)
			ZoneObject[] zones = ZoneMapper.mapZoneCentroid2NearestNode(this.zones, network);
			log.info(zones.length + "  measurement points are now processing ...");
			
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
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				LinkImpl nearestLink = network.getNearestLink(coordFromZone);
				// captures the distance (as walk time) between a zone centroid and its nearest node
				double offsetDistance2NearestNode_meter = NetworkUtil.getDistance2Node(nearestLink, 
																					   coordFromZone, 
																					   fromNode);
				double offsetWalkTime2NearestNode_h		= offsetDistance2NearestNode_meter / this.walkSpeedMeterPerHour;
				double offsetDistance2NearestLink_meter = NetworkUtil.getOrthogonalDistance2NearestLink(nearestLink, coordFromZone);
				double offsetWalkTime2NearestLink_h 	= offsetDistance2NearestLink_meter / this.walkSpeedMeterPerHour;
				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				
				double sumCAR = 0.;
				double sumWALK= 0.;	
				
				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int toIndex = 0; toIndex < this.aggregatedOpportunities.length; toIndex++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[toIndex].getNearestNode();
					Id nodeID = destinationNode.getId();
					
					// using number of aggregated workplaces as weight for log sum measure
					int opportunityWeight = this.aggregatedOpportunities[toIndex].getNumberOfObjects();

					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					// walk travel times in hours
					double walkTravelTime_h		= travelDistance_meter / this.walkSpeedMeterPerHour;
					// congested car travel times in hours
					double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
					double congestedCarTravelTime_h = (arrivalTime - depatureTime) / 3600.;

					// for debugging car accessibility
					carTT = getAsUtilCar(betaCarTT, congestedCarTravelTime_h, betaWalkTT, offsetWalkTime2NearestLink_h);
					carTTPower = getAsUtilCar(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, offsetWalkTime2NearestLink_h * offsetWalkTime2NearestLink_h);
					carLnTT	= getAsUtilCar(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2NearestLink_h));
					
					carTD = getAsUtilCar(betaCarTD, travelDistance_meter, betaWalkTD, offsetDistance2NearestNode_meter);
					carTDPower = getAsUtilCar(betaCarTDPower, travelDistance_meter * travelDistance_meter, betaWalkTDPower, offsetDistance2NearestNode_meter * offsetDistance2NearestNode_meter);
					carLnTD = getAsUtilCar(betaCarLnTD, Math.log(travelDistance_meter), betaWalkLnTD, Math.log(offsetDistance2NearestNode_meter));
					
					carTC 		= 0.; 	// since MATSim doesn't gives monetary costs yet 
					carTCPower 	= 0.;	// since MATSim doesn't gives monetary costs yet 
					carLnTC 	= 0.;	// since MATSim doesn't gives monetary costs yet 
					
					// sum congested travel times
					sumCAR += opportunityWeight
							* Math.exp(logitScaleParameterPreFactor *
									  (carTT +
									   carTTPower +
									   carLnTT +
									   carTD + 
									   carTDPower + 
									   carLnTD +
									   carTC + 
									   carTCPower + 
									   carLnTC ));
					
					// for debugging walk accessibility
					walkTT = getAsUtilWalk(betaWalkTT, walkTravelTime_h + offsetWalkTime2NearestLink_h);
					walkTTPower = getAsUtilWalk(betaWalkTTPower, walkTravelTime_h*walkTravelTime_h + offsetWalkTime2NearestLink_h*offsetWalkTime2NearestLink_h );
					walkLnTT = getAsUtilWalk(betaWalkLnTT, Math.log( walkTravelTime_h + offsetWalkTime2NearestNode_h ));
					
					walkTD = getAsUtilWalk(betaWalkTD, travelDistance_meter + offsetDistance2NearestNode_meter);
					walkTDPower = getAsUtilWalk(betaWalkTDPower, travelDistance_meter*travelDistance_meter + offsetDistance2NearestNode_meter*offsetDistance2NearestNode_meter);
					walkLnTD = getAsUtilWalk(betaWalkLnTD, Math.log(travelDistance_meter + offsetDistance2NearestNode_meter));
					
					walkTC 		= 0.;	// since MATSim doesn't gives monetary costs yet 
					walkTCPower = 0.;	// since MATSim doesn't gives monetary costs yet 
					walkLnTC 	= 0.;	// since MATSim doesn't gives monetary costs yet 

					// sum walk travel times (substitute for distances)
					sumWALK += opportunityWeight
							* Math.exp(logitScaleParameterPreFactor *
									(walkTT +
									 walkTTPower +
									 walkLnTT +
									 walkTD +
									 walkTDPower +
									 walkLnTD + 
									 walkTC +
									 walkTCPower +
									 walkLnTC ));
				}
				
				// aggregated value
				double carAccessibility, walkAccessibility;
				if(!useRawSum){ 	// get log sum
					carAccessibility = logitScaleParameterPreFactor * Math.log( sumCAR );
					walkAccessibility= logitScaleParameterPreFactor * Math.log( sumWALK );
				}
				else{ 				// get raw sum
					carAccessibility = logitScaleParameterPreFactor * sumCAR;
					walkAccessibility= logitScaleParameterPreFactor * sumWALK;
				}

				// writing accessibility measures of current node in csv format (UrbanSim input)
				UrbanSimZoneCSVWriterV2.write(originZoneID,
												carAccessibility, 
												walkAccessibility);
				// writing complete zones information for further analysis
				AnalysisZoneCSVWriterV2.write(originZoneID, 
											zones[fromIndex].getZoneCoordinate(), 
											fromNode.getCoord(), 
											carAccessibility,
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
