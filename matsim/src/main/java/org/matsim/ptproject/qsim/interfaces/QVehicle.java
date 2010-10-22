/* *********************************************************************** *
 * project: org.matsim.*
 * SimVehicle.java
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

package org.matsim.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshots.writers.VisVehicle;

@Deprecated // only makes sense for "queue" mobsims.  Should go somewhere else (I think).  kai, oct'10
public interface QVehicle extends Identifiable, VisVehicle {

	public PersonDriverAgent getDriver();
	// yy presumably, this should return DriverAgent

	public void setDriver(final PersonDriverAgent driver);
	// yy presumably, this should set DriverAgent
	
	public Link getCurrentLink();
	
	public void setCurrentLink(final Link link);
	// yy not sure if this needs to be publicly exposed
	
	public double getSizeInEquivalents();
	
	public double getLinkEnterTime();
	// yy not sure if this needs to be publicly exposed
	
	public void setLinkEnterTime(final double time);
	// yy not sure if this needs to be publicly exposed

	public double getEarliestLinkExitTime();
	// yy not sure if this needs to be publicly exposed

	public void setEarliestLinkExitTime(final double time);
	// yy not sure if this needs to be publicly exposed

	/**
	 * @return the <code>Vehicle</code> that this simulation vehicle represents
	 */
	public Vehicle getVehicle();

}
