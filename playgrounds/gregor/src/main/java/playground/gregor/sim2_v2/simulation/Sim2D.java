/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2D.java
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
package playground.gregor.sim2_v2.simulation;

import org.matsim.core.events.EventsManagerImpl;

import playground.gregor.sim2_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d.simulation.SimulationTimer;

/**
 * @author laemmel
 * 
 */
public class Sim2D {

	private final EventsManagerImpl events;
	private final Scenario2DImpl scenario;
	private final double endTime;
	private Sim2DEngine sim2dEngine;

	/**
	 * @param events
	 * @param scenario2dData
	 */
	public Sim2D(EventsManagerImpl events, Scenario2DImpl scenario2dData) {
		this.events = events;
		this.scenario = scenario2dData;
		this.endTime = this.scenario.getConfig().simulation().getEndTime();
	}

	public void run() {
		// prepareSim(); TODO
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			cont = doSimStep(time);
			// afterSimStep(time); TODO
			if (cont) {
				SimulationTimer.incTime();
			}
		}
	}

	private boolean doSimStep(double time) {
		// handleActivityEnds(time); TODO
		// handleAgentRemoves(time); TODO
		this.sim2dEngine.move(time);
		if (time >= this.endTime) {
			return false;
		}
		return true;
	}

}
