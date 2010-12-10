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

		Force f = this.sff.getForceWithin(agent.getPosition(), Sim2DConfig.STATIC_FORCE_RESOLUTION + 0.01);
		double fx = 0.;
		double fy = 0.;

		if (f != null) {
			fx = f.getXComponent();
			fy = f.getYComponent();
		}

		fx = (Sim2DConfig.Apw * fx / agent.getWeight());
		fy = (Sim2DConfig.Apw * fy / agent.getWeight());

		// DEBUG
		ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + 50 * fx, agent.getPosition().y + 50 * fy, 0), 1.f, 0.f, 1.f, 2);
		this.floor.getSim2D().getEventsManager().processEvent(arrow);

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
