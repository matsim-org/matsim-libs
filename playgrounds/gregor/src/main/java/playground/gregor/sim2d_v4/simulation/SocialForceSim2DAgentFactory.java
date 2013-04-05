/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DAgentBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;
import playground.gregor.sim2d_v4.simulation.physics.SimpleAgent;
import playground.gregor.sim2d_v4.simulation.physics.SocialForceVelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirection;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;

public class SocialForceSim2DAgentFactory implements Sim2DAgentFactory {
	
	private final Sim2DConfig conf;
	private final Scenario sc;

	public SocialForceSim2DAgentFactory(Sim2DConfig conf, Scenario sc) {
		this.conf = conf;
		this.sc = sc;
	}

	@Override
	public SimpleAgent buildAgent( QVehicle veh, double spawnX, double spawnY, PhysicalSim2DEnvironment pEnv) {
		LinkSwitcher ls = new LinkSwitcher(this.sc, pEnv);
		SimpleAgent agent = new SimpleAgent(this.sc,veh, spawnX, spawnY, ls, pEnv);
		SocialForceVelocityUpdater vu = new SocialForceVelocityUpdater(new DesiredDirection(agent, ls), new Neighbors(agent, this.conf), this.conf, agent);
		agent.setVelocityUpdater(vu);
		return agent;
	}

}
