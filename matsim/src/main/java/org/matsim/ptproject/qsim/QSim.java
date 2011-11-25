/* *********************************************************************** *
 * project: org.matsim.*
 * QSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.AbstractTransitDriver;
import org.matsim.pt.qsim.TransitDriver;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.pt.qsim.UmlaufDriver;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.ptproject.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimLink;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * Implementation of a queue-based transport simulation. Lanes and SignalSystems
 * are not initialized unless the setter are invoked.
 * <p/>
 * 
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public final class QSim implements VisMobsim, Netsim {

	final private static Logger log = Logger.getLogger(QSim.class);

	/* time since last snapshot */
	private double snapshotTime = 0.0;
	private int snapshotPeriod = 0;

	/* time since last "info" */
	private double infoTime = 0;
	private static final int INFO_PERIOD = 3600;

	private final EventsManager events;

	public final QNetsimEngine netEngine;
	private NetworkChangeEventsEngine changeEventsEngine = null;
	private MultiModalSimEngine multiModalEngine = null;

	private Collection<MobsimEngine> mobsimEngines = new ArrayList<MobsimEngine>();

	private MobsimTimer simTimer;

	private Collection<MobsimAgent> transitAgents;

	/**
	 * Includes all agents that have transportation modes unknown to the
	 * QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue<Tuple<Double, MobsimAgent>>(
			30, new TeleportationArrivalTimeComparator());

	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private final Queue<MobsimAgent> activityEndsList = new PriorityBlockingQueue<MobsimAgent>(
			500, new PlanAgentDepartureTimeComparator());
	// can't use the "Tuple" trick from teleportation list, since we need to be
	// able to "find" agents for replanning. kai, oct'10
	// yy On second thought, this does also not work for the teleportationList
	// since we have the same problem there ... kai, oct'10

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100 * 3600;
	private final SimulationListenerManager listenerManager;
	private final Scenario scenario;
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();
	private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();
	private TransitQSimEngine transitEngine;
	private AgentCounterI agentCounter;
	private Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();
	private List<AgentSource> agentSources = new ArrayList<AgentSource>();

	// everything above this line is private and should remain private. pls
	// contact me if this is in your way. kai, oct'10
	// ============================================================================================================================
	// initialization:

	public QSim(final Scenario scenario, final EventsManager events) {
		this(scenario, events, new DefaultQSimEngineFactory());
	}

	public QSim(final Scenario sc, final EventsManager events,
			final QNetsimEngineFactory netsimEngFactory) {
		this.scenario = sc;
		this.events = events;
		log.info("Using QSim...");
		this.listenerManager = new SimulationListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new MobsimTimer(sc.getConfig().getQSimConfigGroup()
				.getTimeStepSize());

		// create the NetworkEngine ...
		this.netEngine = netsimEngFactory.createQSimEngine(this, MatsimRandom.getRandom());




		this.addDepartureHandler(this.netEngine.getDepartureHandler());

		// configuring multiModalEngine ...
		if (sc.getConfig().multiModal().isMultiModalSimulationEnabled()) {

			// create MultiModalSimEngine
			multiModalEngine = new MultiModalSimEngineFactory()
			.createMultiModalSimEngine(this);

			// add MultiModalDepartureHandler
			this.addDepartureHandler(new MultiModalDepartureHandler(this,
					multiModalEngine, scenario.getConfig().multiModal()));

		}

		// set the agent factory. might be better to have this in the c'tor, but
		// difficult to do as long
		// as the transitEngine changes the AgentFactory. kai, feb'11

		this.addAgentSource(new PopulationAgentSource(sc.getPopulation(), new DefaultAgentFactory(this), this));

		// configuring transit (this changes the agent factory as a side
		// effect).
		if (sc.getConfig().scenario().isUseTransit()) {
			this.transitEngine = new TransitQSimEngine(this);
			this.addDepartureHandler(this.transitEngine);
		}
	}

	// ============================================================================================================================
	// "run" method:

	private boolean locked = false;

	@Override
	public final void run() {
		this.locked = true;
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		// do iterations
		boolean doContinue = true;
		double time = this.simTimer.getTimeOfDay();
		while (doContinue) {
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			doContinue = doSimStep(time);
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (doContinue) {
				time = this.simTimer.incrementTime();
			}
		}
		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();
		cleanupSim(time);

		// delete reference to clear memory
		// this.listenerManager = null;
	}

	// ============================================================================================================================
	// setters that should reasonably be called between constructor and
	// "prepareSim" (triggered by "run"):

	// yy my current intuition is that those should be set in a factory, and the
	// factory should pass them as immutable
	// into the QSim. But I can't change into that direction because the
	// TransitEngine changes the AgentFactory fairly
	// late in the initialization sequence ...

	@Override
	public void setAgentFactory(final AgentFactory fac) {
		if (!locked ) {
			if ( this.agentSources.size() == 1) {
				this.agentSources.clear();
				this.addAgentSource(new PopulationAgentSource(this.scenario.getPopulation(), fac, this));
			} else {
				throw new RuntimeException("there is more than one AgentSource; cannot override the " +
						"agent factory since the code " +
						"would not know to which AgentSource this should refer to.\nTry first changing the agentFactory " +
						"and only then adding other agent sources." ) ;
			}
		} else {
			throw new RuntimeException(
					"too late to set agent factory; aborting ...");
		}
	}

	// ============================================================================================================================
	// prepareSim and related:

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	protected final void prepareSim() {
		if (events == null) {
			throw new RuntimeException(
					"No valid Events Object (events == null)");
		}

		if (this.netEngine != null) {
			this.netEngine.onPrepareSim();
		}
		if (this.multiModalEngine != null) {
			this.multiModalEngine.onPrepareSim();
		}
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}

		createAgents();
		createTransitDrivers();

		this.initSimTimer();

		QSimConfigGroup qSimConfigGroup = this.scenario.getConfig()
				.getQSimConfigGroup();
		this.snapshotPeriod = (int) qSimConfigGroup.getSnapshotPeriod();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already
		this.snapshotTime = Math.floor(this.simTimer.getSimStartTime()
				/ this.snapshotPeriod)
				* this.snapshotPeriod;

		this.snapshotPeriod = (int) qSimConfigGroup.getSnapshotPeriod();
		if (this.snapshotTime < this.simTimer.getSimStartTime()) {
			this.snapshotTime += this.snapshotPeriod;
		}

		this.changeEventsEngine = new NetworkChangeEventsEngine(this);
		if (this.changeEventsEngine != null) {
			this.changeEventsEngine.onPrepareSim();
		}
	}

	private void createAgents() {
		for (AgentSource agentSource : agentSources) {
			List<MobsimAgent> theseAgents = agentSource.insertAgentsIntoMobsim();
			for (MobsimAgent agent : theseAgents) {
				agents.add(agent);
			}
		}
	}

	public void createAndParkVehicleOnLink(Vehicle vehicle, Id linkId) {
		QVehicle veh = new QVehicle(vehicle);
		netEngine.addParkedVehicle(veh, linkId);
	}

	@Override
	public void addParkedVehicle(MobsimVehicle veh, Id startLinkId) {
		netEngine.addParkedVehicle(veh, startLinkId);
	}

	private void createTransitDrivers() {
		if (this.transitEngine != null) {
			Collection<MobsimAgent> a = this.transitEngine.createAdditionalAgents();
			this.transitAgents = a;
			agents.addAll(a);
		}
	}




	// ============================================================================================================================
	// "cleanupSim":

	/**
	 * Close any files, etc.
	 */
	protected final void cleanupSim(final double seconds) {
		if (this.transitEngine != null) { // yyyy do after features
			this.transitEngine.afterSim();
		}

		if (this.netEngine != null) {
			this.netEngine.afterSim();
		}

		if (this.multiModalEngine != null) {
			this.multiModalEngine.afterSim();
		}
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.afterSim();
		}

		double now = this.simTimer.getTimeOfDay();

		for (Tuple<Double, MobsimAgent> entry : this.teleportationList) {
			MobsimAgent agent = entry.getSecond();
			events.processEvent(events.getFactory().createAgentStuckEvent(now,
					agent.getId(), agent.getDestinationLinkId(),
					agent.getMode()));
		}
		this.teleportationList.clear();

		for (MobsimAgent agent : this.activityEndsList) {
			if (agent instanceof UmlaufDriver) {
				log.error("this does not terminate correctly for UmlaufDrivers; needs to be "
						+ "fixed but for the time being we skip the next couple of lines.  kai, dec'10");
			} else {
				if (agent.getDestinationLinkId() != null) {
					events.processEvent(events.getFactory()
							.createAgentStuckEvent(now, agent.getId(),
									agent.getDestinationLinkId(), null));
				}
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}

		// this.netEngine = null;
		// this.events = null; // delete events object to free events handlers,
		// if they are nowhere else referenced
	}

	/**
	 * Do one step of the simulation run.
	 * 
	 * @param time
	 *            the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	protected final boolean doSimStep(final double time) { // do not overwrite
		// in inheritance.
		// (network) change events engine:
		if (this.changeEventsEngine != null) {
			this.changeEventsEngine.doSimStep(time);
		}
		// teleportation "engine":
		this.handleTeleportationArrivals();

		// "facilities" "engine":
		this.handleActivityEnds(time);

		// network engine:
		if (this.netEngine != null) {
			this.netEngine.doSimStep(time);
		}

		// multi modal engine:
		if (this.multiModalEngine != null) {
			this.multiModalEngine.doSimStep(time);
		}

		// "added" engines
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.doSimStep(time);
		}

		// console printout:
		this.printSimLog(time);

		// snapshots:
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}

		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	private final void handleTeleportationArrivals() {
		double now = this.getSimTimer().getTimeOfDay();
		while (this.teleportationList.peek() != null) {
			Tuple<Double, MobsimAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyTeleportToLink(personAgent
						.getDestinationLinkId());
				personAgent.endLegAndAssumeControl(now);
			} else {
				break;
			}
		}
	}
	
	public final void arrangeNextAgentAction( MobsimAgent agent ) {
		// yy the material of the methods could probably be inlined ... but this is not possible as 
		// long as they are public.  kai, nov'11
		
		switch( agent.getState() ) {
		case ACTIVITY: 
			this.arrangeActivityStart(agent) ; 
			break ;
		case LEG: 
			this.arrangeAgentDeparture(agent) ; 
			break ;
		case ABORT:
			this.getAgentCounter().decLiving();
			this.getAgentCounter().incLost();
			break ;
		default:
			throw new RuntimeException("agent with unknown state (possibly null)") ;
		}
	}

	/**
	 * Registers this agent as performing an activity and makes sure that the
	 * agent will be informed once his departure time has come.
	 * 
	 * @param agent
	 * 
	 * @see MobsimDriverAgent#getActivityEndTime()
	 */
