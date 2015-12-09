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
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author dziemke
 */
class ThreadedMatrixCreator implements Runnable {
	private static final Logger log = Logger.getLogger(ThreadedMatrixCreator.class);
	
	Thread thread;
	Integer threadName;
	Scenario scenario;
	Map<Id<Coord>, Coord> locationFacilitiesFromMap;
	Map<Id<Coord>, Coord> locationFacilitiesToMap;
	double departureTime;
	String outputRoot;
	String separator;
	
	
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
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
	    
	    final InputsCSVWriter travelTimeMatrixWriter = new InputsCSVWriter(
	    		this.outputRoot + "travelTimeMatrix_" + this.threadName + ".csv", this.separator);
	    final InputsCSVWriter travelDistanceMatrixWriter = new InputsCSVWriter(
	    		this.outputRoot + "travelDistanceMatrix_" + this.threadName + ".csv", this.separator);
	    
		Network network = scenario.getNetwork();
		
		
		// Create a map with all transit routes and a list holding their network links
		// TODO extract this method
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
					//throw new RuntimeException
					log.error("The leg list is null! This should not happen, because -- even if the "
							+ "origin and destination are the same -- there should be one (transit) walk leg with zero "
							+ "travel time. FromLocation = " + fromLocation + " -- ToLocation = " + toLocation);
				
				} else { // i.e. leg list is NOT null
					for(Leg leg : legList) {
						if(leg == null) {
							throw new RuntimeException("Leg is null.");
						}
						travelTime = travelTime + leg.getTravelTime();

						// TODO why is this cast necessary?
						GenericRouteImpl legRoute = (GenericRouteImpl) leg.getRoute();
						
						String mode = leg.getMode();
						if (legRoute == null) {
							if (mode != TransportMode.transit_walk) {
								throw new RuntimeException("The only route that can be null is a route that belongs to a transit walk.");
							} else { // i.e. mode == TransportMode.transit_walk)
								// This (route = null and mode = transit_walk) is the case for network access and egress)
								// Apparently the beelineWalkSpeed is used to calculate the walk time that we have. So, we need to use this
								// beelineWalkSpeed here, too, in order to come from given walk time (bqck) to walk distance
								double beelineWalkSpeed = transitRouterConfig.getBeelineWalkSpeed();
								double transitWalkDistance = beelineWalkSpeed * leg.getTravelTime();
								
//								log.info("access/egress walk = " + legRoute);
//								counterRouteNull++;
								
								travelDistance = travelDistance + transitWalkDistance;
							}			
						} else { // route != null
							if (mode == TransportMode.pt) {
								// Have to cast it to ExperimenalTransitRoute since otherwise the method getRouteId will not be available
								ExperimentalTransitRoute transitRoute = (ExperimentalTransitRoute) leg.getRoute();
								
								Id<TransitRoute> transitRouteId = transitRoute.getRouteId();
								
								// Need to cast this as TransitRouteImpl since otherwise the method getRoute to get the NetworkRoute would
								// not be available
								// TODO where is that cast?
								List<Id<Link>> linkIdList = transitRouteNetworkLinksMap.get(transitRouteId);
								
								boolean considerLink = false;
								
								Id<TransitStopFacility> accessStopId = transitRoute.getAccessStopId();
								Coord currentLocation = transitSchedule.getFacilities().get(accessStopId).getCoord();
								
								Id<TransitStopFacility> egressStopId = transitRoute.getEgressStopId();
								Coord egressStopLocation = transitSchedule.getFacilities().get(egressStopId).getCoord();
								
								for (Id<Link> linkId : linkIdList) {
									Link link = network.getLinks().get(linkId);
									if (link == null) {
										System.err.println("Link is null!");
										// TODO log error?
									}
									Coord fromNodeCoord = link.getFromNode().getCoord();
									Coord toNodeCoord = link.getToNode().getCoord();
									
									if (linkId == transitRoute.getStartLinkId()) {
										considerLink = true;
										continue; // so that start link does NOT get counted
										// TODO why was that?
									}
									
									if (linkId == transitRoute.getEndLinkId()) {
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
								throw new RuntimeException("No trips with mode other than pt or transit_walk should be observed in this setup.");
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