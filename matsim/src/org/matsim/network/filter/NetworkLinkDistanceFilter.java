/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.network.filter;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;

/**
 * @author dgrether
 */
public class NetworkLinkDistanceFilter implements NetworkLinkFilter {

	private final double distanceFilter;
	private final Node distanceFilterNode;
	
	/**
	 * Extract all links with a distance (in m) smaller than the distance parameter 
	 * from the given centerNode.
	 * @param distance
	 * @param centerNode
	 */
	public NetworkLinkDistanceFilter (final double distance, final Node centerNode) {
		this.distanceFilter = distance;
		this.distanceFilterNode = centerNode;
	}
	
	/**
	 *
	 * @param l
	 * @return <code>true</true> if the Link is not farther away than the
	 * distance specified by the distance filter from the center node of the filter.
	 */	
	public boolean judgeLink(Link l) {
		double dist = l.getCenter().calcDistance(this.distanceFilterNode.getCoord());
		return dist < this.distanceFilter;
	}
		
}
