/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalSim2DEnvironment.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics;

import org.apache.log4j.Logger;

import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

public class PhysicalSim2DEnvironment {
	
	private static final Logger log = Logger.getLogger(PhysicalSim2DEnvironment.class);
	
	private final Sim2DEnvironment env;

	public PhysicalSim2DEnvironment(Sim2DEnvironment env) {
		this.env = env;
	}
	
	public void doSimStep(double time) {
		log.warn("not implemented yet!");
	}

}
