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

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author tschlenther
 */
public class MostCentralDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector{

	private final Map<DrtZone,Link> targetLinks = new HashMap<>();

	public MostCentralDrtZoneTargetLinkSelector(DrtZonalSystem drtZonalSystem){
		drtZonalSystem.getZones().values().stream().forEach(zone -> {
			Double minDistance = Double.MAX_VALUE;
			Link closestLink = null;
			for(Link link : zone.getLinks()){
				// vehicle will be standing at the toNode, this is why we use toNode rather than getCoord
				double dist = NetworkUtils.getEuclideanDistance(zone.getCentroid(), link.getToNode().getCoord());
				if(dist < minDistance){
					minDistance = dist;
					closestLink = link;
				}
			}
			Preconditions.checkNotNull(closestLink, "could not determine most central link for zone %s ", zone);
			this.targetLinks.put(zone, closestLink);
		});
	}

	@Override public Link selectTargetLink(DrtZone zone) { return this.targetLinks.get(zone); }
}
