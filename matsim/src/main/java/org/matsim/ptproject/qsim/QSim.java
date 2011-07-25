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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.Initializable;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.AbstractTransitDriver;
import org.matsim.pt.qsim.TransitDriver;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.pt.qsim.UmlaufDriver;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.ptproject.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.helpers.MobsimTimer;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.MobsimTimerI;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimEngineFactory;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisNetwork;

/**
 * Implementation of a queue-based transport simulation.
 * Lanes and SignalSystems are not initialized unless the setter are invoked.
 * <p/>
 *
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public class QSim implements VisMobsim, Netsim {

	final private static Logger log = Logger.getLogger(QSim.class);

	/* time since last snapshot */
	private double snapshotTime = 0.0;
	private int snapshotPeriod = 0;

	/* time since last "info" */
	private double infoTime = 0;
	private static final int INFO_PERIOD = 3600;

	private final EventsManager events ;

	private final NetsimEngine netEngine ;
	private NetworkChangeEventsEngine changeEventsEngine = null;
	private MultiModalSimEngine multiModalEngine = null;

	private Collection<MobsimEngine> mobsimEngines = new ArrayList<MobsimEngine>();

	private MobsimTimerI simTimer;

	private Collection<MobsimAgent> transitAgents;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final Queue<Tuple<Double, MobsimAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TeleportationArrivalTimeComparator());

	/**
	 * This list needs to be a "blocking" queue since this is needed for thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private final Queue<MobsimAgent> activityEndsList =
		new PriorityBlockingQueue<MobsimAgent>(500, new PlanAgentDepartureTimeComparator());
	// can't use the "Tuple" trick from teleportation list, since we need to be able to "find" agents for replanning. kai, oct'10
	// yy On second thought, this does also not work for the teleportationList since we have the same problem there ... kai, oct'10

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100*3600;
	private AgentFactory agentFactory;
	private final SimulationListenerManager listenerManager;
	private final Scenario scenario ;
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();
	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	private QSimSnapshotWriterManager snapshotManager = new QSimSnapshotWriterManager();
	private TransitQSimEngine transitEngine;
	private AgentCounterI agentCounter;
	private Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();
	private final Map<Id, QVehicleImpl> vehicles = new HashMap<Id, QVehicleImpl>();

	// everything above this line is private and should remain private.  pls contact me if this is in your way.  kai, oct'10
	// ============================================================================================================================
	// initialization:

	public QSim(final Scenario scenario, final EventsManager events) {
		this(scenario, events, new DefaultQSimEngineFactory());
	}

	protected QSim(final Scenario sc, final EventsManager events, final NetsimEngineFactory netsimEngFactory){
		this.scenario = sc;
		this.events = events;
		log.info("Using QSim...");
		this.listenerManager = new SimulationListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new MobsimTimer(sc.getConfig().getQSimConfigGroup().getTimeStepSize());

		// create the NetworkEngine ...
		this.netEngine = netsimEngFactory.createQSimEngine(this, MatsimRandom.getRandom());
		this.addDepartureHandler( this.netEngine.getDepartureHandler() ) ;

		// create the QNetwork ...
		NetsimNetwork network = null ;
		if (sc.getConfig().scenario().isUseLanes()) {
			if (((ScenarioImpl)sc).getLaneDefinitions() == null) {
				throw new IllegalStateException("Lane definitions have to be set if feature is enabled!");
			}
			log.info("Lanes enabled...");
			network = DefaultQNetworkFactory.createQNetwork(this, new QLanesNetworkFactory(new DefaultQNetworkFactory(),
					((ScenarioImpl)sc).getLaneDefinitions()));
			// yyyyyy the above is now a hack; replace by non-static factory method.  kai, feb'11
		}
		else {
			//			network = DefaultQNetworkFactory.staticCreateQNetwork(this);
			network = this.netEngine.getNetsimNetworkFactory().createNetsimNetwork(this) ;
		}
		// overall, one could wonder why this is not done inside the NetsimEngine.  kai, feb'11
		// well, we may want to see the construction explicitly as long as we use factories (and not builders).  kai, feb'11

		// then tell the QNetwork to use the netsimEngine (this also creates qlinks and qnodes)
		// yyyy feels a bit weird to me ...  kai, feb'11
		network.initialize(this.netEngine);


		// configuring signalSystems:
		if (sc.getConfig().scenario().isUseSignalSystems()) {
			log.info("Signals enabled...");
			// ... but not clear where this is actually configured??  Maybe drop completely from here??  kai, feb'11
		}

		// configuring multiModalEngine ...
		if (sc.getConfig().multiModal().isMultiModalSimulationEnabled()) {

			// create MultiModalSimEngine
			multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(this);

			// add MultiModalDepartureHandler
			this.addDepartureHandler(new MultiModalDepartureHandler(this, multiModalEngine, scenario.getConfig().multiModal()));

		}

		// set the agent factory.  might be better to have this in the c'tor, but difficult to do as long
		// as the transitEngine changes the AgentFactory.  kai, feb'11
		this.agentFactory = new DefaultAgentFactory(this);

		// configuring transit (this changes the agent factory as a side effect).
		if (sc.getConfig().scenario().isUseTransit()){
			this.transitEngine = new TransitQSimEngine(this);
			this.addDepartureHandler(this.transitEngine);
			// why is there not addition of pt to non teleported modes here?  kai, jun'10
			// done in TransitQSimEngine.  kai, jun'10
		}
	}

	// ============================================================================================================================
	// "run" method:

	private boolean locked = false ;
	@Override
	public final void run() {
		this.locked = true ;
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		//do iterations
		boolean doContinue = true;
		double time = this.simTimer.getTimeOfDay();
		while (doContinue) {
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			beforeSimStep();
			doContinue = doSimStep(time);
			afterSimStep();
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (doContinue) {
				time = this.simTimer.incrementTime();
			}
		}
		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();
		cleanupSim(time);

		//delete reference to clear memory
		//		this.listenerManager = null;
	}

	// ============================================================================================================================
	// "prepareSim":

	// setters that should reasonably be called between constructor and "prepareSim" (triggered by "run"):

	// yy my current intuition is that those should be set in a factory, and the factory should pass them as immutable
	// into the QSim.  But I can't change into that direction because the TransitEngine changes the AgentFactory fairly
	// late in the initialization sequence ...

	//	public final void setMultiModalSimEngine(MultiModalSimEngine multiModalEngine) {
	//		if ( !locked ) {
	//			this.multiModalEngine = multiModalEngine;
	//		} else {
	//			throw new RuntimeException("too late to set multiModalSimEngine; aborting ...") ;
	//		}
	//	}
	// this is never used, and it is not clear to me how it should be used given the initialization sequence
	// (in particular the departure handlers).  kai, feb'11

	@Override
	public void setAgentFactory(final AgentFactory fac) {
		// not final since WithindayQSim overrides (essentially: disables) it.  kai, nov'10
		if ( !locked ) { 
			this.agentFactory = fac;
		} else {
			throw new RuntimeException("too late to set agent factory; aborting ...") ;
		}
	}

	@Override
	public final void setControlerIO(final ControlerIO controlerIO) {
		if ( !locked ) {
			this.controlerIO = controlerIO;
		} else {
			throw new RuntimeException("too late to set controlerIO; aborting ...") ;
		}
	}

	@Override
	public final void setIterationNumber(final Integer iterationNumber) {
		if ( !locked ) {
			this.iterationNumber = iterationNumber;
		} else {
			throw new RuntimeException("too late to set iterationNumber; aborting ...") ;
		}
	}

	// prepareSim and related:

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	protected final void prepareSim() {
		if (events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
		}

		if ( this.netEngine != null ) {
			this.netEngine.onPrepareSim();
		}
		if (this.multiModalEngine != null) {
			this.multiModalEngine.onPrepareSim();
		}
		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.onPrepareSim();
		}

		createAgents();
		createVehicles();
		createTransitDrivers();
		createAdditionalAgents();

		this.initSimTimer();

		QSimConfigGroup qSimConfigGroup = this.scenario.getConfig().getQSimConfigGroup();
		this.snapshotPeriod = (int) qSimConfigGroup.getSnapshotPeriod();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime() / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(this.simTimer.getSimStartTime() / this.snapshotPeriod) * this.snapshotPeriod;

		// Initialize Snapshot file
		this.snapshotPeriod = (int) qSimConfigGroup.getSnapshotPeriod();
		this.snapshotManager.createSnapshotwriter(this.scenario, this.snapshotPeriod, this.iterationNumber, this.controlerIO);
		if (this.snapshotTime < this.simTimer.getSimStartTime()) {
			this.snapshotTime += this.snapshotPeriod;
		}

		this.changeEventsEngine = new NetworkChangeEventsEngine(this); // yyyy do much earlier
		if ( this.changeEventsEngine != null ) { //yyyy do earlier
			this.changeEventsEngine.onPrepareSim();
		}
	}

	protected void createAdditionalAgents() {

		// Empty for inheritance. (only one test)

	}

	private void createVehicles() {
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		for (MobsimAgent agent : agents) {
			if ( agent instanceof MobsimDriverAgent ) {
				createAndAddDefaultVehicle((MobsimAgent) agent, defaultVehicleType);
				parkVehicleOnInitialLink((MobsimAgent) agent);
			}
		}
	}

	private static int vehWrnCnt = 0 ;

	private void createAgents() {
		if (this.scenario.getPopulation() == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			agents.add(agent);
			
			// I would prefer to get rid of the following or understand why it is needed.  But tests fail if I move the material
			// into the ctor.  kai, jun'11
			if ( agent instanceof Initializable ) {
				((Initializable)agent).initialize();
			}
		}
	}

	private void createTransitDrivers() {
		if (this.transitEngine != null){
			Collection<MobsimAgent> a = this.transitEngine.createAdditionalAgents();
			this.transitAgents = a;
			agents.addAll(a);
		}
	}

	private void parkVehicleOnInitialLink(MobsimAgent agent) {
		QVehicle veh = ((MobsimDriverAgent) agent).getVehicle();
		NetsimLink qlink = this.netEngine.getNetsimNetwork().getNetsimLink(((MobsimDriverAgent)agent).getCurrentLinkId());
		qlink.addParkedVehicle(veh);
	}

	private void createAndAddDefaultVehicle(MobsimAgent agent, VehicleType defaultVehicleType) {
		QVehicleImpl veh = null ;
		if ( vehWrnCnt < 1 ) {
			log.warn( "QSim generates default vehicles; not sure what that does to vehicle files; needs to be checked.  kai, nov'10" ) ;
			log.warn( Gbl.ONLYONCE ) ;
			vehWrnCnt++ ;
		}
		VehicleImpl vehicle = new VehicleImpl(agent.getId(), defaultVehicleType);
		veh = new QVehicleImpl(vehicle);
		veh.setDriver((MobsimDriverAgent)agent); // this line is currently only needed for OTFVis to show parked vehicles
		((DriverAgent)agent).setVehicle(veh);
		vehicles.put(veh.getId(), veh);
	}


	/**
	 * Close any files, etc.
	 */
	protected final void cleanupSim(final double seconds) {
		if (this.transitEngine != null) { // yyyy do after features
			this.transitEngine.afterSim();
		}

		if ( this.netEngine != null ) {
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
			events.processEvent(events.getFactory().
					createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), agent.getMode()));
		}
		this.teleportationList.clear();

		for (MobsimAgent agent : this.activityEndsList) {
			if ( agent instanceof UmlaufDriver ) {
				log.error( "this does not terminate correctly for UmlaufDrivers; needs to be " +
				"fixed but for the time being we skip the next couple of lines.  kai, dec'10") ;
			} else {
				if (agent.getDestinationLinkId() != null) {
					events.processEvent(events.getFactory().
							createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), null));
				}
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
			writer.finish();
		}

		//		this.netEngine = null;
		//		this.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	protected void beforeSimStep() {
		// left empty for inheritance; for experimentation only (for production use MobsimListeners, or become a regular SimEngine).  kai, oct'10
	}


	/**
	 * Do one step of the simulation run.
	 *
	 * @param time the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	protected final boolean doSimStep(final double time) { // do not overwrite in inheritance.
		// (network) change events engine:
		if ( this.changeEventsEngine != null ) {
			this.changeEventsEngine.doSimStep(time);
		}
		// teleportation "engine":
		this.handleTeleportationArrivals();

		// "facilities" "engine":
		this.handleActivityEnds(time);

		// network engine:
		if ( this.netEngine != null ) {
			this.netEngine.doSimStep(time);
		}

		// multi modal engine:
		if (this.multiModalEngine != null) {
			this.multiModalEngine.doSimStep(time);
		}

		for (MobsimEngine mobsimEngine : mobsimEngines) {
			mobsimEngine.doSimStep(time);
		}

		this.printSimLog(time);
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}

		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep() {
		// left empty for inheritance; for experimentation only (for production use MobsimListeners, or become a regular SimEngine).  kai, oct'10
	}

	protected final void handleTeleportationArrivals() {
		double now = this.getSimTimer().getTimeOfDay() ;
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, MobsimAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyTeleportToLink(personAgent.getDestinationLinkId());
				personAgent.endLegAndAssumeControl(now) ;
			}
			else {
				break;
			}
		}
	}

	/**
	 * Registers this agent as performing an activity and makes sure that the agent will be informed once his departure time has come.
	 * @param agent
	 *
	 * @see MobsimDriverAgent#getActivityEndTime()
	 */
	@Override
	public final void scheduleActivityEnd(final MobsimAgent agent) {
		this.activityEndsList.add(agent);
		registerAgentAtActivityLocation(agent);
	}

	@Override
	public final void rescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime ) {
		// yyyy quite possibly, this should be "notifyChangedPlan".  kai, oct'10
		// yy the "newTime" is strictly speaking not necessary.  kai, oct'10

		// change the position in the queue:
		this.activityEndsList.remove( agent ) ;
		this.activityEndsList.add( agent ) ;

		// The intention in the following is that an agent that is no longer alive has an activity end time of infinity.  The number of
		// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11
		if ( oldTime==Double.POSITIVE_INFINITY && newTime!=Double.POSITIVE_INFINITY) {
			this.getAgentCounter().incLiving() ;
		} else if ( oldTime!=Double.POSITIVE_INFINITY && newTime==Double.POSITIVE_INFINITY ) {
			this.getAgentCounter().decLiving() ;
		}

	}

	private void registerAgentAtActivityLocation(final MobsimAgent agent) {
		// if the "activities" engine were separate, this would need to be public.  kai, aug'10
		if (! (agent instanceof AbstractTransitDriver)) {
//			PlanElement pe = agent.getCurrentPlanElement();
//			if (pe instanceof Leg) {
//				throw new RuntimeException();
//			} else {
//				Activity act = (Activity) pe;
//				Id linkId = act.getLinkId();
				Id linkId = agent.getCurrentLinkId() ;
				NetsimLink qLink = this.netEngine.getNetsimNetwork().getNetsimLink(linkId);
				qLink.registerAgentOnLink(agent);
//			}
		}
	}

	@Override
	public final void registerAgentAtPtWaitLocation(final MobsimAgent planAgent) {
		// called by TransitEngine
		if (planAgent instanceof PersonDriverAgentImpl) { // yyyy but why is this needed?  Does the driver get registered?
//			Leg leg = (Leg) planAgent.getCurrentPlanElement() ;
//			Id linkId = leg.getRoute().getStartLinkId();
			Id linkId = planAgent.getCurrentLinkId() ; // yyyyyy it is not fully clear that this is always set correctly.  kai, jun'11
			NetsimLink qLink = this.netEngine.getNetsimNetwork().getNetsimLink(linkId) ;
			qLink.registerAgentOnLink( planAgent ) ;
		}
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		if (! (agent instanceof TransitDriver)) {
//			PlanElement pe = agent.getCurrentPlanElement();
//			if (pe instanceof Leg) {
//				throw new RuntimeException();
//			} else {
//				Activity act = (Activity) pe;
//				Id linkId = act.getLinkId();
			    Id linkId = agent.getCurrentLinkId() ;
				NetsimLink qLink = this.netEngine.getNetsimNetwork().getNetsimLink(linkId);
				qLink.unregisterAgentOnLink(agent);
//			}
		}
	}

	@Override
	public final void unregisterAgentAtPtWaitLocation( final MobsimAgent planAgent ) {
		// called by TransitDriver
		if (planAgent instanceof PersonDriverAgentImpl) { // yyyy but why is this needed?
//			Leg leg = (Leg) planAgent.getCurrentPlanElement() ;
//			Id linkId = leg.getRoute().getStartLinkId();
			Id linkId = planAgent.getCurrentLinkId() ;
			NetsimLink qLink = this.netEngine.getNetsimNetwork().getNetsimLink(linkId) ;
			qLink.unregisterAgentOnLink( planAgent ) ;
		}
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			MobsimAgent agent = this.activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndAssumeControl(time) ;
				// gives control to agent; comes back via "agentDeparts" or "scheduleActivityEnd"
			} else {
				return;
			}
		}
	}

	/**
	 * Informs the simulation that the specified agent wants to depart from its current activity.
	 * The simulation can then put the agent onto its vehicle on a link or teleport it to its destination.
	 * @param agent
	 * @param link the link where the agent departs
	 */
	@Override
	// unclear if this is "actEnd" or "departure"!  kai, may'10
	// depending on this, it is a "PersonAgent" or "DriverAgent".  kai, may'10
	// I think it is departure, but still a person agent.  kai, aug'10
	// Now a PlanAgent, which makes more sense (I think). kai, nov'10
	// Since this is now by force a PlanAgent, one could replace arrangeAgentDeparture and
	// scheduleActivityEnd by joint startPlanElement.  kai, nov'10
	// It is no longer a PlanAgent. :-)  kai, jul'11
	public final void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = this.getSimTimer().getTimeOfDay() ;
