package playground.santiago.gtfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
public class TransitScheduleSimplifier{
	
	private TransitSchedule transitSchedule;
	private TransitSchedule mergedSchedule;
	
	private final Map<Id,Double[]> departures = new HashMap<Id,Double[]>();
	
	private List<TransitStopFacility> checkedFacilities = new ArrayList<TransitStopFacility>();
	
	private int cnt = 0;
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with equal route profiles.
	 * The simplified schedule is also written into a new file.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 * @return the simplified transit schedule
	 */
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
	
	/**
	 * Simplifies a transit schedule by merging transit routes within a transit line with touching route profiles.
	 * The initial transit routes are split into sections on which they overlap. A new section is created if the number
	 * of overlapping transit routes changes.
	 * 
	 * @param scenario the scenario containing the transit schedule to simplify
	 * @param outputDirectory the destination folder for the simplified transit schedule file
	 * @return the simplified transit schedule
	 */
	public static TransitSchedule mergeTouchingRoutes(Scenario scenario, String outputDirectory){
		
		return new TransitScheduleSimplifier().mergeTouchingTransitRoutes(scenario, outputDirectory);
		
	}
	
	private TransitSchedule mergeEqualTransitRoutes(final TransitSchedule schedule, String outputDirectory) {

		final Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("equal transit routes within a transit line will be merged...");
		
		final String UNDERLINE = "___";

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		Map<Id<TransitLine>, TransitLine> transitLines = schedule.getTransitLines();
		
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
					for(Id<TransitRoute> transitRouteId : transitRoutes.keySet()){
						
						double earliest = Double.POSITIVE_INFINITY;
						double latest = Double.NEGATIVE_INFINITY;
						
						for(Departure d : transitRoutes.get(transitRouteId).getDepartures().values()){
							if(d.getDepartureTime() < earliest){
								earliest = d.getDepartureTime();
							}
							if(d.getDepartureTime() > latest){
								latest = d.getDepartureTime();
							}
						}
						
						Double[] d = new Double[2];
						d[0] = earliest;
						d[1] = latest;
						this.departures.put(transitRouteId, d);
					
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
					
					mergedTransitRoute = factory.createTransitRoute(Id.create(id, TransitRoute.class),
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
		
		Map<Id<TransitLine>,TransitLine> transitLines = new TreeMap<>();

		transitLines.put(Id.create("line1", TransitLine.class), factory.createTransitLine(Id.create("line1", TransitLine.class)));
		
		for(TransitLine transitLine : schedule.getTransitLines().values()){
			
			if(transitLine.getRoutes().size() > 0){
			
				for(TransitRoute route : transitLine.getRoutes().values())
					transitLines.get(Id.create("line1", TransitLine.class)).addRoute(route);
			
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
		
			Map<Id<TransitRoute>,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			TransitRoute refTransitRoute = null;
		
			TransitLine mergedTransitLine = factory.createTransitLine(transitLine.getId());
			
			TransitRoute mergedTransitRoute = null;
			
			routesCounter += transitRoutes.size();
			
			Map<Id<TransitRoute>,List<TransitRouteStop>> routeProfiles = new TreeMap<>();
			Map<Id<TransitRoute>,String> routeIds = new HashMap<>();
			Map<Id<TransitRoute>,NetworkRoute> networkRoutes = new HashMap<>(); 
			
			boolean added = false;
			
			int profileCounter = 0;
			
			for(TransitRoute transitRoute : transitRoutes.values()){
				
				if(transitRoute.getDepartures().size() > 0){
				
					added = false;
					
					List<TransitRouteStop> routeProfile = transitRoute.getStops();
					NetworkRoute networkRoute = transitRoute.getRoute();
					String id = transitRoute.getId().toString();
					
					if(routeIds.size() < 1){
						routeIds.put(Id.create(profileCounter, TransitRoute.class), id);
						routeProfiles.put(Id.create(profileCounter, TransitRoute.class), routeProfile);
						networkRoutes.put(Id.create(profileCounter, TransitRoute.class), networkRoute);
					}
					else{
						for(Id<TransitRoute> routeId : routeIds.keySet()){
							if(routeProfilesEqual(routeProfile, routeProfiles.get(routeId))){
								routeIds.put(routeId, routeIds.get(routeId) + UNDERLINE + id);
								added = true;
								break;
							}
						}
						if(!added){
							routeIds.put(Id.create(profileCounter, TransitRoute.class), id);
							routeProfiles.put(Id.create(profileCounter, TransitRoute.class), routeProfile);
							networkRoutes.put(Id.create(profileCounter, TransitRoute.class), networkRoute);
						}
						
					}
					
					profileCounter++;
				
				}
				
			}
			
			for(Id<TransitRoute> routeId : routeIds.keySet()){
				
				String id = routeIds.get(routeId);
				
				if(transitRoutes.containsKey(Id.create(id, TransitRoute.class))){
					mergedTransitLine.addRoute(transitRoutes.get(Id.create(id, TransitRoute.class)));
					mergedRoutesCounter++;
					continue;
				}
				
				String[] listOfRoutes = id.split(UNDERLINE);
				
				NetworkRoute newRoute = networkRoutes.get(routeId);
				
				refTransitRoute = transitRoutes.get(Id.create(listOfRoutes[0], TransitRoute.class));
				
				List<TransitRouteStop> newStops = computeNewRouteProfile(factory, refTransitRoute, transitRoutes, listOfRoutes, null);
				compareRouteProfiles(refTransitRoute.getStops(), newStops);
				
				mergedTransitRoute = factory.createTransitRoute(Id.create(id, TransitRoute.class), newRoute, newStops, TransportMode.pt);
				
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

	private TransitSchedule mergeTouchingTransitRoutes(Scenario scenario, String outputDirectory){
		
		final String UNDERLINE = "_____";
		
		Logger log = Logger.getLogger(TransitScheduleSimplifier.class);
		
		log.info("starting simplify method for given transit schedule...");
		log.info("transit routes within a transit line that overlap at least at one stop facility will be merged...");
		
		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		
		this.transitSchedule = scenario.getTransitSchedule();
		
		mergedSchedule = factory.createTransitSchedule();
		
		for(TransitStopFacility facility : transitSchedule.getFacilities().values()){
			mergedSchedule.addStopFacility(facility);
		}
		
		Map<Id<TransitLine>,TransitLine> transitLines = transitSchedule.getTransitLines();
		
		int routesCounter = 0;
		int mergedRoutesCounter = 0;
		
		Iterator<TransitLine> transitLineIterator = transitLines.values().iterator();
		
		while(transitLineIterator.hasNext()){
			
			this.checkedFacilities = new ArrayList<TransitStopFacility>();
		
			TransitLine transitLine = transitLineIterator.next();
			
			if(transitLine.getRoutes().size() <= 0){
				continue;
			}
			
			TransitLine mergedTransitLine = factory.createTransitLine(transitLine.getId());
			
			Map<Id<TransitRoute>,TransitRoute> transitRoutes = transitLine.getRoutes();
			
			routesCounter += transitRoutes.size();
		
			TransitRoute currentTransitRoute = null;
		
			PriorityQueue<Id<TransitRoute>> uncheckedRoutes = new PriorityQueue<Id<TransitRoute>>();
			uncheckedRoutes.addAll(transitRoutes.keySet());
		
			List<TransitStopFacility> checkedFacilities = new ArrayList<TransitStopFacility>();

			boolean reduced = false;
			boolean breakpoint = false;
			int n = 1;
			
			//iterate over all transit routes
			while(uncheckedRoutes.size() > 0){
				
				currentTransitRoute = transitRoutes.get(uncheckedRoutes.poll());
				Set<Id<TransitRoute>> endedTransitRoutes = new HashSet<Id<TransitRoute>>();
				
				if(currentTransitRoute.getDepartures().size() > 0){
				
					String id = currentTransitRoute.getId().toString();
					String prevId = null;

					LinkedList<TransitStopFacility> drivenStops = new LinkedList<TransitStopFacility>();
					
					TransitStopFacility lastFacility = null;

					for(int i = 0; i < currentTransitRoute.getStops().size(); i++){
						
						TransitRouteStop stop = currentTransitRoute.getStops().get(i);
						
						TransitStopFacility currentFacility = stop.getStopFacility();

						drivenStops.addLast(currentFacility);
						
						
						if(checkedFacilities.contains(currentFacility)){
							if(drivenStops.size() < 2){
								drivenStops = new LinkedList<TransitStopFacility>();
								id = currentTransitRoute.getId().toString();
								continue;
							} else{
								breakpoint = true;
							}
						}
						
						for(TransitRoute transitRoute : transitRoutes.values()){
							
							if(!transitRoute.equals(currentTransitRoute) && transitRoutesHaveSameDirection(currentTransitRoute, transitRoute) && !endedTransitRoutes.contains(transitRoute.getId())){
								
								if(transitRoute.getStop(currentFacility) != null){
									
									id += UNDERLINE + transitRoute.getId().toString();
									
								} else{
									if(prevId != null){
										if(prevId.contains(transitRoute.getId().toString())){
											endedTransitRoutes.add(transitRoute.getId());
											drivenStops.remove(currentFacility);
											reduced = true;
										}
										
									}
									
								}
								
							}
							
						}
						
						if(prevId != null && drivenStops.size() > 1){
								
							if(!prevId.equals(id) || i >= currentTransitRoute.getStops().size()-1 || breakpoint){
//								System.out.println(prevId);
//								if(!id.contains(UNDERLINE)){
//									mergedTransitLine.addRoute(currentTransitRoute);
//								} else{
									mergedTransitLine.addRoute(createNewMergedRoute((prevId+"==="+n),drivenStops,scenario,factory, transitRoutes));
//								}
								n++;
								mergedRoutesCounter++;
								
								drivenStops = new LinkedList<TransitStopFacility>();
								
								if(reduced){
									i--;
									checkedFacilities.remove(lastFacility);
//									checkedFacilities.remove(currentFacility);
//									drivenStops.add(lastFacility);
//									drivenStops.addLast(currentFacility);
									reduced = false;
								}
								
								i--;
								
//								System.out.println(currentTransitRoute.getStops().get(i));
								
							}
							
						} else if(prevId != null && drivenStops.size() <= 1){
							if(!prevId.equals(id) || i >= currentTransitRoute.getStops().size()-1 || breakpoint){
								drivenStops = new LinkedList<TransitStopFacility>();
								id = currentTransitRoute.getId().toString();
							}
						}
						
						prevId = id;
						id = currentTransitRoute.getId().toString();
						if(i < 0){
							checkedFacilities.add(currentTransitRoute.getStops().get(i+1).getStopFacility());
							lastFacility = currentTransitRoute.getStops().get(i+1).getStopFacility();
						} else{
							checkedFacilities.add(currentTransitRoute.getStops().get(i).getStopFacility());
							lastFacility = currentTransitRoute.getStops().get(i).getStopFacility();
						}
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
			TransitScheduleFactoryImpl factory, Map<Id<TransitRoute>,TransitRoute> transitRoutes){
		
		final String UNDERLINE = "_____";
		
		String[] prevId = s.split("===");
		
		String[] listOfRoutes = prevId[0].split(UNDERLINE);
		
		TransitRoute referenceRoute = null;
	
		for(String string : listOfRoutes){
		
			TransitRoute tr = transitRoutes.get(Id.create(string, TransitRoute.class));
		
			if(referenceRoute == null)
				referenceRoute = tr;
			else{
				
				if(tr.getStops().size() > referenceRoute.getStops().size() &&
						tr.getStop(routeProfile.get(0))!=null && tr.getStop(routeProfile.get(routeProfile.size()-1))!=null){
					referenceRoute = tr;
				}
			
			}
		
		}
		
		List<TransitRouteStop> newRouteProfile = createNewSplittedRouteProfile(factory,listOfRoutes,transitRoutes,referenceRoute,routeProfile);
	
		Node fromNode = scenario.getNetwork().getLinks().get(routeProfile.get(0).getLinkId()).getToNode();
		Node toNode = scenario.getNetwork().getLinks().get(routeProfile.get(routeProfile.size()-1).getLinkId()).getFromNode();
		
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(referenceRoute.getRoute().getStartLinkId());
		linkIds.addAll(referenceRoute.getRoute().getLinkIds());
		linkIds.add(referenceRoute.getRoute().getEndLinkId());
		
		NetworkRoute newNetworkRoute = createSubRoute(referenceRoute.getRoute(), fromNode, toNode, scenario.getNetwork(), routeProfile);
	
		TransitRoute mergedTransitRoute = factory.createTransitRoute(Id.create(s, TransitRoute.class), newNetworkRoute, newRouteProfile, TransportMode.pt);
		
		mergeDeparturesTouching(factory, transitRoutes, mergedTransitRoute, listOfRoutes, routeProfile);
		
		return mergedTransitRoute;
		
	}
	
	private NetworkRoute createSubRoute(NetworkRoute route, Node fromNode,
			Node toNode, Network network, List<TransitStopFacility> routeProfile) {
		
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		
		List<Id<Link>> newLinkIds = new ArrayList<Id<Link>>();
		boolean on = false;
		List<Id> mustHave = new ArrayList<Id>();
		
		for(TransitStopFacility st : routeProfile){
			mustHave.add(st.getLinkId());
		}
		
		for(Id<Link> linkId : linkIds){
			
			if(linkId == mustHave.get(0)){
				on = true;
			}
			
			if(on){
				newLinkIds.add(linkId);
			}
			
			if(newLinkIds.containsAll(mustHave) && linkId == mustHave.get(mustHave.size()-1)){
				break;
			}
			
		}
		
		if(newLinkIds.size() < 2){
			Map<Id<Link>, ? extends Link> outLinks = network.getLinks().get(newLinkIds.get(0)).getToNode().getOutLinks();
			
			do{
				for(Link l : outLinks.values()){
					if(l.getAllowedModes().contains(TransportMode.pt)&&!newLinkIds.contains(l.getId())){
						newLinkIds.add(l.getId());
						break;
					}
				}
			}while(newLinkIds.size() < 2);
		}
		
		return RouteUtils.createNetworkRoute(newLinkIds, network);
		
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
			TransitRoute refTransitRoute, Map<Id<TransitRoute>,TransitRoute> transitRoutes, String[] listOfRoutes,
			List<TransitRouteStop> stops){
		
		LinkedList<TransitRouteStop> newStops = new LinkedList<TransitRouteStop>();

		for(int i = 0; i < refTransitRoute.getStops().size(); i++){
			
			double arrivalOffset = 0;
			int arrCounter = 0;
			double departureOffset = 0;
			int depCounter = 0;
//			int nDepartures = 0;
			
			for(int j = 0; j < listOfRoutes.length; j++){
				
				TransitRoute route = transitRoutes.get(Id.create(listOfRoutes[j], TransitRoute.class));
				TransitRouteStop stop = route.getStops().get(i);
				arrivalOffset += stop.getArrivalOffset()*route.getDepartures().size();
				arrCounter++;
				departureOffset += stop.getDepartureOffset()*route.getDepartures().size();
				depCounter++;
//				nDepartures += route.getDepartures().size();
				
			}
			
			TransitRouteStop newStop = factory.createTransitRouteStop(refTransitRoute.getStops().get(i).getStopFacility(), arrivalOffset/(arrCounter),
					departureOffset/(depCounter));
			
			newStop.setAwaitDepartureTime(refTransitRoute.getStops().get(i).isAwaitDepartureTime());
			
			newStops.addLast(newStop);
			
		}
		
		return newStops;
		
	}
	
	private List<TransitRouteStop> createNewSplittedRouteProfile(TransitScheduleFactoryImpl factory, String[] listOfRoutes,
			Map<Id<TransitRoute>,TransitRoute> transitRoutes, TransitRoute referenceRoute, List<TransitStopFacility> routeProfile){

		LinkedList<TransitRouteStop> newRouteProfile = new LinkedList<TransitRouteStop>();
		
		TransitStopFacility f = routeProfile.get(0);
		
		for(TransitStopFacility facility : routeProfile){
			
			double arrivalOffset = 0;
			double departureOffset = 0;

			for(String s : listOfRoutes){

				TransitRoute tr = transitRoutes.get(Id.create(s, TransitRoute.class));
				TransitRouteStop stop = tr.getStop(facility);

				if(tr.getStops().indexOf(stop) > tr.getStops().indexOf(tr.getStop(f))){
					arrivalOffset += stop.getArrivalOffset() - tr.getStop(f).getDepartureOffset();
					departureOffset += stop.getDepartureOffset() - tr.getStop(f).getDepartureOffset();
				}
					
			}
			
			TransitRouteStop newStop = null;
			
			if(!this.checkedFacilities.contains(facility)){
				newStop = factory.createTransitRouteStop(facility, arrivalOffset/listOfRoutes.length,
						departureOffset/listOfRoutes.length);
			} else{
				TransitStopFacility newFacility = factory.createTransitStopFacility(Id.create(facility.getId().toString()+"-=virtual=-"+cnt, TransitStopFacility.class), facility.getCoord(), facility.getIsBlockingLane());
				newFacility.setLinkId(facility.getLinkId());
				cnt++;
				newStop = factory.createTransitRouteStop(newFacility, arrivalOffset/listOfRoutes.length,
						departureOffset/listOfRoutes.length);
				this.mergedSchedule.addStopFacility(newFacility);
				this.checkedFacilities.add(newFacility);
			}
			
			this.checkedFacilities.add(facility);
			
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
	private void mergeDepartures(TransitScheduleFactoryImpl factory, Map<Id<TransitRoute>,TransitRoute> transitRoutes,
			TransitRoute mergedTransitRoute,String[] listOfTransitRoutes) {

		TransitRouteStop startTransitRouteStop = mergedTransitRoute.getStops().get(0);
		
		for(int i = 0; i < listOfTransitRoutes.length; i++){

			TransitRoute transitRoute = transitRoutes.get(Id.create(listOfTransitRoutes[i], TransitRoute.class));
			
			if(mergedTransitRouteContainsTransitRouteStops(mergedTransitRoute, transitRoute, startTransitRouteStop)){
				
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
	
	private void mergeDeparturesTouching(TransitScheduleFactoryImpl factory, Map<Id<TransitRoute>,TransitRoute> transitRoutes,
			TransitRoute mergedTransitRoute,String[] listOfTransitRoutes,List<TransitStopFacility> routeProfile) {

		for(int i = 0; i < listOfTransitRoutes.length; i++){

			TransitRoute transitRoute = transitRoutes.get(Id.create(listOfTransitRoutes[i], TransitRoute.class));
			
				for(Departure departure : transitRoute.getDepartures().values()){
				
					String departureId = mergedTransitRoute.getDepartures().size() < 10 ?
							"0"+Integer.toString(mergedTransitRoute.getDepartures().size()) :
							Integer.toString(mergedTransitRoute.getDepartures().size());
					
					Departure dep = factory.createDeparture(Id.create(departureId, Departure.class),
							departure.getDepartureTime() + transitRoute.getStop(routeProfile.get(0)).getDepartureOffset());
					dep.setVehicleId(departure.getVehicleId());
					
					mergedTransitRoute.addDeparture(dep);
					
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
		
		TransitRoute shortestRoute = transitRoute1.getStops().size() > transitRoute2.getStops().size() ?
				transitRoute2 : transitRoute1;
		TransitRoute otherRoute = transitRoute1.getStops().size() < transitRoute2.getStops().size() ?
				transitRoute2 : transitRoute1;
		
		double d = 0;
		int i = 0;
		
		for(i = 0; i < shortestRoute.getStops().size()-1; i++){

			TransitStopFacility facility = shortestRoute.getStops().get(i).getStopFacility();
			TransitStopFacility nextFacility = shortestRoute.getStops().get(i+1).getStopFacility();
			
			if(otherRoute.getStop(facility) != null && otherRoute.getStop(nextFacility) != null){
				
				int diff = shortestRoute.getStops().indexOf(shortestRoute.getStop(nextFacility)) - shortestRoute.getStops().indexOf(shortestRoute.getStop(facility));
				int diff2 = otherRoute.getStops().indexOf(otherRoute.getStop(nextFacility)) - otherRoute.getStops().indexOf(otherRoute.getStop(facility));

				d += diff - diff2;
				
			} else{
				d += 1;
			}
			
		}
		
		if(d == 0){
			return true;
		}
		
		return false;
		
	}
	
}
