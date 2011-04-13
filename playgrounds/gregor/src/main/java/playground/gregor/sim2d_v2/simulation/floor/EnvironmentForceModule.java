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
		// nothing to initialize here

	}

}
