/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.TrainDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.Set;

/**
 * QSim Engine to integrate microscopically simulated train movement.
 */
public class RailsimQSimEngine implements DepartureHandler, MobsimEngine {

	private final QSim qsim;
	private final RailsimConfigGroup config;
	private final RailResourceManager res;
	private final TrainDisposition disposition;
	private final Set<String> modes;
	private final TransitStopAgentTracker agentTracker;
	private InternalInterface internalInterface;

	private RailsimEngine engine;

	@Inject
	public RailsimQSimEngine(QSim qsim, RailResourceManager res, TrainDisposition disposition, TransitStopAgentTracker agentTracker) {
		this.qsim = qsim;
		this.config = ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), RailsimConfigGroup.class);
		this.res = res;
		this.disposition = disposition;
		this.modes = config.getNetworkModes();
		this.agentTracker = agentTracker;
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public void onPrepareSim() {
		engine = new RailsimEngine(qsim.getEventsManager(), config, res, disposition);
	}

	@Override
	public void afterSim() {
		engine.clearTrains(qsim.getSimTimer().getTimeOfDay());
	}

	@Override
	public void doSimStep(double time) {
		engine.doSimStep(time);
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!modes.contains(agent.getMode())) return false;

		NetsimLink link = qsim.getNetsimNetwork().getNetsimLink(linkId);

		// Lots of implicit type checking here to get the required information from the agent
		if (!(agent instanceof MobsimDriverAgent driver)) {
			throw new IllegalStateException("Departing agent " + agent.getId() + " is not a DriverAgent");
		}
		if (!(agent instanceof PlanAgent plan)) {
			throw new IllegalStateException("Agent " + agent + " is not of type PlanAgent and therefore incompatible.");
		}
		PlanElement el = plan.getCurrentPlanElement();
		if (!(el instanceof Leg leg)) {
			throw new IllegalStateException("Plan element of agent " + agent + " is not a leg with a route.");
		}
		Route route = leg.getRoute();
		if (!(route instanceof NetworkRoute networkRoute)) {
			throw new IllegalStateException("A network route is required for agent " + agent + ".");
		}

		// Vehicles were inserted into the qsim as parked
		// Remove them as soon as we depart
		if (link instanceof QLinkI qLink) {
			qLink.unregisterAdditionalAgentOnLink(agent.getId());
			qLink.removeParkedVehicle(driver.getVehicle().getId());
		}

		return engine.handleDeparture(now, driver, linkId, networkRoute);
	}

}
