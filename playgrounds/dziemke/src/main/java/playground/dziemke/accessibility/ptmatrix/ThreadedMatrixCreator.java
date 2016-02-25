package playground.dziemke.accessibility.ptmatrix;

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
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting.TransitRouter;
import playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting.TransitRouterConfig;
import playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting.TransitRouterImpl;
//import org.matsim.pt.router.TransitRouter;
//import org.matsim.pt.router.TransitRouterConfig;
//import org.matsim.pt.router.TransitRouterImpl;

/**
 * @author dziemke, gthunig
 */
class ThreadedMatrixCreator implements Runnable {
	private static final Logger log = Logger.getLogger(ThreadedMatrixCreator.class);

	private Thread thread;
	private Integer threadName;
	private Scenario scenario;
	private Map<Id<Coord>, Coord> locationFacilitiesFromMap;
	private Map<Id<Coord>, Coord> locationFacilitiesToMap;
	private double departureTime;
	private String outputRoot;
	private String separator;
	
	
	ThreadedMatrixCreator(Scenario scenario, Map<Id<Coord>, Coord> locationFacilitiesFromMap,
			Map<Id<Coord>, Coord> locationFacilitiesToMap,
			double departureTime, String outputRoot, String separator, int threadName){
		this.scenario = scenario;
		this.locationFacilitiesFromMap = locationFacilitiesFromMap;
		this.locationFacilitiesToMap = locationFacilitiesToMap;
		this.departureTime = departureTime;
		this.outputRoot = outputRoot;
		this.separator = separator;
		this.threadName = threadName;

		thread = new Thread (this, this.threadName.toString());
		thread.start ();
	}


