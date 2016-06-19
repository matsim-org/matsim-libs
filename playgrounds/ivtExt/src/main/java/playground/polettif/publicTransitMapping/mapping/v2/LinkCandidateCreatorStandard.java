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

package playground.polettif.publicTransitMapping.mapping.v2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.LinkCandidateImpl;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.*;

public class LinkCandidateCreatorStandard implements LinkCandidateCreator {

	protected static Logger log = Logger.getLogger(LinkCandidateCreatorStandard.class);

	private static Set<String> loopLinkModes = CollectionUtils.stringToSet(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE+","+PublicTransitMappingConfigGroup.STOP_FACILITY_LOOP_LINK);

	private TransitSchedule schedule;
	private Network network;
	private PublicTransitMappingConfigGroup config;

	private Map<String, Map<TransitStopFacility, Set<LinkCandidateImpl>>> linkCandidates = new HashMap<>();
	private Set<Tuple<TransitStopFacility, String>> loopLinks = new HashSet<>();

	public LinkCandidateCreatorStandard(TransitSchedule schedule, Network network, PublicTransitMappingConfigGroup config) {
		this.schedule = schedule;
		this.network = network;
		this.config = config;
	}

	public void createLinkCandidates() {
		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					Set<LinkCandidateImpl> modeLinkCandidates = MapUtils.getSet(stopFacility, MapUtils.getMap(scheduleTransportMode, linkCandidates));

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {

						// if stop facilty already has a referenced link
						if(stopFacility.getLinkId() != null) {
							modeLinkCandidates.add(new LinkCandidateImpl(network.getLinks().get(stopFacility.getLinkId()), stopFacility));
						} else {
							// limits number of links, for all links within search radius
							List<Link> closestLinks = NetworkTools.findClosestLinks(network,
									stopFacility.getCoord(), config.getNodeSearchRadius(),
									config.getMaxNClosestLinks(), config.getLinkDistanceTolerance(),
									config.getModeRoutingAssignment().get(scheduleTransportMode), config.getMaxLinkCandidateDistance());

							// if no close links are nearby, a loop link is created and referenced to the facility.
							if(closestLinks.size() == 0) {
								Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network, config.getPrefixArtificial(), 20, loopLinkModes);
								closestLinks.add(loopLink);
								loopLinks.add(new Tuple<>(stopFacility, scheduleTransportMode));
							}

							/**
							 * generate a LinkCandidate for each close link
							 */
							for(Link link : closestLinks) {
								modeLinkCandidates.add(new LinkCandidateImpl(link, stopFacility));
							}
						}
					}
				}
			}
		}
	}

	@Override
	public Set<LinkCandidateImpl> getLinkCandidates(String scheduleTransportMode, TransitStopFacility transitStopFacility) {
		return linkCandidates.get(scheduleTransportMode).get(transitStopFacility);
	}

	@Override
	public void addManualLinkCandidates(Set<PublicTransitMappingConfigGroup.ManualLinkCandidates> manualLinkCandidatesSet) {
		for(PublicTransitMappingConfigGroup.ManualLinkCandidates manualCandidates : manualLinkCandidatesSet) {

			Set<String> modes = manualCandidates.getModes();
			if(modes.size() == 0) {
				modes = linkCandidates.keySet();
			}

			TransitStopFacility parentStopFacility = schedule.getFacilities().get(manualCandidates.getStopFacilityId());
			if(parentStopFacility == null) {
				log.warn("stopFacility id " + manualCandidates.getStopFacilityId() + " not available in schedule. Manual link candidates are ignored.");
			} else {
				for(String mode : modes) {
					Set<LinkCandidateImpl> lcSet = (manualCandidates.replaceCandidates() ? new HashSet<>() : MapUtils.getSet(parentStopFacility, MapUtils.getMap(mode, linkCandidates)));
					for(Id<Link> linkId : manualCandidates.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						if(link == null) {
							log.warn("link " + linkId + " not found in network.");
						} else {
							if(CoordUtils.calcEuclideanDistance(link.getCoord(), parentStopFacility.getCoord()) > config.getMaxLinkCandidateDistance()) {
								log.warn("Distance from manual link candidate " + link.getId() + " to stop facility " +
										manualCandidates.getStopFacilityIdStr() + " is more than " + config.getMaxLinkCandidateDistance() +
										"("+CoordUtils.calcEuclideanDistance(link.getCoord(), parentStopFacility.getCoord())+")");
								log.info("Manual link candidate will still be used");
							}
							lcSet.add(new LinkCandidateImpl(link, parentStopFacility));
						}
					}
					MapUtils.getMap(mode, linkCandidates).put(parentStopFacility, lcSet);
				}
			}
		}

	}

	@Override
	public boolean stopFacilityOnlyHasLoopLink(TransitStopFacility stopFacility, String transportMode) {
		return loopLinks.contains(new Tuple<>(stopFacility, transportMode));
	}
	
}