/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLinkSlopes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class CalcLinkSlopes {

	public static final String ATTRIBUTE_NAME = "zCoordinate";
	
	/**
	 * If height information for a network is available, the slope of each link
	 * is calculated and returned in a Map<LinkId, Slope in %>.
	 *  
	 * It is assumed that the heightInformation ObjectAttributes contain a field
	 * "zCoordinate" for each node Id in the network.
	 * 
	 * @return Map<LinkId, Slope in %>
	 */
	public Map<Id, Double> calcLinkSlopes(Network network, ObjectAttributes heightInformation) {
		Map<Id, Double> linkSlopes = new HashMap<Id, Double>();
		
		this.calcLinkSlopes(network, heightInformation, linkSlopes);
		
		return linkSlopes;
	}
	
	private void calcLinkSlopes(Network network, ObjectAttributes heightInformation, Map<Id, Double> linkSlopes) {
		for (Link link : network.getLinks().values()) {
			calcLinkSlope(link, heightInformation, linkSlopes);
		}
	}
	
	private void calcLinkSlope(Link link, ObjectAttributes heightInformation, Map<Id, Double> linkSlopes) {
		
		double slope = 0.0;
		double length = link.getLength();
		if (length > 0.0) {
			
			String fromHeightString = heightInformation.getAttribute(link.getFromNode().getId().toString(), ATTRIBUTE_NAME).toString();
			String toHeightString = heightInformation.getAttribute(link.getToNode().getId().toString(), ATTRIBUTE_NAME).toString();
			
			/*
			 * If height information is available, calculate the link's slope.
			 */
			if (fromHeightString == null || toHeightString == null) return;
			
			Double fromHeight = Double.valueOf(fromHeightString);
			Double toHeight = Double.valueOf(toHeightString);
			
			double dHeight = toHeight - fromHeight;
			slope = dHeight / length;

			// convert slope to % and add it to the map
			linkSlopes.put(link.getId(), 100 * slope);
		} else linkSlopes.put(link.getId(), 0.0);	// link length is 0.0, therefore no slope
	}
}