//	@Override
	private final void arrangeActivityStart(final MobsimAgent agent) {
		this.activityEndsList.add(agent);
		if (!(agent instanceof AbstractTransitDriver)) {
			netEngine.registerAdditionalAgentOnLink(agent);
		}
	}

	@Override
	public final void rescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime ) {
		// yyyy quite possibly, this should be "notifyChangedPlan".  kai, oct'10
		// yy the "newTime" is strictly speaking not necessary.  kai, oct'10

		// remove agent from queue
		this.activityEndsList.remove(agent);

		// The intention in the following is that an agent that is no longer alive has an activity end time of infinity.  The number of
		// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11
		/*
		 * If an agent performs only a single iteration, the old departure time is Time.UNDEFINED which
		 * is Double.NEGATIVE_INFINITY. If an agent performs the last of several activities, the old
		 * departure time is Double.POSITIVE_INFINITY.
		 * If an agent is (re)activated, it is also (un)registered at an activity location. cdobler, oct'11
		 */
		if (oldTime == Double.POSITIVE_INFINITY || oldTime == Time.UNDEFINED_TIME) {
			if (newTime == Double.POSITIVE_INFINITY) {
				// agent was de-activated and still should be de-activated - nothing to do here
			} else {
				// newTime != Double.POSITIVE_INFINITY - re-activate the agent
				this.activityEndsList.add(agent);
				this.netEngine.registerAdditionalAgentOnLink(agent);
				this.getAgentCounter().incLiving();				
			}
		} 
		/*
		 * After the re-planning the agent's current activity has changed to its last activity.
		 * Therefore the agent is de-activated. cdobler, oct'11
		 */
		else if (newTime == Double.POSITIVE_INFINITY) {
			this.unregisterAgentAtActivityLocation(agent);
			this.getAgentCounter().decLiving();
		} 
		/*
		 *  The activity is just rescheduled during the day, so we keep the agent active. cdobler, oct'11
		 */
		else {
			this.activityEndsList.add(agent);
		}
	}

	@Override
	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		netEngine.registerAdditionalAgentOnLink(planAgent);
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		if (!(agent instanceof TransitDriver)) {
			Id agentId = agent.getId();
			Id linkId = agent.getCurrentLinkId();
			netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
	}

	@Override
	public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
		return netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			MobsimAgent agent = this.activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndAssumeControl(time);
				// gives control to agent; comes back via "agentDeparts" or
				// "scheduleActivityEnd"
			} else {
				return;
			}
		}
	}

	/**
	 * Informs the simulation that the specified agent wants to depart from its
	 * current activity. The simulation can then put the agent onto its vehicle
	 * on a link or teleport it to its destination.
	 * 
	 * @param agent
	 */
	@Override
	public final void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = this.getSimTimer().getTimeOfDay();
		String mode = agent.getMode();
		Id linkId = agent.getCurrentLinkId();
		events.processEvent(events.getFactory().createAgentDepartureEvent(now,
				agent.getId(), linkId, mode));

		// The following seems like a good idea, but it does not work when agents have round trips (such as cruising, going
		// for a hike, or driving a bus).  kai, nov'11
		//		if ( linkId.equals(agent.getDestinationLinkId())) {
		//			// no physical travel is necessary.  We still treat this as a departure and an arrival, since there is a 
		//			// "leg".  Some of the design allows to have successive activities without invervening legs, but this is not 
		//			// consistently implemented.  One could also decide to not have these departure/arrival events here
		//			// (we would still have actEnd/actStart events).  kai, nov'11
		//			events.processEvent(events.getFactory().createAgentArrivalEvent(now, agent.getId(), linkId, mode)) ;
		//			agent.endLegAndAssumeControl(now) ;
		//			return ;
		//		}

		if (handleKnownLegModeDeparture(now, agent, linkId)) {
			return;
		} else {
			handleUnknownLegMode(now, agent);
			events.processEvent(new AdditionalTeleportationDepartureEvent(now,
					agent.getId(), linkId, mode, agent.getDestinationLinkId(),
					agent.getExpectedTravelTime()));
		}
	}

	private void handleUnknownLegMode(final double now,
			final MobsimAgent planAgent) {
		double arrivalTime = now + planAgent.getExpectedTravelTime();
		this.teleportationList.add(new Tuple<Double, MobsimAgent>(arrivalTime,
				planAgent));
	}

	private boolean handleKnownLegModeDeparture(final double now,
			final MobsimAgent planAgent, final Id linkId) {
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, planAgent, linkId)) {
				return true;
			}
			// The code is not (yet?) very beautiful. But structurally, this
			// goes through all departure handlers and tries to
			// find one that feels responsible. If it feels responsible, it
			// returns true, and so this method returns true.
			// Otherwise, this method will return false, and then teleportation
			// will be called. kai, jun'10
		}
		return false;
	}

	// ############################################################################################################################
	// private methods
	// ############################################################################################################################

	private void initSimTimer() {
		QSimConfigGroup qSimConfigGroup = this.scenario.getConfig()
				.getQSimConfigGroup();
		Double startTime = qSimConfigGroup.getStartTime();
		this.stopTime = qSimConfigGroup.getEndTime();
		if (startTime == Time.UNDEFINED_TIME) {
			startTime = 0.0;
		}
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) {
			this.stopTime = Double.MAX_VALUE;
		}

		double simStartTime = 0;
		if (QSimConfigGroup.MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END
				.equals(qSimConfigGroup.getSimStarttimeInterpretation())) {
			MobsimAgent firstAgent = this.activityEndsList.peek();
			if (firstAgent != null) {
				// set sim start time to config-value ONLY if this is LATER than
				// the first plans starttime
				simStartTime = Math.floor(Math.max(startTime,
						firstAgent.getActivityEndTime()));
			}
		} else if (QSimConfigGroup.ONLY_USE_STARTTIME.equals(qSimConfigGroup
				.getSimStarttimeInterpretation())) {
			simStartTime = startTime;
		} else {
			throw new RuntimeException(
					"unkonwn starttimeInterpretation; aborting ...");
		}

		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(simStartTime);

	}

	// ############################################################################################################################
	// utility methods (presumably no state change)
	// ############################################################################################################################

	private void printSimLog(final double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.realWorldStarttime
					.getTime()) / 1000;
			double diffsim = time - this.simTimer.getSimStartTime();
			int nofActiveLinks = this.netEngine.getNumberOfSimulatedLinks();
			int nofActiveNodes = this.netEngine.getNumberOfSimulatedNodes();
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time)
					+ " : #Veh=" + this.agentCounter.getLiving() + " lost="
					+ this.agentCounter.getLost() + " #links=" + nofActiveLinks
					+ " #nodes=" + nofActiveNodes + " simT=" + diffsim
					+ "s realT=" + (diffreal) + "s; (s/r): "
					+ (diffsim / (diffreal + Double.MIN_VALUE)));

			if (this.multiModalEngine != null) {
				nofActiveLinks = this.multiModalEngine
						.getNumberOfSimulatedLinks();
				nofActiveNodes = this.multiModalEngine
						.getNumberOfSimulatedNodes();
				log.info("SIMULATION (MultiModalSim) AT "
						+ Time.writeTime(time) + " #links=" + nofActiveLinks
						+ " #nodes=" + nofActiveNodes);
			}

			Gbl.printMemoryUsage();
		}
	}

	private void doSnapshot(final double time) {
		if (!this.snapshotWriters.isEmpty()) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
			for (NetsimLink link : this.getNetsimNetwork().getNetsimLinks()
					.values()) {
				link.getVisData().getVehiclePositions(positions);
			}
			for (SnapshotWriter writer : this.snapshotWriters) {
				writer.beginSnapshot(time);
				for (AgentSnapshotInfo position : positions) {
					writer.addAgent(position);
				}
				writer.endSnapshot();
			}
		}
	}

	// ############################################################################################################################
	// no real functionality beyond this point
	// ############################################################################################################################

	@Override
	public final EventsManager getEventsManager() {
		return events;
	}

	@Override
	public final NetsimNetwork getNetsimNetwork() {
		return this.netEngine.getNetsimNetwork();
	}

	@Override
	public final VisNetwork getVisNetwork() {
		return this.netEngine.getNetsimNetwork();
	}

	@Override
	public final Scenario getScenario() {
		return this.scenario;
	}

	public final MultiModalSimEngine getMultiModalSimEngine() {
		return this.multiModalEngine;
	}

	@Override
	public final MobsimTimer getSimTimer() {
		return this.simTimer;
	}

	// Just for one test. Not happy. michaz 11/11
	public final MobsimEngine getNetsimEngine() {
		return this.netEngine;
	}

	@Override
	public final void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotWriters.add(snapshotWriter);
	}

	public final void addMobsimEngine(MobsimEngine mobsimEngine) {
		this.mobsimEngines.add(mobsimEngine);
	}

	@Override
	public final AgentCounterI getAgentCounter() {
		return this.agentCounter;
	}

	public final void addDepartureHandler(
			final DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance given as parameters as listener
	 * to this QueueSimulation instance.
	 * 
	 * @param listeners
	 */
	@Override
	public final void addQueueSimulationListeners(
			final SimulationListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	public final TransitQSimEngine getTransitEngine() {
		return transitEngine;
	}

	@Override
	public final Collection<MobsimAgent> getAgents() {
		return Collections.unmodifiableCollection(this.agents);
	}

	@Override
	public final Collection<MobsimAgent> getActivityEndsList() {
		return Collections.unmodifiableCollection(activityEndsList);
	}

	public final Collection<MobsimAgent> getTransitAgents() {
		return Collections.unmodifiableCollection(this.transitAgents);
	}

	public final void addAgentSource(AgentSource agentSource) {
		agentSources.add(agentSource);
	}

}
