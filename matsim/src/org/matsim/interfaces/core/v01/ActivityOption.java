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

package org.matsim.interfaces.core.v01;

import java.util.Map;
import java.util.SortedSet;

import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.interfaces.basic.v01.facilities.BasicActivityOption;

public interface ActivityOption extends BasicActivityOption {

// not used (I think).  kn, mar09	
//	public boolean containsOpentime(final BasicOpeningTime o);

	@Deprecated
	public void setCapacity(final int capacity);

	public void setOpentimes(Map<DayType, SortedSet<BasicOpeningTime>> opentimes);

	public Facility getFacility();

	public Map<DayType, SortedSet<BasicOpeningTime>> getOpentimes();
	// FIXME: Opentimes vs. Openingtimes, and sometimes they are maps and sometimes sets!!!!
	
}
