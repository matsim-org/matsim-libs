package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.messages.SimStepMessageProcessor;
import org.matsim.dsim.simulation.net.NetworkTrafficEngine;
import org.matsim.dsim.simulation.pt.DistributedPtEngine;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisNetwork;

import java.util.*;

@Log4j2
public class SimProcess implements Steppable, LP, SimStepMessageProcessor, Netsim, InternalInterface {

	// The Qsim has flexible engines. However, Activity, Teleportation and Netsim Engine are treated
	// in a special way. I'll have them as explicit members here, until we need more flexibility.
	private final Collection<? extends DistributedMobsimEngine> engines;
	private final DistributedTeleportationEngine teleportationEngine;
	private final NetworkTrafficEngine networkTrafficEngine;
	private final DistributedPtEngine ptEngine;

	private final SimStepMessaging messaging;
	private final NetworkPartition partition;

	private final QSimCompatibility qsim;

	private final EventsManager em;
	//private final Config config;
	private final Set<String> mainModes;
	private final MobsimTimer currentTime;
	private final AgentCounter agentCounter = new DummyAgentCounter();

	SimProcess(NetworkPartition partition, SimStepMessaging messaging, QSimCompatibility qsim, DistributedTeleportationEngine teleportationEngine,
			   NetworkTrafficEngine networkTrafficEngine, EventsManager em, Config config) {
		this.partition = partition;
		this.qsim = qsim;
		this.em = em;
		this.messaging = messaging;
		this.teleportationEngine = teleportationEngine;
		this.networkTrafficEngine = networkTrafficEngine;
		this.currentTime = new MobsimTimer();
		this.mainModes = new HashSet<>(config.qsim().getMainModes());
		log.info("#{} has {} links, and {} nodes",
			messaging.getPart(),
			networkTrafficEngine.getSimNetwork().getLinks().size(),
			networkTrafficEngine.getSimNetwork().getNodes().size());

		// There can be components that register additional event handler
		// These must be created the simulation starts running
		this.qsim.init(this);

		// TODO this is a little hacky and should be done differently. But we need the sim network which is created with constructor injection and
		// the transitqSimEngine, which is bound via injector, but only after qsim.init(this) was called
		var qsimTransitEngine = qsim.getQsimInjector().getInstance(TransitQSimEngine.class);
		ptEngine = new DistributedPtEngine(getScenario(), qsimTransitEngine, networkTrafficEngine.getSimNetwork());
		this.engines = List.of(ptEngine, networkTrafficEngine, teleportationEngine);
		qsim.getDepartureHandlers().remove(qsimTransitEngine);
		qsim.getEngines().remove(qsimTransitEngine);
	}

	@Override
	public void onPrepareSim() {
		currentTime.setSimStartTime(getScenario().getConfig().qsim().getStartTime().orElse(0));

		for (DistributedMobsimEngine engine : engines) {
			engine.setInternalInterface(this);
		}

		for (MobsimEngine engine : qsim.getEngines()) {
			engine.setInternalInterface(this);
			engine.onPrepareSim();
		}

		for (DistributedAgentSource source : qsim.getAgentSources()) {
			source.createAgentsAndVehicles(partition, this);
		}
	}

	@Override
	public void doSimStep(double time) {

		this.currentTime.setTime(time);

		// Within day engine must be processed first
		if (this.qsim.getWithinDayEngine() != null)
			this.qsim.getWithinDayEngine().doSimStep(time);

		for (MobsimEngine engine : qsim.getEngines()) {

			if (engine == this.qsim.getWithinDayEngine())
				continue;

			engine.doSimStep(time);
		}

		for (DistributedMobsimEngine engine : engines) {
			engine.doSimStep(time);
		}

		messaging.sendMessages(time);
	}

	@Override
	public void process(SimStepMessage msg) {

		assert msg.simstep() <= currentTime.getTimeOfDay() : "Message time (%.2f) does not match current time (%.2f)".formatted(msg.simstep(), currentTime.getTimeOfDay());

		for (DistributedMobsimEngine engine : engines) {
			engine.process(msg, currentTime.getTimeOfDay());
		}
	}

	@Override
	public IntSet waitForOtherRanks(double time) {
		return messaging.getNeighbors();
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
		return qsim.getInjector().getInstance(Scenario.class);
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
		// TODO: probably needed
		throw new UnsupportedOperationException();
	}

	@Override
	public VisData getNonNetworkAgentSnapshots() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		for (ActivityHandler activityHandler : qsim.getActivityEngines()) {
			Gbl.assertNotNull(activityHandler);
			activityHandler.rescheduleActivityEnd(agent);
		}
	}

	@Override
	public Netsim getMobsim() {
		return this;
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
		for (ActivityHandler activityHandler : this.qsim.getActivityEngines()) {
			if (activityHandler.handleActivity(agent)) {
				return;
			}
		}
	}

	@Override
	public void registerAdditionalAgentOnLink(MobsimAgent agent) {
		// This method is for visualization purposes only and not supported
	}

	@Override
	public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
		// This method is for visualization purposes only and not supported
		return null;
	}

	@Override
	public List<DepartureHandler> getDepartureHandlers() {
		return qsim.getDepartureHandlers();
	}

	@Override
	public void run() {
		// This method is not called
	}

	private void arrangeAgentDeparture(DistributedMobsimAgent agent, double now) {

		String routingMode = routingModeOrNull(agent);
		em.processEvent(new PersonDepartureEvent(
			now, agent.getId(), agent.getCurrentLinkId(), agent.getMode(), routingMode
		));

		Id<Link> linkId = agent.getCurrentLinkId();

		// TODO move the logic into the engines
		if (getScenario().getConfig().transit().getTransitModes().contains(agent.getMode())) {
			ptEngine.handleDeparture(now, agent, linkId);
		}

		// TODO: network traffic engine must check for itself if it is responsible for the agent
		if (mainModes.contains(agent.getMode())) {
			networkTrafficEngine.handleDeparture(now, agent, linkId);
			return;
		}

		// Try to handle departure with standard qsim handlers
		for (DepartureHandler departureHandler : this.qsim.getDepartureHandlers()) {
			if (departureHandler.handleDeparture(now, agent, linkId)) {
				return;
			}
		}

		// At last, teleport agent
		teleportationEngine.handleDeparture(now, agent, linkId);
	}

	private String routingModeOrNull(MobsimAgent agent) {
		return agent instanceof PlanAgent pa ? TripStructureUtils.getRoutingMode((Leg) pa.getCurrentPlanElement()) : null;
	}
}
