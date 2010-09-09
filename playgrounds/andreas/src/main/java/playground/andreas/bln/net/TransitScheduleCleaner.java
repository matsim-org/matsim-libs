package playground.andreas.bln.net;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class TransitScheduleCleaner {
	
	private static final Logger log = Logger.getLogger(TransitScheduleCleaner.class);
	
	public static TransitSchedule removeEmptyLines(TransitSchedule transitSchedule){
		
		log.info("Removing empty lines");
		
		printStatistic(transitSchedule);
		
		List<Id> linesToRemove = new LinkedList<Id>();
		
		for (Id lineId : transitSchedule.getTransitLines().keySet()) {
			if(transitSchedule.getTransitLines().get(lineId).getRoutes().size() == 0){
				linesToRemove.add(lineId);
			}
		}
		
		StringBuffer sB = new StringBuffer();
		
		for (Id lineId : linesToRemove) {
			transitSchedule.getTransitLines().remove(lineId);
			sB.append(lineId + ", ");
		}
		
		printStatistic(transitSchedule);
		log.info("Removed " + linesToRemove.size() + " lines from transitSchedule: " + sB.toString());	
		
		return transitSchedule;
	}
	
	public static TransitSchedule removeStopsNotUsed(TransitSchedule transitSchedule){
		
		log.info("Removing stops not used");
		printStatistic(transitSchedule);
		
		Set<Id> stopsInUse = new TreeSet<Id>();
		Set<Id> stopsToBeRemoved = new TreeSet<Id>();
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (TransitRouteStop stop : transitRoute.getStops()) {
					stopsInUse.add(stop.getStopFacility().getId());
				}
			}
		}
		
		for (TransitStopFacility transitStopFacility : transitSchedule.getFacilities().values()) {
			if(!stopsInUse.contains(transitStopFacility.getId())){
				stopsToBeRemoved.add(transitStopFacility.getId());
			}
		}
		
		StringBuffer sB = new StringBuffer();
		
		for (Id transitStopFacilityId : stopsToBeRemoved) {
			transitSchedule.getFacilities().remove(transitStopFacilityId);
			sB.append(transitStopFacilityId.toString() + ", ");
		}
		
		printStatistic(transitSchedule);
		log.info("Removed " + stopsToBeRemoved.size() + " stops from transitSchedule: " + sB.toString());	
		
		return transitSchedule;
	}
	
	
	private static void printStatistic(TransitSchedule transitSchedule){
		log.info("Transit schedule with " + transitSchedule.getTransitLines().size() + " lines and " + transitSchedule.getFacilities().size() + " stops.");
	}
	
	/**
	 * Adding TransportMode.pt flags to all links referred by transit schedule
	 * @param transitSchedule The transit schedule.
	 * @param network The network to be tagged.
	 * @return
	 */
	public static Network tagTransitLinksInNetwork(TransitSchedule transitSchedule, Network network){
		
		log.info("Tagging pt network links");
		
		for (TransitStopFacility stopFacitlity : transitSchedule.getFacilities().values()) {
			network.getLinks().get(stopFacitlity.getLinkId()).getAllowedModes().add(TransportMode.pt);
		}
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				NetworkRoute route = transitRoute.getRoute();
				
				Set<String> allowedModes;
				
				allowedModes = network.getLinks().get(route.getStartLinkId()).getAllowedModes();
				allowedModes.add(TransportMode.pt);				
				network.getLinks().get(route.getStartLinkId()).setAllowedModes(allowedModes);
				
				for (Id linkId : route.getLinkIds()) {
					allowedModes = network.getLinks().get(linkId).getAllowedModes();
					allowedModes.add(TransportMode.pt);
					network.getLinks().get(linkId).setAllowedModes(allowedModes);
				}
				
				allowedModes = network.getLinks().get(route.getEndLinkId()).getAllowedModes();
				allowedModes.add(TransportMode.pt);				
				network.getLinks().get(route.getEndLinkId()).setAllowedModes(allowedModes);
			}
		}
		
		int taggedLinks = 0;
		for (Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.pt)){
				taggedLinks++;
			}
		}
		
		log.info("Finished - " + taggedLinks + " links were tagged");
		
		return network;
	}
	
	/**
	 * Removes all TransportMode.pt flags from the network
	 * @param network The network to be processed.
	 * @return
	 */
	public static Network removeAllPtTagsFromNetwork(Network network){		
		log.info("Untagging pt network links");	
		int removedTags = 0;
		for (Link link : network.getLinks().values()) {
			Set<String> allowedModes = link.getAllowedModes();
			if(allowedModes.remove(TransportMode.pt)){
				removedTags++;
			}
			link.setAllowedModes(allowedModes);
		}
		log.info("Finished - Removed " + removedTags + " tags from links.");
		return network;
	}

	public static TransitSchedule removeAllRoutesWithMissingLinksFromSchedule(TransitSchedule transitSchedule, Network network){
		log.info("Removing stops and routes with missing links from the schedule");
		printStatistic(transitSchedule);
		int removedRoutes = 0;
		
		// Remove routes with missing links
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			
			TreeSet<TransitRoute> transitRouteToBeRemoved = new TreeSet<TransitRoute>();
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				
				// Remove Route, when links are missing in the network
				if(network.getLinks().get(transitRoute.getRoute().getStartLinkId()) == null){
					transitRouteToBeRemoved.add(transitRoute);
					continue;					
				}
				
				for (Id linkId : transitRoute.getRoute().getLinkIds()) {
					if(network.getLinks().get(linkId) == null){
						transitRouteToBeRemoved.add(transitRoute);
						break;
					}
				}
								
				if(network.getLinks().get(transitRoute.getRoute().getEndLinkId()) == null){
					transitRouteToBeRemoved.add(transitRoute);
					continue;					
				}
				
				// Remove route, if one of its stops, has a missing link
				for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					if(network.getLinks().get(transitRouteStop.getStopFacility().getLinkId()) == null){
						transitRouteToBeRemoved.add(transitRoute);
						break;
					}
				}
			}			
			
			for (TransitRoute transitRoute : transitRouteToBeRemoved) {
				if(transitLine.removeRoute(transitRoute) == true){
					removedRoutes++;
				}
			}			
		}
		
		log.info("Removed " + removedRoutes + " routes due to missing links or stops");
		printStatistic(transitSchedule);
				
		return transitSchedule;
	}
	
	
}
