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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.ptproject.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.helpers.QSimTimer;
import org.matsim.ptproject.qsim.interfaces.AcceptsVisMobsimFeatures;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimEngineFactory;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.interfaces.SimTimerI;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.CarDepartureHandler;
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
import org.matsim.vis.snapshots.writers.VisMobsimFeature;
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
public class QSim implements VisMobsim, AcceptsVisMobsimFeatures, Mobsim {

	final private static Logger log = Logger.getLogger(QSim.class);

	/* time since last snapshot */
	private double snapshotTime = 0.0;
	private int snapshotPeriod = 0;

	/* time since last "info" */
	private double infoTime = 0;
	private static final int INFO_PERIOD = 3600;


//	private QNetwork network;
	private EventsManager events = null;

	private NetsimEngine netEngine = null;
	private NetworkChangeEventsEngine changeEventsEngine = null;
	private MultiModalSimEngine multiModalEngine = null;

	private CarDepartureHandler carDepartureHandler;

	private SimTimerI simTimer;

	private Collection<PersonAgent> transitAgents;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */
	private final Queue<Tuple<Double, PlanAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, PlanAgent>>(30, new TeleportationArrivalTimeComparator());

	/**
	 * This list needs to be a "blocking" queue since this is needed for thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private final Queue<PlanAgent> activityEndsList =
		new PriorityBlockingQueue<PlanAgent>(500, new PlanAgentDepartureTimeComparator());
	// can't use the "Tuple" trick from teleportation list, since we need to be able to "find" agents for replanning. kai, oct'10
	// yy On second thought, this does also not work for the teleportationList since we have the same problem there ... kai, oct'10

	private final Date realWorldStarttime = new Date();
	private double stopTime = 100*3600;
	private AgentFactory agentFactory;
	private SimulationListenerManager listenerManager;
	private Scenario scenario = null;

//	private final List<MobsimFeature> queueSimulationFeatures = new ArrayList<MobsimFeature>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	private QSimSnapshotWriterManager snapshotManager = new QSimSnapshotWriterManager();

	private TransitQSimEngine transitEngine;

	private AgentCounterI agentCounter;

	private Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();

	// everything above this line is private and should remain private.  pls contact me if this is in your way.  kai, oct'10
	// ============================================================================================================================
	// initialization:

	public QSim(final Scenario scenario, final EventsManager events) {
		this(scenario, events, new DefaultQSimEngineFactory());
	}

	protected QSim(final Scenario sc, final EventsManager events, final NetsimEngineFactory simEngineFac){
		this.scenario = sc;
		this.events = events;
		init(simEngineFac);
	}

	// ============================================================================================================================
	// "run" method:

	@Override
	public final void run() {
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
		this.listenerManager = null;
	}

	// ============================================================================================================================
	// "prepareSim":

	// setters that should reasonably be called between constructor and "prepareSim" (triggered by "run"):

	public void setMultiModalSimEngine(MultiModalSimEngine multiModalEngine) {
		this.multiModalEngine = multiModalEngine;
	}

	@Override
	public void setAgentFactory(final AgentFactory fac) {
		this.agentFactory = fac;
	}

	@Override
	public void setControlerIO(final ControlerIO controlerIO) {
		this.controlerIO = controlerIO;
	}

	@Override
	public void setIterationNumber(final Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
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

		createAgents();

		this.initSimTimer();

		this.snapshotPeriod = (int) this.scenario.getConfig().getQSimConfigGroup().getSnapshotPeriod();
		this.infoTime = Math.floor(this.simTimer.getSimStartTime() / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(this.simTimer.getSimStartTime() / this.snapshotPeriod) * this.snapshotPeriod;

		// Initialize Snapshot file
		this.snapshotPeriod = (int) this.scenario.getConfig().getQSimConfigGroup().getSnapshotPeriod();
		this.snapshotManager.createSnapshotwriter(this.netEngine.getQNetwork(), this.scenario, this.snapshotPeriod, this.iterationNumber, this.controlerIO);
		if (this.snapshotTime < this.simTimer.getSimStartTime()) {
			this.snapshotTime += this.snapshotPeriod;
		}

		this.changeEventsEngine = new NetworkChangeEventsEngine(this); // yyyy do much earlier
		if ( this.changeEventsEngine != null ) { //yyyy do earlier
			this.changeEventsEngine.onPrepareSim();
		}
	}

	private static int vehWrnCnt = 0 ;
	protected void createAgents() {
		if (this.scenario.getPopulation() == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			PersonAgent agent = this.agentFactory.createPersonAgent(p);
			QVehicle veh = null ;
			if ( agent instanceof PersonDriverAgent ) {
				if ( vehWrnCnt < 1 ) {
					log.warn( "QSim generates default vehicles; not sure what that does to vehicle files; needs to be checked.  kai, nov'10" ) ;
					log.warn( Gbl.ONLYONCE ) ;
					vehWrnCnt++ ;
				}
				veh = new QVehicleImpl(new VehicleImpl(agent.getPerson().getId(), defaultVehicleType));
				//not needed in new agent class
				veh.setDriver((PersonDriverAgent)agent); // this line is currently only needed for OTFVis to show parked vehicles
				((DriverAgent)agent).setVehicle(veh);
			}
			agents.add(agent);
			if (agent.initializeAndCheckIfAlive()) {
				if ( agent instanceof PersonDriverAgent ) {
					NetsimLink qlink = this.netEngine.getQNetwork().getNetsimLink(((PersonDriverAgent)agent).getCurrentLinkId());
					qlink.addParkedVehicle(veh);
				}
			}
		}

		if (this.transitEngine != null){
			Collection<PersonAgent> a = this.transitEngine.createAgents();
			this.transitAgents = a;
			agents.addAll(a);
		}

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

		double now = this.simTimer.getTimeOfDay();

		for (Tuple<Double, PlanAgent> entry : this.teleportationList) {
			PlanAgent agent = entry.getSecond();
			events.processEvent(events.getFactory().
					createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (PlanAgent agent : this.activityEndsList) {
			if (agent.getDestinationLinkId() != null) {
				events.processEvent(events.getFactory().
						createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), null));
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
			writer.finish();
		}

		this.netEngine = null;
		this.events = null; // delete events object to free events handlers, if they are nowhere else referenced
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

		this.printSimLog(time);
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}

//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			queueSimulationFeature.afterAfterSimStep(time);
//		}
		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep() {
		// left empty for inheritance; for experimentation only (for production use MobsimListeners, or become a regular SimEngine).  kai, oct'10
	}

	protected final void handleTeleportationArrivals() {
		double now = this.getSimTimer().getTimeOfDay() ;
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, PlanAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				PlanAgent personAgent = entry.getSecond();
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
	 * @see PersonDriverAgent#getActivityEndTime()
	 */
	@Override
	public final void scheduleActivityEnd(final PlanAgent agent) {
		this.activityEndsList.add(agent);
		registerAgentAtActivityLocation(agent);
	}

