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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.ObservableSimulation;
import org.matsim.core.mobsim.queuesim.SignalEngine;
import org.matsim.core.mobsim.queuesim.listener.QueueSimListenerManager;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vis.netvis.streaming.SimStateWriterI;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFFileWriterQSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.snapshots.writers.KmlSnapshotWriter;
import org.matsim.vis.snapshots.writers.PlansFileSnapshotWriter;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;
import org.matsim.vis.snapshots.writers.TransimsSnapshotWriter;

/**
 * Implementation of a queue-based transport simulation.
 * Lanes and SignalSystems are not initialized unless the setter are invoked.
 *
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 */
public class QueueSimulation implements org.matsim.core.mobsim.IOSimulation, ObservableSimulation{

	private int snapshotPeriod = 0;
	private double snapshotTime = 0.0;
	protected static final int INFO_PERIOD = 3600;
	private double infoTime = 0;
	private final Config config;
	protected final PopulationImpl population;
	protected QueueNetwork network;
	protected Network networkLayer;
	private static EventsManager events = null;
	protected  SimStateWriterI netStateWriter = null;
	private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();
	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;
	protected QueueSimEngine simEngine = null;
	private CarDepartureHandler carDepartureHandler;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */
	protected final PriorityQueue<Tuple<Double, DriverAgent>> teleportationList = new PriorityQueue<Tuple<Double, DriverAgent>>(30, new TeleportationArrivalTimeComparator());
	private final Date starttime = new Date();
	private double stopTime = 100*3600;
	final private static Logger log = Logger.getLogger(QueueSimulation.class);
	private AgentFactory agentFactory;
	private QueueSimListenerManager<QueueSimulation> listenerManager;
	protected final PriorityBlockingQueue<DriverAgent> activityEndsList = new PriorityBlockingQueue<DriverAgent>(500, new DriverAgentDepartureTimeComparator());
	protected Scenario scenario = null;
	private LaneDefinitions laneDefintions;
	private boolean useActivityDurations = true;
	private QueueSimSignalEngine signalEngine = null;
	private final Set<TransportMode> notTeleportedModes = new HashSet<TransportMode>();
	
