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

package soc.ai.matsim.dbsim;

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
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
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
public class DBSimulation implements IOSimulation, ObservableSimulation {

	private int snapshotPeriod = 0;

	/* time since lasat snapshot */
	private double snapshotTime = 0.0;

	protected static final int INFO_PERIOD = 3600;

	/* time since last "info" message */
	private double infoTime = 0;

	private final Config config;
	protected final Population population;
	protected DBSimNetwork network;
	protected Network networkLayer;

	private static EventsManager events = null;

	private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();

	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;

	protected DBSimEngine simEngine = null;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	protected final PriorityQueue<Tuple<Double, DriverAgent>> teleportationList = new PriorityQueue<Tuple<Double, DriverAgent>>(30, new TeleportationArrivalTimeComparator());

	private final Date starttime = new Date();

	private double stopTime = 100*3600;

	final private static Logger log = Logger.getLogger(DBSimulation.class);

	private AgentFactory agentFactory;

	private SimulationListenerManager<DBSimulation> listenerManager;

	protected final PriorityBlockingQueue<DriverAgent> activityEndsList = new PriorityBlockingQueue<DriverAgent>(500, new DriverAgentDepartureTimeComparator());

	protected Scenario scenario = null;

	/** @see #setTeleportVehicles(boolean) */
	private boolean teleportVehicles = true;
	private int cntTeleportVehicle = 0;

	private boolean useActivityDurations = true;

	private final Set<String> notTeleportedModes = new HashSet<String>();

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;

	/**
	 * Initialize the QueueSimulation without signal systems
	 * @param scenario
	 * @param events
	 */
	public DBSimulation(final Scenario scenario, final EventsManager events) {
		this.scenario = scenario;
		this.listenerManager = new SimulationListenerManager<DBSimulation>(this);
		AbstractSimulation.reset();
		this.config = scenario.getConfig();
		SimulationTimer.reset(this.config.simulation().getTimeStepSize());
		setEvents(events);
		this.population = scenario.getPopulation();

		this.networkLayer = scenario.getNetwork();

		this.network = new DBSimNetwork(this.networkLayer); //roadShapeConfig

		this.agentFactory = new AgentFactory(this);

		this.notTeleportedModes.add(TransportMode.car);

		this.simEngine = new DBSimEngine(this.network, MatsimRandom.getRandom());
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
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));

		//TODO read xml
		for (Person p : this.population.getPersons().values()) {
			PersonAgent agent = this.agentFactory.createPersonAgent(p);

			// この機を逃すとPersonAgentへのアクセスが面倒なので，ここでモデルをセットしておく．
			//TODO agent.setDriver(xml.get(p.id));
			DBSimVehicle veh = new DBSimVehicleImpl(new VehicleImpl(agent.getPerson().getId(), defaultVehicleType));
			//not needed in new agent class
			veh.setDriver(agent); // this line is currently only needed for OTFVis to show parked vehicles
			agent.setVehicle(veh);
			if (agent.initialize()) {
				DBSimLink qlink = this.network.getQueueLink(agent.getCurrentLinkId());
				qlink.addParkedVehicle(veh);
			}
		}

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
			/*
			if (snapshotFormat.contains("otfvis")) {
				String snapshotFile = this.controlerIO.getIterationFilename(itNumber, "otfvis.mvi");
				OTFFileWriter writer = new OTFFileWriter(this.snapshotPeriod, new OTFQueueSimServerQuadBuilder(this.network), snapshotFile, new OTFFileWriterQueueSimConnectionManagerFactory());
				this.snapshotWriters.add(writer);
			}
			 */
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
		//TODO prepareNetwork();

		if (events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
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
	}

	private void prepareNetwork() {
		//TODO read additional link parameter from XML (RoadShape.xml)
		//TODO RoadShapeConfig roadShapeConfig = (new RoadShapeConfigReader("RoadShape.xml")).read();
		//TODO for each this.network.dbsimLinks do setShape(roadShapeConfig);
	}

	/**
	 * Close any files, etc.
	 */
	protected void cleanupSim() {
		this.simEngine.afterSim();
		double now = SimulationTimer.getTime();
		for (Tuple<Double, DriverAgent> entry : this.teleportationList) {
			DriverAgent agent = entry.getSecond();
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (DriverAgent agent : this.activityEndsList) {
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), null));
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}

		this.simEngine = null;
		DBSimulation.events = null; // delete events object to free events handlers, if they are nowhere else referenced
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
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + AbstractSimulation.getLiving() + " lost=" + AbstractSimulation.getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return (AbstractSimulation.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep(final double time) {
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
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

	}

	public static final EventsManager getEvents() {
		return events;
	}

	private static final void setEvents(final EventsManager events) {
		DBSimulation.events = events;
	}

	protected void handleUnknownLegMode(double now, final DriverAgent agent) {
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
	protected void handleAgentArrival(final double now, DriverAgent agent){
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
	protected void scheduleActivityEnd(final DriverAgent agent) {
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
	protected void agentDeparts(double now, final DriverAgent agent, final Id linkId) {
		Leg leg = agent.getCurrentLeg();
		String mode = leg.getMode();
		events.processEvent(new AgentDepartureEventImpl(now, agent.getPerson().getId(), linkId, leg.getMode()));
		if (this.notTeleportedModes.contains(mode)){
			this.handleKnownLegModeDeparture(now, agent, linkId, mode);
		}
		else {
			this.handleUnknownLegMode(now, agent);
		}
	}

	protected void handleKnownLegModeDeparture(double now, DriverAgent agent, Id linkId, String mode) {
		Leg leg = agent.getCurrentLeg();
		if (mode.equals(TransportMode.car)) {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id vehicleId = route.getVehicleId();
			if (vehicleId == null) {
				vehicleId = agent.getPerson().getId(); // backwards-compatibility
			}
			DBSimLink qlink = this.network.getQueueLink(linkId);
			DBSimVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
			if (vehicle == null) {
				// try to fix it somehow
				if (this.teleportVehicles && (agent instanceof PersonAgent)) {
					vehicle = ((PersonAgent) agent).getVehicle();
					if (vehicle.getCurrentLink() != null) {
						if (this.cntTeleportVehicle < 9) {
							this.cntTeleportVehicle++;
							log.info("teleport vehicle " + vehicle.getId() + " from link " + vehicle.getCurrentLink().getId() + " to link " + linkId);
							if (this.cntTeleportVehicle == 9) {
								log.info("No more occurrences of teleported vehicles will be reported.");
							}
						}
						DBSimLink qlinkOld = this.network.getQueueLink(vehicle.getCurrentLink().getId());
						qlinkOld.removeParkedVehicle(vehicle.getId());
					}
				}
			}
			if (vehicle == null) {
				throw new RuntimeException("vehicle not available for agent " + agent.getPerson().getId() + " on link " + linkId);
			}
			vehicle.setDriver(agent);
			if ((route.getEndLinkId().equals(linkId)) && (agent.chooseNextLinkId() == null)) {
				agent.legEnds(now);
				qlink.processVehicleArrival(now, vehicle);
			} else {
				qlink.addDepartingVehicle(vehicle);
			}
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
		this.teleportVehicles = teleportVehicles;
	}

	private static class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, DriverAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final Tuple<Double, DriverAgent> o1, final Tuple<Double, DriverAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getPerson().getId().compareTo(o1.getSecond().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}

	public DBSimNetwork getQueueNetwork() {
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

	public Set<String> getNotTeleportedModes() {
		return notTeleportedModes;
	}


	public Integer getIterationNumber() {
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


}
