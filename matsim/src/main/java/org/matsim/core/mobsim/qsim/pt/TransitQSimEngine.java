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

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.disim.DistributedAgentSource;
import org.matsim.core.mobsim.disim.DistributedMobsimAgent;
import org.matsim.core.mobsim.disim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.HasAgentTracker;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleMessage;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements DepartureHandler, MobsimEngine, AgentSource, DistributedAgentSource, HasAgentTracker {


	private Collection<MobsimAgent> ptDrivers;
	private final UmlaufBuilder umlaufBuilder;

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static final Logger log = LogManager.getLogger(TransitQSimEngine.class);

	private final Netsim qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private final TimeInterpretation timeInterpretation;
	private final TransitDriverAgentFactory transitDriverFactory;

	private InternalInterface internalInterface = null;

	private Map<Id<Umlauf>, Umlauf> umlaeufe = null;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	TransitQSimEngine(Netsim queueSimulation) {
		this(queueSimulation, new SimpleTransitStopHandlerFactory(),
			new ReconstructingUmlaufBuilder(queueSimulation.getScenario()),
			new TransitStopAgentTracker(queueSimulation.getEventsManager()),
			TimeInterpretation.create(queueSimulation.getScenario().getConfig()),
			new DefaultTransitDriverAgentFactory());
	}

	@Inject
	public TransitQSimEngine(Netsim queueSimulation, TransitStopHandlerFactory stopHandlerFactory,
							 UmlaufBuilder umlaufBuilder, TransitStopAgentTracker tracker,
							 TimeInterpretation timeInterpretation,
							 TransitDriverAgentFactory transitDriverFactory) {
		this.qSim = queueSimulation;
		this.schedule = queueSimulation.getScenario().getTransitSchedule();
		this.umlaufBuilder = umlaufBuilder;
		this.agentTracker = tracker;
		this.stopHandlerFactory = stopHandlerFactory;
		this.timeInterpretation = timeInterpretation;
		this.transitDriverFactory = transitDriverFactory;
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
		double now = this.qSim.getSimTimer().getTimeOfDay();
		for (Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());
			for (PTPassengerAgent agent : agentsAtStop.getValue()) {
				this.qSim.getEventsManager().processEvent(new PersonStuckEvent(now, agent.getId(), stop.getLinkId(), ((MobsimAgent) agent).getMode()));
				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithUmlaeufe(NetworkPartition partition, InsertableMobsim mobsim) {
		Scenario scenario = this.qSim.getScenario();
		Vehicles vehicles = scenario.getTransitVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<>();
		Map<Id<Umlauf>, Umlauf> umlaufCache = getOrCreateUmlaufe(scenario);

		for (Umlauf umlauf : umlaufCache.values()) {
			Vehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				Id<Link> startLinkId = umlauf.getUmlaufStuecke().getFirst().getCarRoute().getStartLinkId();
				if (partition.containsLink(startLinkId)) {
					AbstractTransitDriverAgent driver = createAndScheduleVehicleAndDriver(mobsim, umlauf, basicVehicle);
					drivers.add(driver);
				}
			}
		}
		return drivers;
	}

	private Map<Id<Umlauf>, Umlauf> getOrCreateUmlaufe(final Scenario scenario) {

		if (umlaeufe != null)
			return umlaeufe;

		Collection<Umlauf> result = umlaufBuilder.build();

		umlaeufe = new IdMap<>(Umlauf.class);

		for (Umlauf umlauf : result) {
			if (umlaeufe.containsKey(umlauf.getId())) {
				throw new RuntimeException("Duplicate Umlauf ID: " + umlauf.getId());
			}

			umlaeufe.put(umlauf.getId(), umlauf);
		}

		return umlaeufe;
	}

	private AbstractTransitDriverAgent createAndScheduleVehicleAndDriver(InsertableMobsim mobsim, Umlauf umlauf, Vehicle vehicle) {
		TransitQVehicle veh = new TransitQVehicle(vehicle);
		AbstractTransitDriverAgent driver = this.transitDriverFactory.createTransitDriver(umlauf, internalInterface, agentTracker);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
		driver.setVehicle(veh);
		Leg firstLeg = (Leg) driver.getNextPlanElement();
		Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
		mobsim.addParkedVehicle(veh, startLinkId);
		mobsim.insertAgentIntoMobsim(driver);
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
			this.internalInterface.registerAdditionalAgentOnLink(planAgent);
		} else {
			throw new TransitAgentTriesToTeleportException("Agent " + planAgent.getId() + " tries to enter a transit stop at link " + stop.getLinkId() + " but really is at " + linkId + "!");
		}
	}


	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		String requestedMode = agent.getMode();
		if (qSim.getScenario().getConfig().transit().getTransitModes().contains(requestedMode)) {
			handleAgentPTDeparture(agent, linkId);
			return true;
		}
		return false;
	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
		this.stopHandlerFactory = stopHandlerFactory;
	}

	@Override
	public void doSimStep(double time) {
		// Nothing to do here.
	}

	@Override
	public void insertAgentsIntoMobsim() {
		ptDrivers = createVehiclesAndDriversWithUmlaeufe(NetworkPartition.SINGLE_INSTANCE, qSim);
	}

	public Collection<MobsimAgent> getPtDrivers() {
		return Collections.unmodifiableCollection(ptDrivers);
	}

	@Override
	public void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim) {
		ptDrivers = createVehiclesAndDriversWithUmlaeufe(partition, mobsim);
	}

	@Override
	public Set<Class<? extends DistributedMobsimAgent>> getAgentClasses() {
		return Set.of(TransitAgent.class, TransitDriverAgentImpl.class);
	}

	@Override
	public DistributedMobsimAgent agentFromMessage(Class<? extends DistributedMobsimAgent> type, Message message) {
		if (type == TransitAgent.class) {
			BasicPlanAgentImpl delegate = new BasicPlanAgentImpl((BasicPlanAgentImpl.BasicPlanAgentMessage) message, qSim.getScenario(),
				qSim.getEventsManager(), qSim.getSimTimer(), timeInterpretation);
			return TransitAgent.createTransitAgent(delegate, qSim.getScenario());
		} else {
			TransitDriverAgentImpl.TransitDriverMessage driverMessage = (TransitDriverAgentImpl.TransitDriverMessage) message;
			Umlauf umlauf = umlaeufe.get(driverMessage.umlaufId());
			// The transport mode here does not seem to matter
			return new TransitDriverAgentImpl(driverMessage, umlauf, TransportMode.car, agentTracker, internalInterface);
		}
	}

	@Override
	public Set<Class<? extends DistributedMobsimVehicle>> getVehicleClasses() {
		return Set.of(TransitQVehicle.class);
	}

	@Override
	public DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {
		TransitQVehicle transitVehicle = new TransitQVehicle((QVehicleMessage) message);
		transitVehicle.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(transitVehicle.getVehicle()));
		return transitVehicle;
	}
}
