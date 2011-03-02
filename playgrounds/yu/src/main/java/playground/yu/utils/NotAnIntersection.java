/* *********************************************************************** *
 * project: org.matsim.*
 * NotAnIntersection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * recognizes the real intersection in network
 * 
 * @author yu
 * 
 */
public class NotAnIntersection {

	/***/
	public static boolean notAnIntersection(Node node) {

		return getIncidentNodeIds(node).size() <= 2;

	}

	private static Set<Id> getIncidentNodeIds(final Node node) {
		Set<Id> ids = new HashSet<Id>();
		for (Link inLink : node.getInLinks().values()) {
			ids.add(inLink.getFromNode().getId());
		}
		for (Link outLink : node.getOutLinks().values()) {
			ids.add(outLink.getToNode().getId());
		}
		return ids;
	}
}
