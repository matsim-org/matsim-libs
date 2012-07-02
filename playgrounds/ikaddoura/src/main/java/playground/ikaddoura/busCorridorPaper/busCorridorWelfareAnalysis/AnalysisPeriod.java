/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisPeriod.java
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
public class AnalysisPeriod {

	private double start; 
	private double end;
	private int entering;
	private int leaving;
	
	private SortedMap <Id, RouteInfo> routeId2RouteInfo = new TreeMap<Id, RouteInfo>();
	
	public AnalysisPeriod(double start, double end) {
		this.start = start;
		this.end = end;
	}

	public double getStart() {
		return start;
	}

	public double getEnd() {
		return end;
	}

	public void setRouteId2RouteInfo(SortedMap <Id, RouteInfo> routeId2RouteInfo) {
		this.routeId2RouteInfo = routeId2RouteInfo;
	}

	public SortedMap <Id, RouteInfo> getRouteId2RouteInfo() {
		return routeId2RouteInfo;
	}

	public void setEntering(int entering) {
		this.entering = entering;
	}

	public int getEntering() {
		return entering;
	}

	public void setLeaving(int leaving) {
		this.leaving = leaving;
	}

	public int getLeaving() {
		return leaving;
	}

}
