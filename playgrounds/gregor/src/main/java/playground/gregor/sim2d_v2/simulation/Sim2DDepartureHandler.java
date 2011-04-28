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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

/**
 * @author laemmel
 * 
 */
public class Sim2DDepartureHandler implements DepartureHandler {

	private final Sim2D sim2D;

	/**
	 * @param sim2d
	 */
	public Sim2DDepartureHandler(Sim2D sim2d) {
		this.sim2D = sim2d;
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
	public boolean handleDeparture(double now, PlanAgent agent, Id linkId, Leg leg) {
		// TODO new TransportMode is needed i.e. TransportMode.walk2d or the
		// like
		if (agent instanceof PersonDriverAgentImpl) {
			handleAgent2DDeparture((PersonDriverAgentImpl)agent, linkId);
			return true;
		}
		return false;
	}

	/**
	 * @param now
	 * @param agent
	 * @param linkId
	 * @param leg
	 */
	private void handleAgent2DDeparture(PersonDriverAgent agent, Id linkId) {
		this.sim2D.getSim2DEngine().getFloor(linkId).agentDepart(agent);

	}

}
