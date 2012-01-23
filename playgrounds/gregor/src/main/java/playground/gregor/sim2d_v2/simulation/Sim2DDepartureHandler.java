/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DDepartureHandler.java
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
package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;



/**
 * @author laemmel
 * 
 */
public class Sim2DDepartureHandler implements DepartureHandler {

	private final Sim2DEngine engine;

	/**
	 * @param sim2d
	 */
	public Sim2DDepartureHandler(Sim2DEngine engine) {
		this.engine = engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.DepartureHandler#handleDeparture
	 * (double, org.matsim.core.mobsim.framework.PersonAgent,
	 * org.matsim.api.core.v01.Id, org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent instanceof Agent2D && agent.getMode().equals("walk2d")) {
			//TODO agents can not depart directly since their actual departure time might be later than now (because of the
			//sub-second time res. Instead we trap the agents in "limbo" until their time is up. 
			handleAgent2DDeparture((Agent2D)agent);
			return true;
		}
		return false;
	}


	private void handleAgent2DDeparture(Agent2D agent) {
//		this.engine.getFloor(linkId).agentDepart(agent);
		this.engine.putDepartingAgentInLimbo(agent);
	}

}
