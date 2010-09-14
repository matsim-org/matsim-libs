/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulation.java
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
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
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
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.ptproject.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.ptproject.qsim.comparators.PersonAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.helpers.QSimTimer;
import org.matsim.ptproject.qsim.helpers.QVehicleImpl;
import org.matsim.ptproject.qsim.interfaces.AcceptsVisMobsimFeatures;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QNetworkI;
import org.matsim.ptproject.qsim.interfaces.QSimEngineFactory;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.interfaces.QVehicle;
import org.matsim.ptproject.qsim.interfaces.SimTimerI;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.ptproject.qsim.netsimengine.CarDepartureHandler;
import org.matsim.ptproject.qsim.netsimengine.DefaultQNetworkFactory;
import org.matsim.ptproject.qsim.netsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.netsimengine.QLanesNetworkFactory;
import org.matsim.ptproject.qsim.netsimengine.QNetwork;
import org.matsim.ptproject.qsim.netsimengine.QSimEngine;
import org.matsim.ptproject.qsim.netsimengine.QSimEngineImpl;
import org.matsim.ptproject.qsim.signalengine.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
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
 *
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public class QSim implements IOSimulation, ObservableSimulation, VisMobsim, AcceptsVisMobsimFeatures, QSimI {

	final private static Logger log = Logger.getLogger(QSim.class);

	/* time since last snapshot */
	private double snapshotTime = 0.0;
	private int snapshotPeriod = 0;

	/* time since last "info" */
	private double infoTime = 0;
	private static final int INFO_PERIOD = 3600;


//	private QNetwork network;
	private EventsManager events = null;

	private QSimEngineImpl netEngine = null;
	private NetworkChangeEventsEngine changeEventsEngine = null;
	private MultiModalSimEngine multiModalEngine = null;
	
	private CarDepartureHandler carDepartureHandler;

	private SimTimerI simTimer;

	private Collection<PersonAgent> transitAgents;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */
	protected final PriorityQueue<Tuple<Double, PersonAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, PersonAgent>>(30, new TeleportationArrivalTimeComparator());
	private final Date realWorldStarttime = new Date();
	private double stopTime = 100*3600;
	private AgentFactory agentFactory;
	private SimulationListenerManager listenerManager;
	protected final PriorityBlockingQueue<PersonAgent> activityEndsList =
		new PriorityBlockingQueue<PersonAgent>(500, new PersonAgentDepartureTimeComparator());
	protected Scenario scenario = null;
	private QSimSignalEngine signalEngine = null;

//	private final List<MobsimFeature> queueSimulationFeatures = new ArrayList<MobsimFeature>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	private QSimSnapshotWriterManager snapshotManager = new QSimSnapshotWriterManager();

	private TransitQSimEngine transitEngine;

	private AgentCounterI agentCounter;

	private Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();

	public QSim(final Scenario scenario, final EventsManager events) {
		this(scenario, events, new DefaultQSimEngineFactory());
	}

	protected QSim(final Scenario sc, final EventsManager events, final QSimEngineFactory simEngineFac){
		this.scenario = sc;
		this.events = events;
		init(simEngineFac);
	}

	public void setMultiModalSimEngine(MultiModalSimEngine multiModalEngine) {
		this.multiModalEngine = multiModalEngine;
	}
	
	/**
	 * extended constructor method that can also be after assignments of another constructor
	 * <p/>
	 * Definitely needs to be strongly protected since c'tor should not call non-protected methods. kai, aug'10
	 * @param simEngineFac
	 */
	private void init(QSimEngineFactory simEngineFac){
		log.info("Using QSim...");
		Scenario sc = this.getScenario() ;
		this.listenerManager = new SimulationListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new QSimTimer(sc.getConfig().getQSimConfigGroup().getTimeStepSize());

		// create the NetworkEngine ...
		this.netEngine = (QSimEngineImpl) simEngineFac.createQSimEngine(this, MatsimRandom.getRandom());

		// create the QNetwork ...
		QNetworkI network = null ;
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
			if ((((ScenarioImpl)sc).getSignalSystems() == null)
					|| (((ScenarioImpl)sc).getSignalSystemConfigurations() == null)) {
				throw new IllegalStateException(
						"Signal systems and signal system configurations have to be set if feature is enabled!");
			}
			this.signalEngine  = new QSimSignalEngine(this);
			this.signalEngine.setSignalSystems(((ScenarioImpl)sc).getSignalSystems(), ((ScenarioImpl)sc).getSignalSystemConfigurations());
		}

		this.agentFactory = new AgentFactory(this);

		this.carDepartureHandler = new CarDepartureHandler(this);
		addDepartureHandler(this.carDepartureHandler);

		if (sc.getConfig().scenario().isUseTransit()){
			this.transitEngine = new TransitQSimEngine(this);
			this.addDepartureHandler(this.transitEngine);
			// why is there not addition of pt to non teleported modes here?  kai, jun'10
			// done in TransitQSimEngine.  kai, jun'10
		}

	}


	@Override
	public final void run() {
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		//do iterations
		boolean doContinue = true;
		double time = this.simTimer.getTimeOfDay();
		while (doContinue) {
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			beforeSimStep(time);
			doContinue = doSimStep(time);
			afterSimStep(time);
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

	protected void createAgents() {
		if (this.scenario.getPopulation() == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			DefaultPersonDriverAgent agent = this.agentFactory.createPersonAgent(p);
			QVehicle veh = new QVehicleImpl(new VehicleImpl(agent.getPerson().getId(), defaultVehicleType));
			//not needed in new agent class
			veh.setDriver(agent); // this line is currently only needed for OTFVis to show parked vehicles
			agent.setVehicle(veh);
			agents.add(agent);
			if (agent.initializeAndCheckIfAlive()) {
				QLink qlink = this.netEngine.getQNetwork().getQLink(agent.getCurrentLinkId());
				qlink.addParkedVehicle(veh);
			}
		}

		if (this.transitEngine != null){
			Collection<PersonAgent> a = this.transitEngine.createAgents();
			this.transitAgents = a;
			agents.addAll(a);
		}

//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			for (PersonAgent agent : agents) {
//				queueSimulationFeature.agentCreated(agent);
//			}
//		}
//		for (PersonAgent agent : agents) {
//			this.events.processEvent( new AgentCreationEventImpl( Time.UNDEFINED_TIME, agent.getPerson().getId(), null, null )) ;
//		}
	}


	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	protected void prepareSim() {
		if (events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
		}

		if ( this.netEngine != null ) {
			this.netEngine.onPrepareSim();
		}
		if (this.multiModalEngine != null) {
			this.multiModalEngine.onPrepareSim();
		}
		if (this.signalEngine != null) {
			this.signalEngine.onPrepareSim();
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

//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) { // yyyy features should be replaced by listeners
//			queueSimulationFeature.afterPrepareSim();
//		}
	}

	/**
	 * Close any files, etc.
	 */
	protected final void cleanupSim(final double seconds) {
		if (this.transitEngine != null) { // yyyy do after features
			this.transitEngine.afterSim();
		}

		if (this.signalEngine != null) {
			this.signalEngine.afterSim();
		}

//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) { // yyyy features should be replaced by listeners
//			queueSimulationFeature.beforeCleanupSim();
//		}

		if ( this.netEngine != null ) {
			this.netEngine.afterSim();
		}
		
		if (this.multiModalEngine != null) {
			this.multiModalEngine.afterSim();
		}

		double now = this.simTimer.getTimeOfDay();

		for (Tuple<Double, PersonAgent> entry : this.teleportationList) {
			PersonAgent agent = entry.getSecond();
			events.processEvent(events.getFactory().
					createAgentStuckEvent(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (PersonAgent agent : this.activityEndsList) {
			if (agent.getDestinationLinkId() != null) {
				events.processEvent(events.getFactory().
						createAgentStuckEvent(now, agent.getPerson().getId(), agent.getDestinationLinkId(), null));
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
			writer.finish();
		}

		this.netEngine = null;
		this.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	protected void beforeSimStep(final double time) {
		// left empty for inheritance
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
		// signal engine:
		if (this.signalEngine != null) {
			this.signalEngine.doSimStep(time);
		}
		// teleportation "engine":
		this.handleTeleportationArrivals(time);

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

	protected void afterSimStep(final double time) {
		// left empty for inheritance
	}

	protected void handleTeleportationArrivals(final double now) {
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, PersonAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				PersonAgent personAgent = entry.getSecond();
				personAgent.teleportToLink(personAgent.getDestinationLinkId());
				personAgent.endLegAndAssumeControl(now);
			}
			else {
				break;
			}
		}
	}

//	/**
//	 * Should be a PersonAgentI as argument, but is needed because the old events form is still used also for tests
//	 * @param now
//	 * @param agent
//	 */
//	@Override
//	public void handleAgentArrival(final double now, final PersonDriverAgent agent) {
////		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
////			queueSimulationFeature.beforeHandleAgentArrival(agent);
////		}
//		getEventsManager().processEvent(getEventsManager().getFactory().createAgentArrivalEvent(now, agent.getPerson().getId(),
//				agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
//	}


	/**
	 * Registers this agent as performing an activity and makes sure that the agent will be informed once his departure time has come.
	 * @param agent
	 *
	 * @see PersonDriverAgent#getDepartureTime()
	 */
	@Override
	public void scheduleActivityEnd(final PersonAgent agent) {
		this.activityEndsList.add(agent);
		registerAgentAtActivityLocation(agent);
	}

	private void registerAgentAtActivityLocation(final PersonAgent agent) {
		// if the "activities" engine were separate, this would need to be public.  kai, aug'10
		if (agent instanceof DefaultPersonDriverAgent) { // yyyyyy is this necessary?
//			DefaultPersonDriverAgent pa = (DefaultPersonDriverAgent) agent;
			PlanElement pe = agent.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				QLink qLink = this.netEngine.getQNetwork().getQLink(linkId);
				qLink.registerAgentOnLink(agent);
			}
		}
	}
	
	public final void registerAgentAtPtWaitLocation(final PersonAgent agent) {
		if (agent instanceof DefaultPersonDriverAgent) { // yyyy but why is this needed?  Does the driver get registered?
			Leg leg = (Leg) agent.getCurrentPlanElement() ;
			Id linkId = leg.getRoute().getStartLinkId();
			QLink qLink = this.netEngine.getQNetwork().getQLink(linkId) ;
			qLink.registerAgentOnLink( agent ) ;
		}
	}

	private void unregisterAgentAtActivityLocation(final PersonAgent agent) {
		if (agent instanceof DefaultPersonDriverAgent) { // yyyy but why is this needed?
//			DefaultPersonDriverAgent pa = (DefaultPersonDriverAgent) agent;
			PlanElement pe = agent.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				QLink qLink = this.netEngine.getQNetwork().getQLink(linkId);
				qLink.unregisterAgentOnLink(agent);
			}
		}
	}
	
	public final void unregisterAgentAtPtWaitLocation( final PersonAgent agent ) {
		if (agent instanceof DefaultPersonDriverAgent) { // yyyy but why is this needed?
			Leg leg = (Leg) agent.getCurrentPlanElement() ;
			Id linkId = leg.getRoute().getStartLinkId();
			QLink qLink = this.netEngine.getQNetwork().getQLink(linkId) ;
			qLink.unregisterAgentOnLink( agent ) ;
		}
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			PersonAgent agent = this.activityEndsList.peek();
			if (agent.getDepartureTime() <= time) {
				this.activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndAssumeControl(time); 
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
	@Deprecated // unclear if this is "actEnd" or "departure"!  kai, may'10
	// depending on this, it is a "PersonAgent" or "DriverAgent".  kai, may'10
	// I think it is departure, but still a person agent.  kai, aug'10
	public void agentDeparts(final PersonAgent agent, final Id linkId) {
		double now = this.getSimTimer().getTimeOfDay() ;
		Leg leg = agent.getCurrentLeg();
//		Route route = leg.getRoute();
		String mode = leg.getMode();
		events.processEvent(events.getFactory().createAgentDepartureEvent(now, agent.getPerson().getId(), linkId, mode ));
		if ( handleKnownLegModeDeparture(now, agent, linkId, leg) ) {
			return ;
		} else {
			handleUnknownLegMode( now, agent, linkId, leg ) ;
			events.processEvent(new AdditionalTeleportationDepartureEvent( now, agent.getPerson().getId(), linkId, mode, agent.getDestinationLinkId(), leg.getTravelTime() )) ;
		}
	}

	private void handleUnknownLegMode(final double now, final PersonAgent personAgent, final Id linkId, final Leg leg) {
		double arrivalTime = now + personAgent.getCurrentLeg().getTravelTime();
		this.teleportationList.add(new Tuple<Double, PersonAgent>(arrivalTime, personAgent));
	}

	private boolean handleKnownLegModeDeparture(final double now, final PersonAgent personAgent, final Id linkId, final Leg leg)
	{
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if ( departureHandler.handleDeparture(now, personAgent, linkId, leg) ) {
				return true ;
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
		double startTime = this.scenario.getConfig().getQSimConfigGroup().getStartTime();
		this.stopTime = this.scenario.getConfig().getQSimConfigGroup().getEndTime();
		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;
		this.simTimer.setSimStartTime(24*3600);
		this.simTimer.setTime(startTime);
		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		double simStartTime = 0;
		PersonAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getDepartureTime()));
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
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time) + ": #Veh=" + this.agentCounter.getLiving()
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
			for (QLink link : this.getQNetwork().getLinks().values()) {
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
	public void setAgentFactory(final AgentFactory fac) {
		this.agentFactory = fac;
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
	/*package*/ void setTeleportVehicles(final boolean teleportVehicles) {
		this.carDepartureHandler.setTeleportVehicles(teleportVehicles);
	}

	@Override
	public QNetwork getQNetwork() {
		if ( this.netEngine != null ) {
			return this.netEngine.getQNetwork() ;
		} else {
			return null ;
		}
	}

	@Override
	public VisNetwork getVisNetwork() {
		return this.netEngine.getQNetwork() ;
	}

	@Override
	public Scenario getScenario() {
		return this.scenario;
	}

//	public boolean isUseActivityDurations() {
//		return this.scenario.getConfig().vspExperimental().isUseActivityDurations();
//	}

	public SignalEngine getQSimSignalEngine() {
		return this.signalEngine;
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
	public void setIterationNumber(final Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	@Override
	public void setControlerIO(final ControlerIO controlerIO) {
		this.controlerIO = controlerIO;
	}

	@Override
	public SimTimerI getSimTimer() {
		return this.simTimer ;
	}

	public QSimEngine getQSimEngine() {
	  return this.netEngine;
	}

	public Collection<PersonAgent> getTransitAgents(){
		return this.transitAgents; // set null to save memory
	}

	@Override
	public void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotManager.addSnapshotWriter(snapshotWriter);
	}

//	public double getStuckTime(){
//		return this.stuckTime;
//	}

	@Override
	public AgentCounterI getAgentCounter(){
		return this.agentCounter;
	}

	public void addDepartureHandler(final DepartureHandler departureHandler) {
		this.departureHandlers.add(departureHandler);
	}

	/**
	 * Adds the QueueSimulationListener instance  given as parameters as
	 * listener to this QueueSimulation instance.
	 * @param listeners
	 */
	@Override
	public void addQueueSimulationListeners(final SimulationListener listener){
		this.listenerManager.addQueueSimulationListener(listener);
	}

	@Override
	public Collection<MobsimAgent> getAgents() {
		return this.agents ;
	}

	public TransitQSimEngine getQSimTransitEngine() {
		return transitEngine;
	}

}
