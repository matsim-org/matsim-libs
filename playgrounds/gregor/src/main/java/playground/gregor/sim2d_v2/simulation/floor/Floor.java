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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d.events.XYZAzimuthEvent;
import playground.gregor.sim2d.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;

/**
 * @author laemmel
 * 
 */
public class Floor {

	private final List<ForceModule> forceModules = new ArrayList<ForceModule>();
	private final List<ForceModule> drivingForceModules = new ArrayList<ForceModule>();
	private final List<Agent2D> agents = new ArrayList<Agent2D>();
	private final List<Agent2D> departedAgents = new ArrayList<Agent2D>();
	private final Scenario2DImpl scenario;
	private final List<Link> links;
	private final Sim2D sim2D;

	public Floor(Scenario2DImpl scenario, List<Link> list, Sim2D sim) {
		this.scenario = scenario;
		this.links = list;
		this.sim2D = sim;

	}

	/**
	 * 
	 */
	public void init() {
		this.forceModules.add(new AgentInteractionModule(this, this.scenario));
		this.drivingForceModules.add(new DrivingForceModule(this, this.scenario));
		this.forceModules.add(new EnvironmentForceModule(this, this.scenario));
		this.drivingForceModules.add(new PathForceModule(this, this.scenario));

		// TODO phantom force module

	}

	/**
	 * @param time
	 */
	public void move(double time) {
		updateForces();
		moveAgents(time);

	}

	/**
	 * 
	 */
	private void moveAgents(double time) {
		for (Agent2D agent : this.agents) {
			Force f = agent.getForce();
			Coordinate oldPos = agent.getPosition();
			Coordinate newPos = new Coordinate(oldPos.x + f.getXComponent(), oldPos.y + f.getYComponent(), 0);
			agent.moveToPostion(newPos);
			XYZAzimuthEvent e = new XYZAzimuthEventImpl(agent.getPerson().getId(), agent.getPosition(), 0, time);
			this.sim2D.getEventsManager().processEvent(e);
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
		for (Agent2D agent : this.departedAgents) {
			for (ForceModule m : this.drivingForceModules) {
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
		this.agents.add(agent);
		Activity act = (Activity) agent.getCurrentPlanElement();
		agent.setPostion(MGC.coord2Coordinate(act.getCoord()));

	}

	/**
	 * 
	 * @return list of agents
	 */
	public List<Agent2D> getAgents() {
		throw new RuntimeException("not (yet) implemented!");
	}

	/**
	 * @param agent
	 */
	public void agentDepart(Agent2D agent) {
		this.departedAgents.add(agent);

	}

}
