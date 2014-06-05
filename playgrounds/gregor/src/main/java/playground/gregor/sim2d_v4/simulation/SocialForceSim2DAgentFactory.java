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
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.SocialForce2005VelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.SocialForceVelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.KDTreeNeighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.NearestPointAtTargetLine;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.VDNeighbors;

public class SocialForceSim2DAgentFactory implements Sim2DAgentFactory {
	
	private final Sim2DConfig conf;
	private final Scenario sc;

	public SocialForceSim2DAgentFactory(Sim2DConfig conf, Scenario sc) {
		this.conf = conf;
		this.sc = sc;
	}

	@Override
	public Sim2DAgent buildAgent( QVehicle veh, double spawnX, double spawnY, PhysicalSim2DEnvironment pEnv) {
		LinkSwitcher ls = new LinkSwitcher(this.sc, pEnv);
		Sim2DAgent agent = new Sim2DAgent(this.sc,veh, spawnX, spawnY, ls, pEnv);
		
		if (Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
			VDNeighbors nn = new VDNeighbors(agent);
			SocialForceVelocityUpdater vu = new SocialForceVelocityUpdater(new NearestPointAtTargetLine(agent, ls), nn, this.conf, agent);
			agent.setVelocityUpdater(vu);
		} else {

			KDTreeNeighbors nn = new KDTreeNeighbors(agent, this.conf);
			nn.setRangeAndMaxNrOfNeighbors(20, 32);			
//			SocialForceVelocityUpdater vu = new SocialForceVelocityUpdater(new NearestPoixxxntAtTargetLine(agent, ls), nn, this.conf, agent);
//			SocialForceVelocityUpdater vu = new SocialForceVelocityUpdater(new PathAndDrivingDirection(agent, ls), nn, this.conf, agent);
//			SocialForce2005VelocityUpdater vu = new SocialForce2005VelocityUpdater(new PathAndDrivingDirection(agent, ls), nn, this.conf, agent);
			SocialForce2005VelocityUpdater vu = new SocialForce2005VelocityUpdater(new NearestPointAtTargetLine(agent, ls), nn, this.conf, agent);
//			SimpleVelocityUpdater vu = new SimpleVelocityUpdater(agent, ls, this.sc);
			agent.setVelocityUpdater(vu);
		}
		
		
		return agent;
	}

}
