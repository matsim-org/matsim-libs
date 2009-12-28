/* *********************************************************************** *
 * project: org.matsim.*
 * IdMapping.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.johannes.plans.view.Facility;

/**
 * @author illenberger
 *
 */
public class IdMapping {

	public static Network network;
	
	public static Node getNode(Id id) {
		return network.getNodes().get(id);
	}
	
	public static Id getId(Node node) {
		return node.getId();
	}
	
	public static Link getLink(Id id) {
		return network.getLinks().get(id);
	}
	
	public static Id getId(Link link) {
		return link.getId();
	}
	
	public static Facility getFacility(Id id) {
		return null;
	}
	
	public static Id getId(Facility facility) {
		return null;
	}
	
}
