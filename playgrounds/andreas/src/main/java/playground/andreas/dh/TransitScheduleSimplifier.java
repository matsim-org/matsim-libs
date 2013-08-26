package playground.andreas.dh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * Simplifies a given transit schedule by merging transit routes.
 * 
 * @author dhosse
 *
 *
 */
public class TransitScheduleSimplifier{
	
	private static Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
	
	private static final String UNDERLINE = "__";
	
	private static TransitSchedule schedule = null;
	
	private static Map<Id,TransitRoute> transitRoutes = null;
	
	private static Network network = null;
	
	private static TransitScheduleFactory factory = null;
	
	private static List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
	
	private static int mergedRoutesCounter = 0;
	private static int routesCounter = 0;
	
	private static boolean isMergeTouching = false;
	
	private static Comparator<TransitRouteStop> arrivalOffsetComparator = new Comparator<TransitRouteStop>() {

		@Override
		public int compare(TransitRouteStop o1, TransitRouteStop o2) {
			return Double.compare(o1.getArrivalOffset(), o2.getArrivalOffset());
		}
	};
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with equal route profiles.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 */
	public static void mergeEqualTransitRoutes(Scenario scenario, String outputDirectory) {
		
		log.info("starting simplify method for given transit schedule...");
		log.info("equal transit routes within a transit line will be merged...");

		schedule = scenario.getTransitSchedule();
		network = scenario.getNetwork();
		
		Map<Id,TransitLine> transitLines = schedule.getTransitLines();
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
			
			TransitLine transitLine = transitLineIterator.next();
		
			transitRoutes = transitLine.getRoutes();
			
			routesCounter += transitRoutes.size();
			
			TransitRoute refTransitRoute = null;
		
			TransitRoute mergedTransitRoute;
		
			//add all transit routes of this transit line to a queue
			PriorityQueue<Id> uncheckedRoutes = new PriorityQueue<Id>();
			uncheckedRoutes.addAll(transitRoutes.keySet());

			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
			
				mergedTransitRoute = null;
				//make the current transit route the reference for the equal method
				refTransitRoute = transitRoutes.get(uncheckedRoutes.remove());
			
				String id = refTransitRoute.getId().toString();
			
				//check all other transit routes, except for the reference route
				for(Id transitRouteId : transitRoutes.keySet()){
				
					if(transitRouteId.equals(refTransitRoute.getId()))
						continue;
			
					TransitRoute transitRoute = transitRoutes.get(transitRouteId);
				
					//if the route profiles are equal, "mark" current transit route by adding it to an id array
					if(routeProfilesEqual(transitRoute,refTransitRoute)){
					
						id += UNDERLINE+transitRoute.getId().toString();
					
						uncheckedRoutes.remove(transitRoute.getId());
					
					}
				
				}
			
				//if the new id equals the old one, there are no routes to be merged...
				if(id.equals(refTransitRoute.getId().toString())){
					mergedRoutesCounter++;
					continue;
				}
			
				if(factory == null)
					factory = new TransitScheduleFactoryImpl();

				//split new id in order to access the original routes
				String[] listOfRoutes = id.split(UNDERLINE);
			
				NetworkRoute newRoute = computeNetworkRoute(refTransitRoute);
			
				List<TransitRouteStop> newStops = computeNewRouteProfile(listOfRoutes,newRoute);
				
				mergedTransitRoute = factory.createTransitRoute(new IdImpl(id),
					newRoute, newStops, TransportMode.pt);
			
				mergeDepartures(mergedTransitRoute.getStops().get(0), mergedTransitRoute,listOfRoutes);

				//add merged transit route to the transit line
				transitLine.addRoute(mergedTransitRoute);
				mergedRoutesCounter++;
			
				for(int i=0;i<listOfRoutes.length;i++){
				
					//remove transitRoutes that have been merged from the transit schedule
					transitLine.removeRoute(transitLine.getRoutes().get(new IdImpl(listOfRoutes[i])));
				
				}
			
			}
		
		}
		
		log.info("number of initial transit routes: " + routesCounter);
		String diff = routesCounter > mergedRoutesCounter ? Integer.toString(mergedRoutesCounter - routesCounter) 
				: "+"+Integer.toString(mergedRoutesCounter - routesCounter);
		log.info("number of merged transit routes: " + mergedRoutesCounter + " ( " + diff + " )");
		
		log.info("writing simplified transit schedule to " + outputDirectory);
		
		new TransitScheduleWriter(schedule).writeFile(outputDirectory);
		
		log.info("... done.");
		
	}
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with touching route profiles.
	 * The initial transit routes are split into sections on which they overlap. A new section is created if the number
	 * of overlapping transit routes changes.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 */
	public static void mergeTouchingTransitRoutes(Scenario scenario, String outputDirectory){
		
		log.info("starting simplify method for given transit schedule...");
		log.info("transit routes within a transit line that overlap at least at one stop facility will be merged...");
		
		isMergeTouching = true;
		
		schedule = scenario.getTransitSchedule();
		network = scenario.getNetwork();
		
		Map<Id,TransitLine> transitLines = schedule.getTransitLines();
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
		
			TransitLine transitLine = transitLineIterator.next();
			
			transitRoutes = transitLine.getRoutes();
		
			TransitRoute refTransitRoute = null;
		
			TransitRoute mergedTransitRoute;
		
			PriorityQueue<Id> uncheckedRoutes = new PriorityQueue<Id>();
			uncheckedRoutes.addAll(transitRoutes.keySet());
		
			List<TransitRouteStop> stopsEqual;
		
			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
			
				stops.clear();
				stopsEqual = new ArrayList<TransitRouteStop>();
			
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
			
				if(factory == null)
					factory = new TransitScheduleFactoryImpl();
			
				while(stops.size() > 0){

					//create new network routes and afterwards new route profiles and transit routes
					List<NetworkRoute> newRoutes = computeNetworkRoutesByTransitRouteStops(listOfRoutes);
				
					for(NetworkRoute networkRoute : newRoutes){
					
						List<TransitRouteStop> newStops = computeNewRouteProfile(listOfRoutes,networkRoute);
					
						TransitRouteStop start = newStops.get(0);
					
						mergedTransitRoute = factory.createTransitRoute(new IdImpl("merged_" + mergedRoutesCounter), networkRoute, newStops, TransportMode.pt);
					
						mergedRoutesCounter++;
					
						mergeDepartures(start,mergedTransitRoute,listOfRoutes);
				
						transitLine.addRoute(mergedTransitRoute);
					
					}
				
				}
			
				//remove transit routes that have been merged from the transit schedule
				for(int i = 0; i < listOfRoutes.length; i++)
					transitLine.removeRoute(transitRoutes.get(new IdImpl(listOfRoutes[i])));
			
			}
		
		}
		
		log.info("writing simplified transit schedule to " + outputDirectory);
		
		new TransitScheduleWriter(schedule).writeFile(outputDirectory);
		
		log.info("... done.");
		
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
	private static NetworkRoute computeNetworkRoute(TransitRoute transitRoute) {
		
		List<Id> routeLinkIds = new ArrayList<Id>();
		int startIndex = 0;
		
		//get the start and the end link ids from the first and the last transit route stop
		Id startLinkId = transitRoute.getStops().get(0).getStopFacility().getLinkId();
		Id endLinkId = transitRoute.getStops().get(transitRoute.getStops().size()-1).getStopFacility().getLinkId();
		
		//if the initial network route doesn't contain the link id of the first stop it is added as first link
		if(!transitRoute.getRoute().getLinkIds().contains(startLinkId))
			routeLinkIds.add(startLinkId);
		//if the initial network route contains the start link id
		//set start index at the position of the start link id inside the initial network route
		else{
			startIndex = transitRoute.getRoute().getLinkIds().indexOf(startLinkId);
			routeLinkIds.add(transitRoute.getRoute().getLinkIds().get(startIndex));
			startIndex++;
		}
		
		//add all link ids of the initial network route to the new route as long as the end link is not reached yet
		for(int i = startIndex; i < transitRoute.getRoute().getLinkIds().size() ; i++){
			routeLinkIds.add(transitRoute.getRoute().getLinkIds().get(i));
			if(transitRoute.getRoute().getLinkIds().get(i).equals(endLinkId))
				break;
		}
		
		//if the new network route doesn't contain the end link so far, add it
		if(!routeLinkIds.contains(endLinkId))
			routeLinkIds.add(endLinkId);
		
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
	private static List<NetworkRoute> computeNetworkRoutesByTransitRouteStops(String[] listOfRoutes) {
		
		List<NetworkRoute> newNetworkRoutes = new ArrayList<NetworkRoute>();
		
		PriorityQueue<Id> uncheckedTransitRoutes = new PriorityQueue<Id>();
		
		for(int i=0;i<listOfRoutes.length;i++){
			uncheckedTransitRoutes.add(new IdImpl(listOfRoutes[i]));
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
			
			List<Id> routeLinkIds = new ArrayList<Id>();
			
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
							newNetworkRoutes.add(RouteUtils.createNetworkRoute(routeLinkIds, network));//TODO check: config: umsteigezeit = 0!, taucht route in dijkstra auf = ja!!??
							transitRoutesContaining = containing;
							
							for(int i=0;i<routeLinkIds.size()-1;i++)
								routeLinkIds.remove(i);
							
						}
						
					}
					
					Id nextLinkId = stop.getStopFacility().getLinkId();
					
					//if the last and the current link aren't adjacent, add the intervening links from the initial network route
					if(routeLinkIds.size() > 0){
						
						Id lastLinkId = routeLinkIds.get(routeLinkIds.size()-1);
						
						List<Id> linkIds = currentTransitRoute.getRoute().getLinkIds();
						
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


//	private static List<TransitRouteStop> computeNewTransitRouteStops(String[] listOfRoutes, Map<Id, TransitRoute> transitRoutes) {
//		
//		Map<TransitStopFacility,Double> arrivalOffsets = new HashMap<TransitStopFacility,Double>();
//		Map<TransitStopFacility,Double> departureOffsets = new HashMap<TransitStopFacility,Double>();
//		
//		Map<TransitStopFacility,Integer> arrivalCounter = new HashMap<TransitStopFacility,Integer>();
//		Map<TransitStopFacility,Integer> departureCounter = new HashMap<TransitStopFacility,Integer>();
//		
//		List<TransitRouteStop> newStops = new ArrayList<TransitRouteStop>();
//		
//		for(int i = 0; i< listOfRoutes.length; i++){
//			
//			TransitRoute transitRoute = transitRoutes.get(new IdImpl(listOfRoutes[i]));
//			
//			for(TransitRouteStop stop : transitRoute.getStops()){
//				
//				if(!arrivalOffsets.containsKey(stop.getStopFacility())){
//					arrivalOffsets.put(stop.getStopFacility(), stop.getArrivalOffset());
//					arrivalCounter.put(stop.getStopFacility(), 1);
//				} else{
//					
//					double arrivalOffset = ( arrivalOffsets.get(stop.getStopFacility()) * arrivalCounter.get(stop.getStopFacility()) +
//							stop.getArrivalOffset() ) / ( arrivalCounter.get(stop.getStopFacility()) + 1 );
//					arrivalOffsets.put(stop.getStopFacility(), arrivalOffset);
//					arrivalCounter.put(stop.getStopFacility(), arrivalCounter.get(stop.getStopFacility())+1);
//					
//				}
//				
//				if(!departureOffsets.containsKey(stop.getStopFacility())){
//					departureOffsets.put(stop.getStopFacility(), stop.getDepartureOffset());
//					departureCounter.put(stop.getStopFacility(), 1);
//				} else{
//					
//					double departureOffset = ( departureOffsets.get(stop.getStopFacility()) * departureCounter.get(stop.getStopFacility()) +
//							stop.getDepartureOffset() ) / ( departureCounter.get(stop.getStopFacility()) + 1 );
//					departureOffsets.put(stop.getStopFacility(), departureOffset);
//					departureCounter.put(stop.getStopFacility(), departureCounter.get(stop.getStopFacility())+1);
//
//				}
//				
//			}
//			
//		}
//		
//		for( TransitStopFacility stop : arrivalOffsets.keySet() ){
//			TransitRouteStop newStop = factory.createTransitRouteStop(stop, arrivalOffsets.get(stop), departureOffsets.get(stop));
//			newStop.setAwaitDepartureTime(true);
//			newStops.add(newStop);
//		}
//		
//		Collections.sort(newStops,transitRouteStopComparator);
//		
//		return newStops;
//		
//	}

//	private static void mergeDepartures(String[] listOfRoutes, TransitRoute mergedTransitRoute, Map<Id,TransitRoute> transitRoutes) {
//		
//		for(int i = 0; i < listOfRoutes.length; i++){
//			
//			TransitRoute transitRoute = transitRoutes.get(new IdImpl(listOfRoutes[i]));
//			
//			for(Departure departure : transitRoute.getDepartures().values()){
//				
//				String departureId = (String) (mergedTransitRoute.getDepartures().size() < 10 ?
//						"0"+Integer.toString(mergedTransitRoute.getDepartures().size()) :
//							Integer.toString(mergedTransitRoute.getDepartures().size()));
//				Departure dep = factory.createDeparture(new IdImpl(departureId), departure.getDepartureTime());
//				dep.setVehicleId(departure.getVehicleId());
//				
//				mergedTransitRoute.addDeparture(dep);
//				
//			}
//			
//		}
//		
//	}
	
	/**
	 * Merges the departures of all transit routes that are to be merged.
	 * 
	 * @param startTransitRouteStop the first stop of the new transit route
	 * @param mergedTransitRoute the new transit route
	 */
	private static void mergeDepartures(TransitRouteStop startTransitRouteStop,
			TransitRoute mergedTransitRoute,String[] listOfTransitRoutes) {

		all:for(int i = 0; i < listOfTransitRoutes.length; i++){

			TransitRoute transitRoute = transitRoutes.get(new IdImpl(listOfTransitRoutes[i]));
			
			if(transitRoute.getStops().contains(transitRoute.getStop(startTransitRouteStop.getStopFacility()))&&!transitRoute.getId().toString().contains("merged")){

				for(TransitRouteStop stop : mergedTransitRoute.getStops())
					if(!transitRoute.getStops().contains(transitRoute.getStop(stop.getStopFacility())))
						continue all;
				
				for(Departure departure : transitRoute.getDepartures().values()){
				
					String departureId = (String) (mergedTransitRoute.getDepartures().size() < 10 ?
							"0"+Integer.toString(mergedTransitRoute.getDepartures().size()) :
							Integer.toString(mergedTransitRoute.getDepartures().size()));
					
					Departure dep = factory.createDeparture(new IdImpl(departureId),
							departure.getDepartureTime() + transitRoute.getStop(startTransitRouteStop.getStopFacility()).getDepartureOffset());
					dep.setVehicleId(departure.getVehicleId());
					
					mergedTransitRoute.addDeparture(dep);
					
				}
				
			}
			
		}
	
	}

	/**
	 * 
	 * Creates a new route profile for a simplified transit route.
	 * The arrival and departure offsets of each stop are also merged to
	 * get the average travel and stop time for all routes to that stop.
	 * 
	 * @param newRoute the new network route
	 * @return
	 */
	private static List<TransitRouteStop> computeNewRouteProfile(String[] listOfRoutes,NetworkRoute newRoute){
		
		Map<TransitStopFacility,Double> arrivalOffsets = new HashMap<TransitStopFacility,Double>();
		Map<TransitStopFacility,Double> departureOffsets = new HashMap<TransitStopFacility,Double>();
		
		Map<TransitStopFacility,Integer> arrivalCounter = new HashMap<TransitStopFacility,Integer>();
		Map<TransitStopFacility,Integer> departureCounter = new HashMap<TransitStopFacility,Integer>();
		
		List<TransitRouteStop> stops2Remove = new ArrayList<TransitRouteStop>();
		
		List<TransitRouteStop> newStops = new ArrayList<TransitRouteStop>();
		
		for(int i=0; i < listOfRoutes.length; i++){
			
			TransitRoute transitRoute = transitRoutes.get(new IdImpl(listOfRoutes[i]));
			
			for(TransitRouteStop stop : transitRoute.getStops()){
				
				Id linkId = stop.getStopFacility().getLinkId();
				
				if(newRoute.getLinkIds().contains(linkId)||linkId.equals(newRoute.getStartLinkId())||linkId.equals(newRoute.getEndLinkId())){
		
					//if the current route stop has not yet been visited add it to the collection of arrival offsets
					//else compute the average arrival offset of the contained routes and the new one
					if(!arrivalOffsets.containsKey(stop.getStopFacility())){
						arrivalOffsets.put(stop.getStopFacility(), stop.getArrivalOffset());
						arrivalCounter.put(stop.getStopFacility(), 1);
					} else{
						
						double arrivalOffset = ( arrivalOffsets.get(stop.getStopFacility()) * arrivalCounter.get(stop.getStopFacility()) +
								stop.getArrivalOffset() ) / ( arrivalCounter.get(stop.getStopFacility()) + 1 );
						arrivalOffsets.put(stop.getStopFacility(), arrivalOffset);
						arrivalCounter.put(stop.getStopFacility(), arrivalCounter.get(stop.getStopFacility())+1);
						
					}
					
					//same as arrival offsets
					if(!departureOffsets.containsKey(stop.getStopFacility())){
						departureOffsets.put(stop.getStopFacility(), stop.getDepartureOffset());
						departureCounter.put(stop.getStopFacility(), 1);
					} else{
						
						double departureOffset = ( departureOffsets.get(stop.getStopFacility()) * departureCounter.get(stop.getStopFacility()) +
								stop.getDepartureOffset() ) / ( departureCounter.get(stop.getStopFacility()) + 1 );
						departureOffsets.put(stop.getStopFacility(), departureOffset);
						departureCounter.put(stop.getStopFacility(), departureCounter.get(stop.getStopFacility())+1);

					}
					
					if(!stops2Remove.contains(stop))
						stops2Remove.add(stop);
					
				}
				
			}
			
		}
		
		double arrivalOffset = 0.;
		TransitRouteStop initialStop = null;
		
		//create new transit route stops and add them
		for( TransitStopFacility stop : arrivalOffsets.keySet() ){
			
			TransitRouteStop newStop = null;
			
			if(stop.getLinkId().equals(newRoute.getStartLinkId())){
				arrivalOffset = arrivalOffsets.get(stop);
				newStop = factory.createTransitRouteStop(stop, 0., departureOffsets.get(stop) - arrivalOffset);
				newStop.setAwaitDepartureTime(true);
			} else{
				
				for(TransitRouteStop routeStop : stops2Remove)
					if(routeStop.getStopFacility().equals(stop))
						initialStop = routeStop;
				
//				if(stop.getLinkId().equals(newRoute.getEndLinkId()))
//					newStop = factory.createTransitRouteStop(stop, arrivalOffsets.get(stop) - arrivalOffset, arrivalOffsets.get(stop) - arrivalOffset);
				/*else*/ newStop = factory.createTransitRouteStop(stop, arrivalOffsets.get(stop) - arrivalOffset, departureOffsets.get(stop) - arrivalOffset);
				newStop.setAwaitDepartureTime(initialStop.isAwaitDepartureTime());
			}
			
			
			newStops.add(newStop);
		}
		
		//remove visited stops for they are not contained in any other transit route
		for(TransitRouteStop stop : stops2Remove)
			stops.remove(stop);
		
		Collections.sort(newStops,arrivalOffsetComparator);
		
		return newStops;
		
	}

	/**
	 * Compares the route profiles of two given transit routes for equality.
	 * 
	 * @param transitRoute
	 * @param transitRoute2
	 * @return true if the route profiles are equal, false if not
	 */
	private static boolean routeProfilesEqual(TransitRoute transitRoute,
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
	private static List<TransitRouteStop> routeProfilesTouch(TransitRoute transitRoute,
			TransitRoute refTransitRoute) {
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		
		for(TransitRouteStop stop : refTransitRoute.getStops()){
			if(transitRoute.getStops().contains(stop))
				stops.add(stop);
		}
		
		return stops;
	}

}
