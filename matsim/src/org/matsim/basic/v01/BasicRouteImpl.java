/* *********************************************************************** *
 * project: org.matsim.*
 * BasicRoute.java
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

package org.matsim.basic.v01;

import java.util.List;

import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.misc.Time;

public class BasicRouteImpl <T extends BasicNode> implements BasicRoute<T>{
	
	
	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;

	private List<Id> linkIds;

	public double getDist() {
		return dist;
	}

	public final void setDist(final double dist) {
		this.dist = dist;
	}

	public final double getTravTime() {
		return this.travTime;
	}
	
	public final void setTravTime(final double travTime) {
		this.travTime = travTime;
	}

	public void setLinkIds(List<Id> linkids){
		this.linkIds = linkids;
	}
	
	public List<Id> getLinkIds() {
		return this.linkIds;
	}

}
