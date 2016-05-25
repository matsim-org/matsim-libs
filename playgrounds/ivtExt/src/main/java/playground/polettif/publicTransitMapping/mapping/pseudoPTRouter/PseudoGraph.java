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


package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A pseudo graph with PseudoRouteStops and PseudoRoutePaths
 * used to calculate the best path and thus link sequence
 * from the first stop to the last stop of a transit route.
 * <p/>
 * Used by {@link DijkstraAlgorithm}
 *
 * @author polettif
 */
public class PseudoGraph {

	protected static Logger log = Logger.getLogger(PseudoGraph.class);


	public static final String SOURCE = "SOURCE";
	public static final String DESTINATION = "DESTINATION";

	public static final PseudoRouteStop SOURCE_PSEUDO_STOP = new PseudoRouteStop(SOURCE);
	public static final PseudoRouteStop DESTINATION_PSEUDO_STOP = new PseudoRouteStop(DESTINATION);

	private final PublicTransitMappingConfigGroup config;
	private final List<PseudoRoutePath> edges;

	public PseudoGraph(PublicTransitMappingConfigGroup configGroup) {
		this.config = configGroup;
		PseudoRoutePath.setConfig(configGroup);
		PseudoRouteStop.setConfig(configGroup);
		this.edges = new ArrayList<>();
	}

	public List<PseudoRoutePath> getEdges() {
		return edges;
	}

	public void addPath(PseudoRoutePath pseudoRoutePath) {
		edges.add(pseudoRoutePath);
	}

	public PseudoRouteStop getSource() {
		return SOURCE_PSEUDO_STOP;
	}

	public PseudoRouteStop getDestination() {
		return DESTINATION_PSEUDO_STOP;
	}

	/**
	 * debug
	 */
	public boolean pathExists(TransitStopFacility currentStopFacility, TransitStopFacility nextStopFacility) {
		for(PseudoRoutePath e : edges) {
			if(!e.getFromPseudoStop().getId().equals("SOURCE") && !e.getFromPseudoStop().getId().equals("DESTINATION") && !e.getToPseudoStop().getId().equals("SOURCE") && !e.getToPseudoStop().getId().equals("DESTINATION")) {
				if(e.getFromPseudoStop().getParentStopFacilityId().equals(currentStopFacility.getId().toString()) && e.getToPseudoStop().getParentStopFacilityId().equals(nextStopFacility.getId().toString())) {
					return true;
				}
			}
		}
		return false;
	}

	public void addSourceDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidate> linkCandidates) {
		for(LinkCandidate lc : linkCandidates) {
			edges.add(new PseudoRoutePath(SOURCE_PSEUDO_STOP, new PseudoRouteStop(order, routeStop, lc), 1.0, true));
		}
	}

	public void addDestinationDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidate> linkCandidates) {
		for(LinkCandidate lc : linkCandidates) {
			PseudoRouteStop s = new PseudoRouteStop(order, routeStop, lc);
			PseudoRoutePath p = new PseudoRoutePath(s, DESTINATION_PSEUDO_STOP, 1.0, true);
			edges.add(p);
			log.debug(p.getId() + " added");
			log.debug(edges.size());
		}
	}
}


