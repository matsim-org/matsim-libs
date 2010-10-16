/* *********************************************************************** *
 * project: org.matsim.*
 * AgentInteractionModule.java
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

import playground.gregor.sim2_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class AgentInteractionModule implements ForceModule {

	private final Floor floor;
	private final Scenario2DImpl sceanrio;

	/**
	 * @param floor
	 * @param sceanrio
	 */
	public AgentInteractionModule(Floor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.sceanrio = scenario;
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
		List<Coordinate> neighbors = getNeighbors(agent.getPosition());
		updateForces(agent, neighbors);
	}

	/**
	 * @param agent
	 * @param neighbors
	 */
	/* package */void updateForces(Agent2D agent, List<Coordinate> neighbors) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param position
	 * @return
	 */
	private List<Coordinate> getNeighbors(Coordinate position) {
		// TODO Auto-generated method stub
		return null;
	}

}
