/* *********************************************************************** *
 * project: org.matsim.*
 *
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

package org.matsim.core.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.HasAgentTracker;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import jakarta.inject.Inject;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements DepartureHandler, MobsimEngine, AgentSource, HasAgentTracker {


	private Collection<MobsimAgent> ptDrivers;
	private final UmlaufBuilder umlaufBuilder;

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static final Logger log = LogManager.getLogger(TransitQSimEngine.class);

	private final QSim qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private final TransitDriverAgentFactory transitDriverFactory;

	private InternalInterface internalInterface = null ;

	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	TransitQSimEngine(QSim queueSimulation) {
		this(queueSimulation, new SimpleTransitStopHandlerFactory(),
			new ReconstructingUmlaufBuilder(queueSimulation.getScenario()),
			new TransitStopAgentTracker(queueSimulation.getEventsManager()),
			new DefaultTransitDriverAgentFactory());
	}

	@Inject
	public TransitQSimEngine(QSim queueSimulation, TransitStopHandlerFactory stopHandlerFactory,
							 UmlaufBuilder umlaufBuilder, TransitStopAgentTracker tracker,
							 TransitDriverAgentFactory transitDriverFactory) {
		// This should be package-private.  See https://github.com/google/guice/wiki/KeepConstructorsHidden .

		this.qSim = queueSimulation;
		this.schedule = queueSimulation.getScenario().getTransitSchedule();
		this.umlaufBuilder = umlaufBuilder;
		this.agentTracker = tracker;
		this.stopHandlerFactory = stopHandlerFactory;
		this.transitDriverFactory = transitDriverFactory;
	}

	// For tests (which create an Engine, and externally create Agents as well).
	public InternalInterface getInternalInterface() {
		// yyyy exposing this in this way defeats its purpose (which was to restrict who can add or remove agents into or from the mobsim).  --> move
		// test into same package so we can remove the public.  kai, feb'25
		return this.internalInterface;
	}

	@Override
	public void onPrepareSim() {
		//nothing to do here
	}


	@Override
	public void afterSim() {
		double now = this.qSim.getSimTimer().getTimeOfDay();
		for (Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());
			for (PTPassengerAgent agent : agentsAtStop.getValue()) {
				this.qSim.getEventsManager().processEvent(new PersonStuckEvent( now, agent.getId(), stop.getLinkId(), ((MobsimAgent)agent).getMode()));
				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithUmlaeufe() {
		Scenario scenario = this.qSim.getScenario();
		Vehicles vehicles = scenario.getTransitVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<>();
		UmlaufCache umlaufCache = getOrCreateUmlaufCache( scenario );

		for (Umlauf umlauf : umlaufCache.getUmlaeufe()) {
			Vehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				MobsimAgent driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private UmlaufCache getOrCreateUmlaufCache(final Scenario scenario) {
		UmlaufCache umlaufCache;

		Collection<Umlauf> umlaeufe = umlaufBuilder.build();
		umlaufCache = new UmlaufCache(scenario.getTransitSchedule(), umlaeufe);

		return umlaufCache;
	}

	private AbstractTransitDriverAgent createAndScheduleVehicleAndDriver(Umlauf umlauf, Vehicle vehicle) {
		TransitQVehicle veh = new TransitQVehicle(vehicle);
		AbstractTransitDriverAgent driver = this.transitDriverFactory.createTransitDriver(umlauf, internalInterface, agentTracker);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
		driver.setVehicle(veh);
		Leg firstLeg = (Leg) driver.getNextPlanElement();
		Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
		this.qSim.addParkedVehicle(veh, startLinkId);
		this.qSim.insertAgentIntoMobsim(driver);
		return driver;
	}

	private void handleAgentPTDeparture(final MobsimAgent planAgent, Id<Link> linkId) {
		// this puts the agent into the transit stop.
		Id<TransitStopFacility> accessStopId = ((PTPassengerAgent) planAgent).getDesiredAccessStopId();
		if (accessStopId == null) {
			// looks like this agent has a bad transit route, likely no
			// route could be calculated for it
			log.error("pt-agent doesn't know to what transit stop to go. Removing agent from simulation. Agent " + planAgent.getId().toString());
			this.qSim.getAgentCounter().decLiving();
			this.qSim.getAgentCounter().incLost();
			return;
		}
		TransitStopFacility stop = this.schedule.getFacilities().get(accessStopId);
		if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
			double now = this.qSim.getSimTimer().getTimeOfDay();
			this.agentTracker.addAgentToStop(now, (PTPassengerAgent) planAgent, stop.getId());
			this.internalInterface.registerAdditionalAgentOnLink(planAgent) ;
		} else {
			throw new TransitAgentTriesToTeleportException("Agent "+planAgent.getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
		}
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		String requestedMode = agent.getMode();
		if (qSim.getScenario().getConfig().transit().getTransitModes().contains(requestedMode)) {
			handleAgentPTDeparture(agent, linkId);
			return true ;
		}
		return false ;
	}

	@Override
	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	@Override
	public void doSimStep(double time) {
		// Nothing to do here.
	}

	@Override
	public void insertAgentsIntoMobsim() {
		ptDrivers = createVehiclesAndDriversWithUmlaeufe();
	}

	public Collection<MobsimAgent> getPtDrivers() {
		return Collections.unmodifiableCollection(ptDrivers);
	}


}
