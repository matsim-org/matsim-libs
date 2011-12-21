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

package org.matsim.core.mobsim.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * Implementation of a queue-based transport simulation.
 * Lanes and SignalSystems are not initialized unless the setter are invoked.
 *
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 */
public class QueueSimulation implements ObservableSimulation, VisMobsim, Netsim {
	// yyyy not sure if I want this public but something has to give for integration with OTFVis.  kai, may'10

	private int snapshotPeriod = 0;
	private double snapshotTime = 0.0; 	/* time since lasat snapshot */

	protected static final int INFO_PERIOD = 3600;
	private double infoTime = 0; 	/* time since last "info" message */

	private final Config config;
	private final Population population;
	private QueueNetwork network;
	private Network networkLayer;

	private static EventsManager events = null;

	private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();

	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;

	private QueueSimEngine netSimEngine = null;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final PriorityQueue<Tuple<Double, MobsimAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TeleportationArrivalTimeComparator());

	private final Date starttime = new Date();

	private double stopTime = 100*3600;

	final private static Logger log = Logger.getLogger(QueueSimulation.class);

	private AgentFactory agentFactory;

	private SimulationListenerManager listenerManager;

	private final PriorityBlockingQueue<MobsimAgent> activityEndsList =
		new PriorityBlockingQueue<MobsimAgent>(500, new PlanAgentDepartureTimeComparator());

	private Scenario scenario = null;

	/** @see #setTeleportVehicles(boolean) */
	private boolean teleportVehicles = true;
	private int cntTeleportVehicle = 0;

	private boolean useActivityDurations = true;

	private final Set<String> notTeleportedModes = new HashSet<String>();

	private AgentCounterI agentCounter = new AgentCounter() ;
	private MobsimTimer simTimer ;
	
    static boolean NEW = true ;

	/**
	 * The InternalInterface technically is not needed for the QueueSimulation since everything is in the same 
	 * package.  It helps with a stepwise transition, though.  May be removed again once the transition is completed.
	 * kai, dec'11
	 */
    /*package */ InternalInterface internalInterface = new InternalInterface() {
		@Override
		public void arrangeNextAgentState(MobsimAgent agent) {
			if ( NEW ) {
				QueueSimulation.this.arrangeNextAgentAction(agent) ;
			} else {
			}
		}

		@Override
		public Netsim getMobsim() {
			return QueueSimulation.this ;
		}
		
		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
			throw new UnsupportedOperationException() ;
		}

//		@Override
//		public void rescheduleActivityEnd(MobsimAgent agent, double oldTime, double newTime) {
//			throw new UnsupportedOperationException() ;
//		}

	};
	
