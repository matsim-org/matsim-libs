package playground.dhosse.frequencyBasedPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

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
public class TransitScheduleSimplifier{
	
	public static TransitSchedule mergeEqualRouteProfiles(TransitSchedule schedule, String outputDirectory){
		
		return new TransitScheduleSimplifier().mergeEqualTransitRoutes(schedule, outputDirectory);
		
	}
	
	/**
	 * 
	 * Simplifies a transit schedule by merging all transit routes within it that have equal route profiles.
	 * There will be only one transit line in the end containing all the merged transit routes.
	 * 
	 * @param schedule the transit schedule to be simplified
	 * @param outputDirectory the destination for the merged schedule
	 * @return
	 */
	public static TransitSchedule mergeEqualProfilesOfAllRoutes(TransitSchedule schedule, String outputDirectory){
		
		return new TransitScheduleSimplifier().mergeEqualTransitRoutesV1(schedule, outputDirectory);
		
	}
	
	public static TransitSchedule mergeTouchingRoutes(Scenario scenario, String outputDirectory){
		
		return new TransitScheduleSimplifier().mergeTouchingTransitRoutes(scenario, outputDirectory);
		
	}
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with equal route profiles.
	 * The simplified schedule is also written into a new file.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 * @return the simplified transit schedule
	 */
	private TransitSchedule mergeEqualTransitRoutes(final TransitSchedule schedule, String outputDirectory) {

		final Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("equal transit routes within a transit line will be merged...");
		
		final String UNDERLINE = "___";

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		Map<Id,TransitLine> transitLines = schedule.getTransitLines();
		
		TransitSchedule mergedSchedule = factory.createTransitSchedule();
		
		//add all stop facilities of the originial schedule to the new one
		for(TransitStopFacility stop : schedule.getFacilities().values())
			mergedSchedule.addStopFacility(stop);
			
		int routesCounter = 0;
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
			
			TransitLine transitLine = transitLineIterator.next();
		
			Map<Id,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			if(transitRoutes.size() > 0){
			
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
							
							if(transitRoute.getDepartures().size() > 0){

								//if the route profiles are equal, "mark" current transit route by adding it to a string array
								if(routeProfilesEqual(transitRoute.getStops(), refTransitRoute.getStops())){
						
									id += UNDERLINE+transitRoute.getId().toString();
						
									uncheckedRoutes.remove(transitRoute.getId());
								
								}
						
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
				
					NetworkRoute newRoute = refTransitRoute.getRoute();
	//				NetworkRoute newRoute = computeNetworkRoute(scenario.getNetwork(), refTransitRoute);
				
					List<TransitRouteStop> newStops = computeNewRouteProfile(factory, refTransitRoute, transitRoutes, listOfRoutes, null);
					compareRouteProfiles(refTransitRoute.getStops(), newStops);
					
					mergedTransitRoute = factory.createTransitRoute(new IdImpl(id),
						newRoute, newStops, TransportMode.pt);
					
					mergeDepartures(factory, transitRoutes, mergedTransitRoute, listOfRoutes);
	
					//add merged transit route to the transit line
					mergedTransitLine.addRoute(mergedTransitRoute);
					mergedRoutesCounter++;
				
				}
				
				mergedSchedule.addTransitLine(mergedTransitLine);
			
			}
			
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
	
	private TransitSchedule mergeEqualTransitRoutesV1(final TransitSchedule schedule, String outputDirectory){
		
		final Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("all equal transit routes will be merged...");
		
		final String UNDERLINE = "___";

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		Map<Id,TransitLine> transitLines = new TreeMap<Id,TransitLine>();

		transitLines.put(new IdImpl("line1"), factory.createTransitLine(new IdImpl("line1")));
		
		for(TransitLine transitLine : schedule.getTransitLines().values()){
			
			if(transitLine.getRoutes().size() > 0){
			
				for(TransitRoute route : transitLine.getRoutes().values())
					transitLines.get(new IdImpl("line1")).addRoute(route);
			
			}
			
		}
		
		TransitSchedule mergedSchedule = factory.createTransitSchedule();
		
		//add all stop facilities of the originial schedule to the new one
		for(TransitStopFacility stop : schedule.getFacilities().values())
			mergedSchedule.addStopFacility(stop);
			
		int routesCounter = 0;
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
			
			TransitLine transitLine = transitLineIterator.next();
		
			Map<Id,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			TransitRoute refTransitRoute = null;
		
			TransitLine mergedTransitLine = factory.createTransitLine(transitLine.getId());
			
			TransitRoute mergedTransitRoute = null;
			
			routesCounter += transitRoutes.size();
			
			Map<Id,List<TransitRouteStop>> routeProfiles = new TreeMap<Id,List<TransitRouteStop>>();
			Map<Id,String> routeIds = new HashMap<Id,String>();
			Map<Id,NetworkRoute> networkRoutes = new HashMap<Id,NetworkRoute>(); 
			
			boolean added = false;
			
			int profileCounter = 0;
			
			for(TransitRoute transitRoute : transitRoutes.values()){
				
				if(transitRoute.getDepartures().size() > 0){
				
					added = false;
					
					List<TransitRouteStop> routeProfile = transitRoute.getStops();
					NetworkRoute networkRoute = transitRoute.getRoute();
					String id = transitRoute.getId().toString();
					
					if(routeIds.size() < 1){
						routeIds.put(new IdImpl(profileCounter), id);
						routeProfiles.put(new IdImpl(profileCounter), routeProfile);
						networkRoutes.put(new IdImpl(profileCounter), networkRoute);
					}
					else{
						for(Id routeId : routeIds.keySet()){
							if(routeProfilesEqual(routeProfile, routeProfiles.get(routeId))){
								routeIds.put(routeId, routeIds.get(routeId) + UNDERLINE + id);
								added = true;
								break;
							}
						}
						if(!added){
							routeIds.put(new IdImpl(profileCounter), id);
							routeProfiles.put(new IdImpl(profileCounter), routeProfile);
							networkRoutes.put(new IdImpl(profileCounter), networkRoute);
						}
						
					}
					
					profileCounter++;
				
				}
				
			}
			
			for(Id routeId : routeIds.keySet()){
				
				String id = routeIds.get(routeId);
				
				if(transitRoutes.containsKey(new IdImpl(id))){
					mergedTransitLine.addRoute(transitRoutes.get(new IdImpl(id)));
					mergedRoutesCounter++;
					continue;
				}
				
				String[] listOfRoutes = id.split(UNDERLINE);
				
				NetworkRoute newRoute = networkRoutes.get(routeId);
				
				refTransitRoute = transitRoutes.get(new IdImpl(listOfRoutes[0]));
				
				List<TransitRouteStop> newStops = computeNewRouteProfile(factory, refTransitRoute, transitRoutes, listOfRoutes, null);
				compareRouteProfiles(refTransitRoute.getStops(), newStops);
				
				mergedTransitRoute = factory.createTransitRoute(new IdImpl(id), newRoute, newStops, TransportMode.pt);
				
				mergeDepartures(factory, transitRoutes, mergedTransitRoute, listOfRoutes);

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
		
		final String UNDERLINE = "___";
		
		Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("transit routes within a transit line that overlap at least at one stop facility will be merged...");
		
		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitSchedule mergedSchedule = factory.createTransitSchedule();
		
		for(TransitStopFacility facility : schedule.getFacilities().values()){
			mergedSchedule.addStopFacility(facility);
		}
		
		Map<Id,TransitLine> transitLines = schedule.getTransitLines();
		
		int routesCounter = 0;
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
		
			TransitLine transitLine = transitLineIterator.next();
			
			if(transitLine.getRoutes().size() <= 0) continue;
			
			TransitLine mergedTransitLine = factory.createTransitLine(new IdImpl(transitLine.getId().toString()));
			
			Map<Id,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			routesCounter += transitRoutes.size();
		
			TransitRoute currentTransitRoute = null;
		
			PriorityQueue<Id> uncheckedRoutes = new PriorityQueue<Id>();
			uncheckedRoutes.addAll(transitRoutes.keySet());
		
			List<TransitStopFacility> checkedFacilities = new ArrayList<TransitStopFacility>();

			boolean reduced = false;
			boolean breakpoint = false;
			int n = 1;
			
			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
				
				currentTransitRoute = transitRoutes.get(uncheckedRoutes.poll());
				
				if(currentTransitRoute.getDepartures().size() > 0){
				
					String id = currentTransitRoute.getId().toString();
					String prevId = null;
					
					LinkedList<TransitStopFacility> drivenStops = new LinkedList<TransitStopFacility>();
					
					TransitStopFacility lastFacility = null;

					for(TransitRouteStop stop : currentTransitRoute.getStops()){
						
						TransitStopFacility currentFacility = stop.getStopFacility();
						
						drivenStops.addLast(currentFacility);
						
						if(!checkedFacilities.contains(currentFacility)){
							
							for(TransitRoute transitRoute : transitRoutes.values()){
								
								if(!transitRoute.equals(currentTransitRoute)&&transitRoutesHaveSameDirection(currentTransitRoute, transitRoute)){
									
									if(transitRoute.getStop(currentFacility) != null){
										
										id += UNDERLINE + transitRoute.getId().toString();
										
									} else{
										
										drivenStops.remove(currentFacility);
										reduced = true;
										
									}
									
								}
								
							}
							
						} else{
							if(drivenStops.size() < 2){
								drivenStops = new LinkedList<TransitStopFacility>();
								continue;
							}
							else breakpoint = true;
						}
						
						if(prevId != null&&drivenStops.size()>1){
								
							if(!prevId.equals(id) || currentTransitRoute.getStops().indexOf(stop) >= currentTransitRoute.getStops().size()-1 || breakpoint){

								mergedTransitLine.addRoute(createNewMergedRoute((prevId+"==="+n),drivenStops,scenario,factory, transitRoutes));
								n++;
								mergedRoutesCounter++;
								
								drivenStops = new LinkedList<TransitStopFacility>();
								if(reduced){
									drivenStops.addLast(lastFacility);
									reduced = false;
								}
								
								drivenStops.addLast(currentFacility);
								
							}
							
						}
						
						prevId = id;
						id = currentTransitRoute.getId().toString();
						checkedFacilities.add(currentFacility);
						lastFacility = currentFacility;
						breakpoint = false;
						
					}
					
				}
				
			}
			
			mergedSchedule.addTransitLine(mergedTransitLine);
			
		}
			
		log.info("number of initial transit routes: " + routesCounter);
		String diff = routesCounter > mergedRoutesCounter ? Integer.toString(mergedRoutesCounter - routesCounter) 
				: "+" + Integer.toString(mergedRoutesCounter - routesCounter);
		log.info("number of merged transit routes: " + mergedRoutesCounter + " ( " + diff + " )");
		
		log.info("writing simplified transit schedule to " + outputDirectory);
		
		new TransitScheduleWriter(mergedSchedule).writeFile(outputDirectory);
		
		log.info("... done.");
		
		return mergedSchedule;
		
	}
	
