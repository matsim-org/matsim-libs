package playground.santiago.gtfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * Simplifies a given transit schedule by merging its transit routes.
 * 
 * @author dhosse
 *
 *
 */
public class TransitScheduleSimplifierAndreas{
	
//	private final Comparator<TransitRouteStop> arrivalOffsetComparator = new Comparator<TransitRouteStop>() {
//
//		@Override
//		public int compare(TransitRouteStop o1, TransitRouteStop o2) {
//			return Double.compare(o1.getArrivalOffset(), o2.getArrivalOffset());
//		}
//	};
	
	public static TransitSchedule simplifyTransitSchedule(Scenario scenario, String outputDirectory){
		
		return new TransitScheduleSimplifierAndreas().mergeEqualTransitRoutes(scenario, outputDirectory);
		
	}
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with equal route profiles.
	 * The simplified schedule is also written into a new file.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 * @return the simplified transit schedule
	 */
	private TransitSchedule mergeEqualTransitRoutes(final Scenario scenario, String outputDirectory) {

		final Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("equal transit routes within a transit line will be merged...");
		
		final String UNDERLINE = "__";

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		Map<Id<TransitLine>,TransitLine> transitLines = schedule.getTransitLines();
		
		TransitSchedule mergedSchedule = factory.createTransitSchedule();
		
		//add all stop facilities of the originial schedule to the new one
		for(TransitStopFacility stop : schedule.getFacilities().values())
			mergedSchedule.addStopFacility(stop);
			
		int routesCounter = 0;
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
			
			TransitLine transitLine = transitLineIterator.next();
		
			Map<Id<TransitRoute>,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			TransitRoute refTransitRoute = null;
		
			TransitLine mergedTransitLine = factory.createTransitLine(transitLine.getId());
			
			TransitRoute mergedTransitRoute = null;
			
			routesCounter += transitRoutes.size();
		
			//add all transit routes of this transit line to a queue
			PriorityQueue<Id> uncheckedRoutes = new PriorityQueue<Id>();
			uncheckedRoutes.addAll(transitRoutes.keySet());

			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
			
				//make the current transit route the reference for the equality test
				refTransitRoute = transitRoutes.get(uncheckedRoutes.remove());
			
				String id = refTransitRoute.getId().toString();
			
				//check all other transit routes, except for the reference route
				for(Id transitRouteId : transitRoutes.keySet()){
				
					if(!transitRouteId.equals(refTransitRoute.getId())){
			
						TransitRoute transitRoute = transitRoutes.get(transitRouteId);
				
						//if the route profiles are equal, "mark" current transit route by adding it to a string array
						if(routeProfilesEqual(transitRoute, refTransitRoute)){
					
							id += UNDERLINE+transitRoute.getId().toString();
					
							uncheckedRoutes.remove(transitRoute.getId());
					
						}
					}
				
				}
			
				//if the new id equals the old one, there are no routes to be merged...
				if(id.equals(refTransitRoute.getId().toString())){
					mergedTransitLine.addRoute(refTransitRoute);
					mergedRoutesCounter++;
					continue;
				}
			
				//split new id in order to access the original routes
				String[] listOfRoutes = id.split(UNDERLINE);
			
				NetworkRoute newRoute = refTransitRoute.getRoute();//computeNetworkRoute(scenario.getNetwork(), refTransitRoute);
			
				List<TransitRouteStop> newStops = computeNewRouteProfile(factory, refTransitRoute, transitRoutes, listOfRoutes, newRoute, null);
				compareRouteProfiles(refTransitRoute.getStops(), newStops);
				
				mergedTransitRoute = factory.createTransitRoute(Id.create(id, TransitRoute.class),
					newRoute, newStops, TransportMode.pt);
				
				mergeDepartures(factory, transitRoutes, mergedTransitRoute.getStops().get(0), mergedTransitRoute, listOfRoutes);

				//add merged transit route to the transit line
				mergedTransitLine.addRoute(mergedTransitRoute);
				mergedRoutesCounter++;
			
			}
			
