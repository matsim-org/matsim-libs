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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.AbstractTransitDriver;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitAgentFactory;
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
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngine;
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

	/** time since last snapshot */
	private double snapshotTime = 0.0;

	private int snapshotPeriod = 0;

	/** time since last "info" */
	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;

	private final EventsManager events;

	private final QNetsimEngine netEngine;
	private NetworkChangeEventsEngine changeEventsEngine = null;
	private MultiModalSimEngine multiModalEngine = null;

	private Collection<MobsimEngine> mobsimEngines = new ArrayList<MobsimEngine>();

	private MobsimTimer simTimer;

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
	private AgentCounterI agentCounter;
	private Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();
	private List<AgentSource> agentSources = new ArrayList<AgentSource>();
    private TransitQSimEngine transitEngine;
    
//    static boolean NEW = true ;

	/*package (for tests)*/ InternalInterface internalInterface = new InternalInterface() {
		@Override
		public final void arrangeNextAgentState(MobsimAgent agent) {
//			if ( NEW ) {
				QSim.this.arrangeNextAgentAction(agent) ;
//			} else {
//			}
		}

		@Override
		public final Netsim getMobsim() {
			return QSim.this ;
		}
		
		@Override
		public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
//			if ( NEW ) { 
				QSim.this.netEngine.registerAdditionalAgentOnLink(planAgent);
//			} else {
//			}
		}
		
		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
//			if ( NEW ) {
				return QSim.this.netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
//			} else {
//				return null ;
//			}
		}
		

	};
	
