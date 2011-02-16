package playground.andreas.utils.pt;

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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.osmBB.extended.TransitScheduleImpl;

public class TransitScheduleCleaner {
	
	private static final Logger log = Logger.getLogger(TransitScheduleCleaner.class);
	
	public static TransitSchedule removeEmptyLines(TransitSchedule transitSchedule){
		
		log.info("Removing empty lines");		
		TransitSchedule tS = TransitScheduleCleaner.makeTransitScheduleModifiable(transitSchedule);
		printStatistic(tS);
		
		List<Id> linesToRemove = new LinkedList<Id>();
		
		for (Id lineId : tS.getTransitLines().keySet()) {
			if(tS.getTransitLines().get(lineId).getRoutes().size() == 0){
				linesToRemove.add(lineId);
			}
		}
		
		StringBuffer sB = new StringBuffer();
		
		for (Id lineId : linesToRemove) {
			tS.getTransitLines().remove(lineId);
			sB.append(lineId + ", ");
		}
		
		printStatistic(tS);
		log.info("Removed " + linesToRemove.size() + " lines from transitSchedule: " + sB.toString());	
		
		return tS;
	}
	
	public static TransitSchedule removeStopsNotUsed(TransitSchedule transitSchedule){
		
		log.info("Removing stops not used");
		TransitSchedule tS = TransitScheduleCleaner.makeTransitScheduleModifiable(transitSchedule);
		printStatistic(tS);
		
		Set<Id> stopsInUse = new TreeSet<Id>();
		Set<Id> stopsToBeRemoved = new TreeSet<Id>();
		
		for (TransitLine transitLine : tS.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (TransitRouteStop stop : transitRoute.getStops()) {
					stopsInUse.add(stop.getStopFacility().getId());
				}
			}
		}
		
		for (TransitStopFacility transitStopFacility : tS.getFacilities().values()) {
			if(!stopsInUse.contains(transitStopFacility.getId())){
				stopsToBeRemoved.add(transitStopFacility.getId());
			}
		}
		
		StringBuffer sB = new StringBuffer();
		
		for (Id transitStopFacilityId : stopsToBeRemoved) {
			tS.getFacilities().remove(transitStopFacilityId);
			sB.append(transitStopFacilityId.toString() + ", ");
		}
		
		printStatistic(tS);
		log.info("Removed " + stopsToBeRemoved.size() + " stops from transitSchedule: " + sB.toString());	
		
		return tS;
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
		
		if(transitSchedule == null){
			log.info("No transit schedule given. Returning unmodified network...");
			return network;
		}
		
		for (TransitStopFacility stopFacitlity : transitSchedule.getFacilities().values()) {
			Set<String> allowedModes = new TreeSet<String>(network.getLinks().get(stopFacitlity.getLinkId()).getAllowedModes());
			allowedModes.add(TransportMode.pt);
			network.getLinks().get(stopFacitlity.getLinkId()).setAllowedModes(allowedModes);
		}
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				NetworkRoute route = transitRoute.getRoute();
				
				Set<String> allowedModes;
				
				allowedModes = new TreeSet<String>(network.getLinks().get(route.getStartLinkId()).getAllowedModes());
				allowedModes.add(TransportMode.pt);				
				network.getLinks().get(route.getStartLinkId()).setAllowedModes(allowedModes);
				
				for (Id linkId : route.getLinkIds()) {
					allowedModes = new TreeSet<String>(network.getLinks().get(linkId).getAllowedModes());
					allowedModes.add(TransportMode.pt);
					network.getLinks().get(linkId).setAllowedModes(allowedModes);
				}
				
				allowedModes = new TreeSet<String>(network.getLinks().get(route.getEndLinkId()).getAllowedModes());
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
			Set<String> allowedModes = new TreeSet<String>(link.getAllowedModes());
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
		TransitSchedule tS = TransitScheduleCleaner.makeTransitScheduleModifiable(transitSchedule);
		printStatistic(tS);
		int removedRoutes = 0;
		
		// Remove routes with missing links
		for (TransitLine transitLine : tS.getTransitLines().values()) {
			
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
		printStatistic(tS);
				
		return tS;
	}
	
	private static TransitSchedule makeTransitScheduleModifiable(TransitSchedule transitSchedule){
		TransitSchedule tS = new TransitScheduleImpl(transitSchedule.getFactory());
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			tS.addStopFacility(stop);			
		}
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			tS.addTransitLine(line);
		}
		
		return tS;
	}
}
