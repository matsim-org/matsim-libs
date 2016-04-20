/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package playground.polettif.multiModalMap.mapping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;

/**
 * provides a container for parent and child stopfacilities and referenced link candidates
 */
@Deprecated
public class StopFacilityTree {

	private static final String PREFIX_ARTIFICIAL_LINKS = "pt_";
	/**
	 * Suffix used for child stop facilities. A number for each child of a parent stop facility is appended (i.e. stop0123.fac:2)
	 * senozon via uses . as identifier for sub stop facilities.
	 */
	private static final String SUFFIX_CHILD_STOPFACILITIES = ".fac:";
	private final TransitScheduleFactory scheduleFactory;
	private final TransitSchedule schedule;
	private final double nodeSearchRadius;
	private final int maxNclosestLinks;
	private final double maxLinkFacilityDistance;

	private final String transportMode;

	private int artificialId = 0;

	private Map<TransitStopFacility, List<LinkCandidate>> linkCandidates = new HashMap<>();
	private Map<TransitLine, Map<TransitRoute, Map<TransitStopFacility, TransitStopFacility>>> replacementMap = new HashMap<>();

	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where which no link can
	 * be found within nodeSearchRadius an ARTIFICIAL_LINK_MODE node and two ARTIFICIAL_LINK_MODE links (in & out) are
	 * created and added to the network. For each link candiate a child stop facility is generated
	 * and referenced to the link. Child stop facilities are added to the schedule.
	 *
	 * @param transitSchedule  with stopFacilities, is modified.
	 * @param network          where link candidates should be mapped, is modified.
	 * @param nodeSearchRadius only links within this radius from the stop facility are considered for closest links
	 *                         calculations.
	 * @param maxNclosestLinks the maximum number of closest links (and thus link candidates) should be used.
	 *                         Note: if two links have the same distance to the stop facility both are used
	 *                         regardless whether the maximum was already reached.
	 */
	@Deprecated
	public StopFacilityTree(TransitSchedule transitSchedule, Network network, String transportMode, double nodeSearchRadius, int maxNclosestLinks, double maxLinkFacilityDistance) {

		this.schedule = transitSchedule;
		this.nodeSearchRadius = nodeSearchRadius;
		this.maxNclosestLinks = maxNclosestLinks;
		this.maxLinkFacilityDistance = maxLinkFacilityDistance;
		this.scheduleFactory = schedule.getFactory();

		this.transportMode = transportMode;

		Map<TransitStopFacility, Integer> childFacilityCounter = new HashMap<>();

		List<TransitStopFacility> childFacilities = new ArrayList<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility
		 */
		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			// limits number of links, for all links within search radius use networkTools.findClosestLinks()
			List<Link> closestLinks = NetworkTools.findNClosestLinks(networkImpl, stopFacility.getCoord(), nodeSearchRadius, maxNclosestLinks, maxLinkFacilityDistance);

			if(closestLinks.size() == 0) {
				closestLinks.addAll(NetworkTools.connectFacilityToNearestNode(stopFacility.getCoord(), network, PREFIX_ARTIFICIAL_LINKS, artificialId++));
			}

			for(Link link : closestLinks) {
				MapUtils.getList(stopFacility, linkCandidates).add(new LinkCandidate(link, stopFacility));
			}
		}

