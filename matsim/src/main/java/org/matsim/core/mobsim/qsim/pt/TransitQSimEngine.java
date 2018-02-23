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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import javax.inject.Inject;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements  DepartureHandler, MobsimEngine, AgentSource {


	private Collection<MobsimAgent> ptDrivers;

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static final Logger log = Logger.getLogger(TransitQSimEngine.class);

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private TransitDriverAgentFactory transitDriverFactory;

	private InternalInterface internalInterface = null ;
	
	final private MobsimTimer mobsimTimer;
	final private AgentCounter agentCounter;
	final private Config config;
	final private Scenario scenario;
	final private EventsManager eventsManager;
	
	@Deprecated
	final private QSim qSim;

	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
		transitDriverFactory = new DefaultTransitDriverAgentFactory(internalInterface, agentTracker, scenario, eventsManager);
	}

	@Inject
	public TransitQSimEngine(QSim qsim, Config config, Scenario scenario, EventsManager eventsManager, MobsimTimer mobsimTimer, AgentCounter agentCounter) {
		this.config = config;
		this.scenario = scenario;
		this.schedule = scenario.getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker(eventsManager);
		this.mobsimTimer = mobsimTimer;
		this.agentCounter = agentCounter;
		this.eventsManager = eventsManager;
		
		
		// TODO: This cannot be resolved yet due to the structure of the AgentSource!
		this.qSim = qsim;
	}

	// For tests (which create an Engine, and externally create Agents as well).
	public InternalInterface getInternalInterface() {
		return this.internalInterface;
	}

	@Override
	public void onPrepareSim() {
		//nothing to do here
	}


	@Override
	public void afterSim() {
		double now = mobsimTimer.getTimeOfDay();
		for (Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());
			for (PTPassengerAgent agent : agentsAtStop.getValue()) {
				eventsManager.processEvent(new PersonStuckEvent( now, agent.getId(), stop.getLinkId(), ((MobsimAgent)agent).getMode()));
				agentCounter.decLiving();
				agentCounter.incLost();
			}
		}
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithUmlaeufe() {
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

	private static UmlaufCache getOrCreateUmlaufCache(final Scenario scenario) {
		UmlaufCache umlaufCache;

		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder =
				new ReconstructingUmlaufBuilder(
						scenario.getNetwork(),
						scenario.getTransitSchedule().getTransitLines().values(),
						scenario.getTransitVehicles(),
						scenario.getConfig().planCalcScore());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		umlaufCache = new UmlaufCache(scenario.getTransitSchedule(), umlaeufe);

		return umlaufCache;
	}

	private AbstractTransitDriverAgent createAndScheduleVehicleAndDriver(Umlauf umlauf, Vehicle vehicle) {
		TransitQVehicle veh = new TransitQVehicle(vehicle);
		AbstractTransitDriverAgent driver = this.transitDriverFactory.createTransitDriver(umlauf);
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
			agentCounter.decLiving();
			agentCounter.incLost();
			return;
		}
		TransitStopFacility stop = this.schedule.getFacilities().get(accessStopId);
		if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
			double now = mobsimTimer.getTimeOfDay();
			this.agentTracker.addAgentToStop(now, (PTPassengerAgent) planAgent, stop.getId());
			this.internalInterface.registerAdditionalAgentOnLink(planAgent) ;
		} else {
			throw new TransitAgentTriesToTeleportException("Agent "+planAgent.getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
		}
	}


	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		String requestedMode = agent.getMode();
		if (config.transit().getTransitModes().contains(requestedMode)) {
			handleAgentPTDeparture(agent, linkId);
			return true ;
		}
		return false ;
	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	@Inject
	public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
		this.stopHandlerFactory = stopHandlerFactory;
	}

	public void setAbstractTransitDriverFactory(final TransitDriverAgentFactory abstractTransitDriverFactory) {
		this.transitDriverFactory = abstractTransitDriverFactory;
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