//	@Deprecated // to be replaced by internalInterface.arrangeNextAgentState()
//	public final void reInsertAgentIntoMobsim( MobsimAgent agent ) {
//		if ( NEW ) {
//		} else {
//			this.arrangeNextAgentAction( agent) ;
//		}
//	}


    // everything above this line is private and should remain private. pls
	// contact me if this is in your way. kai, oct'10
	// ============================================================================================================================
	// initialization:


    public static QSim createQSimAndAddAgentSource(final Scenario sc, final EventsManager events, final QNetsimEngineFactory netsimEngFactory) {
        QSim qSim = new QSim(sc, events, netsimEngFactory);
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
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
     }

     public static QSim createQSimAndAddAgentSource(final Scenario scenario, final EventsManager events) {
         return createQSimAndAddAgentSource(scenario, events, new DefaultQSimEngineFactory());
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
		this.netEngine.setInternalInterface(this.internalInterface) ;
		// (the netEngine is never ``added'', thus this needs to be done manually. kai, dec'11)



		this.addDepartureHandler(this.netEngine.getDepartureHandler());
	}

	// ============================================================================================================================
	// "run" method:


    @Override
	public final void run() {
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
	}

	// ============================================================================================================================
	// setters that should reasonably be called between constructor and
	// "prepareSim" (triggered by "run"):

	@Override
	@Deprecated // use agent source instead.  kai/michaz, nov'11
	public void setAgentFactory(final AgentFactory fac) {
		throw new RuntimeException();
	}

	// ============================================================================================================================
	// prepareSim and related:

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	 final void prepareSim() {
		if (events == null) {
			throw new RuntimeException(
					"No valid Events Object (events == null)");
		}

		if (this.netEngine != null) {
			this.netEngine.onPrepareSim();
		}

		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}

		createAgents();

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
		this.changeEventsEngine.setInternalInterface(internalInterface) ;
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




	// ============================================================================================================================
	// "cleanupSim":

	/**
	 * Close any files, etc.
	 */
	 final void cleanupSim(@SuppressWarnings("unused") final double seconds) {


		if (this.netEngine != null) {
			this.netEngine.afterSim();
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
				if ( agent.getActivityEndTime()!=Double.POSITIVE_INFINITY 
						&& agent.getActivityEndTime()!=Time.UNDEFINED_TIME ) {
					if (agent.getDestinationLinkId() != null) {
						events.processEvent(events.getFactory()
								.createAgentStuckEvent(now, agent.getId(),
										agent.getDestinationLinkId(), null));
					}
				}
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}

	}

	/**
	 * Do one step of the simulation run.
	 * 
	 * @param time
	 *            the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	 final boolean doSimStep(final double time) { // do not overwrite
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
				this.internalInterface.arrangeNextAgentState(personAgent) ;
			} else {
				break;
			}
		}
	}
	
	public final void insertAgentIntoMobsim( MobsimAgent agent ) {
		if ( this.agents.contains(agent) ) {
			throw new RuntimeException("agent is already in mobsim; aborting ...") ;
		}
		arrangeNextAgentAction(agent);
	}
	
	private void arrangeNextAgentAction(MobsimAgent agent) {
		switch( agent.getState() ) {
		case ACTIVITY: 
			this.arrangeActivityStart(agent) ; 
			break ;
		case LEG: 
			this.arrangeAgentDeparture(agent) ; 
			break ;
		case ABORT:
			this.agents.remove(agent) ;
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
		if ( agent.getActivityEndTime()==Double.POSITIVE_INFINITY ) {
			this.agentCounter.decLiving() ;
		}
	}

	@Override
	public final void rescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime ) {
		// yyyy quite possibly, this should be "notifyChangedPlan".  kai, oct'10
		// yy the "newTime" is strictly speaking not necessary.  kai, oct'10

		internalRescheduleActivityEnd(agent, oldTime, newTime);
	}


	private void internalRescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime) {
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

//	@Deprecated // use InternalInterface
//	@Override
//	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
//		if ( NEW ) { 
//		} else {
//			netEngine.registerAdditionalAgentOnLink(planAgent);
//		}
//	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		if (!(agent instanceof TransitDriver)) {
			Id agentId = agent.getId();
			Id linkId = agent.getCurrentLinkId();
			netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
	}

//	@Deprecated // use InternalInterface
//	@Override
//	public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
//		if ( NEW ) {
//			return null ;
//		} else {
//			return netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
//		}
//	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			MobsimAgent agent = this.activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndAssumeControl(time);
				this.internalInterface.arrangeNextAgentState(agent) ;
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
	private final void arrangeAgentDeparture(final MobsimAgent agent) {
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

						agent.endLegAndAssumeControl(now) ;
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

	@Override
	public final MobsimTimer getSimTimer() {
		return this.simTimer;
	}

	/*package*/ final MobsimEngine getNetsimEngine() {
		 // For a test
		return this.netEngine;
	}

	// For a test 
	// the corresponding test could be moved into this package. kai
	public final MultiModalSimEngine getMultiModalSimEngine() {
		return this.multiModalEngine;
	}
	
	@Override
	public final void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotWriters.add(snapshotWriter);
	}

	public final void addMobsimEngine(MobsimEngine mobsimEngine) {
        if (mobsimEngine instanceof TransitQSimEngine) {
        	if ( this.transitEngine != null ) {
        		log.warn("pre-existing transitEngine != null; will be overwritten; with the current design, " +
        				"there can only be one TransitQSimEngine") ;
        	}
            this.transitEngine = (TransitQSimEngine) mobsimEngine;
        } if (mobsimEngine instanceof MultiModalSimEngine) {
        	if ( this.multiModalEngine != null ) {
        		log.warn("pre-existing multiModalEngine != null; will be overwritten; with the current design, " +
        				"there can only be one MultiModalSimEngine") ;
        	}
        	this.multiModalEngine = (MultiModalSimEngine) mobsimEngine;
        }
        
        mobsimEngine.setInternalInterface(this.internalInterface);
		this.mobsimEngines.add(mobsimEngine);
	}

	@Override
	public final AgentCounterI getAgentCounter() {
		return this.agentCounter;
	}

	public final void addDepartureHandler(DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance given as parameters as listener
	 * to this QueueSimulation instance.
	 * 
	 * @param listeners
	 */
	@Override
	public final void addQueueSimulationListeners(SimulationListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

    /**
     * Only OTFVis is allowed to use this. If you want access to the TransitQSimEngine,
     * just "inline" the factory method of this class to plug together your own QSim, and you've got it!
     * This getter will disappear very soon. michaz 11/11
     */
    @Deprecated
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

    public final void addAgentSource(AgentSource agentSource) {
		agentSources.add(agentSource);
	}

}
