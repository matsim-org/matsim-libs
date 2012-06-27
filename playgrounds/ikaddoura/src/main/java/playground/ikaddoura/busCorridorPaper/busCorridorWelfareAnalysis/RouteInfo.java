/* *********************************************************************** *
 * project: org.matsim.*
 * RouteInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class RouteInfo {

	private Id routeId;
	private SortedMap<Id, FacilityLoadInfo> transitStopId2FacilityLoadInfo = new TreeMap<Id, FacilityLoadInfo>();

	public RouteInfo(Id id) {
		this.routeId = id;
	}

	public Id getRouteId() {
		return routeId;
	}

	public void setTransitStopId2FacilityLoadInfo(SortedMap<Id, FacilityLoadInfo> id2FacilityLoadInfo) {
		this.transitStopId2FacilityLoadInfo = id2FacilityLoadInfo;
	}

	public SortedMap<Id, FacilityLoadInfo> getTransitStopId2FacilityLoadInfo() {
		return transitStopId2FacilityLoadInfo;
	}

}
