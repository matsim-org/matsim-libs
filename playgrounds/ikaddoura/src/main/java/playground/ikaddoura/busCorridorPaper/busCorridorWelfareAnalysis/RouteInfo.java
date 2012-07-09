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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class RouteInfo {

	private Id routeId;
	private Map<Id, FacilityLoadInfo> transitStopId2FacilityLoadInfo = new HashMap<Id, FacilityLoadInfo>();
	private int passengersAllVeh;
	private boolean test = false;
	private List<Id> stopIDs = new ArrayList<Id>();
	
	public RouteInfo(Id id) {
		this.routeId = id;
	}

	public Id getRouteId() {
		return routeId;
	}

	public void setTransitStopId2FacilityLoadInfo(Map<Id, FacilityLoadInfo> id2FacilityLoadInfo) {
		this.transitStopId2FacilityLoadInfo = id2FacilityLoadInfo;
	}

	public Map<Id, FacilityLoadInfo> getTransitStopId2FacilityLoadInfo() {
		return transitStopId2FacilityLoadInfo;
	}

	public void setPassengersAllVeh(int passengersAllVeh) {
		this.passengersAllVeh = passengersAllVeh;
	}

	public int getPassengersAllVeh() {
		return passengersAllVeh;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public boolean isTest() {
		return test;
	}

	public void setStopIDs(List<Id> routeIDs) {
		this.stopIDs = routeIDs;
	}

	public List<Id> getStopIDs() {
		return stopIDs;
	}

}
