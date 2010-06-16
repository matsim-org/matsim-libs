/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.network.api;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.sim.api.SimVehicle;

public interface SimLink {

	public static final double POSITION_AT_FROM_NODE = 0.0;
	public static final double POSITION_AT_TO_NODE = 1.0;

	public static final double PRIORITY_AS_SOON_AS_SPACE_AVAILABLE = 0.0;
	public static final double PRIORITY_IMMEDIATELY = 1.0;

	public Id getId();

	public void addVehicle(final SimVehicle vehicle, final double position, final double priority);

	public double removeVehicle(final SimVehicle vehicle);

	public void stopVehicle(final SimVehicle vehicle);

	public void continueVehicle(final SimVehicle vehicle);

}
