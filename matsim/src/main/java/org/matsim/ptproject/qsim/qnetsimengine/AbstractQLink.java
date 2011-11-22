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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;

/**
 * 
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class AbstractQLink extends AbstractQLane implements NetsimLink {
	
	// the question if variables that exist both in QLinkImpl and QLinkLanesImpl are pulled to here
	// depends on the question if they are needed here (for methods that are pulled up).  kai, nov'11
	
	final Link link ;
	
	AbstractQLink(Link link) {
		this.link = link ;
	}

	abstract void setQSimEngine(QSimEngineInternalI qsimEngine);

	abstract boolean moveLink(double now);

	abstract QVehicle removeParkedVehicle(Id vehicleId);
	// in contrast to "addParkedVehicle", this here does not need to be public since it is only used internally.  kai, aug'10

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);

	abstract QSimEngineInternalI getQSimEngine() ;

	void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) {
		vehicle.setDriver(agent);
		if ( agent.getDestinationLinkId().equals(link.getId()) && (agent.chooseNextLinkId() == null)) {
			// yyyy this should be handled at person level, not vehicle level.  kai, feb'10

			agent.endLegAndAssumeControl(now);
			this.addParkedVehicle(vehicle);
		} else {
			this.addDepartingVehicle(vehicle);
		}
	}

	/**
     * Design thoughts:<ul>
     * <li> This method could also be private (never called from outside the class). kai, nov'11
     * </ul>
	 */
	abstract QVehicle getParkedVehicle( Id id ) ;
	
	/**
	 * modifying the return type ...
	 */
	@Override
	public abstract QSim getMobsim() ;

	
	// joint implementation for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}
	
	@Override
	public abstract QNode getToNetsimNode() ;
	// return type change 

}
