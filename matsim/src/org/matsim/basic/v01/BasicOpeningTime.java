/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.basic.v01;


/**
 * @author dgrether
 *
 */
public interface BasicOpeningTime {
	
	public enum DayType {mon, tue, wed, thu, fri, sat, sun, wkday, wkend, wk};
	
	public DayType getDay();
	
	public void setDay(DayType day);
	
	public double getStartTime();
	
	public void setStartTime(double starttime);
	
	public double getEndTime();
	
	public void setEndTime(double endtime);
}
