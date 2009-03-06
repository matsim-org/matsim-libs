/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 */
public interface BasicActivityOption {
	// FIXME: needs to go into basic interfaces
	
	/**
	 * @deprecated If anything, then the facility has a location
	 */
	@Deprecated
	public BasicLocation getLocation();
	
	public Integer getCapacity();
	// FIXME this should use Double
	
	public void setCapacity(Integer cap);
	// FIXME this should use Double

	public void addOpeningTime(BasicOpeningTime openingTime);

	public SortedSet<BasicOpeningTime> getOpeningTimes(DayType day);
	
	public String getType();

}
