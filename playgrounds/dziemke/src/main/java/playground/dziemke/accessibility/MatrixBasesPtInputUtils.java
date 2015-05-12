package playground.dziemke.accessibility;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author dziemke
 */
public class MatrixBasesPtInputUtils {
	private static final Logger log = Logger.getLogger(MatrixBasesPtInputUtils.class);

	public static void main(String[] args) {
		String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";
		String networkFile = "../../matsim/examples/pt-tutorial/multimodalnetwork.xml";
		String outputFileStops = "stops.csv";
		String outputFileRoot = "";
				
		double departureTime = 8. * 60 * 60;
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

		scenario.getConfig().scenario().setUseTransit(true);
		
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transitScheduleFile);
		
		Map<Id<Coord>, Coord> locationFacilitiesMap = new HashMap<Id<Coord>, Coord>();
		
		for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
			Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
			Coord coord = transitStopFacility.getCoord();
			locationFacilitiesMap.put(id, coord);
		}
				
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFile);
		
		createStopsFileBasedOnSchedule(scenario, outputFileStops, ",");
		
		new ThreadedMatrixCreator(scenario, locationFacilitiesMap, locationFacilitiesMap, departureTime, outputFileRoot, "\t", 1);		
	}
	
	
	/**
	 * Creates a csv file containing the public transport stops as they are given in the transit schedule
	 * 
	 * @param scenario
	 * @param locationFacilitiesMap
	 * @param outputFileStops
	 * @param separator
	 */
	public static void createStopsFileBasedOnSchedule(Scenario scenario, String outputFileStops, String separator) {
		Map<Id<TransitStopFacility>, TransitStopFacility> transitStopFacilitiesMap = scenario.getTransitSchedule().getFacilities();
		
		final InputsCSVWriter stopsWriter = new InputsCSVWriter(outputFileStops, separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();
			
		for (TransitStopFacility transitStopFacility : transitStopFacilitiesMap.values()) {
			stopsWriter.writeField(transitStopFacility.getId());
			stopsWriter.writeField(transitStopFacility.getCoord().getX());
			stopsWriter.writeField(transitStopFacility.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		stopsWriter.close();
		log.info("Stops file based on schedule written.");
	}
	
	
	/**
	 * Creates a csv file based on the measuring points
	 * 
	 * @param scenario
	 * @param measuringPoints
	 * @param outputFileStops
	 * @param separator
	 */
	public static void createStopsFileBasedOnMeasuringPoints(Scenario scenario, ActivityFacilitiesImpl measuringPoints,
			String outputFileStops, String separator) {
				
		final InputsCSVWriter stopsWriter = new InputsCSVWriter(outputFileStops, separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();
			
		for (ActivityFacility transitStopFacility : measuringPoints.getFacilities().values()) {
			stopsWriter.writeField(transitStopFacility.getId());
			stopsWriter.writeField(transitStopFacility.getCoord().getX());
			stopsWriter.writeField(transitStopFacility.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		stopsWriter.close();
		log.info("Stops file based on measuring points written.");
	}
}


/**
 * 
 * @author dziemke
 *
 */
class ThreadedMatrixCreator implements Runnable {
//	private static final Logger log = Logger.getLogger(ThreadedMatrixCreator.class);
	
	Thread thread;
	Integer threadName;
	Scenario scenario;
	Map<Id<Coord>, Coord> locationFacilitiesFromMap;
	Map<Id<Coord>, Coord> locationFacilitiesToMap;
	double departureTime;
	String outputFileRoot;
	String separator;
	
	
	ThreadedMatrixCreator(Scenario scenario, Map<Id<Coord>, Coord> locationFacilitiesFromMap,
			Map<Id<Coord>, Coord> locationFacilitiesToMap,
			double departureTime, String outputFileRoot, String separator, int threadName){
		this.scenario = scenario;
		this.locationFacilitiesFromMap = locationFacilitiesFromMap;
		this.locationFacilitiesToMap = locationFacilitiesToMap;
		this.departureTime = departureTime;
		this.outputFileRoot = outputFileRoot;
		this.separator = separator;
		this.threadName = threadName;
		
		thread = new Thread (this, this.threadName.toString());
		thread.start ();
	}


	public void run() {
		TransitSchedule transitSchedule = this.scenario.getTransitSchedule();
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
	    
	    final InputsCSVWriter travelTimeMatrixWriter = new InputsCSVWriter(
	    		this.outputFileRoot + "travelTimeMatrix_" + this.threadName + ".csv", this.separator);
	    final InputsCSVWriter travelDistanceMatrixWriter = new InputsCSVWriter(
	    		this.outputFileRoot + "travelDistanceMatrix_" + this.threadName + ".csv", this.separator);
	    
		Network network = scenario.getNetwork();
		
		
		// Create a map with all transit routes and a list holding their network links
		Map<Id<TransitRoute>, List<Id<Link>>> transitRouteNetworkLinksMap = new HashMap<Id<TransitRoute>, List<Id<Link>>>();
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Id<Link>> fullLinkIdList = new LinkedList<Id<Link>>();
				fullLinkIdList.add(transitRoute.getRoute().getStartLinkId()); //start link
				fullLinkIdList.addAll(transitRoute.getRoute().getLinkIds()); // intermediate links
				fullLinkIdList.add(transitRoute.getRoute().getEndLinkId()); // end link

				transitRouteNetworkLinksMap.put(transitRoute.getId(), fullLinkIdList);
			}
		}
	

		for (Id<Coord> fromLocation : locationFacilitiesFromMap.keySet()) {
			for (Id<Coord> toLocation : locationFacilitiesToMap.keySet()) {
				Coord fromCoord = locationFacilitiesFromMap.get(fromLocation);
				Coord toCoord = locationFacilitiesToMap.get(toLocation);
				
				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, departureTime, null);
//				Path path = transitRouterDistanceRouter.calcPath(fromCoord, toCoord, departureTime, null);
				
				double travelTime = 0.;
				double travelDistance = 0.;
				
//				int counterRouteNull = 0;
//				int counterRoutePt = 0;
//				int counterRouteWalk = 0;
								
				if (legList == null) {
					throw new RuntimeException("The leg list is null! This should not happen, because -- even if the origin and destination"
							+ "are the same -- there should be one (transit) walk leg with zero travel time.");
				
				} else { // i.e. leg list is NOT null
					for(Leg leg : legList) {
						if(leg == null) {
							throw new RuntimeException("Leg is null.");
						}
						travelTime = travelTime + leg.getTravelTime();

						GenericRouteImpl legRoute = (GenericRouteImpl) leg.getRoute();
						
						String mode = leg.getMode();
						if (legRoute == null) {
							if (mode == TransportMode.transit_walk) {
								// This (route = null and mode = transit_walk) is the case for network access and egress)
								// Apparently the beelineWalkSpeed is used to calculate the walk time that we have. So, we need to use this
								// beelineWalkSpeed here, too, in order to come from given walk time (bqck) to walk distance
								double beelineWalkSpeed = transitRouterConfig.getBeelineWalkSpeed();
								double transitWalkDistance = beelineWalkSpeed * leg.getTravelTime();
								
//								log.info("access/egress walk = " + legRoute);
//								counterRouteNull++;
								
								travelDistance = travelDistance + transitWalkDistance;
							} else {
								throw new RuntimeException("The only route that can be null, is a route that belongs to a transit walk.");
							}			
						} else { // route != null
							if (mode == TransportMode.pt) {
								// Have to cast it to ExperimenalTransitRoute since otherwise the method getRouteId will not be available
								ExperimentalTransitRoute transitLegRoute = (ExperimentalTransitRoute) leg.getRoute();
								
								Id<TransitRoute> legRouteId = transitLegRoute.getRouteId();
								
								// Need to cast this as TransitRouteImpl since otherwise the method getRoute to get the NetworkRoute would
								// not be available
								List<Id<Link>> linkIdList = transitRouteNetworkLinksMap.get(legRouteId);
								
								boolean considerLink = false;
								
								Id<TransitStopFacility> accessStopId = transitLegRoute.getAccessStopId();
								Coord currentLocation = transitSchedule.getFacilities().get(accessStopId).getCoord();
								
								Id<TransitStopFacility> egressStopId = transitLegRoute.getEgressStopId();
								Coord egressStopLocation = transitSchedule.getFacilities().get(egressStopId).getCoord();
								
								for (Id<Link> linkId : linkIdList) {
									Link link = network.getLinks().get(linkId);
									if (link == null) {
										System.err.println("Link is null!");
									}
									Coord fromNodeCoord = link.getFromNode().getCoord();
									Coord toNodeCoord = link.getToNode().getCoord();
									
									if (linkId == transitLegRoute.getStartLinkId()) {
										considerLink = true;
										continue; // so that start link does NOT get counted
									}
									
									if (linkId == transitLegRoute.getEndLinkId()) {
										considerLink = false;
										
										// Dependent on the link the stop facility is mapped to, there maybe a gap between the LAST considered link and the
										// stop facility. To prevent neglecting this gap, it is considered as beeline distance.
										double egressGap = Math.sqrt( Math.pow(egressStopLocation.getX() - fromNodeCoord.getX(), 2) + 
												Math.pow(egressStopLocation.getY() - fromNodeCoord.getY(), 2) );
										travelDistance = travelDistance + egressGap;
									}
									
									if (considerLink == true) {
										// Dependent on the link the stop facility is mapped to, there maybe a gap between the FIRST considered link and the
										// stop facility. To prevent neglecting this gap, it is considered as beeline distance.
										double accessGap = Math.sqrt( Math.pow(currentLocation.getX() - fromNodeCoord.getX(), 2) + 
												Math.pow(currentLocation.getY() - fromNodeCoord.getY(), 2) );
										double linkLength = link.getLength();
										travelDistance = travelDistance + linkLength + accessGap;
										
										// Set the current location to the end of the link, so that the access gap should be zero for intermediate links.
										currentLocation =  toNodeCoord;
									}
								}
//								log.info("pt = " + legRoute);
//								counterRoutePt++;
								
							} else if (mode == TransportMode.transit_walk) {
								// This (route != null and mode = transit_walk) is the case for intermediate transit walks between stops)
								double beelineWalkSpeed = transitRouterConfig.getBeelineWalkSpeed();
								double transitWalkDistance = beelineWalkSpeed * leg.getTravelTime();
								
//								log.info("intermediate transit walk = " + legRoute);
//								counterRouteWalk++;

								travelDistance = travelDistance + transitWalkDistance;
							} else { // i.e. mode neither pt nor transit_walk
								throw new RuntimeException("No trips with mode other than pt or transit_walk should be pbserved in this setup.");
							}
						}
					}
				}
				
//				// The following procedure does not consider walking. It takes the paths, which go from stop to stop. A link (not a physical one,
//				// but a logical one) connects them. The length of this link is taken. This link length is the beeline distance between the stops
//				// This is an approximation since the distance driven by the vehicle may obviously be longer than the beeline distance between stops.
//				for (Link link : path.links) {
//					System.out.println("link.getLength() = " + link.getLength());
//					travelDistance = travelDistance + link.getLength();
//				}
				
				travelTimeMatrixWriter.writeField(fromLocation);
				travelTimeMatrixWriter.writeField(toLocation);
				travelTimeMatrixWriter.writeField(travelTime);
				travelTimeMatrixWriter.writeNewLine();
				
				travelDistanceMatrixWriter.writeField(fromLocation);
				travelDistanceMatrixWriter.writeField(toLocation);
				travelDistanceMatrixWriter.writeField(travelDistance);
				travelDistanceMatrixWriter.writeNewLine();
				
//				log.info("counterRouteNull = " + counterRouteNull + " -- counterRoutePt = " + counterRoutePt + " -- counterRouteWalk = " + counterRouteWalk);
			}
		}
		travelTimeMatrixWriter.close();
		travelDistanceMatrixWriter.close();
	}
}