			mergedSchedule.addTransitLine(mergedTransitLine);
			
		}
		
		log.info("number of initial transit routes: " + routesCounter);
		String diff = routesCounter > mergedRoutesCounter ? Integer.toString(mergedRoutesCounter - routesCounter) 
				: "+"+Integer.toString(mergedRoutesCounter - routesCounter);
		log.info("number of merged transit routes: " + mergedRoutesCounter + " ( " + diff + " )");
		
		log.info("writing simplified transit schedule to " + outputDirectory);
		
		new TransitScheduleWriter(mergedSchedule).writeFile(outputDirectory);
		
		log.info("... done.");
		
		return mergedSchedule;
		
	}
	
	private void compareRouteProfiles(List<TransitRouteStop> stops,
			List<TransitRouteStop> newStops) {
		
		for(TransitRouteStop stop : stops){
			
			if(stops.indexOf(stop) != newStops.indexOf(stop))
				newStops.set(stops.indexOf(stop), stop);
			
		}
		
	}

	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with touching route profiles.
	 * The initial transit routes are split into sections on which they overlap. A new section is created if the number
	 * of overlapping transit routes changes.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 * @return the simplified transit schedule
	 */
	private TransitSchedule mergeTouchingTransitRoutes(Scenario scenario, String outputDirectory){
		
		final String UNDERLINE = "__";
		
		Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("transit routes within a transit line that overlap at least at one stop facility will be merged...");
		
		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		Map<Id<TransitLine>, TransitLine> transitLines = schedule.getTransitLines();
		
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
		
			TransitLine transitLine = transitLineIterator.next();
			
			Map<Id<TransitRoute>,TransitRoute> transitRoutes = transitLine.getRoutes();
		
			TransitRoute refTransitRoute = null;
		
			TransitRoute mergedTransitRoute;
		
			PriorityQueue<Id> uncheckedRoutes = new PriorityQueue<Id>();
			uncheckedRoutes.addAll(transitRoutes.keySet());
		
			List<TransitRouteStop> stopsEqual = new ArrayList<TransitRouteStop>();
		
			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
			
				stops.clear();
				
				mergedTransitRoute = null;
				//make current transit route the reference route
				refTransitRoute = transitRoutes.get(uncheckedRoutes.remove());
			
				String id = refTransitRoute.getId().toString();
			
				//iterate over all other transit routes
				for(Id transitRouteId : transitRoutes.keySet()){
				
					if(transitRouteId.equals(refTransitRoute.getId()))
						continue;
			
					TransitRoute transitRoute = transitRoutes.get(transitRouteId);
				
					//if the reference route and the current transit route overlap at one point ore more
					//add the current transit route id to an id array
					if((stopsEqual = routeProfilesTouch(transitRoute,refTransitRoute)).size() > 0){
					
						id += UNDERLINE+transitRoute.getId().toString();
						uncheckedRoutes.remove(transitRoute.getId());
					
					}
				
					//add overlaps (stops) for creating new route stops later... 
					for(TransitRouteStop stop : stopsEqual)
						if(!stops.contains(stop))
							stops.add(stop);
				}
			
				if(id.equals(refTransitRoute.getId().toString()))
					continue;
			
				String[] listOfRoutes = id.split(UNDERLINE);
			
				while(stops.size() > 0){

					//create new network routes and afterwards new route profiles and transit routes
					List<NetworkRoute> newRoutes = computeNetworkRoutesByTransitRouteStops(scenario.getNetwork(), transitRoutes, listOfRoutes);
				
					for(NetworkRoute networkRoute : newRoutes){
					
						List<TransitRouteStop> newStops = computeNewRouteProfile(factory, refTransitRoute, transitRoutes, listOfRoutes,networkRoute, stops);
					
						TransitRouteStop start = newStops.get(0);
					
						mergedTransitRoute = factory.createTransitRoute(Id.create("merged_" + mergedRoutesCounter, TransitRoute.class), networkRoute, newStops, TransportMode.pt);
					
						mergedRoutesCounter++;
					
						mergeDepartures(factory, transitRoutes, start,mergedTransitRoute,listOfRoutes);
				
						transitLine.addRoute(mergedTransitRoute);
					
					}
				
				}
			
				//remove transit routes that have been merged from the transit schedule
				for(int i = 0; i < listOfRoutes.length; i++)
					transitLine.removeRoute(transitRoutes.get(Id.create(listOfRoutes[i], TransitRoute.class)));
			
			}
		
		}
		
		log.info("writing simplified transit schedule to " + outputDirectory);
		
		new TransitScheduleWriter(schedule).writeFile(outputDirectory);
		
		log.info("... done.");
		
		return null;
		
	}

	/**
	 * 
	 * This method creates a simplified transit route out of the reference transit route.
	 * The route of the resulting transit route equals the initial route, except that
	 * it starts at the first and ends at the last transit route stop (no depot
	 * tours etc.).
	 * 
	 * @param transitRoute the reference transit route
	 * @return the simplified network route
	 */
	private static NetworkRoute computeNetworkRoute(Network network, TransitRoute transitRoute) {
		
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		double startOffset = Double.MAX_VALUE;
		double endOffset = Double.MIN_VALUE;
		TransitRouteStop start = null;
		TransitRouteStop end = null;
		
		for(TransitRouteStop stop : transitRoute.getStops()){
			if(stop.getArrivalOffset() < startOffset){
				startOffset = stop.getArrivalOffset();
				start = stop;
			}
			if(stop.getArrivalOffset() > endOffset){
				endOffset = stop.getArrivalOffset();
				end = stop;
			}
		}
		
		Id startLinkId = start.getStopFacility().getLinkId();
		Id endLinkId = end.getStopFacility().getLinkId();
		
		routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
		
		for(Id linkId : transitRoute.getRoute().getLinkIds())
			routeLinkIds.add(linkId);
		
		routeLinkIds.add(transitRoute.getRoute().getEndLinkId());
		
		int startIndex = routeLinkIds.indexOf(startLinkId);
		int endIndex = routeLinkIds.indexOf(endLinkId);
		
		for(int i = 0; i < routeLinkIds.size(); i++){
			if(routeLinkIds.indexOf(routeLinkIds.get(i)) < startIndex)
				routeLinkIds.remove(routeLinkIds.get(i));
			if(routeLinkIds.indexOf(routeLinkIds.get(i)) > endIndex)
				routeLinkIds.remove(routeLinkIds.get(i));
		}
		
		
//		//get the start and the end link ids from the first and the last transit route stop
//		Id startLinkId = transitRoute.getStops().get(0).getStopFacility().getLinkId();
//		Id endLinkId = transitRoute.getStops().get(transitRoute.getStops().size()-1).getStopFacility().getLinkId();
//		
//		//if the initial network route doesn't contain the link id of the first stop it is added as first link
//		if(!transitRoute.getRoute().getLinkIds().contains(startLinkId))
//			routeLinkIds.add(startLinkId);
//		//if the initial network route contains the start link id
//		//set start index at the position of the start link id inside the initial network route
//		else{
//			startIndex = transitRoute.getRoute().getLinkIds().indexOf(startLinkId);
//			routeLinkIds.add(transitRoute.getRoute().getLinkIds().get(startIndex));
//			startIndex++;
//		}
//		
//		//add all link ids of the initial network route to the new route as long as the end link is not reached yet
//		for(int i = startIndex; i < transitRoute.getRoute().getLinkIds().size() ; i++){
//			routeLinkIds.add(transitRoute.getRoute().getLinkIds().get(i));
//			if(transitRoute.getRoute().getLinkIds().get(i).equals(endLinkId))
//				break;
//		}
//		
//		//if the new network route doesn't contain the end link so far, add it
//		if(!routeLinkIds.contains(endLinkId))
//			routeLinkIds.add(endLinkId);
		
		return RouteUtils.createNetworkRoute(routeLinkIds, network);
		
	}
	
	/**
	 * Creates a list of new network routes. These routes are parts of the initial
	 * network routes. Every time the number of overlapping transit routes on a link changes,
	 * a new network route is created.
	 * 
	 * @param listOfRoutes the id list of all touching transit routes
	 * @return a list of network routes for the merged transit routes
	 */
	private static List<NetworkRoute> computeNetworkRoutesByTransitRouteStops(Network network, Map<Id<TransitRoute>,TransitRoute> transitRoutes, String[] listOfRoutes) {
		
		List<NetworkRoute> newNetworkRoutes = new ArrayList<NetworkRoute>();
		
		PriorityQueue<Id<TransitRoute>> uncheckedTransitRoutes = new PriorityQueue<Id<TransitRoute>>();
		
		for(int i=0;i<listOfRoutes.length;i++){
			uncheckedTransitRoutes.add(Id.create(listOfRoutes[i], TransitRoute.class));
		}
		
		List<TransitRouteStop> checkedTransitRouteStops = new ArrayList<TransitRouteStop>();
		
		int maxStops = Integer.MIN_VALUE;
		for(TransitRoute transitRoute : transitRoutes.values()){
			
			int size = transitRoute.getStops().size();
			
			if(size > maxStops)
				maxStops = size;
			
		}
		
		int transitRoutesContaining = 0;
		
		TransitRoute currentTransitRoute = null;
		
		//until all transit route stops have been visited...
		while(checkedTransitRouteStops.size() < maxStops){
			
			List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
			
			//check transit route
			currentTransitRoute = transitRoutes.get(uncheckedTransitRoutes.remove());
			
			//counter to store the number of routes containing the LAST stop
			transitRoutesContaining = 1;
			
			//iterate over all transit route stops in the current transit route
			for(TransitRouteStop stop : currentTransitRoute.getStops()){
				
				//if this stop has not been visited yet
				if(!checkedTransitRouteStops.contains(stop)){
				
					//counter to store the number of routes containing the CURRENT stop
					int containing = 1;
					
					//iterate over all OTHER transit routes
					for(TransitRoute transitRoute : transitRoutes.values()){
					
						if(!transitRoute.getId().equals(currentTransitRoute.getId())){

							//if the investigated transit route contains the current stop, increment counter
							if(transitRoute.getStop(stop.getStopFacility()) != null){
								
								containing++;
								
							}
						
						}
					
					}

					//if the number of containing transit routes changes and there are route links inside the new network route
					//split the initial network route. add the current route to the list to be returned and continue with creating
					//another network route
					if(transitRoutesContaining != containing){
						
						if(routeLinkIds.size() < 1){
							transitRoutesContaining = containing;
						}
						else{
							newNetworkRoutes.add(RouteUtils.createNetworkRoute(routeLinkIds, network));
							transitRoutesContaining = containing;
							
							for(int i=0;i<routeLinkIds.size()-1;i++)
								routeLinkIds.remove(i);
							
						}
						
					}
					
					Id nextLinkId = stop.getStopFacility().getLinkId();
					
					//if the last and the current link aren't adjacent, add the intervening links from the initial network route
					if(routeLinkIds.size() > 0){
						
						Id lastLinkId = routeLinkIds.get(routeLinkIds.size()-1);
						
						List<Id<Link>> linkIds = currentTransitRoute.getRoute().getLinkIds();
						
						int lastLinkIndex = linkIds.contains(lastLinkId) ? linkIds.indexOf(lastLinkId)+1 : 0;
						int nextLinkIndex = linkIds.contains(nextLinkId) ? linkIds.indexOf(nextLinkId) : 0;
						
						for(int i = lastLinkIndex; i < nextLinkIndex-1; i++){
							if(!routeLinkIds.contains(linkIds.get(i)))
								routeLinkIds.add(linkIds.get(i));
						}
						
					}
					
					routeLinkIds.add(stop.getStopFacility().getLinkId());
					checkedTransitRouteStops.add(stop);
					
					//if the last stop of the current transit route is reached, create one last network route and add it to the list
					if(currentTransitRoute.getStops().indexOf(stop) >= currentTransitRoute.getStops().size()-1)
						newNetworkRoutes.add(RouteUtils.createNetworkRoute(routeLinkIds, network));
					
				}
				
			}
			
		}
		
		return newNetworkRoutes;
		
	}

	
	/**
	 * 
	 * Creates a new route profile for a simplified transit route.
	 * The arrival and departure offsets of each stop are merged to
	 * get the average travel time to and stop time for all routes at that stop.
	 * 
	 * @param newRoute the new network route
	 * @return merged route profile
	 */
	private List<TransitRouteStop> computeNewRouteProfile(TransitScheduleFactoryImpl factory,
			TransitRoute refTransitRoute, Map<Id<TransitRoute>,TransitRoute> transitRoutes, String[] listOfRoutes,NetworkRoute newRoute,
			List<TransitRouteStop> stops){
		
		List<TransitRouteStop> newStops = new ArrayList<TransitRouteStop>();
		
		for(int i = 0; i < refTransitRoute.getStops().size(); i++){
			
			double arrivalOffset = 0;
			int arrCounter = 0;
			double departureOffset = 0;
			int depCounter = 0;
			
			for(int j = 0; j < listOfRoutes.length; j++){
				
				TransitRouteStop stop = transitRoutes.get(Id.create(listOfRoutes[j], TransitRoute.class)).getStops().get(i);
				arrivalOffset += stop.getArrivalOffset();
				arrCounter++;
				departureOffset += stop.getDepartureOffset();
				depCounter++;
				
			}
			
			TransitRouteStop newStop = factory.createTransitRouteStop(refTransitRoute.getStops().get(i).getStopFacility(), arrivalOffset/arrCounter,
					departureOffset/depCounter);
			
			newStop.setAwaitDepartureTime(refTransitRoute.getStops().get(i).isAwaitDepartureTime());
			
			newStops.add(newStop);
			
		}
		
		return newStops;
		
	}

	/**
	 * Merges the departures of all transit routes that are to be merged.
	 * 
	 * @param startTransitRouteStop the first stop of the new transit route
	 * @param mergedTransitRoute the new transit route
	 */
	private void mergeDepartures(TransitScheduleFactoryImpl factory, Map<Id<TransitRoute>,TransitRoute> transitRoutes, TransitRouteStop startTransitRouteStop,
			TransitRoute mergedTransitRoute,String[] listOfTransitRoutes) {

		for(int i = 0; i < listOfTransitRoutes.length; i++){

			TransitRoute transitRoute = transitRoutes.get(Id.create(listOfTransitRoutes[i], TransitRoute.class));
			
			if(mergedTransitRouteContainsTransitRouteStops(mergedTransitRoute, transitRoute, startTransitRouteStop)){
//			if(transitRoute.getStops().contains(transitRoute.getStop(startTransitRouteStop.getStopFacility()))&&!transitRoute.getId().toString().contains("merged")){
//
//				for(TransitRouteStop stop : mergedTransitRoute.getStops())
//					if(!transitRoute.getStops().contains(transitRoute.getStop(stop.getStopFacility())))
//						continue all;
				
				for(Departure departure : transitRoute.getDepartures().values()){
				
					String departureId = mergedTransitRoute.getDepartures().size() < 10 ?
							"0"+Integer.toString(mergedTransitRoute.getDepartures().size()) :
							Integer.toString(mergedTransitRoute.getDepartures().size());
					
					Departure dep = factory.createDeparture(Id.create(departureId, Departure.class),
							departure.getDepartureTime() + transitRoute.getStop(startTransitRouteStop.getStopFacility()).getDepartureOffset());
					dep.setVehicleId(departure.getVehicleId());
					
					mergedTransitRoute.addDeparture(dep);
					
				}
				
			}
			
		}
	
	}

	/**
	 * Compares the route profiles of two given transit routes for equality.
	 * 
	 * @param transitRoute
	 * @param transitRoute2
	 * @return true if the route profiles are equal, false if not
	 */
	private boolean routeProfilesEqual(TransitRoute transitRoute,
			TransitRoute transitRoute2) {
		
		if(transitRoute.getStops().size() != transitRoute2.getStops().size())
			return false;

		for(int i=0;i<transitRoute.getStops().size();i++){
			if(!(transitRoute.getStops().get(i).getStopFacility().getId().equals(transitRoute2.getStops().get(i).getStopFacility().getId())))
				break;
			if(i == transitRoute.getStops().size()-1)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Checks two given transit routes for overlaps.
	 * 
	 * @param transitRoute
	 * @param refTransitRoute
	 * @return an empty list if the transit routes do not overlap, else the collection of the stops that both transit routes contain
	 */
	private List<TransitRouteStop> routeProfilesTouch(TransitRoute transitRoute,
			TransitRoute refTransitRoute) {
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		
		for(TransitRouteStop stop : refTransitRoute.getStops()){
			if(transitRoute.getStops().contains(stop))
				stops.add(stop);
		}
		
		return stops;
	}
	
	private boolean mergedTransitRouteContainsTransitRouteStops(TransitRoute mergedTransitRoute, TransitRoute transitRoute, TransitRouteStop start){
		
		if(!transitRoute.getStops().contains(transitRoute.getStop(start.getStopFacility()))||transitRoute.getId().toString().contains("merged"))
			return false;
		
		for(TransitRouteStop stop : mergedTransitRoute.getStops()){
			
			if(!transitRoute.getStops().contains(transitRoute.getStop(stop.getStopFacility())))
				return false;
			
		}
		
		return true;
		
	}

}
