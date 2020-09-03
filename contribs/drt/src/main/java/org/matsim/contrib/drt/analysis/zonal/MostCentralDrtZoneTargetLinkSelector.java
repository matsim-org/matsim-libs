/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

public class MostCentralDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector{

	Map<DrtZone,Link> targetLinks = new HashMap<>();

	@Override
	public Link selectTargetLinkFor(DrtZone zone) {
		if(this.targetLinks.containsKey(zone)) return this.targetLinks.get(zone);

		Double minDistance = Double.MAX_VALUE;
		Link closestLink = null;
		for(Link link : zone.getLinks().values()){
			// vehicle will be standing at the toNode, this is why we use toNode rather than getCoord
			double dist = NetworkUtils.getEuclideanDistance(zone.getCentroid(), link.getToNode().getCoord());
			if(dist < minDistance){
				minDistance = dist;
				closestLink = link;
			}
		}
		if(closestLink == null) throw new RuntimeException("could not determin most central link for zone " + zone);
		this.targetLinks.put(zone, closestLink);
		return closestLink;
	}
}
