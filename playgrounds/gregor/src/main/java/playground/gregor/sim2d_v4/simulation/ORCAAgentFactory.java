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
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirectionCalculator;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.FNDDependentSpeed;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.KDTreeNeighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.NearestPointAtTargetLine;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.PathAndDrivingDirection;

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
		Sim2DAgent agent = new Sim2DAgent(this.sc,veh, spawnX, spawnY, ls, pEnv,.19);

		if (Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
//			VDNeighbors nn = new VDNeighbors(agent);
//			VelocityUpdater vu = new ORCAVelocityUpdater(new PerceivedPersonalSpaceApproach(),new NearestPointAtTargetLine(agent, ls), nn, this.config, agent);
//			agent.setVelocityUpdater(vu);
			KDTreeNeighbors nn = new KDTreeNeighbors(agent, this.config);
			nn.setRangeAndMaxNrOfNeighbors(8, 5);			
			VelocityUpdater vu = new ORCAVelocityUpdater(new FNDDependentSpeed(),new NearestPointAtTargetLine(agent, ls), nn, this.config, agent);
			agent.setVelocityUpdater(vu);
		} else {
			KDTreeNeighbors nn = new KDTreeNeighbors(agent, this.config);
			nn.setRangeAndMaxNrOfNeighbors(8, 5);			
			DesiredDirectionCalculator dd = new PathAndDrivingDirection(agent, ls);
			dd = new NearestPointAtTargetLine(agent, ls);
////			VelocityUpdater vu = new ORCAVelocityUpdater(new FNDDependentSpeed(),new NearestPointAtTargetLine(agent, ls), nn, this.config, agent);
//			if (agent.getId().toString().startsWith("d")) {
//				double rx =2* MatsimRandom.getRandom().nextDouble()-1;
//				double ry =2* MatsimRandom.getRandom().nextDouble()-1;
//				double r = MatsimRandom.getRandom().nextDouble();
//				if (r<.4) {	
//					dd = new TowardsActivityModellingInSim2D(agent, dd,300+MatsimRandom.getRandom().nextInt(20), 10+rx, 10+3*(0.5+ry));
//				} else if (r <.65){
//					dd = new TowardsActivityModellingInSim2D(agent, dd, 300+MatsimRandom.getRandom().nextInt(20), 5+rx, 15+ry);
//				} else if (r <.9){
//					dd = new TowardsActivityModellingInSim2D(agent, dd, 300+MatsimRandom.getRandom().nextInt(20), 15+rx, 15+ry);
//				} else {
//					dd = new TowardsActivityModellingInSim2D(agent, dd, 300+MatsimRandom.getRandom().nextInt(20), 10+5*rx, 4+Math.abs(rx));
//				}
//			} else if (agent.getId().toString().hashCode() % 2 != 0){
//				dd = new QuadTreePathWalker(agent,dd,this.sc.getNetwork(),pEnv.getEventsManager());
//			}
			VelocityUpdater vu = new ORCAVelocityUpdater(new FNDDependentSpeed(),dd, nn, this.config, agent);
			agent.setVelocityUpdater(vu);
		}
		
		return agent;
	}

}
