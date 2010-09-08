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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.comparators.PersonAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.interfaces.AcceptsVisMobsimFeatures;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.QNetworkI;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.interfaces.QVehicle;
import org.matsim.ptproject.qsim.interfaces.SimTimerI;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFFileWriterQueueSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFQueueSimServerQuadBuilder;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.KmlSnapshotWriter;
import org.matsim.vis.snapshots.writers.PlansFileSnapshotWriter;
import org.matsim.vis.snapshots.writers.SnapshotWriter;
import org.matsim.vis.snapshots.writers.TransimsSnapshotWriter;
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
 */
public class QueueSimulation implements IOSimulation, ObservableSimulation, VisMobsim, AcceptsVisMobsimFeatures, QSimI {
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

	private QueueSimEngine simEngine = null;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final PriorityQueue<Tuple<Double, PersonAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, PersonAgent>>(30, new TeleportationArrivalTimeComparator());

	private final Date starttime = new Date();

	private double stopTime = 100*3600;

	final private static Logger log = Logger.getLogger(QueueSimulation.class);

	private AgentFactory agentFactory;

	private SimulationListenerManager listenerManager;

	private final PriorityBlockingQueue<PersonAgent> activityEndsList =
		new PriorityBlockingQueue<PersonAgent>(500, new PersonAgentDepartureTimeComparator());

	private Scenario scenario = null;

	/** @see #setTeleportVehicles(boolean) */
	private boolean teleportVehicles = true;
	private int cntTeleportVehicle = 0;

	private boolean useActivityDurations = true;

	private final Set<String> notTeleportedModes = new HashSet<String>();

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;

//	private final List<MobsimFeature> queueSimulationFeatures = new ArrayList<MobsimFeature>() ;
	private AgentCounterI agentCounter = new AgentCounter() ;
	private SimTimerI simTimer ;

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
		this.listenerManager = new SimulationListenerManager(this);
//		AbstractSimulation.reset(this.config.simulation().getStuckTime());

//		this.agentCounter.setLiving(0);
//		this.agentCounter.setLost(0);
		this.agentCounter.reset();

//		SimulationTimer.resetStatic(this.config.simulation().getTimeStepSize());
		simTimer = StaticFactoriesContainer.createSimulationTimer(this.config.simulation().getTimeStepSize()) ;

		setEvents(events);
		this.population = scenario.getPopulation();

		this.networkLayer = scenario.getNetwork();

		this.network = new QueueNetwork(this.networkLayer, factory, this);

		this.agentFactory = new AgentFactory( this);

		this.notTeleportedModes.add(TransportMode.car);