//		Leg leg = agent.getCurrentLeg();
		//		Route route = leg.getRoute();
		String mode = agent.getMode();
		Id linkId = agent.getCurrentLinkId() ;
		events.processEvent(events.getFactory().createAgentDepartureEvent(now, agent.getId(), linkId, mode ));
		if (handleKnownLegModeDeparture(now, agent, linkId)) {
			return;
		} else {
			handleUnknownLegMode(now, agent);
			events.processEvent(new AdditionalTeleportationDepartureEvent( now, agent.getId(), linkId, mode, 
					agent.getDestinationLinkId(), agent.getExpectedTravelTime() )) ;
		}
	}

	private void handleUnknownLegMode(final double now, final MobsimAgent planAgent) {
		double arrivalTime = now + planAgent.getExpectedTravelTime();
		this.teleportationList.add(new Tuple<Double, MobsimAgent>(arrivalTime, planAgent));
	}

	private boolean handleKnownLegModeDeparture(final double now, final MobsimAgent planAgent, final Id linkId) {
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, planAgent, linkId)) {
				return true;
			}
			// The code is not (yet?) very beautiful.  But structurally, this goes through all departure handlers and tries to
			// find one that feels responsible.  If it feels responsible, it returns true, and so this method returns true.
			// Otherwise, this method will return false, and then teleportation will be called. kai, jun'10
		}
		return false ;
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
		if ( QSimConfigGroup.MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END.equals(
				qSimConfigGroup.getSimStarttimeInterpretation() ) ) {
			MobsimAgent firstAgent = this.activityEndsList.peek();
			if (firstAgent != null) {
				// set sim start time to config-value ONLY if this is LATER than the first plans starttime
				simStartTime = Math.floor(Math.max(startTime, firstAgent.getActivityEndTime()));
			}
		} else if ( QSimConfigGroup.ONLY_USE_STARTTIME.equals(
				qSimConfigGroup.getSimStarttimeInterpretation() ) ) {
			simStartTime = startTime ;
		} else {
			throw new RuntimeException( "unkonwn starttimeInterpretation; aborting ...") ;
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
			long diffreal = (endtime.getTime() - this.realWorldStarttime.getTime())/1000;
			double diffsim  = time - this.simTimer.getSimStartTime();
			int nofActiveLinks = this.netEngine.getNumberOfSimulatedLinks();
			int nofActiveNodes = this.netEngine.getNumberOfSimulatedNodes();
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time) + " (it." + this.iterationNumber + "): #Veh=" + this.agentCounter.getLiving()
					+ " lost=" + this.agentCounter.getLost() + " #links=" + nofActiveLinks + " #nodes=" + nofActiveNodes
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));

			if (this.multiModalEngine != null) {
				nofActiveLinks = this.multiModalEngine.getNumberOfSimulatedLinks();
				nofActiveNodes = this.multiModalEngine.getNumberOfSimulatedNodes();
				log.info("SIMULATION (MultiModalSim) AT " + Time.writeTime(time) +
						" #links=" + nofActiveLinks +
						" #nodes=" + nofActiveNodes);
			}

			Gbl.printMemoryUsage();
		}
	}

	private void doSnapshot(final double time) {
		if (!this.snapshotManager.getSnapshotWriters().isEmpty()) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
			for (NetsimLink link : this.getNetsimNetwork().getNetsimLinks().values()) {
				link.getVisData().getVehiclePositions( positions);
			}
			for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
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
	public final EventsManager getEventsManager(){
		return events;
	}



	@Override
	public final NetsimNetwork getNetsimNetwork() {
		if ( this.netEngine != null ) {
			return this.netEngine.getNetsimNetwork() ;
		} else {
			return null ;
		}
	}

	@Override
	public final VisNetwork getVisNetwork() {
		return this.netEngine.getNetsimNetwork() ;
	}

	@Override
	public final Scenario getScenario() {
		return this.scenario;
	}

	public final MultiModalSimEngine getMultiModalSimEngine() {
		return this.multiModalEngine;
	}

	Integer getIterationNumber() {
		return this.iterationNumber;
	}


	@Override
	public final MobsimTimerI getSimTimer() {
		return this.simTimer ;
	}

	public final NetsimEngine getNetsimEngine() {
		return this.netEngine;
	}

	@Override
	public final void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotManager.addSnapshotWriter(snapshotWriter);
	}

	public final void addMobsimEngine(MobsimEngine mobsimEngine) {
		this.mobsimEngines.add(mobsimEngine);
	}

	@Override
	public final AgentCounterI getAgentCounter(){
		return this.agentCounter;
	}

	public final void addDepartureHandler(final DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance  given as parameters as
	 * listener to this QueueSimulation instance.
	 * @param listeners
	 */
	@Override
	public final void addQueueSimulationListeners(final SimulationListener listener){
		this.listenerManager.addQueueSimulationListener(listener);
	}

	public final TransitQSimEngine getTransitEngine() {
		return transitEngine;
	}

	// different agent sublists:

	@Override
	public final Collection<MobsimAgent> getAgents() {
		return Collections.unmodifiableCollection( this.agents ) ;
		// changed this to unmodifiable in oct'10.  kai
	}

	@Override
	public final Collection<MobsimAgent> getActivityEndsList() {
		return Collections.unmodifiableCollection( activityEndsList ) ;
		// changed this to unmodifiable in oct'10.  kai
	}

	public final Collection<MobsimAgent> getTransitAgents(){
		return Collections.unmodifiableCollection( this.transitAgents ) ;
		// changed this to unmodifiable in oct'10.  kai
	}

	public Map<Id, QVehicleImpl> getVehicles() {
		return Collections.unmodifiableMap( this.vehicles ) ;
	}

}
