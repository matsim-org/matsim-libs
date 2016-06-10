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
package playground.jbischoff.parking.sim;

import java.util.Collection;
import java.util.HashMap;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author  jbischoff
 *
 */

public class ParkingAgentSource implements AgentSource {

	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;
	private HashMap modeVehicleTypes;
	private Collection<String> mainModes;
	@Inject
	public ParkingAgentSource(Population population, AgentFactory agentFactory, QSim qsim ) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		for (String mode : mainModes) {
			// initialize each mode with default vehicle type:
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub

	}

}
