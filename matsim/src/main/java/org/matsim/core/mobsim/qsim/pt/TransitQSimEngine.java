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

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements DepartureHandler, MobsimEngine, AgentSource, DistributedAgentSource, HasAgentTracker {

	private final UmlaufBuilder umlaufBuilder;

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		@Serial
		private static final long serialVersionUID = 1L;

	}

	private static final Logger log = LogManager.getLogger(TransitQSimEngine.class);

	private final Netsim qSim;

	private final TransitSchedule schedule;

	protected final TransitStopAgentTracker agentTracker;

	private final TransitStopHandlerFactory stopHandlerFactory;

	private final TimeInterpretation timeInterpretation;
	private final TransitDriverAgentFactory transitDriverFactory;

	private InternalInterface internalInterface = null;

	private UmlaufCache umlaeufe = null;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	TransitQSimEngine(Netsim queueSimulation) {
		this(queueSimulation, new SimpleTransitStopHandlerFactory(),
			new ReconstructingUmlaufBuilder(queueSimulation.getScenario()),
			new TransitStopAgentTracker(queueSimulation.getEventsManager(), queueSimulation.getScenario().getTransitSchedule()),
			TimeInterpretation.create(queueSimulation.getScenario().getConfig()),
			new DefaultTransitDriverAgentFactory());
	}

	@Inject
	public TransitQSimEngine(Netsim queueSimulation, TransitStopHandlerFactory stopHandlerFactory,
							 UmlaufBuilder umlaufBuilder, TransitStopAgentTracker tracker,
							 TimeInterpretation timeInterpretation,
							 TransitDriverAgentFactory transitDriverFactory) {
		// This should be package-private.  See https://github.com/google/guice/wiki/KeepConstructorsHidden .

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
				this.qSim.getEventsManager().processEvent(new PersonStuckEvent(now, agent.getId(), stop.getLinkId(), agent.getMode()));
				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private void createVehiclesAndDriversWithUmlaeufe(NetworkPartition partition, InsertableMobsim mobsim) {
		Scenario scenario = this.qSim.getScenario();
		Vehicles vehicles = scenario.getTransitVehicles();
		UmlaufCache umlaufCache = getOrCreateUmlaufe();

		for (Umlauf umlauf : umlaufCache.getUmlaeufe()) {
			Vehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				Id<Link> startLinkId = umlauf.getUmlaufStuecke().getFirst().getCarRoute().getStartLinkId();
				if (partition.containsLink(startLinkId)) {
					createAndScheduleVehicleAndDriver(mobsim, umlauf, basicVehicle, umlaufCache.getDeparturesDependingOnChains());
				}
			}
		}
	}

	private UmlaufCache getOrCreateUmlaufe() {

		if (umlaeufe != null)
			return umlaeufe;

		Collection<Umlauf> result = umlaufBuilder.build();

		umlaeufe = new UmlaufCache(this.schedule, result);

		return umlaeufe;
	}

	private void createAndScheduleVehicleAndDriver(InsertableMobsim mobsim, Umlauf umlauf, Vehicle vehicle, Object2IntMap<Id<Departure>> departuresDependingOnChains) {
		TransitQVehicle veh = new TransitQVehicle(vehicle);
		AbstractTransitDriverAgent driver = this.transitDriverFactory.createTransitDriver(umlauf, internalInterface, agentTracker);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
		driver.setVehicle(veh);
		Leg firstLeg = (Leg) driver.getNextPlanElement();
		Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
		mobsim.addParkedVehicle(veh, startLinkId);
		mobsim.insertAgentIntoMobsim(driver);

		// A departure that depends on a previous chain cannot depart before the first connecting leg has ended
		if (departuresDependingOnChains.containsKey(umlauf.getUmlaufStuecke().getFirst().getDeparture().getId())) {
			driver.setWaitForDeparture();
		}
	}

	private void handleAgentPTDeparture(final MobsimAgent planAgent, Id<Link> linkId) {
		// this puts the agent into the transit stop.
		Id<TransitStopFacility> accessStopId = ((PTPassengerAgent) planAgent).getDesiredAccessStopId();
		if (accessStopId == null) {
			// looks like this agent has a bad transit route, likely no
			// route could be calculated for it
			log.error("pt-agent doesn't know to what transit stop to go. Removing agent from simulation. Agent {}", planAgent.getId().toString());
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
		createVehiclesAndDriversWithUmlaeufe(NetworkPartition.SINGLE_INSTANCE, qSim);
	}

	@Override
	public void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim) {
		createVehiclesAndDriversWithUmlaeufe(partition, mobsim);
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
		} else if (type == TransitDriverAgentImpl.class) {
			TransitDriverAgentImpl.TransitDriverMessage driverMessage = (TransitDriverAgentImpl.TransitDriverMessage) message;
			Umlauf umlauf = umlaeufe.getUmlauf(driverMessage.umlaufId());
			// The transport mode here does not seem to matter
			return new TransitDriverAgentImpl(driverMessage, umlauf, TransportMode.car, agentTracker, internalInterface);
		}

		return null;
	}

	@Override
	public Set<Class<? extends DistributedMobsimVehicle>> getVehicleClasses() {
		return Set.of(TransitQVehicle.class);
	}

	@Override
	public DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {

		// wow!1! type pattern in java...
		if (message instanceof TransitQVehicle.Msg(Message baseMessage, Message handlerMessage)) {
			TransitQVehicle transitVehicle = new TransitQVehicle((QVehicleImpl.Msg) baseMessage);
			var handler = stopHandlerFactory.createTransitStopHandler(handlerMessage);
			transitVehicle.setStopHandler(handler);
			return transitVehicle;
		} else {
			throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
		}
	}
}
