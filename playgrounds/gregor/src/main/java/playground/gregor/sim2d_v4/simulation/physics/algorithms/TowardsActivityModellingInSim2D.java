/* *********************************************************************** *
 * project: org.matsim.*
 * TowardsActivityModellingInSim2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class TowardsActivityModellingInSim2D implements
		DesiredDirectionCalculator {
	
	
	private final DesiredDirectionCalculator dd;
	private final Sim2DAgent agent;
	private final double deadline;
	private final double x;
	private final double y;

	public TowardsActivityModellingInSim2D(Sim2DAgent agent, DesiredDirectionCalculator dd, double deadline, double x, double y) {
		this.agent = agent;
		this.dd = dd;
		this.deadline = deadline;
		this.x = x;
		this.y = y;
		
	}

	@Override
	public double[] computeDesiredDirection() {
		if (this.deadline <= this.agent.getPSec().getPhysicalEnvironment().getTime()) {
			this.agent.setDesiredSpeed(1.34);
			return this.dd.computeDesiredDirection();
		}
//		this.agent.setDesiredSpeed(0);
		double dx = this.x - this.agent.getPos()[0];
		double dy = this.y - this.agent.getPos()[1];
		double dist = Math.sqrt(dx*dx+dy*dy);
		if (dist < 0.5) {
			this.agent.setDesiredSpeed(0.001);
		} else {
			this.agent.setDesiredSpeed(1.34);
		}
		
		return new double[]{dx/dist,dy/dist};
	}

}
