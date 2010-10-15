/* *********************************************************************** *
 * project: org.matsim.*
 * PhantomForceModule.java
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

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2_v2.simulation.Agent2D;
import playground.gregor.sim2_v2.simulation.PhantomManager;

/**
 * @author laemmel
 * 
 */
public class PhantomForceModule extends AgentInteractionModule implements ForceModule {

	private PhantomManager phantomMgr;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent) {
		List<Coordinate> phantomCoords = this.phantomMgr.getPhatomsNear(agent.getPosition());
		updateForces(agent, phantomCoords);
	}

}
