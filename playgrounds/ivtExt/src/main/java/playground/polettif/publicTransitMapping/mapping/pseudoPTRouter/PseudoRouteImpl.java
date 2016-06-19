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

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.List;

/**
 * A container class for pseudo route stop sequences. Used to store
 * the original transit route and transit line.
 *
 * @author polettif
 */
public class PseudoRouteImpl implements PseudoRoute {

 	private final Id<TransitLine> transitLineId;
	private final List<PseudoRouteStop> pseudoRouteStops;
	private final TransitRoute transitRoute;

	public PseudoRouteImpl(TransitLine transitLine, TransitRoute transitRoute, List<PseudoRouteStop> pseudoRouteStops) {
		this.transitLineId = transitLine.getId();
		this.transitRoute = transitRoute;
		this.pseudoRouteStops = pseudoRouteStops;
	}

	public Id<TransitLine> getTransitLineId() {
		return transitLineId;
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public List<PseudoRouteStop> getPseudoStops() {
		return pseudoRouteStops;
	}
}
