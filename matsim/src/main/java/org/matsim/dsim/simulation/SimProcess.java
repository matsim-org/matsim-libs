package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.MobsimListenerManager;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.dsim.messages.SimStepMessageProcessor;
import org.matsim.dsim.simulation.net.NetworkTrafficEngine;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisNetwork;

import java.util.*;

public class SimProcess implements Steppable, LP, SimStepMessageProcessor, Netsim, InternalInterface {

	private static final Logger log = LogManager.getLogger(SimProcess.class);

	private final List<DistributedMobsimEngine> engines = new ArrayList<>();
	private final List<DistributedDepartureHandler> departureHandlers = new ArrayList<>();
	private final List<DistributedActivityHandler> activityHandlers = new ArrayList<>();
	private final MobsimListenerManager listenerManager = new MobsimListenerManager(this);
	private final SimStepMessaging messaging;
	private final Scenario scenario;
	private final NetworkPartition partition;
	private final AgentSourcesContainer asc;
	private final EventsManager em;
	private final MobsimTimer currentTime;
	private final AgentCounter agentCounter = new DummyAgentCounter();
	private NetworkTrafficEngine networkTrafficEngine;
	/**
	 * Additional agents that have been registered using {@link #registerAdditionalAgentOnLink(MobsimAgent)}.
	 * This map does not contain the full set of agents in the simulation.
	 */
	private final IdMap<Person, MobsimAgent> agents = new IdMap<>(Person.class);

	@Inject
	SimProcess(Scenario scenario, NetworkPartition partition, SimStepMessaging messaging, AgentSourcesContainer asc, EventsManager em) {
		this.scenario = scenario;
		this.partition = partition;
		this.messaging = messaging;
		this.asc = asc;
		this.em = em;
		this.currentTime = new MobsimTimer();
	}

	/**
	 * Add a mobsim component to the simulation.
	 */
	public void addMobsimComponent(QSimComponent component) {
		if (component instanceof DistributedMobsimEngine d) {
			this.engines.add(d);
			d.setInternalInterface(this);
			engines.sort(Comparator.comparingDouble(DistributedMobsimEngine::getEnginePriority).reversed());
		} else if (component instanceof MobsimEngine e) {
			log.warn("Ignoring non-distributed mobsim engine : {}", e.getClass().getName());
		}

		if (component instanceof DistributedActivityHandler d) {
			this.activityHandlers.add(d);
			activityHandlers.sort(Comparator.comparingDouble(DistributedActivityHandler::priority).reversed());
		}

		if (component instanceof DistributedDepartureHandler d) {
			this.departureHandlers.add(d);
			departureHandlers.sort(Comparator.comparingDouble(DistributedDepartureHandler::priority).reversed());
		}

		if (component instanceof NetworkTrafficEngine n) {
			this.networkTrafficEngine = n;
		}
	}

	@Override
	public void onPrepareSim() {
		currentTime.setSimStartTime(getScenario().getConfig().qsim().getStartTime().orElse(0));

		for (DistributedMobsimEngine engine : engines) {
			engine.onPrepareSim();
		}

		for (DistributedAgentSource source : asc.getAgentSources()) {
			source.createAgentsAndVehicles(partition, this);
		}

		listenerManager.fireQueueSimulationInitializedEvent();
	}

	@Override
	public void doSimStep(double time) {

		currentTime.setTime(time);

		listenerManager.fireQueueSimulationBeforeSimStepEvent(time);

		for (MobsimEngine engine : engines) {
			engine.doSimStep(time);
		}

		messaging.sendMessages(time);

		listenerManager.fireQueueSimulationAfterSimStepEvent(time);
	}

	@Override
	public void onCleanupSim() {

		listenerManager.fireQueueSimulationBeforeCleanupEvent();

		for (DistributedMobsimEngine engine : engines) {
			engine.afterSim();
		}

	}

	@Override
	public void process(SimStepMessage msg) {

		assert msg.simstep() <= currentTime.getTimeOfDay() : "Message time (%.2f) does not match current time (%.2f)".formatted(msg.simstep(), currentTime.getTimeOfDay());

		for (DistributedMobsimEngine engine : engines) {
			engine.process(msg, currentTime.getTimeOfDay());
		}
	}