	@Override
	public final void rescheduleActivityEnd(final PersonAgent agent, final double oldTime, final double newTime ) {
		// yyyy quite possibly, this should be "notifyChangedPlan".  kai, oct'10
		// yy the "newTime" is strictly speaking not necessary.  kai, oct'10

		// change the position in the queue:
		this.activityEndsList.remove( agent ) ;
		this.activityEndsList.add( agent ) ;

		if ( oldTime==Double.POSITIVE_INFINITY && newTime!=Double.POSITIVE_INFINITY) {
			this.getAgentCounter().incLiving() ;
		} else if ( oldTime!=Double.POSITIVE_INFINITY && newTime==Double.POSITIVE_INFINITY ) {
			this.getAgentCounter().decLiving() ;
		}

	}

	private void registerAgentAtActivityLocation(final PlanAgent agent) {
		// if the "activities" engine were separate, this would need to be public.  kai, aug'10
		if (agent instanceof PersonDriverAgentImpl) { // yyyyyy is this necessary?
//			DefaultPersonDriverAgent pa = (DefaultPersonDriverAgent) agent;
			PlanElement pe = agent.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				NetsimLink qLink = this.netEngine.getQNetwork().getNetsimLink(linkId);
				qLink.registerAgentOnLink(agent);
			}
		}
	}

	@Override
	public final void registerAgentAtPtWaitLocation(final PlanAgent planAgent) {
		// called by TransitEngine
		if (planAgent instanceof PersonDriverAgentImpl) { // yyyy but why is this needed?  Does the driver get registered?
			Leg leg = (Leg) planAgent.getCurrentPlanElement() ;
			Id linkId = leg.getRoute().getStartLinkId();
			NetsimLink qLink = this.netEngine.getQNetwork().getNetsimLink(linkId) ;
			qLink.registerAgentOnLink( planAgent ) ;
		}
	}

	private void unregisterAgentAtActivityLocation(final PlanAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) { // yyyy but why is this needed?
//			DefaultPersonDriverAgent pa = (DefaultPersonDriverAgent) agent;
			PlanElement pe = agent.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				NetsimLink qLink = this.netEngine.getQNetwork().getNetsimLink(linkId);
				qLink.unregisterAgentOnLink(agent);
			}
		}
	}

	@Override
	public final void unregisterAgentAtPtWaitLocation( final PlanAgent planAgent ) {
		// called by TransitDriver
		if (planAgent instanceof PersonDriverAgentImpl) { // yyyy but why is this needed?
			Leg leg = (Leg) planAgent.getCurrentPlanElement() ;
			Id linkId = leg.getRoute().getStartLinkId();
			NetsimLink qLink = this.netEngine.getQNetwork().getNetsimLink(linkId) ;
			qLink.unregisterAgentOnLink( planAgent ) ;
		}
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			PlanAgent agent = this.activityEndsList.peek();
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
	// yy Since this is now by force a PlanAgent, one could replace arrangeAgentDeparture and
	// scheduleActivityEnd by joint startPlanElement.  kai, nov'10
	public void arrangeAgentDeparture(final PlanAgent agent, final Id linkId) {
		double now = this.getSimTimer().getTimeOfDay() ;
		Leg leg = agent.getCurrentLeg();
//		Route route = leg.getRoute();
		String mode = leg.getMode();
		events.processEvent(events.getFactory().createAgentDepartureEvent(now, agent.getId(), linkId, mode ));
		if (handleKnownLegModeDeparture(now, agent, linkId, leg)) {
			return;
		} else {
			handleUnknownLegMode(now, agent);
			events.processEvent(new AdditionalTeleportationDepartureEvent( now, agent.getId(), linkId, mode, agent.getDestinationLinkId(), leg.getTravelTime() )) ;
		}
	}

	private void handleUnknownLegMode(final double now, final PlanAgent planAgent) {
		double arrivalTime = now + planAgent.getCurrentLeg().getTravelTime();
		this.teleportationList.add(new Tuple<Double, PlanAgent>(arrivalTime, planAgent));
	}

	private boolean handleKnownLegModeDeparture(final double now, final PlanAgent planAgent, final Id linkId, final Leg leg) {
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, planAgent, linkId, leg)) {
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

	/**
	 * extended constructor method that can also be after assignments of another constructor
	 * <p/>
	 * Definitely needs to be strongly protected (e.g. private) since c'tor should not call non-protected methods. kai, aug'10
	 * @param simEngineFac
	 */
	private void init(NetsimEngineFactory simEngineFac){
		log.info("Using QSim...");
		Scenario sc = this.getScenario() ;
		this.listenerManager = new SimulationListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new QSimTimer(sc.getConfig().getQSimConfigGroup().getTimeStepSize());

		// create the NetworkEngine ...
		this.netEngine = simEngineFac.createQSimEngine(this, MatsimRandom.getRandom());

		// create the QNetwork ...
		NetsimNetwork network = null ;
		if (sc.getConfig().scenario().isUseLanes()) {
			if (((ScenarioImpl)sc).getLaneDefinitions() == null) {
				throw new IllegalStateException("Lane definitions have to be set if feature is enabled!");
			}
			log.info("Lanes enabled...");
			network = DefaultQNetworkFactory.createQNetwork(this, new QLanesNetworkFactory(new DefaultQNetworkFactory(),
					((ScenarioImpl)sc).getLaneDefinitions()));
		}
		else {
			network = DefaultQNetworkFactory.createQNetwork(this);
		}

		// then tell the QNetwork to use the simEngine (this also creates qlinks and qnodes)
		network.initialize(this.netEngine);

		if (sc.getConfig().scenario().isUseSignalSystems()) {
			log.info("Signals enabled...");
		}

		if (sc.getConfig().multiModal().isMultiModalSimulationEnabled()) {

			/*
			 * Create a MultiModalTravelTime Calculator. It is passed over the the MultiModalQNetwork which
			 * needs it to estimate the TravelTimes of the NonCarModes.
			 * If the Controller uses a TravelTimeCalculatorWithBuffer (which is strongly recommended), a
			 * BufferedTravelTime Object is created and set as TravelTimeCalculator in the MultiModalTravelTimeCost
			 * Object.
			 */
//			MultiModalTravelTimeCost multiModalTravelTime = new MultiModalTravelTimeCost(sc.getConfig().plansCalcRoute(), sc.getConfig().multiModal());

//			TravelTime travelTime = ((MultiModalMobsimFactory)simEngineFac).getTravelTime();
//
//			if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
//				BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
//				bufferedTravelTime.setScaleFactor(1.25);
//				multiModalTravelTime.setTravelTime(bufferedTravelTime);
//			} else {
//				log.warn("TravelTime is not instance of TravelTimeCalculatorWithBuffer!");
//				log.warn("No BufferedTravelTime Object could be created. Using FreeSpeedTravelTimes instead.");
//			}

			// create MultiModalSimEngine
			multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(this);

			// add MultiModalDepartureHandler
			this.addDepartureHandler(new MultiModalDepartureHandler(this, multiModalEngine, scenario.getConfig().multiModal()));
		}

		this.agentFactory = new DefaultAgentFactory(this);

		this.carDepartureHandler = new CarDepartureHandler(this);
		addDepartureHandler(this.carDepartureHandler);

		if (sc.getConfig().scenario().isUseTransit()){
			this.transitEngine = new TransitQSimEngine(this);
			this.addDepartureHandler(this.transitEngine);
			// why is there not addition of pt to non teleported modes here?  kai, jun'10
			// done in TransitQSimEngine.  kai, jun'10
		}

	}


	private void initSimTimer() {
		double startTime = this.scenario.getConfig().getQSimConfigGroup().getStartTime();
		this.stopTime = this.scenario.getConfig().getQSimConfigGroup().getEndTime();
		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;
		this.simTimer.setSimStartTime(24*3600);
		this.simTimer.setTime(startTime);
		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		double simStartTime = 0;
		PlanAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getActivityEndTime()));
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
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time) + " (it." + this.iterationNumber + "): #Veh=" + this.agentCounter.getLiving()
					+ " lost=" + this.agentCounter.getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));

			if (this.multiModalEngine != null) {
				nofActiveLinks = this.multiModalEngine.getNumberOfSimulatedLinks();
				int nofActiveNodes = this.multiModalEngine.getNumberOfSimulatedNodes();
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

	/** Specifies whether the simulation should track vehicle usage and throw an Exception
	 * if an agent tries to use a car on a link where the car is not available, or not.
	 * Set <code>teleportVehicles</code> to <code>true</code> if agents always have a
	 * vehicle available. If the requested vehicle is parked somewhere else, the vehicle
	 * will be teleported to wherever it is requested to for usage. Set to <code>false</code>
	 * will generate an Exception in the case when an tries to depart with a car on link
	 * where the car is not parked.
	 *
	 * @param teleportVehicles
	 */
	@Deprecated // I don't think that something that has so obvious access to config (via scenario) should also be separately
	// configurable.  (But we do not all agree on this.)  kai, oct'10
	/*package*/ void setTeleportVehicles(final boolean teleportVehicles) {
		this.carDepartureHandler.setTeleportVehicles(teleportVehicles);
	}

	@Override
	public final NetsimNetwork getNetsimNetwork() {
		if ( this.netEngine != null ) {
			return this.netEngine.getQNetwork() ;
		} else {
			return null ;
		}
	}

	@Override
	public final VisNetwork getVisNetwork() {
		return this.netEngine.getQNetwork() ;
	}

	@Override
	public final Scenario getScenario() {
		return this.scenario;
	}

	public final MultiModalSimEngine getMultiModalSimEngine() {
		return this.multiModalEngine;
	}

	@Override
	@Deprecated // if you think you need to use this, ask kai.  aug'10
	public void addFeature(final VisMobsimFeature queueSimulationFeature) {
//		this.queueSimulationFeatures.add(queueSimulationFeature);
		this.addQueueSimulationListeners(queueSimulationFeature);
		this.getEventsManager().addHandler(queueSimulationFeature) ;
	}


	 Integer getIterationNumber() {
		return this.iterationNumber;
	}


	@Override
	public final SimTimerI getSimTimer() {
		return this.simTimer ;
	}

	public final NetsimEngine getQSimEngine() {
	  return this.netEngine;
	}

	@Override
	public void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotManager.addSnapshotWriter(snapshotWriter);
	}

//	public double getStuckTime(){
//		return this.stuckTime;
//	}

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

	public final TransitQSimEngine getQSimTransitEngine() {
		return transitEngine;
	}

	// different agent sublists:

	@Override
	public final Collection<MobsimAgent> getAgents() {
		return Collections.unmodifiableCollection( this.agents ) ;
		// changed this to unmodifiable in oct'10.  kai
	}

	@Override
	public final Collection<PlanAgent> getActivityEndsList() {
		return Collections.unmodifiableCollection( activityEndsList ) ;
		// changed this to unmodifiable in oct'10.  kai
	}

	public final Collection<PersonAgent> getTransitAgents(){
		return Collections.unmodifiableCollection( this.transitAgents ) ;
		// changed this to unmodifiable in oct'10.  kai
	}



}
