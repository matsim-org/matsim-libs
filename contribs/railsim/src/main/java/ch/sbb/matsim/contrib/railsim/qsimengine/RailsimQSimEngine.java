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

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import ch.sbb.matsim.contrib.railsim.events.NoopEventsManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.AlwaysApprovingDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.NoopResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimFormationEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.TrainDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;

/**
 * QSim Engine to integrate microscopically simulated train movement.
 */
public class RailsimQSimEngine implements DepartureHandler, MobsimEngine {

	private static final Logger log = LogManager.getLogger(RailsimQSimEngine.class);

	private final QSim qsim;
	private final RailsimConfigGroup config;
	private final RailResourceManager res;
	private final TrainDisposition disposition;
	private final SpeedProfile speedProfile;
	private final TrainManager trainManager;
	private final TrainTimeDistanceHandler ttdHandler;
	private final Set<String> modes;
	private final Queue<NetworkChangeEvent> networkChangeEvents;
	private final TransitStopAgentTracker agentTracker;
	private InternalInterface internalInterface;

	private RailsimEngine engine;

	@Inject
	public RailsimQSimEngine(QSim qsim, RailResourceManager res, TrainDisposition disposition, SpeedProfile speedProfile,
							 TransitStopAgentTracker agentTracker, TrainManager trainManager, TrainTimeDistanceHandler ttdHandler) {
		this.qsim = qsim;
		this.config = ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), RailsimConfigGroup.class);
		this.res = res;
		this.disposition = disposition;
		this.speedProfile = speedProfile;
		this.trainManager = trainManager;
		this.ttdHandler = ttdHandler;
		this.modes = config.getNetworkModes();
		this.agentTracker = agentTracker;
		this.networkChangeEvents = new PriorityQueue<>(Comparator.comparing(NetworkChangeEvent::getStartTime));
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public void onPrepareSim() {

		Network network = qsim.getScenario().getNetwork();

		if (network instanceof TimeDependentNetwork tNetwork) {

			// Filter for supported network change events
			List<NetworkChangeEvent> events = tNetwork.getNetworkChangeEvents().stream()
				.filter(ev ->
					ev.getAttributesChanges().stream()
						.map(NetworkChangeEvent.AttributesChangeValue::getAttribute).anyMatch(RailsimUtils.LINK_ATTRIBUTE_CAPACITY::equals)
						|| ev.getFreespeedChange() != null)
				.toList();

			log.info("Found {} network change events to be processed.", events.size());

			networkChangeEvents.addAll(events);
		}

		prepareTimeDistanceData();

		engine = new RailsimEngine(qsim.getEventsManager(), config, res, trainManager, disposition, ttdHandler);
	}

	/**
	 * Constructs a simple engine to compute reference time-distance data.
	 * Currently not in use. Time distance data is computed statically using a simpler approach.
	 */
	@SuppressWarnings("unused")
	private void simulateTimeDistanceData() {

		NoopResourceManager noopRes = new NoopResourceManager(res);
		RailsimEngine engine = new RailsimEngine(new NoopEventsManager(), config, noopRes, trainManager,
			new AlwaysApprovingDisposition(noopRes, speedProfile), ttdHandler);

		ttdHandler.preparePseudoSimulation(
			engine,
			qsim.getScenario().getTransitSchedule(),
			qsim.getScenario().getTransitVehicles()
		);

		// Simulates trajectories with simplified disposition and resources
		double endTime = qsim.getScenario().getConfig().qsim().getEndTime().orElse(24 * 3600);

		//for (double time = 0; time < endTime; time += 1) {
		engine.doSimStep(endTime);
		//}

		// targetSpeed can be 0 in some tests, even though train should accelerate

		ttdHandler.writeInitialData(qsim.getScenario().getTransitSchedule(), qsim.getScenario().getTransitVehicles());
		trainManager.clear();
	}

	/**
	 * Run simple approximation for the time distance data calculation.
	 */
	private void prepareTimeDistanceData() {

		ttdHandler.prepareTimeDistanceApproximation(qsim.getScenario().getTransitSchedule(), qsim.getScenario().getTransitVehicles(), speedProfile);
		ttdHandler.writeInitialData(qsim.getScenario().getTransitSchedule(), qsim.getScenario().getTransitVehicles());

	}

	@Override
	public void afterSim() {

		// It might be null if prepare sim fails
		if (engine != null)
			engine.clearTrains(qsim.getSimTimer().getTimeOfDay());

		ttdHandler.close();
	}

	@Override
	public void doSimStep(double time) {

		NetworkChangeEvent event = networkChangeEvents.peek();
		while (event != null && event.getStartTime() <= time) {
			networkChangeEvents.poll();

			engine.handleNetworkChangeEvent(time, event);

			event = networkChangeEvents.peek();
		}

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

		// Check if this vehicle has a formation and throw formation event
		Id<Vehicle> vehicleId = driver.getVehicle().getId();
		if (trainManager.hasFormation(vehicleId)) {
			List<String> units = trainManager.getFormation(vehicleId);
			RailsimFormationEvent formationEvent = new RailsimFormationEvent(now, vehicleId, units);
			qsim.getEventsManager().processEvent(formationEvent);
		}

		return engine.handleDeparture(now, driver, linkId, networkRoute);
	}

}
