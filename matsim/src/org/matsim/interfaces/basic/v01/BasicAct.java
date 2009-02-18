/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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

package org.matsim.interfaces.basic.v01;

import org.matsim.utils.geometry.Coord;

/**
 *
 * @author dgrether
 */
public interface BasicAct {

	public double getEndTime();

	public void setEndTime(final double seconds);

	/**
	 * Activity type is, until further notice, defined via the config file.
	 * 
	 * @return
	 */
	public String getType();

	public void setType(final String type);

	public Coord getCoord();

	public void setCoord(Coord coordinates);

	public double getStartTime();

	public void setStartTime(double seconds);

	public Id getLinkId();

	public Id getFacilityId();

	@Deprecated
	public double getDuration();
	@Deprecated
	public void setDuration(double duration);

}