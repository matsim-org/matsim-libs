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

package playground.polettif.crossings.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Public transport link with crossings on it
 */
public class RailwayLink {
	
	private Id<Link> ptId;
	private Set<Crossing> crossings = new HashSet<>();

	public RailwayLink(String ptId) {
		this.ptId = Id.createLinkId(ptId);
	}
	
	public void addCrossing(Crossing crossing) {
		this.crossings.add(crossing);
	}
	
	public Id<Link> getId() {
		return ptId;
	}

	public Set<Crossing> getCrossings() {
		return crossings;
	}
}