	private final List<QueueSimulationFeature> queueSimulationFeatures = new ArrayList<QueueSimulationFeature>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();
	
	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	
	/**
	 * Initialize the QueueSimulation without signal systems
	 * @param network
	 * @param plans
	 * @param events
	 */
	public QueueSimulation(final Network network, final Population plans, final EventsManager events) {
		// In my opinion, this should be marked as deprecated in favor of the constructor with Scenario. marcel/16july2009
		this.listenerManager = new QueueSimListenerManager<QueueSimulation>(this);
		Simulation.reset();
		this.config = Gbl.getConfig();
		SimulationTimer.reset(this.config.simulation().getTimeStepSize());
		setEvents(events);
		this.population = (PopulationImpl) plans;
		this.networkLayer = network;
		this.network = new QueueNetwork(this.networkLayer);
		this.agentFactory = new AgentFactory(this);
		this.notTeleportedModes.add(TransportMode.car);	
		this.simEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom());
		installCarDepartureHandler();
	}

	/**
	 * Initialize the QueueSimulation without signal systems
	 * @param scenario
	 * @param events
	 */
	public QueueSimulation(final Scenario scenario, final EventsManager events) {
		this(scenario.getNetwork(), scenario.getPopulation(), events);
		this.scenario = scenario;
	}

	private void installCarDepartureHandler() {
		this.carDepartureHandler = new CarDepartureHandler(this);
		addDepartureHandler(this.carDepartureHandler);
	}

	public void addDepartureHandler(DepartureHandler departureHandler) {
		departureHandlers.add(departureHandler);
	}

	
	/**
	 * Adds the QueueSimulationListener instance  given as parameters as
	 * listener to this QueueSimulation instance.
	 * @param listeners
	 */
	public void addQueueSimulationListeners(final QueueSimulationListener listener){
		this.listenerManager.addQueueSimulationListener(listener);
	}

	/**
	 * Set the lanes used in the simulation
	 * @param laneDefs
	 */
	public void setLaneDefinitions(final LaneDefinitions laneDefs){
		this.laneDefintions = laneDefs;
	}

	/**
	 * Set the signal systems to be used in simulation
	 * @param signalSystems
	 * @param signalSystemConfigurations
	 */
	public void setSignalSystems(final SignalSystems signalSystems, final SignalSystemConfigurations signalSystemConfigurations){
		this.signalEngine  = new QueueSimSignalEngine(this);
		this.signalEngine.setSignalSystems(signalSystems, signalSystemConfigurations);
	}

	public final void run() {
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		//do iterations
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			beforeSimStep(time);
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			cont = doSimStep(time);
			afterSimStep(time);
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (cont) {
				SimulationTimer.incTime();
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
		BasicVehicleType defaultVehicleType = new BasicVehicleTypeImpl(new IdImpl("defaultVehicleType"));

		for (Person p : this.population.getPersons().values()) {
			PersonAgent agent = this.agentFactory.createPersonAgent(p);
			
			QueueVehicle veh = new QueueVehicleImpl(new BasicVehicleImpl(agent.getPerson().getId(), defaultVehicleType));
			//not needed in new agent class
			veh.setDriver(agent); // this line is currently only needed for OTFVis to show parked vehicles
			agent.setVehicle(veh);

			if (agent.initialize()) {
				QueueLink qlink = this.network.getQueueLink(agent.getCurrentLinkId());
				qlink.addParkedVehicle(veh);
			}
		}
		
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterCreateAgents();
		}
	}


	public void openNetStateWriter(final String snapshotFilename, final String networkFilename, final int snapshotPeriod) {
		/* TODO [MR] I don't really like it that we change the configuration on the fly here.
		 * In my eyes, the configuration should usually be a read-only object in general, but
		 * that's hard to be implemented...
		 */
		this.config.network().setInputFile(networkFilename);
		this.config.simulation().setSnapshotFormat("netvis");
		this.config.simulation().setSnapshotPeriod(snapshotPeriod);
		this.config.simulation().setSnapshotFile(snapshotFilename);
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
				this.snapshotWriters.add(new PlansFileSnapshotWriter(snapshotFilePrefix,snapshotFileSuffix));
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
				log.warn("Snapshot format netvis is no longer supported by this simulation");
			}
			if (snapshotFormat.contains("otfvis")) {
				String snapshotFile = this.controlerIO.getIterationFilename(itNumber, "otfvis.mvi");
				OTFFileWriter writer = null;
				writer = new OTFFileWriter(this.snapshotPeriod, new OTFQSimServerQuadBuilder(this.network), snapshotFile, new OTFFileWriterQSimConnectionManagerFactory());
//				if (this.config.scenario().isUseLanes() && ! this.config.scenario().isUseSignalSystems()) {
//					OTFConnectionManager connect = writer.getConnectionManager();
//					// data source to writer
//					connect.add(QueueLink.class, DgOtfLaneWriter.class);
//					// writer -> reader: from server to client
//					connect
//					.add(DgOtfLaneWriter.class, DgOtfLaneReader.class);
//					// reader to drawer (or provider to receiver)
//					connect.add(DgOtfLaneReader.class, DgLaneSignalDrawer.class);
//					// drawer -> layer
//					connect.add(DgLaneSignalDrawer.class, DgOtfLaneLayer.class);
//
//				}
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

		prepareLanes();

		if (this.signalEngine != null) {
			this.signalEngine.prepareSignalSystems();
		}

		// Initialize Snapshot file
		this.snapshotPeriod = (int) this.config.simulation().getSnapshotPeriod();

		double startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;

		SimulationTimer.setSimStartTime(24*3600);
		SimulationTimer.setTime(startTime);

		createAgents();

		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		double simStartTime = 0;
		DriverAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getDepartureTime()));
		}
		this.infoTime = Math.floor(simStartTime / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(simStartTime / this.snapshotPeriod) * this.snapshotPeriod;
		if (this.snapshotTime < simStartTime) {
			this.snapshotTime += this.snapshotPeriod;
		}
		SimulationTimer.setSimStartTime(simStartTime);
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());

		createSnapshotwriter();

		prepareNetworkChangeEventsQueue();
		
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterPrepareSim();
		}
	}

	protected void prepareLanes(){
		if (this.laneDefintions != null){
			log.info("Lanes enabled...");
			for (LanesToLinkAssignment laneToLink : this.laneDefintions.getLanesToLinkAssignments().values()){
				QueueLink link = this.network.getQueueLink(laneToLink.getLinkId());
				if (link == null) {
					String message = "No Link with Id: " + laneToLink.getLinkId() + ". Cannot create lanes, check lanesToLinkAssignment of signalsystems definition!";
					log.error(message);
					throw new IllegalStateException(message);
				}
				link.createLanes(laneToLink.getLanes());
			}
		}
	}

	/**
	 * Close any files, etc.
	 */
	protected void cleanupSim() {
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeCleanupSim();
		}
		
		this.simEngine.afterSim();
		double now = SimulationTimer.getTime();
		for (Tuple<Double, DriverAgent> entry : this.teleportationList) {
			DriverAgent agent = entry.getSecond();
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (DriverAgent agent : this.activityEndsList) {
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}

		if (this.netStateWriter != null) {
			try {
				this.netStateWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.netStateWriter = null;
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
			double diffsim  = time - SimulationTimer.getSimStartTime();
			int nofActiveLinks = this.simEngine.getNumberOfSimulatedLinks();
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + Simulation.getLiving() + " lost=" + Simulation.getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return (Simulation.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep(final double time) {
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterAfterSimStep(time);
		}
	}

	private void doSnapshot(final double time) {
		if (!this.snapshotWriters.isEmpty()) {
			Collection<PositionInfo> positions = this.network.getVehiclePositions();
			for (SnapshotWriter writer : this.snapshotWriters) {
				writer.beginSnapshot(time);
				for (PositionInfo position : positions) {
					writer.addAgent(position);
				}
				writer.endSnapshot();
			}
		}

		if (this.netStateWriter != null) {
			try {
				this.netStateWriter.dump((int)time);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static final EventsManager getEvents() {
		return events;
	}

	private static final void setEvents(final EventsManager events) {
		QueueSimulation.events = events;
	}

	protected void handleUnknownLegMode(double now, final DriverAgent agent, Id linkId) {
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeHandleUnknownLegMode(now, agent, this.networkLayer.getLinks().get(linkId));
		}
		double arrivalTime = SimulationTimer.getTime() + agent.getCurrentLeg().getTravelTime();
		this.teleportationList.add(new Tuple<Double, DriverAgent>(arrivalTime, agent));
	}

	protected void moveVehiclesWithUnknownLegMode(final double now) {
		while (this.teleportationList.peek() != null ) {
			Tuple<Double, DriverAgent> entry = this.teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				this.teleportationList.poll();
				DriverAgent driver = entry.getSecond();
				driver.teleportToLink(driver.getDestinationLinkId());
				driver.legEnds(now);
//				this.handleAgentArrival(now, driver);
//				getEvents().processEvent(new AgentArrivalEventImpl(now, driver.getPerson(),
//						destinationLink, driver.getCurrentLeg()));
//				driver.legEnds(now);
			} else break;
		}
	}
	
	/**
	 * Should be a PersonAgentI as argument, but is needed because the old events form is still used also for tests
	 * @param now
	 * @param agent
	 */
	public void handleAgentArrival(final double now, DriverAgent agent) {
		for (QueueSimulationFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeHandleAgentArrival(agent);
		}
		getEvents().processEvent(new AgentArrivalEventImpl(now, agent.getPerson().getId(),
				agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
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
	 * @see DriverAgent#getDepartureTime()
	 */
	public void scheduleActivityEnd(final DriverAgent agent) {
		this.activityEndsList.add(agent);
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			DriverAgent agent = this.activityEndsList.peek();
			if (agent.getDepartureTime() <= time) {
				this.activityEndsList.poll();
				agent.activityEnds(time);
			} else {
				return;
			}
		}
	}

	/**
	 * Informs the simulation that the specified agent wants to depart from its current activity.
	 * The simulation can then put the agent onto its vehicle on a link or teleport it to its destination.
	 * @param now 
	 *
	 * @param agent
	 * @param link the link where the agent departs
	 */
	public void agentDeparts(double now, final DriverAgent agent, final Id linkId) {
		Leg leg = agent.getCurrentLeg();
		TransportMode mode = leg.getMode();
		EventsManager e = events;
		events.processEvent(new AgentDepartureEventImpl(now, agent.getPerson().getId(), linkId, mode));
		if (this.notTeleportedModes.contains(mode)){
			this.handleKnownLegModeDeparture(now, agent, linkId, leg);
		} else {
			visAndHandleUnknownLegMode(now, agent, linkId);
		}
	}

	protected void visAndHandleUnknownLegMode(double now, DriverAgent agent, Id linkId) {
		this.handleUnknownLegMode(now, agent, linkId);
	}

	private void handleKnownLegModeDeparture(double now, DriverAgent agent, Id linkId, Leg leg) {
		for (DepartureHandler departureHandler : departureHandlers) {
			departureHandler.handleDeparture(now, agent, linkId, leg);
		}
	}

	public boolean addSnapshotWriter(final SnapshotWriter writer) {
		return this.snapshotWriters.add(writer);
	}

	public boolean removeSnapshotWriter(final SnapshotWriter writer) {
		return this.snapshotWriters.remove(writer);
	}

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
	public void setTeleportVehicles(final boolean teleportVehicles) {
		this.carDepartureHandler.setTeleportVehicles(teleportVehicles);
	}

	private static class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, DriverAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(final Tuple<Double, DriverAgent> o1, final Tuple<Double, DriverAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getPerson().getId().compareTo(o1.getSecond().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}

	public QueueNetwork getQueueNetwork() {
		return this.network;
	}

	public Scenario getScenario() {
		return this.scenario;
	}


	public boolean isUseActivityDurations() {
		return this.useActivityDurations;
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
		log.info("QueueSimulation is working with activity durations: " + this.isUseActivityDurations());
	}

	public SignalEngine getQueueSimSignalEngine() {
		return this.signalEngine;
	}

	public Set<TransportMode> getNotTeleportedModes() {
		return notTeleportedModes;
	}
	
	public void setQueueNetwork(QueueNetwork net) {
		this.network = net;
		this.simEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom());
	}

	public PopulationImpl getPopulation() {
		return population;
	}

	public QueueNetwork getNetwork() {
		return network;
	}

	public void addFeature(QueueSimulationFeature queueSimulationFeature) {
		queueSimulationFeatures.add(queueSimulationFeature);
	}

	
	public Integer getIterationNumber() {
		return iterationNumber;
	}

	
	public void setIterationNumber(Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	public void setControlerIO(ControlerIO controlerIO) {
		this.controlerIO = controlerIO;
	}

}
