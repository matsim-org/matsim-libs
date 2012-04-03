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

package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListenerManager;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounterI;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.UmlaufDriver;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * This has developed over the last couple of months/years towards an increasingly pluggable module.  The current (dec'2011)
 * approach consists of the following elements (and presumably more, developed by mzilske):<ul>
 * <li> QSim itself should have all basic functionality to execute a typical agent plan, i.e. activities and legs.  In this basic
 * version, all legs are teleported.
 * <li> In addition, there are "engines" that plug into QSim.  Those are time-step driven, as is QSim.  Many engines move
 * particles around, i.e. they execute the different modes.  Others are responsible for, e.g., time-variant networks or signals. 
 * <li> A special engine is the netsim engine, which is the original "queue"
 * engine.  It is invoked by default, and it carries the "NetsimNetwork" for which there is a getter.
 * <li> Engines that move particles around need to be able to "end legs".  
 * This used to be such that control went to the agents, which
 * reinserted themselves into QSim.  This has now been changed: The agents compute their next state, but the engines are
 * responsible for reinsertion into QSim.  For this, they obtain an "internal interface" during engine addition.  Naming 
 * conventions will be adapted to this in the future.
 * <li> <i>A caveat is that drivers that move around other agents (such as TransitDriver, TaxicabDriver) need to become
 * "engines".</i>  Possibly, something that executes a leg is not really the same as an "engine", but this is what we have
 * for the time being.
 * <li> Engines that offer new modes also need to be registered as "DepartureHandler"s.
 *  * </ul>
 * Future plans include: pull the agent counter write methods back into QSim (no big deal, I hope); pull the actstart/end, 
 * agent departure/arrival back into QSim+engines; somewhat separate the teleportation engine and the activities engine from the
 * framework part of QSim. 
 * <p/>
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public final class QSim implements VisMobsim, Netsim {

	final private static Logger log = Logger.getLogger(QSim.class);


	/** time since last "info" */
	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;

	private final EventsManager events;

	private QNetsimEngine netEngine;
	private MultiModalSimEngine multiModalEngine = null;

	private Collection<MobsimEngine> mobsimEngines = new ArrayList<MobsimEngine>();

	private MobsimTimer simTimer;

	/**
	 * Includes all agents that have transportation modes unknown to the
	 * QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue<Tuple<Double, MobsimAgent>>(
			30, new TeleportationArrivalTimeComparator());

	private ActivityEngine activityEngine;

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100 * 3600;
	private final MobsimListenerManager listenerManager;
	private final Scenario scenario;
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();
	private AgentCounter agentCounter;
	private Collection<MobsimAgent> agents = new LinkedHashSet<MobsimAgent>();
	private List<AgentSource> agentSources = new ArrayList<AgentSource>();
	private TransitQSimEngine transitEngine;


	/*package (for tests)*/ InternalInterface internalInterface = new InternalInterface() {
		@Override
		public final void arrangeNextAgentState(MobsimAgent agent) {
			QSim.this.arrangeNextAgentAction(agent) ;
		}

		@Override
		public final Netsim getMobsim() {
			return QSim.this ;
		}

		@Override
		public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
			QSim.this.netEngine.registerAdditionalAgentOnLink(planAgent);
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
			return QSim.this.netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
		}


	};

	// everything above this line is private and should remain private. pls
	// contact me if this is in your way. kai, oct'10
	// ============================================================================================================================
	// initialization:


	public static QSim createQSimAndAddAgentSource(final Scenario sc, final EventsManager events, final QNetsimEngineFactory netsimEngFactory) {
		QSim qSim = createQSimWithDefaultEngines(sc, events, netsimEngFactory);
		AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	public static QSim createQSimAndAddAgentSource(final Scenario scenario, final EventsManager events) {
		return createQSimAndAddAgentSource(scenario, events, new DefaultQSimEngineFactory());
	}


	public static QSim createQSimWithDefaultEngines(Scenario sc, EventsManager events, QNetsimEngineFactory netsimEngFactory) {
		QSim qSim = new QSim(sc, events);
		qSim.addMobsimEngine(new ActivityEngine());
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
		qSim.addMobsimEngine(netsimEngine);
		return qSim;
	}

	private QSim(final Scenario sc, final EventsManager events) {
		this.scenario = sc;
		this.events = events;
		log.info("Using QSim...");
		this.listenerManager = new MobsimListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new MobsimTimer(sc.getConfig().getQSimConfigGroup().getTimeStepSize());
	}

	// ============================================================================================================================
	// "run" method:


	@Override
	public void run() {
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
		cleanupSim();
	}

	// ============================================================================================================================
	// prepareSim and related:

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	/*package*/ void prepareSim() {
		if (events == null) {
			throw new RuntimeException(
					"No valid Events Object (events == null)");
		}

		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}

		createAgents();
		this.initSimTimer();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already
	}

	private void createAgents() {
		for (AgentSource agentSource : agentSources) {
			agentSource.insertAgentsIntoMobsim();
		}
	}

	public void createAndParkVehicleOnLink(Vehicle vehicle, Id linkId) {
		QVehicle veh = new QVehicle(vehicle, vehicle.getType().getPcuEquivalents());
		netEngine.addParkedVehicle(veh, linkId);
	}

	@Override
	public void addParkedVehicle(MobsimVehicle veh, Id startLinkId) {
		netEngine.addParkedVehicle(veh, startLinkId);
	}

	private void cleanupSim() {
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
	}

	/**
	 * Do one step of the simulation run.
	 * 
	 * @param time
	 *            the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	/*package*/ boolean doSimStep(final double time) {

		// teleportation "engine":
		this.handleTeleportationArrivals();

		// "added" engines
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.doSimStep(time);
		}
		// console printout:
		this.printSimLog(time);
		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	private void handleTeleportationArrivals() {
		double now = this.getSimTimer().getTimeOfDay();
		while (this.teleportationList.peek() != null) {
			Tuple<Double, MobsimAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyTeleportToLink(personAgent
						.getDestinationLinkId());
				personAgent.endLegAndComputeNextState(now);
				this.internalInterface.arrangeNextAgentState(personAgent) ;
			} else {
				break;
			}
		}
	}

	@Override
	public void insertAgentIntoMobsim( MobsimAgent agent ) {
		if ( this.agents.contains(agent) ) {
			throw new RuntimeException("agent is already in mobsim; aborting ...") ;
		}
		agents.add(agent);
		this.agentCounter.incLiving();
		arrangeNextAgentAction(agent);
	}

	private void arrangeNextAgentAction(MobsimAgent agent) {
		switch( agent.getState() ) {
		case ACTIVITY: 
			this.activityEngine.arrangeActivityStart(agent); 
			break ;
		case LEG: 
			this.arrangeAgentDeparture(agent) ; 
			break ;
		case ABORT:
			this.events.processEvent( this.events.getFactory().createAgentStuckEvent(
					this.simTimer.getTimeOfDay(), agent.getId(), agent.getCurrentLinkId(), agent.getMode()
					)) ;

			this.agents.remove(agent) ;
			this.agentCounter.decLiving();
			this.agentCounter.incLost();
			break ;
		default:
			throw new RuntimeException("agent with unknown state (possibly null)") ;
		}
	}

	@Override
	public void rescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime ) {
		activityEngine.rescheduleActivityEnd(agent, oldTime, newTime);
	}


	/**
	 * Informs the simulation that the specified agent wants to depart from its
	 * current activity. The simulation can then put the agent onto its vehicle
	 * on a link or teleport it to its destination.
	 * 
	 * @param agent
	 */
	private void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = this.getSimTimer().getTimeOfDay();
		String mode = agent.getMode();
		Id linkId = agent.getCurrentLinkId();
		events.processEvent(events.getFactory().createAgentDepartureEvent(now,
				agent.getId(), linkId, mode));

		// The following seems like a good idea, but it does not work when agents have round trips (such as cruising, going
		// for a hike, or driving a bus).  kai, nov'11
		// I think now it works.  kai, dec'11
		if ( ! (agent instanceof UmlaufDriver) ) {
			// (UmlaufDriver somehow is different. kai, dec'11)
			if ( mode.equals(TransportMode.car) && linkId.equals(agent.getDestinationLinkId())) {
				// (yyyy "car" is the current convention; there is a test that checks if "walk" goes through. :-( kai, dec'11)
				if ( agent instanceof DriverAgent ) {
					if ( ((DriverAgent)agent).chooseNextLinkId() == null ) {

						// no physical travel is necessary.  We still treat this as a departure and an arrival, since there is a 
						// "leg".  Some of the design allows to have successive activities without invervening legs, but this is not 
						// consistently implemented.  One could also decide to not have these departure/arrival events here
						// (we would still have actEnd/actStart events).  kai, nov'11

						//					events.processEvent(events.getFactory().createAgentArrivalEvent(now, agent.getId(), linkId, mode)) ;
						// (arrival event currently in agent.  kai, dec'11)

						agent.endLegAndComputeNextState(now) ;
						this.internalInterface.arrangeNextAgentState(agent) ;
						return ;
					}
				} 
			}
		}

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
		QSimConfigGroup qSimConfigGroup = this.scenario.getConfig().getQSimConfigGroup();
		Double startTime = qSimConfigGroup.getStartTime();
		this.stopTime = qSimConfigGroup.getEndTime();
		if (startTime == Time.UNDEFINED_TIME) {
			startTime = 0.0;
		}
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) {
			this.stopTime = Double.MAX_VALUE;
		}

		double simStartTime = 0;
		if (QSimConfigGroup.MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END.equals(qSimConfigGroup.getSimStarttimeInterpretation())) {
			Double nextActivityEndTime = activityEngine.getNextActivityEndTime();
			if (nextActivityEndTime != null) {
				simStartTime = Math.floor(Math.max(startTime, nextActivityEndTime));
			}
		} else if (QSimConfigGroup.ONLY_USE_STARTTIME.equals(qSimConfigGroup.getSimStarttimeInterpretation())) {
			simStartTime = startTime;
		} else {
			throw new RuntimeException("unkonwn starttimeInterpretation; aborting ...");
		}

		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(simStartTime);

	}

	

	// ############################################################################################################################
	// utility methods (presumably no state change)
	// ############################################################################################################################

	private void printSimLog(final double time) {
		if (time >= this.infoTime) {
			//		if(true){
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
				nofActiveLinks = this.multiModalEngine.getNumberOfSimulatedLinks();
				nofActiveNodes = this.multiModalEngine.getNumberOfSimulatedNodes();
				log.info("SIMULATION (MultiModalSim) AT " + Time.writeTime(time) 
						+ " #links=" + nofActiveLinks + " #nodes=" + nofActiveNodes);
			}

			Gbl.printMemoryUsage();
		}
	}


	// ############################################################################################################################
	// no real functionality beyond this point
	// ############################################################################################################################

	@Override
	public EventsManager getEventsManager() {
		return events;
	}

	@Override
	public NetsimNetwork getNetsimNetwork() {
		return this.netEngine.getNetsimNetwork();
	}

	@Override
	public VisNetwork getVisNetwork() {
		return this.netEngine.getNetsimNetwork();
	}

	@Override
	public Scenario getScenario() {
		return this.scenario;
	}

	@Override
	public MobsimTimer getSimTimer() {
		return this.simTimer;
	}

	/*package*/ MobsimEngine getNetsimEngine() {
		// For a test
		return this.netEngine;
	}

	// For a test 
	// the corresponding test could be moved into this package. kai
	public MultiModalSimEngine getMultiModalSimEngine() {
		return this.multiModalEngine;
	}

	public void addMobsimEngine(MobsimEngine mobsimEngine) {
		if (mobsimEngine instanceof TransitQSimEngine) {
			if ( this.transitEngine != null ) {
				log.warn("pre-existing transitEngine != null; will be overwritten; with the current design, " +
						"there can only be one TransitQSimEngine") ;
			}
			this.transitEngine = (TransitQSimEngine) mobsimEngine;
		} 
		if (mobsimEngine instanceof MultiModalSimEngine) {
			if ( this.multiModalEngine != null ) {
				log.warn("pre-existing multiModalEngine != null; will be overwritten; with the current design, " +
						"there can only be one MultiModalSimEngine") ;
			}
			this.multiModalEngine = (MultiModalSimEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof ActivityEngine) {
			this.activityEngine = (ActivityEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof QNetsimEngine) {
			this.netEngine = (QNetsimEngine) mobsimEngine;
			this.addDepartureHandler(this.netEngine.getDepartureHandler());

		}

		mobsimEngine.setInternalInterface(this.internalInterface);
		this.mobsimEngines.add(mobsimEngine);
	}

	@Override
	public AgentCounterI getAgentCounter() {
		return this.agentCounter;
	}

	public void addDepartureHandler(DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance given as parameters as listener
	 * to this QueueSimulation instance.
	 * 
	 * @param listeners
	 */
	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	/**
	 * Only OTFVis is allowed to use this. If you want access to the TransitQSimEngine,
	 * just "inline" the factory method of this class to plug together your own QSim, and you've got it!
	 * This getter will disappear very soon. michaz 11/11
	 */
	@Deprecated
	public TransitQSimEngine getTransitEngine() {
		return transitEngine;
	}

	@Override
	public Collection<MobsimAgent> getAgents() {
		return Collections.unmodifiableCollection(this.agents);
	}

	@Override
	public Collection<MobsimAgent> getActivityEndsList() {
		return activityEngine.getActivityEndsList();
	}

	public void addAgentSource(AgentSource agentSource) {
		agentSources.add(agentSource);
	}

}
