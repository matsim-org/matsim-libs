/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.polettif.multiModalMap.mapping;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRFastAStarLandmarksSimpleRouting;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRouter;
import playground.polettif.multiModalMap.gtfs.GTFSReader;
import playground.polettif.multiModalMap.mapping.containter.AllInterStopRoutes;
import playground.polettif.multiModalMap.mapping.containter.InterStopRoute;
import playground.polettif.multiModalMap.mapping.containter.InterStopRoutes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of PTLinesCreator.
 *
 * V3:
 * combined linking stopFacilities to links and routing
 * calculates routes for all stops and adds weigh for used links in route
 *
 * @author polettif
 */
public class PTMapperV3 extends PTMapper {

	// TODO ensure coordinate system is not WGS84 since this is not suitable for coordinate calculations

	// params TODO move to a config?
	private final static double NODE_SEARCH_RADIUS = 200; //[m] 150
	private final static int MAX_N_CLOSEST_LINKS = 6; // number of link candidates considered for all stops
	private final static int LOOKAHEAD_STOPS = 5; // number of next stops that should be included in calculations
	private final static int MAX_INITIAL_ARTIFICIAL_LINK_LENGTH = 1000; // maximal length of artificial links
	private final static double AZIMUTH_TOLERANCE = Math.PI*50/200; // maximal length of artificial links

	// not used
	private final static double MAX_FACILITY_NODE_DISTANCE = 100;
	private final static double MAX_FACILITY_LINK_DISTANCE = 50;

	private final static double WEIGHT_TRAVELTIME_COMPARED_TO_STOPFACILITY_DISTANCE = 1.0;

