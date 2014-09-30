/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
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
package playground.juliakern.responsibilityOffline;

import org.matsim.api.core.v01.Id;

/**
 * simple class to store information on agents' (car) trips
 * used to calculate exposure values 
 * @author julia
 */
public class EmCarTrip {
	Double startTime;
	Double endTime;
	Id personId;
	Id linkId;
	
	public EmCarTrip(Double startOfTimeInterval, Double endOfTimeInterval, 	Id personId, Id linkId){
		this.startTime=startOfTimeInterval;
		this.endTime=endOfTimeInterval;
		this.personId=personId;
		this.linkId=linkId;
	}

	public Double getStartTime() {
		return this.startTime;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public Double getEndTime() {
		return this.endTime;
	}

}
