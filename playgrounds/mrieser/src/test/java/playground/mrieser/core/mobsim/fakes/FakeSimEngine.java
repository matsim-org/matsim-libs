/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.fakes;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;

import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.MobsimKeepAlive;
import playground.mrieser.core.mobsim.api.TimestepMobsimEngine;

/**
 * Fake implementation of a SimEngine for test purposes.
 *
 * @author mrieser
 */
public class FakeSimEngine implements TimestepMobsimEngine {
	private final EventsManager em = new EventsManagerImpl();
	private double time;
	public int countHandleAgent = 0;
	public int countRunSim = 0;
	public int countAddKeepAlive = 0;
	public double timestepSize = 1.0;

	@Override
	public double getCurrentTime() {
		return this.time;
	}

	public void setCurrentTime(final double time) {
		this.time = time;
	}

	@Override
	public EventsManager getEventsManager() {
		return this.em;
	}

	@Override
	public void handleAgent(final PlanAgent agent) {
		this.countHandleAgent++;
	}

	@Override
	public void runSim() {
		this.countRunSim++;
	}

	@Override
	public void addKeepAlive(MobsimKeepAlive keepAlive) {
		this.countAddKeepAlive++;
	}

	@Override
	public double getTimestepSize() {
		return this.timestepSize;
	}

	public void setTimestepSize(final double timestepSize) {
		this.timestepSize = timestepSize;
	}
}