	private TransitRoute createNewMergedRoute(String s, List<TransitStopFacility> routeProfile, Scenario scenario,
			TransitScheduleFactoryImpl factory, Map<Id,TransitRoute> transitRoutes){
		
		final String UNDERLINE = "___";
		
		String[] prevId = s.split("===");
		
		String[] listOfRoutes = prevId[0].split(UNDERLINE);

		TransitStopFacility firstStop = routeProfile.get(0);
		TransitRoute referenceRoute = null;
	
		for(String string : listOfRoutes){
		
			TransitRoute tr = transitRoutes.get(new IdImpl(string));
		
			if(referenceRoute == null)
				referenceRoute = tr;
			else{
			
				if(referenceRoute.getStops().indexOf(firstStop) < tr.getStops().indexOf(firstStop))
					referenceRoute = tr;
			
			}
		
		}
	
		List<TransitRouteStop> newRouteProfile = createNewSplittedRouteProfile(factory,listOfRoutes,transitRoutes,referenceRoute,routeProfile);
	
		NetworkRoute newNetworkRoute = createNewNetworkRoute(referenceRoute,scenario.getNetwork(),routeProfile);
	
		TransitRoute mergedTransitRoute = factory.createTransitRoute(new IdImpl(s), newNetworkRoute, newRouteProfile, TransportMode.pt);
		
		mergeDepartures(factory, transitRoutes, mergedTransitRoute, listOfRoutes);
		
		return mergedTransitRoute;
		
	}
	
