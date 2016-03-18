/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.crossings;

public class LinkChangeEvent {
	
	private String linkId1;
	private String linkId2;
	private String starttime;
	private String stoptime;
	private String capacity;
     
	public LinkChangeEvent(String linkId1, String linkId2, String starttime, String stoptime, String capacity) {
		this.linkId1 = linkId1;
		this.linkId2 = linkId2;
		this.starttime = starttime;
		this.stoptime = stoptime;
		this.capacity = capacity;
	}
	
	public String getLinkId1() {
		return linkId1;
	}
	
	public String getLinkId2() {
		return linkId2;
	}
	
	public String getStarttime() {
		return starttime;
	}
	
	public String getStoptime() {
		return stoptime;
	}
	
	public String getCapacity() {
		return capacity;
	}
}
