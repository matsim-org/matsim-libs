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

package org.matsim.core.mobsim.queuesim;

import org.matsim.api.basic.v01.Identifiable;
import org.matsim.core.network.LinkImpl;
import org.matsim.vehicles.BasicVehicle;

public interface QueueVehicle extends Identifiable {

	public DriverAgent getDriver();

	public void setDriver(final DriverAgent driver);
	
	public LinkImpl getCurrentLink();
	
	public void setCurrentLink(final LinkImpl link);
	
	public double getSizeInEquivalents();
	
	public double getLinkEnterTime();
	
	public void setLinkEnterTime(final double time);

	public double getEarliestLinkExitTime();

	public void setEarliestLinkExitTime(final double time);

	/**
	 * @return the <code>BasicVehicle</code> that this simulation vehicle represents
	 */
	public BasicVehicle getBasicVehicle();

}