	private final static String PREFIX_SPLIT_LINKS = "split_";
	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	// TODO use transit modes
	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);

	int artificialId = 0;
	private Map<TransitStopFacility, List<Link>> allClosestLinks = new HashMap<>();
	private Map<List<TransitRouteStop>, Map<Id<Link>, Double>> linkWeights = new HashMap<>(); // todo store all linkweights to save time for recurring calculations


	public PTMapperV3(TransitSchedule schedule) {
		super(schedule);
	}

	public PTMapperV3(TransitSchedule schedule, Network network) {
		super(schedule, network);
	}

	@Override
	public void routePTLines(Network networkParam) {
		setNetwork(networkParam);

		// TODO add DUMMY_LINK to avoid null pointer stuff
		// TODO remove after routing
		this.network.addNode(networkFactory.createNode(Id.createNodeId(GTFSReader.DUMMY_NODE_1), new Coord(0.0, 0.0)));
		this.network.addNode(networkFactory.createNode(Id.createNodeId(GTFSReader.DUMMY_NODE_2), new Coord(0.0, 0.0)));
		this.network.addLink(networkFactory.createLink(Id.createLinkId(GTFSReader.DUMMY_LINK), network.getNodes().get(Id.createNodeId(GTFSReader.DUMMY_NODE_1)), network.getNodes().get(Id.createNodeId(GTFSReader.DUMMY_NODE_2))));

		log.info("Creating PT lines...");

		Counter counterLine = new Counter("route # ");

		/*
		* preload closest links
		* stopfacilities with no links within search radius need artificial links and nodes before routing starts
		*/
		List<TransitStopFacility> facilitiesTooFar = new ArrayList<>();
		NetworkImpl networkImpl = ((NetworkImpl) network); // used by search for nearest node
		for(TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {
			// limits number of links, for all links within search radius use Tools.findClosestLinks()
			List<Link> closestLinks = Tools.findOnlyNClosestLinks(networkImpl, stopFacility.getCoord(), NODE_SEARCH_RADIUS, MAX_N_CLOSEST_LINKS);

			if (closestLinks == null) {
				// if there are links outside the map area, the very first links are routed weird (i.e. directly to the covered area)
				if(CoordUtils.calcEuclideanDistance(stopFacility.getCoord(), networkImpl.getNearestNode(stopFacility.getCoord()).getCoord()) > MAX_INITIAL_ARTIFICIAL_LINK_LENGTH) {
					facilitiesTooFar.add(stopFacility);
				}
				else {
					Node newNode = networkFactory.createNode(Id.create(PREFIX_ARTIFICIAL_LINKS + artificialId++, Node.class), stopFacility.getCoord());
					Node nearestNode = networkImpl.getNearestNode(stopFacility.getCoord());
					Link newLink = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), newNode, nearestNode);
					Link newLink2 = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), nearestNode, newNode);

					network.addNode(newNode);
					network.addLink(newLink);
					network.addLink(newLink2);

					List<Link> newList = new ArrayList<>();
					newList.add(newLink);
					allClosestLinks.put(stopFacility, newList);
				}
			} else {
				allClosestLinks.put(stopFacility, closestLinks);
			}
		}

		/*
		* create artificial links for facilities initially too far from a network node. Can now use artificial links.
		* Still not perfect, but acceptable since stopfacilities outside the map area are normally not that relevant
		 */
		networkImpl = ((NetworkImpl) network);
		for(TransitStopFacility stopFacility : facilitiesTooFar) {
			Node newNode = networkFactory.createNode(Id.create(PREFIX_ARTIFICIAL_LINKS + artificialId++, Node.class), stopFacility.getCoord());
			Node nearestNode = networkImpl.getNearestNode(stopFacility.getCoord());
			Link newLink = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), newNode, nearestNode);
			Link newLink2 = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), nearestNode, newNode);

			network.addNode(newNode);
			network.addLink(newLink);
			network.addLink(newLink2);

			List<Link> newList = new ArrayList<>();
			newList.add(newLink);
			allClosestLinks.put(stopFacility, newList);
		}

		new NetworkWriter(network).write("C:/Users/polettif/Desktop/output/test/artificailnet.xml");


		/*
		* initiate router
		*/
		PTLRouter router = new PTLRFastAStarLandmarksSimpleRouting(this.network);

		/*
		* loop throgh
		* - lines
		*   - routes
		* 	   - stops
		* 	     look at pairs of stops
		* 	     - get set of close links (linkCandidates) for each stop
		*          - route between all linkCandidates
		*            assign score value to routes (using traveltime and facility-link distance
		*            use route (between two stops) with the best score
		*      combine best subroutes to route (linkSequence)
		*/
		AllInterStopRoutes allInterStopRoutes = new AllInterStopRoutes();

		for (TransitLine line : this.schedule.getTransitLines().values()) {
			log.info("Line "+ line.getId());
			for(TransitRoute route : line.getRoutes().values()) {

				List<TransitRouteStop> routeStops = route.getStops();
				TransitRouteStop firstStop = routeStops.get(0);
				TransitRouteStop endStop = routeStops.get(routeStops.size() - 1);
				Double firstEndStopDistance = CoordUtils.calcEuclideanDistance(firstStop.getStopFacility().getCoord(), endStop.getStopFacility().getCoord());

				// TODO ! circular lines and so on cannot be routed from first stop to last stop (are excluded now)
				// TODO maybe include some maximal angular difference or so...

				// TODO currently only busses
				if(route.getTransportMode().equals("bus") && firstEndStopDistance > NODE_SEARCH_RADIUS*3) {
					log.info("     Route " + route.getId());
					counterLine.incCounter();

					HashMap<Id<Link>, Double> routeLinkWeights = new HashMap<>();

					// iterate through all stops of the route and calculate best scores
					for (int i = 0; i < routeStops.size(); i++) {
						TransitRouteStop currentStop = routeStops.get(i);
						for (int j = i; j <= i + LOOKAHEAD_STOPS && j < routeStops.size(); j++) {
							TransitRouteStop nextStop = routeStops.get(j);
							InterStopRoutes interStopRoutes = new InterStopRoutes(currentStop, nextStop);

							// check if part of current and next stop was already routed
							if (!allInterStopRoutes.contains(currentStop, nextStop)) {
								List<Link> closestLinksCurrent = allClosestLinks.get(currentStop.getStopFacility());
								List<Link> closestLinksNext = allClosestLinks.get(nextStop.getStopFacility());

								// look at all routes for all linkCandidate combinations
								for (Link linkCandidateCurrent : closestLinksCurrent) {
									for (Link linkCandidateNext : closestLinksNext) {
										if (!linkCandidateCurrent.equals(linkCandidateNext)) {
											LeastCostPathCalculator.Path pathCandidate = router.calcLeastCostPath(linkCandidateCurrent.getToNode(), linkCandidateNext.getFromNode(), null, null);
											interStopRoutes.add(new InterStopRoute(currentStop, nextStop, linkCandidateCurrent, linkCandidateNext, pathCandidate));
										}
									}
								}
								allInterStopRoutes.add(interStopRoutes);
							}

							Map<Id<Link>, Double> tmpLinkWeights = allInterStopRoutes.get(currentStop, nextStop).getLinkWeights();

							for (Map.Entry<Id<Link>, Double> entry : tmpLinkWeights.entrySet()) {
								if (routeLinkWeights.containsKey(entry.getKey())) {
									routeLinkWeights.put(entry.getKey(), routeLinkWeights.get(entry.getKey()) + tmpLinkWeights.get(entry.getKey()));
								} else {
									routeLinkWeights.put(entry.getKey(), tmpLinkWeights.get(entry.getKey()));
								}
							}
						}
					}


					PTLRouter subrouter = new PTLRTransitRouter(network, routeLinkWeights);

					/*
					* create subroutes depending on maximal difference of azimut
					* fix the linkcandidate for breaking stops
					 */
					List<Id<Link>> linkSequence = new LinkedList<>();
					List<InterStopRoutes> subRoutes = new ArrayList<>();
					int i = 0;
					while (i < routeStops.size()-1) {
						int j = i + 1;
						TransitRouteStop stopA = routeStops.get(i);
						double az1 = Tools.getAzimuth(stopA.getStopFacility().getCoord(), routeStops.get(j++).getStopFacility().getCoord());

						while (j < routeStops.size()) {
							double az2 = Tools.getAzimuth(stopA.getStopFacility().getCoord(), routeStops.get(j).getStopFacility().getCoord());

							if (Math.abs(az2 - az1) > AZIMUTH_TOLERANCE) {
								TransitRouteStop stopB = routeStops.get(j - 1);
								List<Link> stopALinks = allClosestLinks.get(stopA.getStopFacility());
								List<Link> stopBLinks = allClosestLinks.get(stopB.getStopFacility());

								Link fromLink = null;
								Link toLink = null;

								// get best scoring link for stop 2
								double maxWeight = 0;
								for (Link check : stopALinks) {
									double checkLinkWeight = routeLinkWeights.get(check.getId());
									if (checkLinkWeight > maxWeight) {
										maxWeight = checkLinkWeight;
										fromLink = check;
									}
								}

								maxWeight = 0;
								for (Link check : stopBLinks) {
									double checkLinkWeight = routeLinkWeights.get(check.getId());
									if (checkLinkWeight > maxWeight) {
										maxWeight = checkLinkWeight;
										toLink = check;
									}
								}

								LeastCostPathCalculator.Path path = subrouter.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(), null, null);

								linkSequence.addAll(new InterStopRoute(stopA, stopB, fromLink, toLink, path).getLinkIds());

								i = j;
								break;
							} else {
								j++;
							}
						}
						i++;
					}

					// find best path from firstStop to lastStop considering linkWeights
					/*
					List<Link> closestLinksStart = allClosestLinks.get(firstStop.getStopFacility());
					List<Link> closestLinksEnd = allClosestLinks.get(endStop.getStopFacility());

					Double maxTravelCost = Double.MAX_VALUE;
					InterStopRoute bestRoute = null;
					for (Link linkStart : closestLinksStart) {
						for (Link linkEnd : closestLinksEnd) {
							LeastCostPathCalculator.Path path = subrouter.calcLeastCostPath(linkStart.getFromNode(), linkEnd.getToNode(), null, null);

							if (path.travelCost < maxTravelCost) {
								maxTravelCost = path.travelCost;
								bestRoute = new InterStopRoute(firstStop, endStop, linkStart, linkEnd, path);
							}
						}
					}


					// assing best path as link sequence to route
					if(bestRoute != null) {
						//	List<Id<Link>> linkSequence = bestPath.links.stream().map(Link::getId).collect(Collectors.toList());
						route.setRoute(RouteUtils.createNetworkRoute(bestRoute.getLinkIds(), this.network));

						// reference links to stopFacilities
						for (TransitRouteStop entry : routeStops) {
							List<Link> closestLinks = allClosestLinks.get(entry.getStopFacility());

							for (Link ll : closestLinks) {
								if (bestRoute.getLinkIds().contains(ll.getId())) {
									this.schedule.getFacilities().get(entry.getStopFacility().getId()).setLinkId(ll.getId());
									break;
								}
							}
						}
					} else {
						log.warn("No route (link sequence) assigned for route " + route.getId());
					}
					*/

					if(linkSequence.size() > 0) {
						route.setRoute(RouteUtils.createNetworkRoute(linkSequence, this.network));
					} else {
						log.warn("No route (link sequence) assigned for route " + route.getId());
					}

					// reference links to stopFacilities
					for (TransitRouteStop entry : routeStops) {
						List<Link> closestLinks = allClosestLinks.get(entry.getStopFacility());

						for (Link ll : closestLinks) {
							if (linkSequence.contains(ll.getId())) {
								this.schedule.getFacilities().get(entry.getStopFacility().getId()).setLinkId(ll.getId());
								break;
							}
						}
					}
				}
			} // - route loop

		} // - line loop

		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}


	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 * Clean also the allowed modes for only the modes, no line-number any more...
	 */
	protected void cleanStationsAndNetwork() {
		log.info("Clean Stations and Network...");
		// TODO get rid of dummy links which are implemented via gtfsreader (not independent)
		network.removeLink(Id.createLinkId(GTFSReader.DUMMY_LINK));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_1));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_2));
		cleanSchedule();
