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
import playground.gregor.sim2d_v2.simulation.Sim2D;

/**
 * Agent interaction forces according to: D. Helbing, I. Farkas, T. Vicsek,
 * Nature 407, 487-490 (2000)
 * 
 * @author laemmel
 * 
 */
public class CircularAgentInteractionModule implements DynamicForceModule {

	protected final Floor floor;
	protected final Scenario2DImpl scenario;

	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;
	protected Quadtree coordsQuad = new Quadtree();

	//Helbing constants 
	private static final double Bi=0.08;
	private static final double Ai=2000;
	private static final double k = 1.2 * 100000;
	private static final double kappa = 2.4 * 100000;
	
	
//	protected final Map<Agent2D, List<Coordinate>> neighbors = new HashMap<Agent2D, List<Coordinate>>();

	/**
	 * @param floor
	 * @param sceanrio
	 */
	public CircularAgentInteractionModule(Floor floor, Scenario2DImpl scenario) {
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
//		List<Coordinate> neighbors = this.neighbors.get(agent);
		updateForces(agent);//, neighbors);
	}

	/**
	 * @param agent
	 * @param neighbors
	 */
	/* package */void updateForces(Agent2D agent) {//, List<Coordinate> neighbors) {
		double fx = 0;
		double fy = 0;

		double minX = agent.getPosition().x - Sim2DConfig.PNeighborhoddRange;
		double maxX = agent.getPosition().x + Sim2DConfig.PNeighborhoddRange;
		double minY = agent.getPosition().y - Sim2DConfig.PNeighborhoddRange;
		double maxY = agent.getPosition().y + Sim2DConfig.PNeighborhoddRange;
		Envelope e = new Envelope(minX, maxX, minY, maxY);
		List<Agent2D> l = this.coordsQuad.query(e);
		
		int otherId = 10;
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}

			double dist = other.getPosition().distance(agent.getPosition());
			if (dist > Sim2DConfig.PNeighborhoddRange) {
				continue;
			}
			double dx = (agent.getPosition().x - other.getPosition().x) / dist;
			double dy = (agent.getPosition().y - other.getPosition().y) / dist;

			double bounderyDist = Agent2D.AGENT_DIAMETER - dist;
			double g = bounderyDist > 0 ? bounderyDist : 0;
			
			double tanDvx = (other.getVx() - agent.getVx()) * -dx;
			double tanDvy = (other.getVy() - agent.getVy()) * dy;
			
			double tanX = tanDvx * -dx;
			double tanY = tanDvy * dy;
			
			double xc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dx+ kappa * g * tanX;
			double yc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dy + kappa * g * tanY;

			fx += xc;
			fy += yc;

//			if (Sim2DConfig.DEBUG) {
//				if (agent.getId().toString().equals("0")) {
//				ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), other.getPosition(), 0.2f, 0.2f, 0.2f, otherId++);
//				this.floor.getSim2D().getEventsManager().processEvent(arrow);
////				
//			
////				Coordinate cc = new Coordinate(agent.getPosition().x +  tanX,agent.getPosition().y + tanY,0);
////				ArrowEvent arrow2 = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), cc, .2f, .2f, .2f, otherId++);
////				this.floor.getSim2D().getEventsManager().processEvent(arrow2);
////				System.out.println(tanX + "   " + tanY);
//				}
//
//			}

		}

//		fx /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;
//		fy /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;

//		if (Sim2DConfig.DEBUG) {
//			ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + fx, agent.getPosition().y + fy, 0), 0.5f,0.5f, 1.f, 3);
//			this.floor.getSim2D().getEventsManager().processEvent(arrow);
//		}

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

//		this.neighbors.clear();
//
//		for (Agent2D agent : this.floor.getAgents()) {
//			double minX = agent.getPosition().x - Sim2DConfig.PNeighborhoddRange;
//			double maxX = agent.getPosition().x + Sim2DConfig.PNeighborhoddRange;
//			double minY = agent.getPosition().y - Sim2DConfig.PNeighborhoddRange;
//			double maxY = agent.getPosition().y + Sim2DConfig.PNeighborhoddRange;
//			Envelope e = new Envelope(minX, maxX, minY, maxY);
//			List<Agent2D> l = this.coordsQuad.query(e);
//			List<Coordinate> n = new ArrayList<Coordinate>();
//			for (Agent2D a2 : l) {
//				if (a2.equals(agent)) {
//					continue;
//				}
//				n.add(a2.getPosition());
//			}
//			this.neighbors.put(agent, n);
//		}

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
