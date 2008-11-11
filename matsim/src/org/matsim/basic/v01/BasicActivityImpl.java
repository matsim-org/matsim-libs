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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.interfaces.basic.v01.BasicLocation;


/**
 * @author dgrether
 *
 */
public class BasicActivityImpl implements BasicActivity {
	
	private Integer capacity = null;
	private BasicLocation location;
	private Map<DayType,SortedSet<BasicOpeningTime>> openingTimes = new HashMap<DayType, SortedSet<BasicOpeningTime>>();
	private Integer frequency = null;
	private String type;
	
	public BasicActivityImpl(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}
	
	public Integer getCapacity() {
		return this.capacity;
	}
	
	public Integer getFrequency() {
		return this.frequency;
	}
	
	public void setFrequency(Integer freq) {
		this.frequency = freq;
	}

	public BasicLocation getLocation() {
		return this.location;
	}

	public void setCapacity(Integer cap) {
		this.capacity = cap;
	}

	public SortedSet<BasicOpeningTime> getOpeningTime(DayType day) {
		return this.openingTimes.get(day);
	}
	

	public void addOpeningTime(BasicOpeningTime openingTime) {
		DayType day = openingTime.getDay();
		if (day == null) {
			throw new IllegalArgumentException("OpeningTime instance must have a day set!");
		}
		SortedSet<BasicOpeningTime> ot;
		if (!this.openingTimes.containsKey(day)) {
			ot = new TreeSet<BasicOpeningTime>();
			this.openingTimes.put(day, ot);
		}
		else {
			ot = this.openingTimes.get(day);
		}
		ot.add(openingTime);
	}

	public void setLocation(BasicLocation location) {
		this.location = location;
	}


	
	
	

}
