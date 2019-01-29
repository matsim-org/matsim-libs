/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

public interface VehicleUsingAgent extends MobsimAgentMarkerInterface {

	/**
	 * @param veh
	 */
	public void setVehicle(final MobsimVehicle veh);
	
	/**
	 * Design thoughts:<ul>
	 * <li> MZ states (in his AdapterAgent) that the DriverAgent should not have this reference.  
	 * I am, in fact, not so sure (any more); maybe it is not so bad to have this.  Clearly, symmetric
	 * connectors between objects would be better.  kai, nov'11
	 * </ul>
	 */
	public MobsimVehicle getVehicle();
	
	public Id<Vehicle> getPlannedVehicleId();
}
