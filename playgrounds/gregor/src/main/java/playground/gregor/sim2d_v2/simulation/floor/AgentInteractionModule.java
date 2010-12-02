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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class AgentInteractionModule implements DynamicForceModule {

	protected final Floor floor;
	protected final Scenario2DImpl scenario;

	private final double quadUpdateInterval = 1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;
	protected Quadtree coordsQuad = new Quadtree();

	protected final Map<Agent2D, List<Coordinate>> neighbors = new HashMap<Agent2D, List<Coordinate>>();

	/**
	 * @param floor
	 * @param sceanrio
	 */
	public AgentInteractionModule(Floor floor, Scenario2DImpl scenario) {
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
		List<Coordinate> neighbors = this.neighbors.get(agent);
		updateForces(agent, neighbors);
	}

	/**
	 * @param agent
	 * @param neighbors
	 */
	/* package */void updateForces(Agent2D agent, List<Coordinate> neighbors) {
		double fx = 0;
		double fy = 0;
		for (Coordinate other : neighbors) {
			if (other.equals(agent.getPosition())) {
				continue;
			}

			double x = agent.getPosition().x - other.x;
			double y = agent.getPosition().y - other.y;
			double sqrLength = Math.pow(x, 2) + Math.pow(y, 2);
			if (sqrLength > Sim2DConfig.PSqrSensingRange) {
				continue;
			}
			if (sqrLength < 0.1) {
				sqrLength = 0.1;
			}

			double length = Math.sqrt(sqrLength);

			// double contrariness = getContrariness(force,length,x,y);

			double exp = Math.exp(Sim2DConfig.Bp / length) / length; // *
			x *= exp;
			y *= exp;

			fx += x;
			fy += y;
		}

		fx = Sim2DConfig.App * fx / agent.getWeight();
		fy = Sim2DConfig.App * fy / agent.getWeight();

		// if (fx != 0 || fy != 0) {
		// // DEBUG
		// ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), new
		// Coordinate(0, 0, 0), new Coordinate(50 * fx, 50 * fy, 0), 0.f, 0.f,
		// 0.f, 3);
		// this.floor.getSim2D().getEventsManager().processEvent(arrow);
		// }

		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	protected void updateAgentQuadtree() {
		this.coordsQuad = new Quadtree();
		for (Agent2D agent : this.floor.getAgents()) {
			Envelope e = new Envelope(agent.getPosition());
			this.coordsQuad.insert(e, agent);
		}

		this.neighbors.clear();

		for (Agent2D agent : this.floor.getAgents()) {
			double minX = agent.getPosition().x - Sim2DConfig.PNeighborhoddRange;
			double maxX = agent.getPosition().x + Sim2DConfig.PNeighborhoddRange;
			double minY = agent.getPosition().y - Sim2DConfig.PNeighborhoddRange;
			double maxY = agent.getPosition().y + Sim2DConfig.PNeighborhoddRange;
			Envelope e = new Envelope(minX, maxX, minY, maxY);
			List<Agent2D> l = this.coordsQuad.query(e);
			List<Coordinate> n = new ArrayList<Coordinate>();
			for (Agent2D a2 : l) {
				if (a2.equals(agent)) {
					continue;
				}
				n.add(a2.getPosition());
			}
			this.neighbors.put(agent, n);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule#update
	 * (double)
	 */
	@Override
	public void update(double time) {
		if (time >= this.lastQuadUpdate + this.quadUpdateInterval) {

			updateAgentQuadtree();

			this.lastQuadUpdate = time;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule#forceUpdate
	 * (double)
	 */
	@Override
	public void forceUpdate() {
		updateAgentQuadtree();
	}

}