	private NetworkRoute createNewNetworkRoute(TransitRoute referenceRoute,
			Network network, List<TransitStopFacility> routeProfile) {
		
		LinkedList<Id> newRoute = new LinkedList<Id>();

		Id firstLinkId = routeProfile.get(0).getLinkId();
		Id lastLinkId = routeProfile.get(routeProfile.size()-1).getLinkId();
		
		if(referenceRoute.getRoute().getStartLinkId().equals(firstLinkId))
			newRoute.addFirst(firstLinkId);
		
		int startIndex = newRoute.isEmpty() ? referenceRoute.getRoute().getLinkIds().indexOf(firstLinkId) : 0;
		int endIndex = newRoute.isEmpty() ? referenceRoute.getRoute().getLinkIds().indexOf(firstLinkId) : 0;
		
		for(TransitStopFacility stop : routeProfile){
			if(referenceRoute.getRoute().getLinkIds().contains(stop.getLinkId())){
				endIndex = referenceRoute.getRoute().getLinkIds().indexOf(stop.getLinkId());
				for(int i = startIndex; i <= endIndex; i++)
					if(!newRoute.contains(referenceRoute.getRoute().getLinkIds().get(i)))
						newRoute.addLast(referenceRoute.getRoute().getLinkIds().get(i));
				startIndex = endIndex;
			}
		}
		
		if(referenceRoute.getRoute().getEndLinkId().equals(lastLinkId))
			newRoute.addLast(lastLinkId);

		NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRoute, network);
		