	@Override
	public void arrangeNextAgentState(MobsimAgent agent) {

		if (!(agent instanceof DistributedMobsimAgent dma)) {
			throw new IllegalArgumentException(
				"Distributed Simulation only works with DistributedMobsimAgent implementations. " +
					"Even though the interface only requires MobsimAgent. Provided agent is of type: " + agent.getClass());
		}

		switch (agent.getState()) {
			case ACTIVITY -> arrangeAgentActivity(dma);
			case LEG -> arrangeAgentDeparture(dma, currentTime.getTimeOfDay());
			case ABORT -> em.processEvent(new PersonStuckEvent(currentTime.getTimeOfDay(), agent.getId(), agent.getCurrentLinkId(), agent.getMode()));
		}
	}

	private void arrangeAgentActivity(final DistributedMobsimAgent agent) {
		for (ActivityHandler activityHandler : activityHandlers) {
			if (activityHandler.handleActivity(agent)) {
				return;
			}
		}
	}

	private void arrangeAgentDeparture(DistributedMobsimAgent agent, double now) {

		String routingMode = routingModeOrNull(agent);
		em.processEvent(new PersonDepartureEvent(
			now, agent.getId(), agent.getCurrentLinkId(), agent.getMode(), routingMode
		));

		Id<Link> linkId = agent.getCurrentLinkId();

		// Try to handle departure with standard qsim handlers
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, agent, linkId)) {
				return;
			}
		}
	}

	private String routingModeOrNull(MobsimAgent agent) {
		return agent instanceof PlanAgent pa ? TripStructureUtils.getRoutingMode((Leg) pa.getCurrentPlanElement()) : null;
	}

	@Override
	public void registerAdditionalAgentOnLink(MobsimAgent agent) {
		Id<Link> link = agent.getCurrentLinkId();
		if (partition.containsLink(link))
			agents.put(agent.getId(), agent);
		else
			throw new IllegalArgumentException("Agent " + agent.getId() + " is not on a link in the partition");
	}

	@Override
	public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
		if (partition.containsLink(linkId))
			return agents.remove(agentId);
		else
			throw new IllegalArgumentException("Link " + linkId + " is not on a link in the partition");
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		for (ActivityHandler activityHandler : activityHandlers) {
			Gbl.assertNotNull(activityHandler);
			activityHandler.rescheduleActivityEnd(agent);
		}
	}

	@Override
	public IntSet waitForOtherRanks(double time) {
		return partition.getNeighbors();
	}

	@Override
	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		networkTrafficEngine.addParkedVehicle(veh, startLinkId);
	}

	@Override
	public void insertAgentIntoMobsim(MobsimAgent agent) {
		arrangeNextAgentState(agent);
	}

	@Override
	public NetsimNetwork getNetsimNetwork() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EventsManager getEventsManager() {
		return em;
	}

	@Override
	public AgentCounter getAgentCounter() {
		return agentCounter;
	}

	@Override
	public Scenario getScenario() {
		return scenario;
	}

	@Override
	public MobsimTimer getSimTimer() {
		return currentTime;
	}

	@Override
	public Collection<AgentTracker> getAgentTrackers() {
		return List.of();
	}

	@Override
	public VisNetwork getVisNetwork() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Id<Person>, MobsimAgent> getAgents() {
		// TODO: Few engines might need this, but did not occurred yet
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Id<Vehicle>, MobsimVehicle> getVehicles() {
		// TODO: Chained departure will need this feature
		// Should be possible via network traffic engine
		throw new UnsupportedOperationException();
	}

	@Override
	public VisData getNonNetworkAgentSnapshots() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	@Override
	public Netsim getMobsim() {
		return this;
	}

	@Override
	public Collection<? extends DepartureHandler> getDepartureHandlers() {
		return departureHandlers;
	}

	@Override
	public void run() {
		// This method is not called
	}
}
