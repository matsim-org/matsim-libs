/* *********************************************************************** *
 * project: org.matsim.*
 * ActOption.java
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

package org.matsim.core.api.facilities;

import java.util.Map;
import java.util.SortedSet;

import org.matsim.api.basic.v01.facilities.BasicActivityOption;
import org.matsim.api.basic.v01.facilities.BasicOpeningTime;
import org.matsim.api.basic.v01.facilities.BasicOpeningTime.DayType;

public interface ActivityOption extends BasicActivityOption {

	@Deprecated
	public void setCapacity(final int capacity);

	public void setOpeningTimes(Map<DayType, SortedSet<BasicOpeningTime>> openingTimes);

	public Facility getFacility();

	public Map<DayType, SortedSet<BasicOpeningTime>> getOpeningTimes();
	
}
