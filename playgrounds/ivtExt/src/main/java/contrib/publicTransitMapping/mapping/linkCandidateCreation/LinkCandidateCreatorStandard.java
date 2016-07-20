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

package contrib.publicTransitMapping.mapping.linkCandidateCreation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import contrib.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import contrib.publicTransitMapping.config.PublicTransitMappingStrings;
import contrib.publicTransitMapping.mapping.networkRouter.Router;
import contrib.publicTransitMapping.tools.MiscUtils;
import contrib.publicTransitMapping.tools.NetworkTools;

import java.util.*;

/**
 * @author polettif
 */
public class LinkCandidateCreatorStandard implements LinkCandidateCreator {

	protected static Logger log = Logger.getLogger(LinkCandidateCreatorStandard.class);

	private static final Set<String> loopLinkModes = CollectionUtils.stringToSet(PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE+","+ PublicTransitMappingStrings.STOP_FACILITY_LOOP_LINK);
	private final Map<String, Router> modeSeparatedRouters;

	private Map<String, PublicTransitMappingConfigGroup.LinkCandidateCreatorParams> lccParams;

	private final TransitSchedule schedule;
	private final Network network;
	private final PublicTransitMappingConfigGroup config;

	private final Map<String, Map<Id<TransitStopFacility>, SortedSet<LinkCandidate>>> linkCandidates = new HashMap<>();
	private final Set<Tuple<TransitStopFacility, String>> loopLinks = new HashSet<>();

	public LinkCandidateCreatorStandard(TransitSchedule schedule, Network network, PublicTransitMappingConfigGroup config, Map<String, Router> modeSeparatedRouters) {
		this.schedule = schedule;
		this.network = network;
		this.config = config;
		this.modeSeparatedRouters = modeSeparatedRouters;
	}

	@Override
	public void createLinkCandidates() {
		log.info("   search radius: " + config.getNodeSearchRadius());
		log.info("   Note: loop links for stop facilities are created if no link candidate can be found.");

		lccParams = config.getLinkCandidateCreatorParams();

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();

					if(!lccParams.containsKey(scheduleTransportMode)) {
						throw new IllegalArgumentException("No LinkCandidateCreatorParams defined for schedule mode " + scheduleTransportMode);
					}

					PublicTransitMappingConfigGroup.LinkCandidateCreatorParams param = lccParams.get(scheduleTransportMode);

					Router modeRouter = modeSeparatedRouters.get(scheduleTransportMode);
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					SortedSet<LinkCandidate> modeLinkCandidates = MiscUtils.getSortedSet(stopFacility.getId(), MapUtils.getMap(scheduleTransportMode, linkCandidates));

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {
						// if stop facilty already has a referenced link
						if(stopFacility.getLinkId() != null) {
							Link link = network.getLinks().get(stopFacility.getLinkId());
							modeLinkCandidates.add(new LinkCandidateImpl(link, stopFacility, modeRouter.getLinkTravelCost(link)));
						// search for close links
						} else {
							List<Link> closestLinks;
							if(param.useArtificialLoopLink()) {
								closestLinks = new ArrayList<>();
							} else {
								closestLinks  = NetworkTools.findClosestLinks(network,
										stopFacility.getCoord(), config.getNodeSearchRadius(),
										param.getMaxNClosestLinks(), param.getLinkDistanceTolerance(),
										param.getNetworkModes(), param.getMaxLinkCandidateDistance());
							}

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
								modeLinkCandidates.add(new LinkCandidateImpl(link, stopFacility, modeRouter.getLinkTravelCost(link)));
							}
						}
					}
				}
			}
		}

		/**
		 * Add manually set link candidates from config
		 */
		if(config.getManualLinkCandidateCsvFile() != null) {
			config.loadManualLinkCandidatesCsv();
		}
		addManualLinkCandidates(config.getManualLinkCandidates());

	}

	private void addManualLinkCandidates(Set<PublicTransitMappingConfigGroup.ManualLinkCandidates> manualLinkCandidatesSet) {
		for(PublicTransitMappingConfigGroup.ManualLinkCandidates manualCandidates : manualLinkCandidatesSet) {

			Set<String> scheduleModes = manualCandidates.getScheduleModes();
			if(scheduleModes.size() == 0) {
				scheduleModes = linkCandidates.keySet();
			}

			TransitStopFacility parentStopFacility = manualCandidates.getStopFacilityId() != null ? schedule.getFacilities().get(manualCandidates.getStopFacilityId()) : null;
			if(parentStopFacility == null && manualCandidates.getStopFacilityId() != null) {
				log.warn("stopFacility id " + manualCandidates.getStopFacilityId() + " not available in schedule. Manual link candidates are for this facility are ignored.");
			}

			if(parentStopFacility != null) {
				for(String scheduleMode : scheduleModes) {
					Router modeRouter = modeSeparatedRouters.get(scheduleMode);

					PublicTransitMappingConfigGroup.LinkCandidateCreatorParams lccParams = config.getLinkCandidateCreatorParams().get(scheduleMode);

					SortedSet<LinkCandidate> lcSet = (manualCandidates.replaceCandidates() ? new TreeSet<>() : MiscUtils.getSortedSet(parentStopFacility.getId(), MapUtils.getMap(scheduleMode, linkCandidates)));
					for(Id<Link> linkId : manualCandidates.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						if(link == null) {
							log.warn("link " + linkId + " not found in network.");
						} else {
							if(CoordUtils.calcEuclideanDistance(link.getCoord(), parentStopFacility.getCoord()) > lccParams.getMaxLinkCandidateDistance()) {
								log.warn("Distance from manual link candidate " + link.getId() + " to stop facility " +
										manualCandidates.getStopFacilityIdStr() + " is more than " + lccParams.getMaxLinkCandidateDistance() +
										"("+CoordUtils.calcEuclideanDistance(link.getCoord(), parentStopFacility.getCoord())+")");
								log.info("Manual link candidate will still be used");
							}
							lcSet.add(new LinkCandidateImpl(link, parentStopFacility, modeRouter.getLinkTravelCost(link)));
						}
					}
					MapUtils.getMap(scheduleMode, linkCandidates).put(parentStopFacility.getId(), lcSet);
				}
			}
		}

	}

	@Override
	public SortedSet<LinkCandidate> getLinkCandidates(Id<TransitStopFacility> transitStopFacility, String scheduleTransportMode) {
		return linkCandidates.get(scheduleTransportMode).get(transitStopFacility);
	}

}