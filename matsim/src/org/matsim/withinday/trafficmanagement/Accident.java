/* *********************************************************************** *
 * project: org.matsim.*
 * Accident.java
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

package org.matsim.withinday.trafficmanagement;

import java.util.StringTokenizer;



/**
 * @author dgrether
 *
 */
public class Accident {

	private String linkId;
	
	private double capacityReductionFactor;
	
	private double startTime;
	
	private double endTime;
	
	
	public void setLinkId(final String linkid) {
		this.linkId = linkid;
	}

	public void setCapacityReductionFactor(final double redFactor) {
		this.capacityReductionFactor = redFactor;
	}

	public void setStartTime(final String content) {
		this.startTime = parseTime(content);
	}

	public void setEndTime(final String content) {
		this.endTime = parseTime(content);
	}
	
	private double parseTime(final String time) {
		StringTokenizer tokenizer = new StringTokenizer(time, ":");
		double t = 0.0;
		if (tokenizer.countTokens() == 3) {
			t = Integer.parseInt(tokenizer.nextToken()) * 3600;
		}
		t = t + (Integer.parseInt(tokenizer.nextToken()) * 60);
		t = t + Integer.parseInt(tokenizer.nextToken());
		return t;
	}

	
	/**
	 * @return the linkId
	 */
	public String getLinkId() {
		return this.linkId;
	}

	
	/**
	 * @return the capacityReductionFactor
	 */
	public double getCapacityReductionFactor() {
		return this.capacityReductionFactor;
	}

	
	/**
	 * @return the startTime
	 */
	public double getStartTime() {
		return this.startTime;
	}

	
	/**
	 * @return the endTime
	 */
	public double getEndTime() {
		return this.endTime;
	}
	
	

}
