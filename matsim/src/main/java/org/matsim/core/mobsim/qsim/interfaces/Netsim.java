/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisMobsim;

import java.util.Collection;
import java.util.Map;

/**
 * Interface that combines the most important interfaces of the QSim.
 */
public interface Netsim extends ObservableMobsim, InsertableMobsim, ActivityEndRescheduler, VisMobsim {

	NetsimNetwork getNetsimNetwork();

	EventsManager getEventsManager();

	AgentCounter getAgentCounter();

	Scenario getScenario();

	MobsimTimer getSimTimer();

	Map<Id<Vehicle>, MobsimVehicle> getVehicles();

	Collection<AgentTracker> getAgentTrackers();
}