	public void run() {
		TransitSchedule transitSchedule = this.scenario.getTransitSchedule();
		
		// constructor of TransitRouterImpl needs TransitRouterConfig. This is why it is instantiated here.
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		
		// TODO check if it is worth setting the following paramters to something other than their defaults
//		transitRouterConfig.setBeelineWalkConnectionDistance(beelineWalkConnectionDistance);
//		transitRouterConfig.setBeelineWalkSpeed(beelineWalkSpeed);
//		transitRouterConfig.setExtensionRadius(extensionRadius);
//		transitRouterConfig.setSearchRadius(200);
		
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
	    
	    final CSVFileWriter travelTimeMatrixWriter = new CSVFileWriter(
	    		this.outputRoot + "travelTimeMatrix_" + this.threadName + ".csv", this.separator);
	    final CSVFileWriter travelDistanceMatrixWriter = new CSVFileWriter(
	    		this.outputRoot + "travelDistanceMatrix_" + this.threadName + ".csv", this.separator);
	    
		Network network = scenario.getNetwork();
		
		double beelineDistanceFactor = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
		
		// create a map with all transit routes and a list for each route holding their network links
		Map<Id<TransitRoute>, List<Id<Link>>> transitRouteNetworkLinksMap = createRoutesNetworkLinksMap(
				transitSchedule);
		
		// initialize counters for statistics
		int counterLegListNull = 0;
		int counterLegListNotNull = 0;
		int counterMoreThan3TransitLegs = 0;
		int counterNoTransitLeg = 0;

		
		// actual computation for each OD relation
		for (Id<Coord> fromLocation : locationFacilitiesFromMap.keySet()) {
			for (Id<Coord> toLocation : locationFacilitiesToMap.keySet()) {
				int counterTransitLegs = 0;				
				
				Coord fromCoord = locationFacilitiesFromMap.get(fromLocation);
				Coord toCoord = locationFacilitiesToMap.get(toLocation);
				
				// legList = list of legs sequentially travelled on a OD relation, e.g. transit_walk ...  pt ... transit_walk
				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, departureTime, null);
				
				double travelTime = 0.;
				double travelDistance = 0.;
				
				if (legList == null) {
					counterLegListNull++;
					
					// if origin == destination, there is one (transit) walk leg with zero travel time, which is
					// correctly picked up below
					log.warn("Leg list is null! Origin = " + fromLocation + " -- Destination = " + toLocation
							+ " Setting time and distance to infinity.");
					// TODO check if a very high value is better than positive infinity
					travelTime = Float.MAX_VALUE;
					travelDistance = Float.MAX_VALUE;
				
				} else { // i.e. leg list is not null
					counterLegListNotNull++;
					
					for(Leg leg : legList) {
						if(leg == null) {
							throw new RuntimeException("Leg is null.");
						}
						travelTime = travelTime + leg.getTravelTime();
						Route legRoute = leg.getRoute();
						String mode = leg.getMode();
						
						if (legRoute == null) {
							if (mode != TransportMode.transit_walk) {
								throw new RuntimeException("The only route that can be null is a \"route\" that belongs to a transit walk.");
							} else { // i.e. mode == TransportMode.transit_walk; this is the case for network access and egress
								
								// the beelineWalkSpeed is used to calculate the (non-beeline or "real") walk time which is returned by leg.getTravelTime()
								double beelineWalkSpeed = transitRouterConfig.getBeelineWalkSpeed();
								
								// beelineWalkSpeed * beelineDistanceFactor = (non-beeline or "real") walk speed
								double transitWalkDistance = beelineWalkSpeed * beelineDistanceFactor * leg.getTravelTime();
								
								travelDistance = travelDistance + transitWalkDistance;
							}			
						} else { // i.e. route != null
							if (mode == TransportMode.pt) {
								counterTransitLegs++;
								
								// have to cast the route to ExperimenalTransitRoute since otherwise the method getRouteId will not be available
								ExperimentalTransitRoute transitRoute = (ExperimentalTransitRoute) leg.getRoute();
								
								Id<TransitRoute> transitRouteId = transitRoute.getRouteId();
								
								List<Id<Link>> linkIdList = transitRouteNetworkLinksMap.get(transitRouteId);
								
								
								
								Id<TransitStopFacility> accessStopId = transitRoute.getAccessStopId();
								Coord currentLocation = transitSchedule.getFacilities().get(accessStopId).getCoord();
								
								Id<TransitStopFacility> egressStopId = transitRoute.getEgressStopId();
								Coord egressStopLocation = transitSchedule.getFacilities().get(egressStopId).getCoord();
								
								// create a marker that is false until the start link is reached
								boolean considerLink = false;
								
								for (Id<Link> linkId : linkIdList) {
									
									Link link = network.getLinks().get(linkId);
									if (link == null) {
										throw new RuntimeException("Link is null!");
									}
									Coord fromNodeCoord = link.getFromNode().getCoord();
									Coord toNodeCoord = link.getToNode().getCoord();
									
									if (linkId == transitRoute.getStartLinkId()) {
										considerLink = true;
										continue; // the start link itself is not counted because it may not be part
										// of the route in its full extend. Instead, the beeline distance from the
										// access stop to the from node of the next link is added, see below
									}
									
									if (linkId == transitRoute.getEndLinkId()) {
										considerLink = false; // the end link itself is not counted anymore because
										// it may not be part of the route in its full extend. Instead, the beeline
										// distance from its from node to the egress stop is added
										double egressGap = Math.sqrt( Math.pow(egressStopLocation.getX() - fromNodeCoord.getX(), 2) + 
												Math.pow(egressStopLocation.getY() - fromNodeCoord.getY(), 2) );
										travelDistance = travelDistance + beelineDistanceFactor * egressGap;
									}
									
									if (considerLink == true) {
										// accessGap = beeline distance b/w currentLocation=accessStopCoord and fromNode
										// of the next link
										double accessGap = Math.sqrt(Math.pow(currentLocation.getX() - fromNodeCoord.getX(), 2) + 
												Math.pow(currentLocation.getY() - fromNodeCoord.getY(), 2));
										
										// linkLength added for all links except start- and endLink
										double linkLength = link.getLength();
										travelDistance = travelDistance + linkLength + beelineDistanceFactor * accessGap;
										
										// set current location to the end of the current link. This makes the access gap
										// zero for intermediate links.
										currentLocation =  toNodeCoord;
									}
								}
								
							} else if (mode == TransportMode.transit_walk) {
								// This (route != null and mode = transit_walk) is the case for intermediate transit walks between stops)
								double beelineWalkSpeed = transitRouterConfig.getBeelineWalkSpeed();
								double transitWalkDistance = beelineWalkSpeed * beelineDistanceFactor * leg.getTravelTime();

								travelDistance = travelDistance + transitWalkDistance;
							} else { // i.e. mode is neither pt nor transit_walk
								throw new RuntimeException("No trips with mode other than pt or transit_walk should be observed in this setup.");
							}
						}
					}
				}
				
				travelTimeMatrixWriter.writeField(fromLocation);
				travelTimeMatrixWriter.writeField(toLocation);
				travelTimeMatrixWriter.writeField(travelTime);
				travelTimeMatrixWriter.writeNewLine();
				
				travelDistanceMatrixWriter.writeField(fromLocation);
				travelDistanceMatrixWriter.writeField(toLocation);
				travelDistanceMatrixWriter.writeField(travelDistance);
				travelDistanceMatrixWriter.writeNewLine();
				
				if (counterTransitLegs >= 3) {
					counterMoreThan3TransitLegs++;
				}
				
				if (counterTransitLegs == 0) {
					counterNoTransitLeg++;
				}
			}
		}
		
		travelTimeMatrixWriter.close();
		travelDistanceMatrixWriter.close();
		
		log.info("Thread = " + threadName + " -- counterLegListNull = " + counterLegListNull);
		log.info("Thread = " + threadName + " -- counterLegListNotNull = " + counterLegListNotNull);
		log.info("Thread = " + threadName + " -- counterMoreThan3TransitLegs = " + counterMoreThan3TransitLegs);
		log.info("Thread = " + threadName + " -- counterNoTransitLeg = " + counterNoTransitLeg);
		
		log.info("Finishing thread = " + threadName);
	}


	/**
	 * Creates a map with all transit routes and a list holding the network links belonging to each route
	 */
	private Map<Id<TransitRoute>, List<Id<Link>>> createRoutesNetworkLinksMap(TransitSchedule transitSchedule) {
		log.info("Start generating transitRouteNetworkLinksMap -- thread = " + threadName);
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
		log.info("Finish generating transitRouteNetworkLinksMap -- thread = " + threadName);
		return transitRouteNetworkLinksMap;
	}

	public Thread getThread() {
		return thread;
	}
}