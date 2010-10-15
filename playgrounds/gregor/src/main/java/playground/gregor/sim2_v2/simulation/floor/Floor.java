/* *********************************************************************** *
 * project: org.matsim.*
 * Floor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2_v2.simulation.floor;

import java.util.List;

import playground.gregor.sim2_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class Floor {

	private List<ForceModule> forceModules;
	private List<Agent2D> agents;

	/**
	 * @param time
	 */
	public void move(double time) {
		updateForces();
		moveAgents();

	}

	/**
	 * 
	 */
	private void moveAgents() {
		for (Agent2D agent : this.agents) {

		}

	}

	/**
	 * 
	 */
	private void updateForces() {
		for (Agent2D agent : this.agents) {
			for (ForceModule m : this.forceModules) {
				m.run(agent);
			}
		}
	}

	/**
	 * 
	 * @param module
	 */
	public void addForceModule(ForceModule module) {
		this.forceModules.add(module);
	}
}
