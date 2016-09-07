/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.ffcs.sim;


import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FFCSVehicleAgentSource implements AgentSource {

	
	private final QSim qsim;
	private final FreefloatingCarsharingManager manager;
	/**
	 * 
	 */
	@Inject
	public FFCSVehicleAgentSource(QSim qsim, FreefloatingCarsharingManager manager) {
		this.qsim = qsim;
		this.manager = manager;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.AgentSource#insertAgentsIntoMobsim()
	 */
	@Override
	public void insertAgentsIntoMobsim() {
		for (Entry<Id<Vehicle>, Id<Link>> e: manager.getIdleVehicleLocations().entrySet()){
			qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(e.getKey(), VehicleUtils.getDefaultVehicleType()), e.getValue());
		}
	}

}
