/* *********************************************************************** *
 * project: org.matsim.*
 * RawRouteImpl.java
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
package playground.johannes.plans.plain.impl;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.johannes.plans.plain.PlainRoute;

/**
 * @author illenberger
 *
 */
public class PlainRouteImpl extends AbstractModifiable implements PlainRoute {

	private List<Id> nodeIds;
	
	public List<Id> getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(List<Id> linkIds) {
		this.nodeIds = Collections.unmodifiableList(linkIds);
		modified();
	}

}
