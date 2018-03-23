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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngineI;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;
import org.matsim.withinday.mobsim.WithinDayEngine;

import com.google.inject.Injector;

import javax.inject.Inject;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

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
 * <p></p>
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public final class QSim extends Thread implements VisMobsim, Netsim, ActivityEndRescheduler {

	final private static Logger log = Logger.getLogger(QSim.class);

	/** time since last "info" */
	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;
	//	private static final int INFO_PERIOD = 10;

	private final EventsManager events;

	private NetsimEngine netEngine;

	private final Collection<MobsimEngine> mobsimEngines = new ArrayList<>();

	private final MobsimTimer simTimer;

	private TeleportationEngine teleportationEngine;

	private WithinDayEngine withindayEngine = null;

	private ActivityHandler activityEngine;

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100 * 3600;
	private final MobsimListenerManager listenerManager;
	private final Scenario scenario;
	private final List<ActivityHandler> activityHandlers = new ArrayList<>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<>();
	private final AgentCounter agentCounter;
	private final Map<Id<Person>, MobsimAgent> agents = new LinkedHashMap<>();
	private final Map<Id<Vehicle>,MobsimVehicle> vehicles = new LinkedHashMap<>() ;
	private final List<AgentSource> agentSources = new ArrayList<>();

	// for detailed run time analysis
	public static boolean analyzeRunTimes = false;
	private long startTime = 0;
	private long qSimInternalTime = 0;
	private final Map<MobsimEngine, AtomicLong> mobsimEngineRunTimes;
	{
		if (analyzeRunTimes) this.mobsimEngineRunTimes = new HashMap<>();
		else this.mobsimEngineRunTimes = null;
	}

	/*package (for tests)*/ final InternalInterface internalInterface = new InternalInterface() {

		// These methods must be synchronized, because they are called back
		// from possibly multi-threaded engines, and they access
		// global mutable data.

		@Override
		public synchronized void arrangeNextAgentState(MobsimAgent agent) {
			QSim.this.arrangeNextAgentAction(agent);
		}

		@Override
		public synchronized void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
			if (QSim.this.netEngine != null) {
				QSim.this.netEngine.registerAdditionalAgentOnLink(planAgent);
			}
		}

		@Override
		public synchronized MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			if (QSim.this.netEngine != null) {
				return QSim.this.netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
			}
			return null;
		}

		@Override
		@Deprecated // use same method from QSim directly and try to get rid of the handle to internal interface. kai, mar'15
		public void rescheduleActivityEnd(MobsimAgent agent) {
			// yy my current intuition would be that this could become a public QSim method.  The original idea was that I wanted external
			// code only to insert agents into the QSim, and from then on the QSim handles it internally.  However, the main thing that truly seems to be
			// done internally is to move the agents between the engines, e.g. around endActivity and endLeg.  In consequence, 
			// "arrangeNextAgentState" and "(un)registerAgentOnLink" need to be protected.  But not this one.  kai, mar'15
			QSim.this.activityEngine.rescheduleActivityEnd(agent);
		}
	};

	private Collection<AgentTracker> agentTrackers = new ArrayList<>() ;

	@Override
	public final void rescheduleActivityEnd(MobsimAgent agent) {
		this.activityEngine.rescheduleActivityEnd(agent);
	}

	/**
	 * Constructs an instance of this simulation which does not do anything by itself, but accepts handlers for Activities and Legs.
	 * Use this constructor if you want to plug together your very own simulation, i.e. you are writing some of the simulation
	 * logic yourself.
	 *
	 * If you wish to use QSim as a product and run a simulation based on a Config file, rather use QSimFactory as your entry point.
	 *
	 */
	/*public QSim(final Scenario sc, EventsManager events, Injector childInjector ) {
		this( sc, events ) ;
	}*/
	/*public QSim(final Scenario sc, EventsManager events ) {
		this.scenario = sc;
		if (sc.getConfig().qsim().getNumberOfThreads() > 1) {
			this.events = EventsUtils.getParallelFeedableInstance(events);
		} else {
			this.events = events;
		}
		this.listenerManager = new MobsimListenerManager(this);
		this.agentCounter = new org.matsim.core.mobsim.qsim.AgentCounter();
		this.simTimer = new MobsimTimer(sc.getConfig().qsim().getTimeStepSize());
		
	}*/
	
	@Inject
	public QSim(Scenario scenario, EventsManager events, AgentCounter agentCounter, MobsimTimer mobsimTimer, ActiveQSimBridge bridge) {
		bridge.setActiveQSim(this);
		
		this.scenario = scenario;
		
		if (scenario.getConfig().qsim().getNumberOfThreads() > 1) {
			this.events = EventsUtils.getParallelFeedableInstance(events);
		} else {
			this.events = events;
		}
		
		this.agentCounter = agentCounter;
		this.simTimer = mobsimTimer;
		this.listenerManager = new MobsimListenerManager(this);
		
		
		// TODO: Since the new injection scheme is not in place yet, AgentCounter and MobsimTimer live in the global scope.
		// This means we need to reset them for the next iteration. When the cleaner injection scheme
		// is in place, both objects should be initialized by Guice!
		
		while (agentCounter.isLiving()) {
			agentCounter.decLiving();
		}
		
		simTimer.setTime(0.0);
	}

	// ============================================================================================================================
	// "run" method:

	@Override
	public void run() {
		try {
			// Teleportation must be last (default) departure handler, so add it
			// only before running.
			addDepartureHandler(this.teleportationEngine);
			prepareSim();
			this.listenerManager.fireQueueSimulationInitializedEvent();

			// Put agents into the handler for their first ("overnight") action,
			// probably the ActivityEngine. This is done before the first
			// beforeSimStepEvent, because the expectation seems to be
			// (e.g. in OTFVis), that agents are doing something
			// (can be located somewhere) before you execute a sim step.
			// Agents can abort in this loop already, so we iterate over
			// a defensive copy of the agent collection.
			for (MobsimAgent agent : new ArrayList<>(this.agents.values())) {
				arrangeNextAgentAction(agent);
			}

			// do iterations
			boolean doContinue = true;
			while (doContinue) {
				doContinue = doSimStep();
			}
		} finally {
			// We really want to perform that. For instance, with QNetsimEngine, threads are cleaned up in this method.
			// Without this finally, in case of a crash, threads are not closed, which lead to process hanging forever
			// at least on the eth euler cluster (but not on our local machines at ivt!?) td oct 15
			cleanupSim();
		}
	}

	// ============================================================================================================================
	// prepareSim and related:

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	/*package*/ void prepareSim() {
		events.initProcessing();

		createAgents();
		this.initSimTimer();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already

		for (MobsimEngine mobsimEngine : this.mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}
	}

	private void createAgents() {
		for (AgentSource agentSource : this.agentSources) {
			agentSource.insertAgentsIntoMobsim();
		}
	}

	private static int wrnCnt = 0;
	public void createAndParkVehicleOnLink(Vehicle vehicle, Id<Link> linkId) {
		QVehicle qveh = new QVehicle(vehicle);
		addParkedVehicle ( qveh, linkId ) ;
	}

	private static int wrnCnt2 = 0;
	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		if (this.netEngine != null) {
			this.netEngine.addParkedVehicle(veh, startLinkId);
		} else {
			if (wrnCnt2 < 1) {
				log.warn( "not able to add parked vehicle since there is no netsim engine.  continuing anyway, but it may "
						+ "not be clear what this means ...") ;
				log.warn(Gbl.ONLYONCE);
				wrnCnt2++;
			}
		}
		if ( this.vehicles.containsKey( veh.getId() ) ) {
			throw new RuntimeException( "vehicle with ID " + veh.getId() + " exists twice. Aborting ..." ) ;
		}
		this.vehicles.put( veh.getId(), veh ) ;
	}
	
	public Map<Id<Vehicle>,MobsimVehicle> getVehicles() {
		return Collections.unmodifiableMap( this.vehicles ) ;
	}

	void cleanupSim() {
		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();

		boolean gotException = false;
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			try {
				// make sure all engines are cleaned up
				mobsimEngine.afterSim();
			}
			catch (Exception e) {
				log.error("got exception while cleaning up", e);
			}
		}

		if (gotException) throw new RuntimeException( "got exception while cleaning up the QSim. Please check the error messages above for details.");
		events.finishProcessing();
		if (analyzeRunTimes) {
			log.info("qsim internal cpu time (nanos): " + qSimInternalTime);
			for (Entry<MobsimEngine, AtomicLong> entry : this.mobsimEngineRunTimes.entrySet()) {
				log.info(entry.getKey().getClass().toString() + " cpu time (nanos): " + entry.getValue().get());				
			}
			log.info("");
			if ( this.netEngine instanceof QNetsimEngine ) {
				((QNetsimEngine)this.netEngine).printEngineRunTimes();
				// (yy should somehow be in afterSim()).
			}
		}
	}

	/**
	 * Do one step of the simulation run.
	 *
	 * @return true if the simulation needs to continue
	 */
	/*package*/ boolean doSimStep() {
		if (analyzeRunTimes) this.startTime = System.nanoTime();

		final double now = simTimer.getTimeOfDay();

		this.listenerManager.fireQueueSimulationBeforeSimStepEvent(now);
		
		if (analyzeRunTimes) this.qSimInternalTime += System.nanoTime() - this.startTime;
		
		/*
		 * The WithinDayEngine has to perform its replannings before
		 * the other engines simulate the sim step.
		 */
		if (this.withindayEngine != null) {
			if (analyzeRunTimes) startTime = System.nanoTime();
			this.withindayEngine.doSimStep(now);
			if (analyzeRunTimes) this.mobsimEngineRunTimes.get(this.withindayEngine).addAndGet(System.nanoTime() - this.startTime);
		}

		// "added" engines
		for (MobsimEngine mobsimEngine : this.mobsimEngines) {
			if (analyzeRunTimes) this.startTime = System.nanoTime();
			
			// withindayEngine.doSimStep(time) has already been called
			if (mobsimEngine == this.withindayEngine) continue;

			mobsimEngine.doSimStep(now);
			
			if (analyzeRunTimes) this.mobsimEngineRunTimes.get(mobsimEngine).addAndGet(System.nanoTime() - this.startTime);
		}

		if (analyzeRunTimes) this.startTime = System.nanoTime();
		
		// console printout:
		this.printSimLog(now);
		boolean doContinue =  (this.agentCounter.isLiving() && (this.stopTime > now));
		this.events.afterSimStep(now);
		this.listenerManager.fireQueueSimulationAfterSimStepEvent(now);

		final QSimConfigGroup qsimConfigGroup = this.scenario.getConfig().qsim();
		if ( qsimConfigGroup.getSimEndtimeInterpretation()==EndtimeInterpretation.onlyUseEndtime ) {
			if ( now > qsimConfigGroup.getEndTime() ) {
				doContinue = false ;
			} else {
				doContinue = true ;
			}
		}

		if (doContinue) {
			this.simTimer.incrementTime();
		}
		
		if (analyzeRunTimes) this.qSimInternalTime += System.nanoTime() - this.startTime;

		return doContinue;
	}

	public void insertAgentIntoMobsim(final MobsimAgent agent) {
		if (this.agents.containsKey(agent.getId())) {
			throw new RuntimeException("Agent with same Id (" + agent.getId().toString() + ") already in mobsim; aborting ... ") ;
		}
		this.agents.put(agent.getId(), agent);
		this.agentCounter.incLiving();
	}

	private void arrangeNextAgentAction(final MobsimAgent agent) {
		switch( agent.getState() ) {
		case ACTIVITY:
			arrangeAgentActivity(agent);
			break ;
		case LEG:
			this.arrangeAgentDeparture(agent);
			break ;
		case ABORT:
			this.events.processEvent( new PersonStuckEvent(this.simTimer.getTimeOfDay(), agent.getId(), agent.getCurrentLinkId(), agent.getMode()));

			// NOTE: in the same way as one can register departure handler or activity handler, we could allow to
			// register abort handlers.  If someone ever comes to this place here and needs this.  kai, nov'17
			
			this.agents.remove(agent.getId()) ;
			this.agentCounter.decLiving();
			this.agentCounter.incLost();
			break ;
		default:
			throw new RuntimeException("agent with unknown state (possibly null)") ;
		}
	}

	private void arrangeAgentActivity(final MobsimAgent agent) {
		for (ActivityHandler activityHandler : this.activityHandlers) {
			if (activityHandler.handleActivity(agent)) {
				return;
			}
		}
	}

	/**
	 * Informs the simulation that the specified agent wants to depart from its
	 * current activity. The simulation can then put the agent onto its vehicle
	 * on a link or teleport it to its destination.
	 *
	 */
	private void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = simTimer.getTimeOfDay();
		Id<Link> linkId = agent.getCurrentLinkId();
		Gbl.assertIf( linkId!=null );
		events.processEvent(new PersonDepartureEvent(now, agent.getId(), linkId, agent.getMode()));

		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, agent, linkId)) {
				return;
			}
		}
		log.warn("no departure handler wanted to handle the departure of agent " + agent.getId());
		// yy my intuition is that this should be followed by setting the agent state to abort. kai, nov'14

	}

	// ############################################################################################################################
	// private methods
	// ############################################################################################################################

	private void initSimTimer() {
		QSimConfigGroup qSimConfigGroup = this.scenario.getConfig().qsim();
		Double configuredStartTime = qSimConfigGroup.getStartTime();
		this.stopTime = qSimConfigGroup.getEndTime();
		if (configuredStartTime == Time.UNDEFINED_TIME) {
			configuredStartTime = 0.0;
		}
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) {
			this.stopTime = Double.MAX_VALUE;
		}

		double simStartTime;
		if (QSimConfigGroup.StarttimeInterpretation.maxOfStarttimeAndEarliestActivityEnd.equals(qSimConfigGroup.getSimStarttimeInterpretation())) {
			double firstAgentStartTime = calculateFirstAgentStartTime();
			simStartTime = Math.floor(Math.max(configuredStartTime, firstAgentStartTime));
		} else if (QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime.equals(qSimConfigGroup.getSimStarttimeInterpretation())) {
			simStartTime = configuredStartTime;
		} else {
			throw new RuntimeException("unkonwn starttimeInterpretation; aborting ...");
		}

		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(simStartTime);

	}

	private double calculateFirstAgentStartTime() {
		double firstAgentStartTime = Double.POSITIVE_INFINITY;
		for (MobsimAgent agent : agents.values()) {
			firstAgentStartTime = Math.min(firstAgentStartTime, agent.getActivityEndTime());
		}
		return firstAgentStartTime;
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
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time)
					+ " : #Veh=" + this.agentCounter.getLiving() + " lost="
					+ this.agentCounter.getLost() + " simT=" + diffsim
					+ "s realT=" + (diffreal) + "s; (s/r): "
					+ (diffsim / (diffreal + Double.MIN_VALUE)));

			Gbl.printMemoryUsage();
		}
	}

	// ############################################################################################################################
	// no real functionality beyond this point
	// ############################################################################################################################

	/*@Override
	public NetsimNetwork getNetsimNetwork() {
		return this.netEngine.getNetsimNetwork();
	}*/

	@Override
	public VisNetwork getVisNetwork() {
		return this.netEngine.getNetsimNetwork();
	}

	public void addMobsimEngine(MobsimEngine mobsimEngine) {
		// yy in all of the instanceof expressions below, the implementation class needs to be replaced
		// by a meaningful interface.  kai, oct'17
		
//		if (mobsimEngine instanceof TransitQSimEngine) {
//			if (this.transitEngine != null) {
//				log.warn("pre-existing transitEngine != null; will be overwritten; with the current design, " +
//						"there can only be one TransitQSimEngine") ;
//			}
//			this.transitEngine = (TransitQSimEngine) mobsimEngine;
//		}
		if ( mobsimEngine instanceof AgentTracker ) {
			agentTrackers.add((AgentTracker) mobsimEngine);
		}
		if (mobsimEngine instanceof ActivityHandler) {
			this.activityEngine = (ActivityHandler) mobsimEngine;
		}
		if (mobsimEngine instanceof NetsimEngine) {
			this.netEngine = (NetsimEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof TeleportationEngine) {
			this.teleportationEngine = (TeleportationEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof WithinDayEngine) {
			this.withindayEngine = (WithinDayEngine) mobsimEngine;
		}
		//mobsimEngine.setInternalInterface(this.internalInterface);
		this.mobsimEngines.add(mobsimEngine);
		
		if (analyzeRunTimes) this.mobsimEngineRunTimes.put(mobsimEngine, new AtomicLong());
	}

	public void addDepartureHandler(DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	public void addActivityHandler(ActivityHandler activityHandler) {
		this.activityHandlers.add(activityHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance given as parameters as listener
	 * to this QueueSimulation instance.
	 */
	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	@Inject
	void addQueueSimulationListeners(Set<MobsimListener> listeners) {
		for (MobsimListener listener : listeners) {
			this.listenerManager.addQueueSimulationListener(listener);
		}
	}

//	/**
//	 * Only OTFVis is allowed to use this. If you want access to the TransitQSimEngine,
//	 * just "inline" the factory method of this class to plug together your own QSim, and you've got it!
//	 * This getter will disappear very soon. michaz 11/11
//	 */
//	@Deprecated
//	public TransitQSimEngine getTransitEngine() {
//		return this.transitEngine;
//	}
	// see new getAgentTrackers method.  kai, nov'17

	@Override
	public Map<Id<Person>, MobsimAgent> getAgents() {
		return Collections.unmodifiableMap(this.agents);
	}

	public void addAgentSource(AgentSource agentSource) {
		this.agentSources.add(agentSource);
	}

	@Override
	public VisData getNonNetworkAgentSnapshots() {
		return new VisData() {

			@Override
			public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
				for (MobsimEngine mobsimEngine : mobsimEngines) {
					if (mobsimEngine instanceof VisData) {
						VisData visData = (VisData) mobsimEngine;
						positions = visData.addAgentSnapshotInfo(positions);
					}
				}
				return positions;
			}
		};
	}

	public Collection<AgentTracker> getAgentTrackers() {
		return Collections.unmodifiableCollection(agentTrackers) ;
	}
	
	/**
	 * Used for refactoring. Should be eliminated eventually. /shoerl, feb18
	 */
	public InternalInterface getInternalInterface() {
		return internalInterface;
	}
	
	/**
	 * Used for refactoring. Should be eliminated eventually. /shoerl, feb18
	 */
	public NetsimNetwork getNetsimNetwork() {
		return netEngine.getNetsimNetwork();
	}
	
	public final void addNetworkChangeEvent( NetworkChangeEvent event ) {
		// used (and thus implicitly tested) by bdi-abm-integration project.  A separate core test would be good. kai, feb'18
		
		boolean processed = false ;
		for ( MobsimEngine engine : this.mobsimEngines ) {
			if ( engine instanceof NetworkChangeEventsEngineI ) {
				((NetworkChangeEventsEngineI) engine).addNetworkChangeEvent( event );
				processed = true ;
			}
		}
		if ( !processed ) {
			throw new RuntimeException("received a network change event, but did not process it.  Maybe " +
											   "the network change events engine was not set up for the qsim?  Aborting ...") ;
		}
	}
	
}
