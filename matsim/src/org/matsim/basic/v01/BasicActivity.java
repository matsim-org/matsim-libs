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

import java.util.SortedSet;

import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.interfaces.basic.v01.BasicLocation;


/**
 * @author dgrether
 *
 */
public interface BasicActivity {
	
	public BasicLocation getLocation();
	
	public Integer getFrequency();
	
	public void setFrequency(Integer freq);
	
	public int getCapacity();
	
	public void setCapacity(int cap);

	public void addOpeningTime(DayType day, BasicOpeningTime openingTime);

	public SortedSet<BasicOpeningTime> getOpeningTime(DayType day);
	
}
