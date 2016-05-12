/* *********************************************************************** *
 * project: org.matsim.*
 * StopTime.java
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

/**
 * Container for GTFS StopTime. Contains stopId, arrivalTime and departureTime
 */
public class StopTime {
	
	//Attributes
	private Integer sequencePosition;
	private Date arrivalTime;
	private Date departureTime;
	private String stopId;
	
	//Methods
	/**
	 * @param arrivalTime
	 * @param departureTime
	 * @param stopId
	 */
	public StopTime(Integer sequencePosition, Date arrivalTime, Date departureTime, String stopId) {
		super();
		this.sequencePosition = sequencePosition;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.stopId = stopId;
	}

	/**
	 * @return the position of the stopTime within the stopSequence
	 */
	public Integer getSeuencePosition() {
		return sequencePosition;
	}

	/**
	 * @return the arrivalTime
	 */
	public Date getArrivalTime() {
		return arrivalTime;
	}

	/**
	 * @return the departureTime
	 */
	public Date getDepartureTime() {
		return departureTime;
	}

	/**
	 * @return the stopId
	 */
	public String getStopId() {
		return stopId;
	}

	/**
	 * @param stopId the stopId to set
	 */
	public void setStopId(String stopId) {
		this.stopId = stopId;
	}
	
}
