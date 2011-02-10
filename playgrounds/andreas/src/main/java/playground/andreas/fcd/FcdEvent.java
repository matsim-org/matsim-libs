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

package playground.andreas.fcd;

import org.matsim.api.core.v01.Id;

public class FcdEvent {
	
	private final double time;
	private final Id linkId;
	private final double averageSpeed;
	private final double linkCoverage;
	private final Id vehId;
	private final int minuteOfWeek;
	
	public FcdEvent(double time, Id linkId, double averageSpeed, double linkCoverage, Id vehId, int minuteOfWeek){
		this.time = time;
		this.linkId = linkId;
		this.averageSpeed = averageSpeed;
		this.linkCoverage = linkCoverage;
		this.vehId = vehId;
		this.minuteOfWeek = minuteOfWeek;
	}

	public double getTime() {
		return this.time;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public double getAverageSpeed() {
		return this.averageSpeed;
	}

	public double getLinkCoverage() {
		return this.linkCoverage;
	}

	public Id getVehId() {
		return this.vehId;
	}

	public int getMinuteOfWeek() {
		return this.minuteOfWeek;
	}

	
}
