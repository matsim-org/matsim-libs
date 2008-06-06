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

package org.matsim.basic.v01;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.utils.geometry.CoordI;

/**
 *
 * @author dgrether
 */
public interface BasicAct {

	public BasicLink getLink();

	public void setLink(final BasicLink link);

	public Facility getFacility();

	public void setFacility(final Facility facility);

	public double getEndTime();

	public void setEndTime(final double endTime);

	public String getType();

	public void setType(final String type);

	public CoordI getCoord();

	public void setCoord(CoordI coordinates);

	public double getStartTime();

	public void setStartTime(double time);

//	public double getDuration();

//	public void setDuration(double duration);

}