		this.simEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom(), this.scenario.getConfig());
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

	protected void createAgents() {
		if (this.population == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		Collection<PersonAgent> agents = new ArrayList<PersonAgent>();

		for (Person p : this.population.getPersons().values()) {
			PersonDriverAgent agent = this.agentFactory.createPersonAgent(p);
			agents.add( agent ) ;

			QVehicle veh = StaticFactoriesContainer.createQueueVehicle(new VehicleImpl(agent.getPerson().getId(), defaultVehicleType));
			//not needed in new agent class
			veh.setDriver(agent); // this line is currently only needed for OTFVis to show parked vehicles
			agent.setVehicle(veh);

			if ( agent.initializeAndCheckIfAlive()) {
				QueueLink qlink = this.network.getQueueLink(agent.getCurrentLinkId());
				qlink.addParkedVehicle(veh);
			}
		}


//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			for (PersonAgent agent : agents) {
//				queueSimulationFeature.agentCreated(agent);
//			}
//		}
//		for (PersonAgent agent : agents) {
//			QueueSimulation.events.processEvent( new AgentCreationEventImpl( Time.UNDEFINED_TIME, agent.getPerson().getId(), null, null )) ;
//		}


	}

	private void createSnapshotwriter() {
		// A snapshot period of 0 or less indicates that there should be NO snapshot written
		if (this.snapshotPeriod > 0 ) {
			String snapshotFormat =  this.config.simulation().getSnapshotFormat();
			Integer itNumber = this.iterationNumber;
			if (this.controlerIO == null) {
				log.error("Not able to create io path via ControlerIO in mobility simulation, not able to write visualizer output!");
				return;
			}
			else if (itNumber == null) {
				log.warn("No iteration number set in mobility simulation using iteration number 0 for snapshot file...");
				itNumber = 0;
			}
			if (snapshotFormat.contains("plansfile")) {
				String snapshotFilePrefix = this.controlerIO.getIterationPath(itNumber) + "/positionInfoPlansFile";
				String snapshotFileSuffix = "xml";
				this.snapshotWriters.add(new PlansFileSnapshotWriter(snapshotFilePrefix,snapshotFileSuffix, this.networkLayer));
			}
			if (snapshotFormat.contains("transims")) {
				String snapshotFile = this.controlerIO.getIterationFilename(itNumber, "T.veh");
				this.snapshotWriters.add(new TransimsSnapshotWriter(snapshotFile));
			}
			if (snapshotFormat.contains("googleearth")) {
				String snapshotFile = this.controlerIO.getIterationFilename(itNumber, "googleearth.kmz");
				String coordSystem = this.config.global().getCoordinateSystem();
				this.snapshotWriters.add(new KmlSnapshotWriter(snapshotFile,
						TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84)));
			}
			if (snapshotFormat.contains("netvis")) {
				throw new IllegalStateException("netvis is no longer supported by this simulation");
			}
			if (snapshotFormat.contains("otfvis")) {
				String snapshotFile = this.controlerIO.getIterationFilename(itNumber, "otfvis.mvi");
				OTFFileWriter writer = new OTFFileWriter(this.snapshotPeriod, new OTFQueueSimServerQuadBuilder(this.network), snapshotFile, new OTFFileWriterQueueSimConnectionManagerFactory());
				this.snapshotWriters.add(writer);
			}
		} else this.snapshotPeriod = Integer.MAX_VALUE; // make sure snapshot is never called
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
	protected void prepareSim() {
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
		PersonAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getDepartureTime()));
		}
		this.infoTime = Math.floor(simStartTime / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(simStartTime / this.snapshotPeriod) * this.snapshotPeriod;
		if (this.snapshotTime < simStartTime) {
			this.snapshotTime += this.snapshotPeriod;
		}
		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(this.simTimer.getSimStartTime());

		createSnapshotwriter();

		prepareNetworkChangeEventsQueue();

//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			queueSimulationFeature.afterPrepareSim();
//		}
	}


	/**
	 * Close any files, etc.
	 */
	protected void cleanupSim() {
//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			queueSimulationFeature.beforeCleanupSim();
//		}

		this.simEngine.afterSim();

//		double now = this.simTimer.getTimeOfDayStatic();
		double now = this.simTimer.getTimeOfDay();

		for (Tuple<Double, PersonAgent> entry : this.teleportationList) {
			PersonAgent agent = entry.getSecond();
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (PersonAgent agent : this.activityEndsList) {
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), null));
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}

		this.simEngine = null;
		QueueSimulation.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	protected void beforeSimStep(final double time) {
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
	protected boolean doSimStep(final double time) {
		this.moveVehiclesWithUnknownLegMode(time);
		this.handleActivityEnds(time);
		this.simEngine.simStep(time);

		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
			double diffsim  = time - this.simTimer.getSimStartTime();
			int nofActiveLinks = this.simEngine.getNumberOfSimulatedLinks();
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + this.agentCounter.getLiving() + " lost=" + this.agentCounter.getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep(final double time) {
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}
//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			queueSimulationFeature.afterAfterSimStep(time);
//		}
	}

	private void doSnapshot(final double time) {
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

	protected void handleUnknownLegMode(double now, final PersonAgent agent) {
//		Id startLinkId = agent.getCurrentLeg().getRoute().getStartLinkId() ;
//		Leg leg = agent.getCurrentLeg() ;

//		Link     currentLink = this.scenario.getNetwork().getLinks().get(startLinkId) ;
//		Link destinationLink = this.scenario.getNetwork().getLinks().get(agent.getDestinationLinkId()) ;


//		for (MobsimFeature queueSimulationFeature : this.queueSimulationFeatures) {
//			queueSimulationFeature.beforeHandleUnknownLegMode(now, agent, currentLink, destinationLink ) ;
//		}

//		double arrivalTime = this.simTimer.getTimeOfDayStatic() + agent.getCurrentLeg().getTravelTime();
		double arrivalTime = this.simTimer.getTimeOfDay() + agent.getCurrentLeg().getTravelTime();

		this.teleportationList.add(new Tuple<Double, PersonAgent>(arrivalTime, agent));
	}

	protected void moveVehiclesWithUnknownLegMode(final double now) {
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, PersonAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				PersonAgent person = entry.getSecond();
				person.teleportToLink(person.getDestinationLinkId());
				person.endLegAndAssumeControl(now);
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

	/**
	 * Registers this agent as performing an activity and makes sure that the
	 * agent will be informed once his departure time has come.
	 *
	 * @param agent
	 *
	 * @see PersonDriverAgent#getDepartureTime()
	 */
	@Override
	public void scheduleActivityEnd(final PersonAgent agent) {
		this.activityEndsList.add(agent);
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			PersonAgent agent = this.activityEndsList.peek();
			if (agent.getDepartureTime() <= time) {
				this.activityEndsList.poll();
				agent.endActivityAndAssumeControl(time);
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
	public void agentDeparts(final PersonAgent agent, final Id linkId) {
		double now = this.getSimTimer().getTimeOfDay() ;
		Leg leg = agent.getCurrentLeg();
		String mode = leg.getMode();
		events.processEvent( events.getFactory().createAgentDepartureEvent( now, agent.getPerson().getId(), linkId, leg.getMode() ) ) ;
		if (this.notTeleportedModes.contains(mode)){
			this.handleKnownLegModeDeparture(now, agent, linkId, mode);
		}
		else {
			this.handleUnknownLegMode(now, agent);
		}
	}

	protected void handleKnownLegModeDeparture(double now, PersonAgent personAgent, Id linkId, String mode) {
		Leg leg = personAgent.getCurrentLeg();
		if (mode.equals(TransportMode.car)) {
			if ( !(personAgent instanceof PersonDriverAgent) ) {
				throw new IllegalStateException("PersonAgent that is not a DriverAgent cannot have car as mode") ;
			}
			PersonDriverAgent driverAgent = (PersonDriverAgent) personAgent ;
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id vehicleId = route.getVehicleId();
			if (vehicleId == null) {
				vehicleId = driverAgent.getPerson().getId(); // backwards-compatibility
			}
			QueueLink qlink = this.network.getQueueLink(linkId);
			QVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
			if (vehicle == null) {
				// try to fix it somehow
				if (this.teleportVehicles) {
					vehicle = driverAgent.getVehicle();
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
				throw new RuntimeException("vehicle not available for agent " + driverAgent.getPerson().getId() + " on link " + linkId);
			}
			vehicle.setDriver(driverAgent);
			if ((route.getEndLinkId().equals(linkId)) && (driverAgent.chooseNextLinkId() == null)) {
				driverAgent.endLegAndAssumeControl(now);
				qlink.processVehicleArrival(now, vehicle);
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


	public boolean isUseActivityDurations() {
		return this.useActivityDurations;
	}

	/*package*/ void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
		log.info("QueueSimulation is working with activity durations: " + this.isUseActivityDurations());
	}

	public Set<String> getNotTeleportedModes() {
		return notTeleportedModes;
	}


	/*package*/ Integer getIterationNumber() {
		return iterationNumber;
	}

	@Override
	public void setIterationNumber(Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	@Override
	public void setControlerIO(ControlerIO controlerIO) {
		this.controlerIO = controlerIO;
	}


	@Override
	public void addFeature(VisMobsimFeature queueSimulationFeature) {
//		this.queueSimulationFeatures.add( queueSimulationFeature ) ;
		this.addQueueSimulationListeners(queueSimulationFeature);
		this.getEventsManager().addHandler(queueSimulationFeature) ;
		throw new UnsupportedOperationException("not tested") ;
	}


	@Override
	public AgentCounterI getAgentCounter() {
		return this.agentCounter ;
	}


	@Override
	public QNetworkI getQNetwork() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public SimTimerI getSimTimer() {
		return this.simTimer ;
	}


	double getStuckTime() {
		return this.getScenario().getConfig().simulation().getStuckTime() ;
	}


	@Override
	public void setAgentFactory( org.matsim.ptproject.qsim.AgentFactory agentFactory) {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public Collection<MobsimAgent> getAgents() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}


}
