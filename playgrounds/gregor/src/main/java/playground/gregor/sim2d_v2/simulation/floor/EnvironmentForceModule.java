/* *********************************************************************** *
 * project: org.matsim.*
 * EnvironmentForceModule.java
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

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

/**
 * Environment interaction forces according to: D. Helbing, I. Farkas, T. Vicsek,
 * Nature 407, 487-490 (2000)
 * 
 * @author laemmel
 * 
 */
public class EnvironmentForceModule implements ForceModule {

	private final Floor floor;
	private final Scenario2DImpl sc;
	private final StaticForceField sff;

	//Helbing constants 
	private static final double Bi=0.08;
	private static final double Ai=1130;
	private static final double k = 1.2 * 100000;
	private static final double kappa = 2.4 * 100000;

	/**
	 * @param floor
	 * @param scenario
	 */
	public EnvironmentForceModule(Floor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.sc = scenario;
		this.sff = this.sc.getStaticForceField();
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
		double fx = 0;
		double fy = 0;
		int envId = 100;

		ForceLocation fl = this.sff.getForceLocationWithin(agent.getPosition(), Sim2DConfig.STATIC_FORCE_RESOLUTION + 0.01);
		if (fl == null) {
			return;
		}
		EnvironmentDistances ed = fl.getEnvironmentDistances();
		for (Coordinate obj : ed.getObjects()) {
			double dist = obj.distance(agent.getPosition());
			if (dist > Sim2DConfig.PNeighborhoddRange) {
				continue;
			}
			double dx =(agent.getPosition().x - obj.x) / dist;
			double dy =(agent.getPosition().y - obj.y) / dist;

			double bounderyDist = Agent2D.AGENT_DIAMETER/4 - dist;
			double g = bounderyDist > 0 ? bounderyDist : 0;

			double tanDvx = (- agent.getVx()) * dx;
			double tanDvy = (- agent.getVy()) * dy;

			double tanX = tanDvx * -dx;
			double tanY = tanDvy * dy;

			double xc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dx+ kappa * g * tanX;
			double yc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dy + kappa * g * tanY;

			fx += xc;
			fy += yc;

		}

//		if (Sim2DConfig.DEBUG) {
//			ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + fx/Sim2DConfig.TIME_STEP_SIZE, agent.getPosition().y + fy/Sim2DConfig.TIME_STEP_SIZE, 0), 1.f, 0.f, 1.f, 2);
//			this.floor.getSim2D().getEventsManager().processEvent(arrow);
//		}
//		
//		if (agent.getId().toString().equals("2")) {
//			System.out.println(Math.sqrt(Math.pow(fx, 2)+Math.pow(fy, 2))/Sim2DConfig.TIME_STEP_SIZE);
//			int iii = 3;
//			iii++;
//		}
		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}

	//	/**
	//	 * @param fl
	//	 */
	//	private synchronized void initForce(ForceLocation fl) {
	//		if (fl.getForce() == null) {
	//			EnvironmentDistances ed = fl.getEnvironmentDistances();
	//			double fx = 0;
	//			double fy = 0;
	//			for (Coordinate obj : ed.getObjects()) {
	//				double dist = obj.distance(ed.getLocation());
	//				if (dist > Sim2DConfig.PNeighborhoddRange) {
	//					continue;
	//				}
	//				double dx = (ed.getLocation().x - obj.x) / dist;
	//				double dy = (ed.getLocation().y - obj.y) / dist;
	//				
	//				double bounderyDist = Agent2D.AGENT_DIAMETER/2 - dist;
	//				double g = bounderyDist > 0 ? bounderyDist : 0;
	//				
	//				double tanDvx = (- agent.getVx()) * -dx;
	//				double tanDvy = (- agent.getVy()) * dy;
	//				
	//				double tanX = tanDvx * -dx;
	//				double tanY = tanDvy * dy;
	//				
	//				fx += Sim2DConfig.Apw * Math.exp((Agent2D.AGENT_DIAMETER - dist) / Sim2DConfig.Bw) * dx;
	//				fy += Sim2DConfig.Apw * Math.exp((Agent2D.AGENT_DIAMETER - dist) / Sim2DConfig.Bw) * dy;
	//			}
	//			fx /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;
	//			fy /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;
	//			Force f = new Force();
	//			f.setXComponent(fx);
	//			f.setYComponent(fy);
	//			fl.setForce(f);
	//		}
	//
	//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {
		// nothing to initialize here

	}

}
