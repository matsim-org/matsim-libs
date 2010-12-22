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
 * @author laemmel
 * 
 */
public class EnvironmentForceModule implements ForceModule {

	private final Floor floor;
	private final Scenario2DImpl sc;
	private final StaticForceField sff;

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

		ForceLocation fl = this.sff.getForceLocationWithin(agent.getPosition(), Sim2DConfig.STATIC_FORCE_RESOLUTION + 0.01);
		if (fl == null) {
			return;
		}

		Force f = fl.getForce();

		if (f == null) {
			initForce(fl);
			f = fl.getForce();
		}

		double fx = f.getXComponent();
		double fy = f.getYComponent();

		if (Sim2DConfig.DEBUG) {
			ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + fx / Sim2DConfig.TIME_STEP_SIZE, agent.getPosition().y + fy / Sim2DConfig.TIME_STEP_SIZE, 0), 1.f, 0.f, 1.f, 2);
			this.floor.getSim2D().getEventsManager().processEvent(arrow);
		}
		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}

	/**
	 * @param fl
	 */
	private synchronized void initForce(ForceLocation fl) {
		if (fl.getForce() == null) {
			EnvironmentDistances ed = fl.getEnvironmentDistances();
			double fx = 0;
			double fy = 0;
			for (Coordinate obj : ed.getObjects()) {
				double dist = obj.distance(ed.getLocation());
				if (dist > Sim2DConfig.Bw) {
					continue;
				}
				double dx = (ed.getLocation().x - obj.x) / dist;
				double dy = (ed.getLocation().y - obj.y) / dist;
				fx += Sim2DConfig.Apw * Math.exp((Agent2D.AGENT_DIAMETER - dist) / Sim2DConfig.Bw) * dx;
				fy += Sim2DConfig.Apw * Math.exp((Agent2D.AGENT_DIAMETER - dist) / Sim2DConfig.Bw) * dy;
			}
			fx /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;
			fy /= Agent2D.AGENT_WEIGHT * Sim2DConfig.TIME_STEP_SIZE;
			Force f = new Force();
			f.setXComponent(fx);
			f.setYComponent(fy);
			fl.setForce(f);
		}

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