		/**
		 * generate child stop facility for each linkcandidate and reference them
		 */
		for(List<LinkCandidate> linkCandidateList : linkCandidates.values()) {
			for(LinkCandidate linkCandidate : linkCandidateList) {
				int counter = (int) MapUtils.addToInteger(linkCandidate.getParentStop(), childFacilityCounter, 0, 1);

				TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
						Id.create(linkCandidate.getParentStop().getId() + SUFFIX_CHILD_STOPFACILITIES + linkCandidate.getLink().getId() + transportMode, TransitStopFacility.class),
						linkCandidate.getParentStop().getCoord(),
						linkCandidate.getParentStop().getIsBlockingLane()
				);
				newFacility.setLinkId(linkCandidate.getLink().getId());
				newFacility.setName(linkCandidate.getParentStop().getName());
				newFacility.setStopPostAreaId(linkCandidate.getParentStop().getStopPostAreaId());
				childFacilities.add(newFacility);

				linkCandidate.setChildStop(newFacility);
			}
		}

		/**
		 * assign new facilities to schedule // todo maybe move out of constructor?
		 */
		childFacilities.forEach(schedule::addStopFacility);
	}


	public StopFacilityTree(TransitSchedule transitSchedule, Network network, double nodeSearchRadius, int maxNclosestLinks, double maxLinkFacilityDistance) {
		this(transitSchedule, network, "", nodeSearchRadius, maxNclosestLinks, maxLinkFacilityDistance);
	}

		/**
		 * @return A list of link candidates for parentStopFacility
		 */
	public List<LinkCandidate> getLinkCandidates(TransitStopFacility parentStopFacility) {
		return MapUtils.getList(parentStopFacility, linkCandidates);
	}

	/**
	 * For the given transitRoute get the best link candidate for each parent stop
	 * and prepare replacement container.
	 *
	 * @param transitRoute       the transit route
	 * @param bestLinkCandidates a list with all the best link candidates for the
	 *                           parent stops of the route. Parent stops are replaced
	 *                           based on this list in {@link #replaceParentWithChildStopFacilities()}.
	 */
	public void setReplacementPairs(TransitLine transitLine, TransitRoute transitRoute, LinkedList<LinkCandidate> bestLinkCandidates) {
		Map<TransitStopFacility, TransitStopFacility> replacementStopPairs;
		if(replacementMap.containsKey(transitLine)) {
			replacementStopPairs = MapUtils.getMap(transitRoute, replacementMap.get(transitLine));
		} else {
			replacementStopPairs = new HashMap<>();
			Map<TransitRoute, Map<TransitStopFacility, TransitStopFacility>> tmpRouteMap = new HashMap<>();
			tmpRouteMap.put(transitRoute, replacementStopPairs);
			replacementMap.put(transitLine, tmpRouteMap);
		}


		for(LinkCandidate lc : bestLinkCandidates) {
			replacementStopPairs.put(lc.getParentStop(), lc.getChildStop());
		}
	}

	/**
	 * Actually replace the parent stopFacilities with child stopFacilties in the schedule. The replacement pairs
	 * must be set.
	 */
	public void replaceParentWithChildStopFacilities() {
		List<Tuple<TransitLine, TransitRoute>> newRoutes = new ArrayList<>();

		for(Map.Entry<TransitLine, Map<TransitRoute, Map<TransitStopFacility, TransitStopFacility>>> lineEntry : replacementMap.entrySet()) {
			for(Map.Entry<TransitRoute, Map<TransitStopFacility, TransitStopFacility>> routeEntry : lineEntry.getValue().entrySet()) {

				TransitLine line = lineEntry.getKey();
				TransitRoute route = routeEntry.getKey();
				Map<TransitStopFacility, TransitStopFacility> replacementPairs = routeEntry.getValue();
				Id<TransitRoute> oldRouteId = route.getId();
				String oldTransportMode = route.getTransportMode();
				List<TransitRouteStop> oldStopSequence = this.schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getStops();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for(TransitRouteStop stop : oldStopSequence) {
					TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(replacementPairs.get(stop.getStopFacility()), stop.getArrivalOffset(), stop.getDepartureOffset());
					newTransitRouteStop.setAwaitDepartureTime(stop.isAwaitDepartureTime());

					if(newTransitRouteStop.getStopFacility() == null) {
						break;
					}

					newStopSequence.add(newTransitRouteStop);
				}

				TransitRoute newRoute = scheduleFactory.createTransitRoute(oldRouteId, null, newStopSequence, oldTransportMode);

				// add departures
				route.getDepartures().values().forEach(newRoute::addDeparture);

				this.schedule.getTransitLines().get(line.getId()).removeRoute(route);

				newRoutes.add(new Tuple<>(line, newRoute));
			}
		}

		// add transit lines and routes again
		for(Tuple<TransitLine, TransitRoute> entry : newRoutes) {
			this.schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}
	}
}
