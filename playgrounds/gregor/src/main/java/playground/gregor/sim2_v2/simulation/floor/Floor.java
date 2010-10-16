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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class Floor {

	private List<ForceModule> forceModules;
	private List<Agent2D> agents;
	private final Scenario2DImpl scenario;
	private final List<Link> links;

	public Floor(Scenario2DImpl scenario, List<Link> list) {
		this.scenario = scenario;
		this.links = list;
	}

	/**
	 * 
	 */
	public void init() {
		this.forceModules.add(new AgentInteractionModule(this, this.scenario));
		this.forceModules.add(new DrivingForceModule(this, this.scenario));
		this.forceModules.add(new EnvironmentForceModule(this, this.scenario));
		this.forceModules.add(new PathForceModule(this, this.scenario));

		// TODO phantom force module

	}

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
			Force f = agent.getForce();
			Coordinate oldPos = agent.getPosition();
			Coordinate newPos = new Coordinate(oldPos.x + f.getXComponent(), oldPos.y + f.getYComponent(), 0);
			agent.moveTotPostion(newPos);
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

	/**
	 * @param agent
	 */
	public void addAgent(Agent2D agent) {
		throw new RuntimeException("not (yet) implemented!");

	}

	/**
	 * 
	 * @return list of agents
	 */
	public List<Agent2D> getAgents() {
		throw new RuntimeException("not (yet) implemented!");
	}

}
