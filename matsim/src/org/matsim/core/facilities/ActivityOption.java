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

package org.matsim.core.facilities;

import java.util.SortedSet;

import org.matsim.core.api.internal.MatsimFacilitiesObject;
import org.matsim.core.facilities.OpeningTime.DayType;

/**
 * @author dgrether
 */
public interface ActivityOption extends MatsimFacilitiesObject {
	
	public Double getCapacity();
	
	public void setCapacity(Double cap);

	public void addOpeningTime(OpeningTime openingTime);

	public SortedSet<OpeningTime> getOpeningTimes(DayType day);
	
	public String getType();

}
