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

package playground.sergioo.ptsim2013;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;
import org.matsim.withinday.mobsim.WithinDayEngine;

import playground.sergioo.ptsim2013.pt.TransitQSimEngine;
import playground.sergioo.ptsim2013.qnetsimengine.PTQNetsimEngine;

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

	private PTQNetsimEngine netEngine;

	private Collection<MobsimEngine> mobsimEngines = new ArrayList<MobsimEngine>();

	private MobsimTimer simTimer;

	private TeleportationEngine teleportationEngine;

	private WithinDayEngine withindayEngine = null; 

	private ActivityEngine activityEngine;

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100 * 3600;
	private final MobsimListenerManager listenerManager;
	private final Scenario scenario;
	private final List<ActivityHandler> activityHandlers = new ArrayList<ActivityHandler>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();
	private AgentCounter agentCounter;
	private Collection<MobsimAgent> agents = new LinkedHashSet<MobsimAgent>();
	private List<AgentSource> agentSources = new ArrayList<AgentSource>();
	private TransitQSimEngine transitEngine;


	/*package (for tests)*/ InternalInterface internalInterface = new InternalInterface() {

		// These methods must be synchronized, because they are called back
		// from possibly multi-threaded engines, and they access
		// global mutable data.

		@Override
		public synchronized void arrangeNextAgentState(MobsimAgent agent) {
			QSim.this.arrangeNextAgentAction(agent) ;
		}

		@Override
		public Netsim getMobsim() {
			return QSim.this ;
		}

		@Override
		public synchronized void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
			QSim.this.netEngine.registerAdditionalAgentOnLink(planAgent);
		}

		@Override
		public synchronized MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			return QSim.this.netEngine.unregisterAdditionalAgentOnLink(agentId, linkId);
		}

		@Override
		public void rescheduleActivityEnd(MobsimAgent agent) {
			QSim.this.activityEngine.rescheduleActivityEnd(agent);
		}

	};

	/**
	 * Constructs an instance of this simulation which does not do anything by itself, but accepts handlers for Activities and Legs.
	 * Use this constructor if you want to plug together your very own simulation, i.e. you are writing some of the simulation
	 * logic yourself.
	 * 
	 * If you wish to use QSim as a product and run a simulation based on a Config file, rather use QSimFactory as your entry point.
	 * 
	 */
	public QSim(final Scenario sc, final EventsManager events) {
		this.scenario = sc;
		this.events = events;
		log.info("Using QSim...");
		this.listenerManager = new MobsimListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new MobsimTimer(sc.getConfig().qsim().getTimeStepSize());
	}

	// ============================================================================================================================
	// "run" method:


	@Override
	public void run() {
		// Teleportation must be last (default) departure handler, so add it
		// only before running.
		addDepartureHandler(this.teleportationEngine); 
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		// do iterations
		boolean doContinue = true;
		double time = this.simTimer.getTimeOfDay();
		while (doContinue) {
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			doContinue = doSimStep(time);
			this.events.afterSimStep(time);
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

		createAgents();
		this.initSimTimer();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already

		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}
	}

	private void createAgents() {
		for (AgentSource agentSource : agentSources) {
			agentSource.insertAgentsIntoMobsim();
		}
	}

	public void createAndParkVehicleOnLink(Vehicle vehicle, Id<Link> linkId) {
		QVehicle veh = new QVehicle(vehicle);
		netEngine.addParkedVehicle(veh, linkId);
	}

	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		netEngine.addParkedVehicle(veh, startLinkId);
	}

	private void cleanupSim() {
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.afterSim();
		}
	}

	/**
	 * Do one step of the simulation run.
	 * 
	 * @param time
	 *            the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	/*package*/ boolean doSimStep(final double time) {

		/*
		 * The WithinDayEngine has to perform its replannings before
		 * the other engines simulate the sim step.
		 */
		if (withindayEngine != null) withindayEngine.doSimStep(time);
		
		// "added" engines
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			
			// withindayEngine.doSimStep(time) has already been called
			if (mobsimEngine == this.withindayEngine) continue;
			
			mobsimEngine.doSimStep(time);
		}

		// console printout:
		this.printSimLog(time);
		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

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
			arrangeAgentActivity(agent); 
			break ;
		case LEG: 
			this.arrangeAgentDeparture(agent) ; 
			break ;
		case ABORT:
			this.events.processEvent( new PersonStuckEvent(this.simTimer.getTimeOfDay(), agent.getId(), agent.getCurrentLinkId(), agent.getMode())) ;

			this.agents.remove(agent) ;
			this.agentCounter.decLiving();
			this.agentCounter.incLost();
			break ;
		default:
			throw new RuntimeException("agent with unknown state (possibly null)") ;
		}
	}

	private void arrangeAgentActivity(MobsimAgent agent) {
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
	 * @param agent
	 */
	private void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = this.getSimTimer().getTimeOfDay();
		String mode = agent.getMode();
		Id<Link> linkId = agent.getCurrentLinkId();
		events.processEvent(new PersonDepartureEvent(now, agent.getId(), linkId, mode));

		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, agent, linkId)) {
				return;
			}
		}

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

		double simStartTime = 0;
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
		for (MobsimAgent agent : agents) {
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

	@Override
	public EventsManager getEventsManager() {
		return events;
	}

	@Override
	public org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork getNetsimNetwork() {
		throw new RuntimeException("not implemented") ;
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

	public void addMobsimEngine(MobsimEngine mobsimEngine) {
		if (mobsimEngine instanceof TransitQSimEngine) {
			if ( this.transitEngine != null ) {
				log.warn("pre-existing transitEngine != null; will be overwritten; with the current design, " +
						"there can only be one TransitQSimEngine") ;
			}
			this.transitEngine = (TransitQSimEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof ActivityEngine) {
			this.activityEngine = (ActivityEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof PTQNetsimEngine) {
			this.netEngine = (PTQNetsimEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof TeleportationEngine) {
			this.teleportationEngine = (TeleportationEngine) mobsimEngine;
		}
		if (mobsimEngine instanceof WithinDayEngine) {
			this.withindayEngine = (WithinDayEngine) mobsimEngine;
		}
		mobsimEngine.setInternalInterface(this.internalInterface);
		this.mobsimEngines.add(mobsimEngine);
	}

	@Override
	public org.matsim.core.mobsim.qsim.interfaces.AgentCounter getAgentCounter() {
		return this.agentCounter;
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

	public void addAgentSource(AgentSource agentSource) {
		agentSources.add(agentSource);
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

}
