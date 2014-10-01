/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAccessEgress.java
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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public interface PassengerAccessEgress {

	/** 
	 * @param agent agent to be handled
	 * @param time time the agent should be handled
	 * @return true, if handled correctly, otherwise false, e.g. vehicle has no capacity left
	 */
	public boolean handlePassengerEntering(final PTPassengerAgent agent, MobsimVehicle vehicle, Id<TransitStopFacility> fromStopFacilityId, final double time);

	/** 
	 * @param agent agent to be handled
	 * @param time time the agent should be handled
	 * @return true, if handled correctly, otherwise false
	 */
	public boolean handlePassengerLeaving(final PTPassengerAgent agent, MobsimVehicle vehicle, Id<Link> toLinkId, final double time);


}