//	@Deprecated // to be replaced by internalInterface.arrangeNextAgentState()
//	@Override
//	public final void reInsertAgentIntoMobsim( MobsimAgent agent ) {
//		if ( NEW ) {
//		} else {
//			this.arrangeNextAgentAction( agent) ;
//		}
//	}


	/**
	 * Initialize the QueueSimulation without signal systems
	 * @param scenario
	 * @param events
	 */
	protected QueueSimulation(final Scenario scenario, final EventsManager events) {
		this(scenario, events, new DefaultQueueNetworkFactory());
	}


	protected QueueSimulation(final Scenario sc, final EventsManager events, final QueueNetworkFactory factory){
		this.scenario = sc;
		this.config = scenario.getConfig();

		if ( this.config.getModule(SimulationConfigGroup.GROUP_NAME) == null ) {
			log.warn("Started QueueSimulation without a `simulation' config module.  Presumably due to removing " +
					"`simulation' from the core modules in nov/dec'10.  Add simulation config module before calling QueueSimulation " +
			"creational method to avoid this warning.  kai, dec'10");
			this.config.addSimulationConfigGroup(new SimulationConfigGroup()) ;
		}

		this.listenerManager = new SimulationListenerManager(this);

		this.agentCounter.reset();

		simTimer = new MobsimTimer(this.config.simulation().getTimeStepSize()) ;

		setEvents(events);
		this.population = scenario.getPopulation();

		this.networkLayer = scenario.getNetwork();

		this.network = new QueueNetwork(this.networkLayer, factory, this);

		this.agentFactory = new AgentFactory( this);

		this.notTeleportedModes.add(TransportMode.car);

		this.netSimEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom(), this.scenario.getConfig());
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
	public final void run() {
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		//do iterations
		boolean cont = true;
		while (cont) {

			//			double time = this.simTimer.getTimeOfDayStatic();
			double time = simTimer.getTimeOfDay() ;

			beforeSimStep(time);
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			cont = doSimStep(time);
			afterSimStep(time);
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (cont) {
				this.simTimer.incrementTime();
			}
		}
		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();
		cleanupSim();
		//delete reference to clear memory
		this.listenerManager = null;
	}

	final void createAgents() {
		if (this.population == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();

		for (Person p : this.population.getPersons().values()) {
			MobsimDriverAgent agent = this.agentFactory.createPersonAgent(p);
			agents.add( agent ) ;
			QueueVehicle veh = new QueueVehicle(new VehicleImpl(agent.getId(), defaultVehicleType));
			agent.setVehicle(veh);
			QueueLink qlink = this.network.getQueueLink(agent.getCurrentLinkId());
			qlink.addParkedVehicle(veh);
		}

	}

	private void prepareNetworkChangeEventsQueue() {
		Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl)(this.networkLayer)).getNetworkChangeEvents();
		if ((changeEvents != null) && (changeEvents.size() > 0)) {
			this.networkChangeEventsQueue = new PriorityQueue<NetworkChangeEvent>(changeEvents.size(), new NetworkChangeEvent.StartTimeComparator());
			this.networkChangeEventsQueue.addAll(changeEvents);
		}
	}

	/**
	 * Prepare the simulation and get all the settings from the configuration.
	 */
	final void prepareSim() {
		if (events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
		}

		// Initialize Snapshot file
		this.snapshotPeriod = (int) this.config.simulation().getSnapshotPeriod();

		double startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;

		this.simTimer.setSimStartTime(24*3600);
		this.simTimer.setTime(startTime);

		createAgents();

		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		double simStartTime = 0;
		MobsimAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getActivityEndTime()));
		}
		this.infoTime = Math.floor(simStartTime / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(simStartTime / this.snapshotPeriod) * this.snapshotPeriod;
		if (this.snapshotTime < simStartTime) {
			this.snapshotTime += this.snapshotPeriod;
		}
		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(this.simTimer.getSimStartTime());

		prepareNetworkChangeEventsQueue();
	}


	/**
	 * Close any files, etc.
	 */
	final void cleanupSim() {

		this.netSimEngine.afterSim();

		double now = this.simTimer.getTimeOfDay();

		for (Tuple<Double, MobsimAgent> entry : this.teleportationList) {
			MobsimAgent agent = entry.getSecond();
			events.processEvent(new AgentStuckEventImpl(now, agent.getId(), agent.getDestinationLinkId(), agent.getMode()));
		}
		this.teleportationList.clear();

		for (MobsimAgent agent : this.activityEndsList) {
			if ( agent.getActivityEndTime()!=Double.POSITIVE_INFINITY 
					&& agent.getActivityEndTime()!=Time.UNDEFINED_TIME ) {
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

		this.netSimEngine = null;
		QueueSimulation.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	final void beforeSimStep(final double time) {
		if ((this.networkChangeEventsQueue != null) && (this.networkChangeEventsQueue.size() > 0)) {
			handleNetworkChangeEvents(time);
		}
	}

	/**
	 * Do one step of the simulation run.
	 *
	 * @param time the current time in seconds after midnight
	 * @return true if the simulation needs to continue
	 */
	final boolean doSimStep(final double time) {
		this.moveVehiclesWithUnknownLegMode(time);
		this.handleActivityEnds(time);
		this.netSimEngine.simStep(time);

		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
			double diffsim  = time - this.simTimer.getSimStartTime();
			int nofActiveLinks = this.netSimEngine.getNumberOfSimulatedLinks();
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + this.agentCounter.getLiving() + " lost=" + this.agentCounter.getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	final void afterSimStep(final double time) {
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}
	}

	final void doSnapshot(final double time) {
		if (!this.snapshotWriters.isEmpty()) {
			Collection<AgentSnapshotInfo> positions = this.network.getVehiclePositions();
			for (SnapshotWriter writer : this.snapshotWriters) {
				writer.beginSnapshot(time);
				for (AgentSnapshotInfo position : positions) {
					writer.addAgent(position);
				}
				writer.endSnapshot();
			}
		}

	}

	/* package */ static final EventsManager getEvents() {
		return events;
	}

	@Override
	public EventsManager getEventsManager() {
		return this.events ;
	}

	private static final void setEvents(final EventsManager events) {
		QueueSimulation.events = events;
	}

	final void handleUnknownLegMode(double now, final MobsimAgent planAgent) {
		double arrivalTime = this.simTimer.getTimeOfDay() + planAgent.getExpectedTravelTime();

		this.teleportationList.add(new Tuple<Double, MobsimAgent>(arrivalTime, planAgent));
	}

	final void moveVehiclesWithUnknownLegMode(final double now) {
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, MobsimAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				MobsimAgent person = entry.getSecond();
				person.notifyTeleportToLink(person.getDestinationLinkId());
				person.endLegAndAssumeControl(now) ;
				this.internalInterface.arrangeNextAgentState(person) ;
			} else break;
		}
	}

	private void handleNetworkChangeEvents(final double time) {
		while ((this.networkChangeEventsQueue.size() > 0) && (this.networkChangeEventsQueue.peek().getStartTime() <= time)){
			NetworkChangeEvent event = this.networkChangeEventsQueue.poll();
			for (Link link : event.getLinks()) {
				this.network.getQueueLink(link.getId()).recalcTimeVariantAttributes(time);
			}
		}
	}
	
	public final void insertAgentIntoMobsim( MobsimAgent agent ) {
		this.arrangeNextAgentAction(agent) ;
	}
	
	private void arrangeNextAgentAction(MobsimAgent agent ) {

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
	private void arrangeActivityStart(final MobsimAgent agent) {
		this.activityEndsList.add(agent);
		if ( agent.getActivityEndTime()==Double.POSITIVE_INFINITY ) {
			this.agentCounter.decLiving() ;
		}
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			MobsimAgent agent = this.activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				agent.endActivityAndAssumeControl(time);
				this.internalInterface.arrangeNextAgentState(agent) ;
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
	private void arrangeAgentDeparture(final MobsimAgent agent) {
		double now = this.getSimTimer().getTimeOfDay() ;
		String mode = agent.getMode();
		Id linkId = agent.getCurrentLinkId() ;
		events.processEvent( events.getFactory().createAgentDepartureEvent( now, agent.getId(), linkId, mode ) ) ;
		if (this.notTeleportedModes.contains(mode)){
			this.handleKnownLegModeDeparture(now, agent, linkId, mode);
		}
		else {
			this.handleUnknownLegMode(now, agent);
		}
	}

	final void handleKnownLegModeDeparture(double now, MobsimAgent agent, Id linkId, String mode) {
		if (mode.equals(TransportMode.car)) {
			if ( !(agent instanceof MobsimDriverAgent) ) {
				throw new IllegalStateException("PersonAgent that is not a DriverAgent cannot have car as mode") ;
			}
			MobsimDriverAgent driverAgent = (MobsimDriverAgent) agent ;
			Id vehicleId = driverAgent.getPlannedVehicleId() ;
			if (vehicleId == null) {
				vehicleId = driverAgent.getId(); // backwards-compatibility
			}
			QueueLink qlink = this.network.getQueueLink(linkId);
			QueueVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
			if (vehicle == null) {
				// try to fix it somehow
				if (this.teleportVehicles) {
					vehicle = (QueueVehicle) driverAgent.getVehicle();
					if (vehicle.getCurrentLink() != null) {
						if (this.cntTeleportVehicle < 9) {
							this.cntTeleportVehicle++;
							log.info("teleport vehicle " + vehicle.getId() + " from link " + vehicle.getCurrentLink().getId() + " to link " + linkId);
							if (this.cntTeleportVehicle == 9) {
								log.info("No more occurrences of teleported vehicles will be reported.");
							}
						}
						QueueLink qlinkOld = this.network.getQueueLink(vehicle.getCurrentLink().getId());
						qlinkOld.removeParkedVehicle(vehicle.getId());
					}
				}
			}
			if (vehicle == null) {
				throw new RuntimeException("vehicle not available for agent " + driverAgent.getId() + " on link " + linkId);
			}
			vehicle.setDriver(driverAgent);
			if (
//					(route.getEndLinkId().equals(linkId))
					agent.getDestinationLinkId().equals(linkId)
					&& (driverAgent.chooseNextLinkId() == null)) {
				driverAgent.endLegAndAssumeControl(now) ;
				qlink.processVehicleArrival(now, vehicle);
				this.internalInterface.arrangeNextAgentState(agent) ;
			} else {
				qlink.addDepartingVehicle(vehicle);
			}
		}
	}

	@Override
	public void addSnapshotWriter(final SnapshotWriter writer) {
		this.snapshotWriters.add(writer);
	}

	/*package*/ void setAgentFactory(final AgentFactory fac) {
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
		this.teleportVehicles = teleportVehicles;
	}

	//	private static class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, PersonDriverAgent>>, Serializable {
	//		private static final long serialVersionUID = 1L;
	//		@Override
	//		public int compare(final Tuple<Double, PersonDriverAgent> o1, final Tuple<Double, PersonDriverAgent> o2) {
	//			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
	//			if (ret == 0) {
	//				ret = o2.getSecond().getPerson().getId().compareTo(o1.getSecond().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
	//			}
	//			return ret;
	//		}
	//	}

	QueueNetwork getQueueNetwork() {
		return this.network;
	}

	@Override
	public VisNetwork getVisNetwork() {
		return this.network ;
	}

	//	@Override
	//	public CapacityInformationNetwork getCapacityInformationNetwork() {
	//		return this.network ;
	//	}

	@Override
	public Scenario getScenario() {
		return this.scenario;
	}


	final boolean isUseActivityDurations() {
		return this.useActivityDurations;
	}

	/*package*/ void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
		log.info("QueueSimulation is working with activity durations: " + this.isUseActivityDurations());
	}

	final Set<String> getNotTeleportedModes() {
		return notTeleportedModes;
	}

	@Override
	public AgentCounterI getAgentCounter() {
		return this.agentCounter ;
	}


	@Override
	public NetsimNetwork getNetsimNetwork() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public MobsimTimer getSimTimer() {
		return this.simTimer ;
	}


	@Override
	public void setAgentFactory( org.matsim.ptproject.qsim.agents.AgentFactory agentFactory) {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public Collection<MobsimAgent> getAgents() {
		throw new UnsupportedOperationException() ;
	}


//	@Override
//	public void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
//		throw new UnsupportedOperationException() ;
//	}


	@Override
	public Collection<MobsimAgent> getActivityEndsList() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public void rescheduleActivityEnd(MobsimAgent agent, double oldTime, double newTime) {
		throw new UnsupportedOperationException() ;
	}


//	@Override
//	public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
//		throw new UnsupportedOperationException() ;
//	}


	@Override
	public void addParkedVehicle(MobsimVehicle veh, Id startLinkId) {
		throw new UnsupportedOperationException() ;
	}

}
