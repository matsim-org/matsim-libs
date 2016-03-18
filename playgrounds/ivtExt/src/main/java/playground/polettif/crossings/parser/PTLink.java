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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/*
 * Public transport link with crossings on it
 */
public class PTLink {
	
	private Id<Link> ptId;
	private ArrayList<String> crossingIds = new ArrayList<>();

	public PTLink(String ptId) {
		this.ptId = Id.createLinkId(ptId);
	}
	
	public void addCrossingLink(String crossingId) {
		this.crossingIds.add(crossingId);
	}
	
	public Id<Link> getId() {
		return ptId;
	}
	
	public List<String> getCrossingLinks() {
		return crossingIds;
	}
 	
}
