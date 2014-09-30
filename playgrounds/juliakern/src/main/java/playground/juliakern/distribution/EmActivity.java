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
package playground.juliakern.distribution;

import org.matsim.api.core.v01.Id;

/**
 * simple class to store information on agents' activities
 * used to calculate exposure values
 * @author julia 
 */

public class EmActivity {
	Double startOfActivity;
	Double endOfActivity;
	Id personId;
	int xBin;
	int yBin;
	String activityType;

	public EmActivity(Double startOfActivity,	Double endOfActivity, Id personId, int xBin, int yBin, String activityType){
		this.startOfActivity=startOfActivity;
		this.endOfActivity=endOfActivity;
		this.personId=personId;
		this.xBin=xBin;
		this.yBin=yBin;
		this.activityType = activityType;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public double getStartTime() {
		return this.startOfActivity;
	}

	public Double getEndTime() {
		return this.endOfActivity;
	}

	public String getActivityType() {
		return this.activityType;
	}

	public int getXBin() {
		return this.xBin;
	}
	public int getYBin() {
		return this.yBin;
	}

	public Double getDuration() {
		return endOfActivity-startOfActivity;
	}
}
