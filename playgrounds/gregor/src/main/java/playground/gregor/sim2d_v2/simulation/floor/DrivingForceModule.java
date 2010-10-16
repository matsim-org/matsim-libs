/* *********************************************************************** *
 * project: org.matsim.*
 * DrivingForceModule.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class DrivingForceModule implements ForceModule {

	private final Scenario2DImpl scenario;
	private final Floor floor;

	/**
	 * @param floor
	 * @param scenario
	 */
	public DrivingForceModule(Floor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.scenario = scenario;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent) {
		// TODO Auto-generated method stub

	}

}
