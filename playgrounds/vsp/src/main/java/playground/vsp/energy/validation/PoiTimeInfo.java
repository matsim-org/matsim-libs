/* *********************************************************************** *
 * project: org.matsim.*
 * PoiTimeInfo
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
package playground.vsp.energy.validation;

import java.util.GregorianCalendar;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author dgrether
 *
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(propOrder = {"startTime", "endTime", "usedCapacity"})
public class PoiTimeInfo {

	private GregorianCalendar startTime;

	private GregorianCalendar endTime;

	private Double usedCapacity;

	@XmlElement(name="start_time", required=true)
	public GregorianCalendar getStartTime() {
		return startTime;
	}

	
	public void setStartTime(GregorianCalendar startTime) {
		this.startTime = startTime;
	}

	@XmlElement(name="end_time", required=true)
	public GregorianCalendar getEndTime() {
		return endTime;
	}

	
	public void setEndTime(GregorianCalendar endTime) {
		this.endTime = endTime;
	}

	
	@XmlElement(name="occupancy", required=true)
	public Double getUsedCapacity() {
		return usedCapacity;
	}

	public void setUsedCapacity(Double usedCapacity) {
		this.usedCapacity = usedCapacity;
	}

	
	
}
