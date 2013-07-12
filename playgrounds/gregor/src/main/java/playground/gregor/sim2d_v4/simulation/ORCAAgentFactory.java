/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAAgentFactory.java
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
import playground.gregor.sim2d_v4.simulation.physics.ORCAVelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.VelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.NearestPointAtTargetLine;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;

public class ORCAAgentFactory implements Sim2DAgentFactory {

	private final Sim2DConfig config;
	private final Scenario sc;

	public ORCAAgentFactory(Sim2DConfig config, Scenario sc) {
		this.config = config;
		this.sc = sc;
	}
	
	@Override
	public Sim2DAgent buildAgent(QVehicle veh, double spawnX, double spawnY,PhysicalSim2DEnvironment pEnv) {
		LinkSwitcher ls = new LinkSwitcher(this.sc, pEnv);
		Sim2DAgent agent = new Sim2DAgent(this.sc,veh, spawnX, spawnY, ls, pEnv);
		Neighbors nn = new Neighbors(agent, this.config);
		nn.setRangeAndMaxNrOfNeighbors(8, 5);
//		nn.setUpdateInterval(0.5);
		VelocityUpdater vu = new ORCAVelocityUpdater(new NearestPointAtTargetLine(agent, ls), nn, this.config, agent);
//		VelocityUpdater vu = new SimpleVelocityUpdater(agent, ls, this.sc);
		agent.setVelocityUpdater(vu);
		return agent;
	}

}
