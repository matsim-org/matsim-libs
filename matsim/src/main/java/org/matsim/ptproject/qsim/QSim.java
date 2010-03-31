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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vis.netvis.streaming.SimStateWriterI;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

/**
 * Implementation of a queue-based transport simulation.
 * Lanes and SignalSystems are not initialized unless the setter are invoked.
 *
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 */
public class QSim implements org.matsim.core.mobsim.framework.IOSimulation, ObservableSimulation {


	/* time since last snapshot */
	private double snapshotTime = 0.0;
	private int snapshotPeriod = 0;

	/* time since last "info" */
	private double infoTime = 0;
	protected static final int INFO_PERIOD = 3600;

	protected PopulationImpl population;
	protected QNetwork network;
	private static EventsManager events = null;
	protected  SimStateWriterI netStateWriter = null;
	private PriorityQueue<NetworkChangeEvent> networkChangeEventsQueue = null;
	protected QSimEngine simEngine = null;
	private CarDepartureHandler carDepartureHandler;

	/**
	 * Includes all agents that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */
	protected final PriorityQueue<Tuple<Double, DriverAgent>> teleportationList = new PriorityQueue<Tuple<Double, DriverAgent>>(30, new TeleportationArrivalTimeComparator());
	private final Date starttime = new Date();
	private double stopTime = 100*3600;
	final private static Logger log = Logger.getLogger(QSim.class);
	private AgentFactory agentFactory;
	private SimulationListenerManager<QSim> listenerManager;
	protected final PriorityBlockingQueue<DriverAgent> activityEndsList = new PriorityBlockingQueue<DriverAgent>(500, new DriverAgentDepartureTimeComparator());
	protected Scenario scenario = null;
	private LaneDefinitions laneDefintions;
	private boolean useActivityDurations = true;
	private QSimSignalEngine signalEngine = null;
	private final Set<TransportMode> notTeleportedModes = new HashSet<TransportMode>();

	private final List<QSimFeature> queueSimulationFeatures = new ArrayList<QSimFeature>();
	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	private QSimSnapshotWriterManager snapshotManager;

	/**
	 * Initialize the QueueSimulation
	 * @param scenario
	 * @param events
	 */
	public QSim(final Scenario scenario, final EventsManager events) {
		this.scenario = scenario;
		init(this.scenario, events);
	}
	/**
	 * extended constructor method that can also be
	 * after assignments of another constructor
	 */
	private void init(Scenario sc, EventsManager eventsManager){
    log.info("Using QSim...");
    // In my opinion, this should be marked as deprecated in favor of the constructor with Scenario. marcel/16july2009
    this.listenerManager = new SimulationListenerManager<QSim>(this);
    Simulation.reset(sc.getConfig().getQSimConfigGroup().getStuckTime());
    QSimTimer.reset(sc.getConfig().getQSimConfigGroup().getTimeStepSize());
    setEvents(eventsManager);
    this.population = (PopulationImpl) sc.getPopulation();
    Config config = sc.getConfig();
    this.simEngine = new QSimEngineImpl(this, MatsimRandom.getRandom());

    if (config.scenario().isUseLanes()) {
      if (((ScenarioImpl)sc).getLaneDefinitions() == null) {
        throw new IllegalStateException("Lane definition have to be set if feature is enabled!");
      }
      this.setLaneDefinitions(((ScenarioImpl)sc).getLaneDefinitions());
      this.network = new QNetwork(this, new QLanesNetworkFactory(new DefaultQNetworkFactory()));
    }
    else {
        this.network = new QNetwork(this);
    }
    this.network.initialize(this.simEngine);
    if (config.scenario().isUseSignalSystems()) {
      if ((((ScenarioImpl)sc).getSignalSystems() == null)
          || (((ScenarioImpl)sc).getSignalSystemConfigurations() == null)) {
        throw new IllegalStateException(
            "Signal systems and signal system configurations have to be set if feature is enabled!");
      }
      this.setSignalSystems(((ScenarioImpl)sc).getSignalSystems(), ((ScenarioImpl)sc).getSignalSystemConfigurations());
    }
    


    this.agentFactory = new AgentFactory(this);
    this.notTeleportedModes.add(TransportMode.car);
    installCarDepartureHandler();
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
	public void addQueueSimulationListeners(final SimulationListener listener){
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
		this.signalEngine  = new QSimSignalEngine(this);
		this.signalEngine.setSignalSystems(signalSystems, signalSystemConfigurations);
	}

	public final void run() {
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		//do iterations
		boolean cont = true;
		double time = QSimTimer.getTime();
		while (cont) {
			time = QSimTimer.getTime();
			beforeSimStep(time);
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			cont = doSimStep(time);
			afterSimStep(time);
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (cont) {
				QSimTimer.incTime();
			}
		}
		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();
		cleanupSim(time);
		//delete reference to clear memory
		this.listenerManager = null;
	}

	protected void createAgents() {
		if (this.population == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		Collection<PersonAgentI> agents = new ArrayList<PersonAgentI>();
		BasicVehicleType defaultVehicleType = new BasicVehicleTypeImpl(new IdImpl("defaultVehicleType"));
		for (Person p : this.population.getPersons().values()) {
			PersonAgent agent = this.agentFactory.createPersonAgent(p);
			QVehicle veh = new QVehicleImpl(new BasicVehicleImpl(agent.getPerson().getId(), defaultVehicleType));
			//not needed in new agent class
			veh.setDriver(agent); // this line is currently only needed for OTFVis to show parked vehicles
			agent.setVehicle(veh);
			agents.add(agent);
			if (agent.initialize()) {
				QLink qlink = this.network.getQLink(agent.getCurrentLinkId());
				qlink.addParkedVehicle(veh);
			}
		}
		
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			Collection<PersonAgentI> moreAgents = queueSimulationFeature.createAgents();
			agents.addAll(moreAgents);
		}
		
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			for (PersonAgentI agent : agents) {
				queueSimulationFeature.agentCreated(agent);
			}
		}
		
	}

