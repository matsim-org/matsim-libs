/* *********************************************************************** *
 * project: org.matsim.*
 * Frequency.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs.containers;

import java.util.Date;

public class Frequency {
	
	//Attributes
	private final Date startTime;
	private final Date endTime;
	private final int secondsPerDeparture;
	
	//Methods
	/**
	 * @param startTime
	 * @param endTime
	 * @param secondsPerDeparture
	 */
	public Frequency(Date startTime, Date endTime, int secondsPerDeparture) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.secondsPerDeparture = secondsPerDeparture;
	}
	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}
	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}
	/**
	 * @return the secondsPerDeparture
	 */
	public int getSecondsPerDeparture() {
		return secondsPerDeparture;
	}
	
}