/* TODO clean network
		prepareNetwork();
		removeNonUsedStopFacilities();
 		setConnectedStopFacilitiesToIsBlocking();
*/
		log.info("Clean Stations and Network... done.");
	}

	private void setConnectedStopFacilitiesToIsBlocking() {
		Set<TransitStopFacility> facilitiesToExchange = new HashSet<>();
		for (TransitStopFacility oldFacility : this.schedule.getFacilities().values()) {
			if (this.network.getLinks().get(oldFacility.getLinkId()).getAllowedModes().contains(TransportMode.car)) {
				TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
						oldFacility.getId(), oldFacility.getCoord(), true);
				newFacility.setName(oldFacility.getName());
				newFacility.setLinkId(oldFacility.getLinkId());
				newFacility.setStopPostAreaId(oldFacility.getStopPostAreaId());
				facilitiesToExchange.add(newFacility);
			}
		}
		for (TransitStopFacility facility : facilitiesToExchange) {
			TransitStopFacility facilityToRemove = this.schedule.getFacilities().get(facility.getId());
			this.schedule.removeStopFacility(facilityToRemove);
			this.schedule.addStopFacility(facility);
		}
	}

	private void cleanSchedule() {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if (networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;
				}
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					if (linkId == null) {
						removeRoute = true;
					}
				}
				if (removeRoute) {
					log.error("NetworkRoute for " + transitRoute.getId().toString() + " incomplete. Remove route.");
					toRemove.add(transitRoute);
				}
			}
			if (!toRemove.isEmpty()) {
				for (TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
	}

	/**
	 * Add to any link that is passed by any route a "pt" in the modes, if it hasn't already one...
	 */
	private void prepareNetwork() {
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinks = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinks.add(networkRoute.getStartLinkId());
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					transitLinks.add(linkId);
				}
				transitLinks.add(networkRoute.getEndLinkId());
			}
		}
		for (Id<Link> transitLinkId : transitLinks) {
			Link transitLink = networkLinks.get(transitLinkId);
			if (!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	private void removeNonUsedStopFacilities() {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for (Id<TransitStopFacility> facilityId : this.schedule.getFacilities().keySet()) {
			if (!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(this.schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for (TransitStopFacility facility : unusedStopFacilites) {
			this.schedule.removeStopFacility(facility);
		}
	}


}