	/**
	 * @deprecated Netvis is no longer supported by this QueueSimulation
	 */
	@Deprecated
  public void openNetStateWriter(final String snapshotFilename, final String networkFilename, final int snapshotPeriod) {
		log.warn("NetVis is no longer supported by this simulation.");
	}

	private void prepareNetworkChangeEventsQueue() {
		Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl)(this.scenario.getNetwork())).getNetworkChangeEvents();
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
		this.simEngine.onPrepareSim();
		prepareLanes();

		if (this.signalEngine != null) {
			this.signalEngine.onPrepareSim();
		}


		double startTime = this.scenario.getConfig().getQSimConfigGroup().getStartTime();
		this.stopTime = this.scenario.getConfig().getQSimConfigGroup().getEndTime();
		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;
		QSimTimer.setSimStartTime(24*3600);
		QSimTimer.setTime(startTime);

		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		double simStartTime = 0;
		createAgents();
		DriverAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getDepartureTime()));
		}
		this.snapshotPeriod = (int) this.scenario.getConfig().getQSimConfigGroup().getSnapshotPeriod();
		this.infoTime = Math.floor(simStartTime / INFO_PERIOD) * INFO_PERIOD; // infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.snapshotTime = Math.floor(simStartTime / this.snapshotPeriod) * this.snapshotPeriod;
		QSimTimer.setSimStartTime(simStartTime);
		QSimTimer.setTime(QSimTimer.getSimStartTime());

		// Initialize Snapshot file
		this.snapshotPeriod = (int) this.scenario.getConfig().getQSimConfigGroup().getSnapshotPeriod();
		this.snapshotManager = new QSimSnapshotWriterManager();
		this.snapshotManager.createSnapshotwriter(this.network, this.scenario, this.snapshotPeriod, this.iterationNumber, this.controlerIO);
    if (this.snapshotTime < simStartTime) {
      this.snapshotTime += this.snapshotPeriod;
    }

		prepareNetworkChangeEventsQueue();

		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterPrepareSim();
		}
	}

	protected void prepareLanes(){
		if (this.laneDefintions != null){
			log.info("Lanes enabled...");
			for (LanesToLinkAssignment laneToLink : this.laneDefintions.getLanesToLinkAssignments().values()){
				QLink link = this.network.getQLink(laneToLink.getLinkId());
				if (link == null) {
					String message = "No Link with Id: " + laneToLink.getLinkId() + ". Cannot create lanes, check lanesToLinkAssignment of signalsystems definition!";
					log.error(message);
					throw new IllegalStateException(message);
				}
				((QLinkLanesImpl)link).createLanes(laneToLink.getLanes());
			}
		}
	}

	/**
	 * Close any files, etc.
	 */
	protected void cleanupSim(double seconds) {
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeCleanupSim();
		}

		this.simEngine.afterSim();
		double now = QSimTimer.getTime();
		for (Tuple<Double, DriverAgent> entry : this.teleportationList) {
			DriverAgent agent = entry.getSecond();
			events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
		}
		this.teleportationList.clear();

		for (DriverAgent agent : this.activityEndsList) {
			if (agent.getDestinationLinkId() != null) {
				events.processEvent(new AgentStuckEventImpl(now, agent.getPerson().getId(), agent.getDestinationLinkId(), null));
			}
		}
		this.activityEndsList.clear();

		for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
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
		QSim.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	protected void beforeSimStep(final double time) {
		if ((this.networkChangeEventsQueue != null) && (this.networkChangeEventsQueue.size() > 0)) {
			handleNetworkChangeEvents(time);
		}
    if (this.signalEngine != null) {
      this.signalEngine.beforeSimStep(time);
    }

	}

	
	private void printSimLog(final double time) {
    if (time >= this.infoTime) {
      this.infoTime += INFO_PERIOD;
      Date endtime = new Date();
      long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
      double diffsim  = time - QSimTimer.getSimStartTime();
      int nofActiveLinks = this.simEngine.getNumberOfSimulatedLinks();
      log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time) + ": #Veh=" + Simulation.getLiving() + " lost=" + Simulation.getLost() + " #links=" + nofActiveLinks
          + " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
      Gbl.printMemoryUsage();
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

		this.printSimLog(time);
		return (Simulation.isLiving() && (this.stopTime > time));
	}

	protected void afterSimStep(final double time) {
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time);
		}
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterAfterSimStep(time);
		}
	}

	private void doSnapshot(final double time) {
		if (!this.snapshotManager.getSnapshotWriters().isEmpty()) {
		  Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
	    for (QLink link : this.getQNetwork().getLinks().values()) {
	      link.getVisData().getVehiclePositions(time, positions);
	    }
			for (SnapshotWriter writer : this.snapshotManager.getSnapshotWriters()) {
				writer.beginSnapshot(time);
				for (AgentSnapshotInfo position : positions) {
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
	/**
	 * @deprecated try to use non static method getEventsManager()
	 */
	@Deprecated
  public static final EventsManager getEvents() {
		return events;
	}

	public final EventsManager getEventsManager(){
	  return events;
	}

	private static final void setEvents(final EventsManager events) {
		QSim.events = events;
	}

	protected void handleUnknownLegMode(double now, final DriverAgent agent, Id linkId) {
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeHandleUnknownLegMode(now, agent, this.scenario.getNetwork().getLinks().get(linkId));
		}
		double arrivalTime = now + agent.getCurrentLeg().getTravelTime();
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
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.beforeHandleAgentArrival(agent);
		}
		getEventsManager().processEvent(new AgentArrivalEventImpl(now, agent.getPerson().getId(),
				agent.getDestinationLinkId(), agent.getCurrentLeg().getMode()));
	}

	private void handleNetworkChangeEvents(final double time) {
		while ((this.networkChangeEventsQueue.size() > 0) && (this.networkChangeEventsQueue.peek().getStartTime() <= time)){
			NetworkChangeEvent event = this.networkChangeEventsQueue.poll();
			for (Link link : event.getLinks()) {
				this.network.getQLink(link.getId()).recalcTimeVariantAttributes(time);
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
	public void scheduleActivityEnd(final DriverAgent agent, int planElementIndex) {
		this.activityEndsList.add(agent);
		addToAgentsInActivities(agent);
		for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
			queueSimulationFeature.afterActivityBegins(agent, planElementIndex);
		}
	}

	private void addToAgentsInActivities(final DriverAgent agent) {
		if (agent instanceof PersonAgent) {
			PersonAgent pa = (PersonAgent) agent;
			PlanElement pe = pa.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				QLink qLink = network.getQLink(linkId);
				qLink.addAgentInActivity(agent);
			}
		}
	}

	private void removeFromAgentsInActivities(DriverAgent agent) {
		if (agent instanceof PersonAgent) {
			PersonAgent pa = (PersonAgent) agent;
			PlanElement pe = pa.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				QLink qLink = network.getQLink(linkId);
				qLink.removeAgentInActivity(agent);
			}
		}
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			DriverAgent agent = this.activityEndsList.peek();
			if (agent.getDepartureTime() <= time) {
				this.activityEndsList.poll();
				removeFromAgentsInActivities(agent);
				agent.activityEnds(time);
				for (QSimFeature queueSimulationFeature : queueSimulationFeatures) {
					queueSimulationFeature.afterActivityEnds(agent, time);
				}
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

	public QNetwork getQNetwork() {
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

	public SignalEngine getQSimSignalEngine() {
		return this.signalEngine;
	}

	public Set<TransportMode> getNotTeleportedModes() {
		return notTeleportedModes;
	}

	public void addFeature(QSimFeature queueSimulationFeature) {
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

	public double getTimeOfDay() {
		return QSimTimer.getTime() ;
	}

}
