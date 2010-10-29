/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level0;

import org.matsim.api.core.v01.Id;

/**
 * Stores a couple of departure related information, when leaving a pt interaction activity
 * 
 * @author aneumann
 *
 */
public class AgentId2PlannedDepartureTimeMapData {
	
	private final Id stopId;
	private final double plannedDepartureTime;
	private final Id lineId;
	private final Id routeId;
	
	public AgentId2PlannedDepartureTimeMapData(Id stopId, double plannedDepartureTime, Id lineId, Id routeId){
		this.stopId = stopId;
		this.plannedDepartureTime = plannedDepartureTime;
		this.lineId = lineId;
		this.routeId = routeId;
	}

	public Id getStopId() {
		return this.stopId;
	}

	public double getPlannedDepartureTime() {
		return this.plannedDepartureTime;
	}

	public Id getLineId() {
		return this.lineId;
	}

	public Id getRouteId() {
		return this.routeId;
	}

}
