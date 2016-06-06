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


import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.HashSet;
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

	private final PublicTransitMappingConfigGroup config;

	private final Set<PseudoRoutePath> edges;

	public static final PseudoRouteStop SOURCE = new PseudoRouteStop("SOURCE");
	public static final PseudoRouteStop DESTINATION = new PseudoRouteStop("DESTINATION");

	public PseudoGraph(PublicTransitMappingConfigGroup configGroup) {
		this.config = configGroup;
		PseudoRoutePath.setConfig(configGroup);
		PseudoRouteStop.setConfig(configGroup);
		this.edges = new HashSet<>();
	}

	public Set<PseudoRoutePath> getEdges() {
		return edges;
	}

	public void addPath(PseudoRoutePath pseudoRoutePath, boolean firstStop, boolean lastStop) {
		edges.add(pseudoRoutePath);

		// add dummy paths before and after route
		if(firstStop) {
			edges.add(new PseudoRoutePath(SOURCE, pseudoRoutePath.getFromPseudoStop(), 1.0, true));
		}
		if(lastStop) {
			edges.add(new PseudoRoutePath(pseudoRoutePath.getToPseudoStop(), DESTINATION, 1.0, true));
		}
	}

	public PseudoRouteStop getSource() {
		return SOURCE;
	}

	public PseudoRouteStop getDestination() {
		return DESTINATION;
	}

	/**
	 * debug
	 */
	public boolean pathExists(TransitStopFacility currentStopFacility, TransitStopFacility nextStopFacility) {
		for(PseudoRoutePath e : edges) {
			if(!e.getFromPseudoStop().getName().equals("SOURCE") && !e.getFromPseudoStop().getName().equals("DESTINATION") && !e.getToPseudoStop().getName().equals("SOURCE") && !e.getToPseudoStop().getName().equals("DESTINATION")) {
				if(e.getFromPseudoStop().getParentStopFacilityId().equals(currentStopFacility.getId().toString()) && e.getToPseudoStop().getParentStopFacilityId().equals(nextStopFacility.getId().toString())) {
					return true;
				}
			}
		}
		return false;
	}
}