		return newNetworkRoute;
		
	}
	
	/**
	 * 
	 * Creates a new route profile for a simplified transit route.
	 * The arrival and departure offsets of each stop are merged to
	 * get the average travel time to and stop time for all routes at that stop.
	 * 
	 * @return merged route profile
	 */
	private List<TransitRouteStop> computeNewRouteProfile(TransitScheduleFactoryImpl factory,
			TransitRoute refTransitRoute, Map<Id,TransitRoute> transitRoutes, String[] listOfRoutes,
			List<TransitRouteStop> stops){
		
		LinkedList<TransitRouteStop> newStops = new LinkedList<TransitRouteStop>();
		
		for(int i = 0; i < refTransitRoute.getStops().size(); i++){
			
			double arrivalOffset = 0;
			int arrCounter = 0;
			double departureOffset = 0;
			int depCounter = 0;
			
			for(int j = 0; j < listOfRoutes.length; j++){
				
				TransitRouteStop stop = transitRoutes.get(new IdImpl(listOfRoutes[j])).getStops().get(i);
				arrivalOffset += stop.getArrivalOffset();
				arrCounter++;
				departureOffset += stop.getDepartureOffset();
				depCounter++;
				
			}
			
			TransitRouteStop newStop = factory.createTransitRouteStop(refTransitRoute.getStops().get(i).getStopFacility(), arrivalOffset/arrCounter,
					departureOffset/depCounter);
			
			newStop.setAwaitDepartureTime(refTransitRoute.getStops().get(i).isAwaitDepartureTime());
			
			newStops.addLast(newStop);
			
		}
		
		return newStops;
		
	}
	
	private List<TransitRouteStop> createNewSplittedRouteProfile(TransitScheduleFactoryImpl factory, String[] listOfRoutes,
			Map<Id,TransitRoute> transitRoutes, TransitRoute referenceRoute, List<TransitStopFacility> routeProfile){

		LinkedList<TransitRouteStop> newRouteProfile = new LinkedList<TransitRouteStop>();
		
		TransitStopFacility f = routeProfile.get(0);
//		System.out.println(referenceRoute + "\t" + f);
		TransitRouteStop referenceStop = referenceRoute.getStops().get(referenceRoute.getStops().indexOf(referenceRoute.getStop(f)));
		
		for(TransitStopFacility facility : routeProfile){
			
			double arrivalOffset = 0;
			int arrCounter = 0;
			double departureOffset = 0;
			int depCounter = 0;
			
			for(String s : listOfRoutes){

				TransitRoute tr = transitRoutes.get(new IdImpl(s));
				TransitRouteStop stop = tr.getStop(facility);
				
//				System.out.println(tr.getId() + "\t" + stop + "\t" + facility.getId());
				
				if(stop != null){
				arrivalOffset += stop.getArrivalOffset();
				arrCounter++;
				departureOffset += stop.getDepartureOffset();
				depCounter++;
				
				if(s.equals(referenceRoute.getId().toString()) && referenceStop != null){
					arrivalOffset -= referenceStop.getArrivalOffset();
					departureOffset -= referenceStop.getArrivalOffset();
				}
				
				}
			}
			
			TransitRouteStop newStop = factory.createTransitRouteStop(facility, arrivalOffset/arrCounter,
					departureOffset/depCounter);
			
			newStop.setAwaitDepartureTime(referenceRoute.getStop(facility).isAwaitDepartureTime());
			
			newRouteProfile.addLast(newStop);
			
		}
		
		return newRouteProfile;
		
	}

	/**
	 * Merges the departures of all transit routes that are to be merged.
	 * 
	 * @param startTransitRouteStop the first stop of the new transit route
	 * @param mergedTransitRoute the new transit route
	 */
	private void mergeDepartures(TransitScheduleFactoryImpl factory, Map<Id,TransitRoute> transitRoutes,
			TransitRoute mergedTransitRoute,String[] listOfTransitRoutes) {

		TransitRouteStop startTransitRouteStop = mergedTransitRoute.getStops().get(0);
		
		for(int i = 0; i < listOfTransitRoutes.length; i++){

			TransitRoute transitRoute = transitRoutes.get(new IdImpl(listOfTransitRoutes[i]));
			
			if(mergedTransitRouteContainsTransitRouteStops(mergedTransitRoute, transitRoute, startTransitRouteStop)){
				
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
	 * Compares the route profiles of two given transit routes for equality.
	 * 
	 * @param transitRoute
	 * @param transitRoute2
	 * @return true if the route profiles are equal, false if not
	 */
	private boolean routeProfilesEqual(List<TransitRouteStop> routeProfile1,
			List<TransitRouteStop> routeProfile2) {
		
		if(routeProfile1.size() != routeProfile2.size())
			return false;

		for(int i=0;i<routeProfile1.size();i++){
			if(!(routeProfile1.get(i).getStopFacility().getId().equals(routeProfile2.get(i).getStopFacility().getId())))
				break;
			if(i == routeProfile1.size()-1)
				return true;
		}
		
		return false;
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
	
	private boolean transitRoutesHaveSameDirection(TransitRoute transitRoute1, TransitRoute transitRoute2){
		
		for(TransitRouteStop stop : transitRoute1.getStops()){
			
			if(transitRoute1.getStops().indexOf(stop) < transitRoute1.getStops().size()-1){
				
				TransitStopFacility facility = stop.getStopFacility();
				TransitStopFacility next = transitRoute1.getStops().get(transitRoute1.getStops().indexOf(stop)+1).getStopFacility();
			
				if(transitRoute2.getStop(facility) != null && transitRoute2.getStop(next) != null){

					int diff = transitRoute1.getStops().indexOf(next) - transitRoute1.getStops().indexOf(facility);
					int diff2 = transitRoute2.getStops().indexOf(next) - transitRoute2.getStops().indexOf(facility);
					
					if(diff == diff2)
						return true;
					
				}
				
			}
			
		}
		
		return false;
		
	}
	
}
