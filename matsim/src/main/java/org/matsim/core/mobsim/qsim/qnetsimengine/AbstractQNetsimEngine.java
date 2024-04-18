/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author droeder@Senozon after
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
abstract class AbstractQNetsimEngine<A extends AbstractQNetsimEngineRunner> implements QNetsimEngineI {

	private NetsimInternalInterface ii = new NetsimInternalInterface(){
		@Override public QNetwork getNetsimNetwork() {
			return qNetwork;
		}
		@Override public void arrangeNextAgentState(MobsimAgent driver) {
			AbstractQNetsimEngine.this.arrangeNextAgentState(driver);
		}
		@Override public void letVehicleArrive(QVehicle veh) {
			AbstractQNetsimEngine.this.letVehicleArrive( veh ) ;
		}
	} ;

	private static final Logger log = LogManager.getLogger(AbstractQNetsimEngine.class);
	private static final int INFO_PERIOD = 3600;

	// for detailed run time analysis - used in combination with QSim.analyzeRunTimes
	public static int numObservedTimeSteps = 24*3600;
	public static boolean printRunTimesPerTimeStep = false;

	private final Map<Id<Vehicle>, QVehicle> vehicles = new HashMap<>();
	private final QSim qsim;
	private final VehicularDepartureHandler dpHandler;
//	private final Set<QLinkI> linksToActivateInitially = new HashSet<>();
	protected final int numOfThreads;
	protected final QNetwork qNetwork;

	private double infoTime = 0;
	private List<A> engines;
	private InternalInterface internalInterface = null;

	AbstractQNetsimEngine(final QSim sim, QNetworkFactory netsimNetworkFactory) {
		this.qsim = sim;

		final Config config = sim.getScenario().getConfig();
		final QSimConfigGroup qSimConfigGroup = config.qsim();

		// configuring the car departure hander (including the vehicle behavior)
		VehicleBehavior vehicleBehavior = qSimConfigGroup.getVehicleBehavior();
		switch(vehicleBehavior) {
		case exception:
		case teleport:
		case wait:
			break;
		default:
			throw new RuntimeException("Unknown vehicle behavior option.");
		}
		dpHandler = new VehicularDepartureHandler(this, vehicleBehavior, qSimConfigGroup);

		if(qSimConfigGroup.getLinkDynamics().equals(LinkDynamics.SeepageQ)) {
			log.info("Seepage is allowed. Seep mode(s) is(are) " + qSimConfigGroup.getSeepModes() + ".");
			if(qSimConfigGroup.isSeepModeStorageFree()) {
				log.warn("Seep mode(s) " + qSimConfigGroup.getSeepModes() + " does not take storage space thus only considered for flow capacities.");
			}
		}

		if (netsimNetworkFactory != null){
			qNetwork = new QNetwork( sim.getScenario().getNetwork(), netsimNetworkFactory ) ;
		} else {
			throw new RuntimeException( "this execution path is no longer allowed; network factory needs to come from elsewhere (in general via injection).  kai, jun'23" );
//			Scenario scenario = sim.getScenario();
//			EventsManager events = sim.getEventsManager() ;
//			final DefaultQNetworkFactory netsimNetworkFactory2 = new DefaultQNetworkFactory( events, scenario );
//			MobsimTimer mobsimTimer = sim.getSimTimer() ;
//			AgentCounter agentCounter = sim.getAgentCounter() ;
//			netsimNetworkFactory2.initializeFactory(agentCounter, mobsimTimer, ii );
//			qNetwork = new QNetwork(sim.getScenario().getNetwork(), netsimNetworkFactory2 );
		}
		qNetwork.initialize(this, sim.getAgentCounter(), sim.getSimTimer() );

		this.numOfThreads = sim.getScenario().getConfig().qsim().getNumberOfThreads();
	}

	static AbstractAgentSnapshotInfoBuilder createAgentSnapshotInfoBuilder(Scenario scenario, SnapshotLinkWidthCalculator linkWidthCalculator) {
		final SnapshotStyle snapshotStyle = scenario.getConfig().qsim().getSnapshotStyle();
		switch(snapshotStyle) {
		case queue:
			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		case withHoles:
		case withHolesAndShowHoles:
			// the difference is not in the spacing, thus cannot be differentiated by using different classes.  kai, sep'14
			// ??? kai, nov'15
			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		case kinematicWaves:
			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not explicitly supported. Using \""+SnapshotStyle.withHoles+ "\" instead.");
			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		case equiDist:
			return new EquiDistAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		default:
			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported. Using equiDist");
			return new EquiDistAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		}
	}

	@Override
	public final void onPrepareSim() {
		this.infoTime =
				Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD;
		/*
		 * infoTime may be < simStartTime, this ensures to print out the
		 * info at the very first timestep already
		 */

		this.engines = initQSimEngineRunners();
		assignNetElementActivators();
		initMultiThreading();
	}

	/**
	 * do everything necessary to start the threads for {@link AbstractQNetsimEngineRunner}
	 */
	protected abstract void initMultiThreading();


	@Override
	public final void afterSim() {

		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
			engine.afterSim();
		}

		finishMultiThreading();

		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QLinkI link : qNetwork.getNetsimLinks().values()) {
			link.clearVehicles();
		}
	}

	/**
	 * do everything to finish multithreading {@link #afterSim()}, e.g. shut down a threadpool
	 */
	protected abstract void finishMultiThreading();

	/**
	 * called during {@link #doSimStep(double)}. Should perform the
	 * simstep-logic in {@link AbstractQNetsimEngineRunner} provided by {@link #getQnetsimEngineRunner()}
	 *
	 * @param time
	 */
	protected abstract void run(double time);

	/**
	 * create all necessary {@link AbstractQNetsimEngineRunner}. Will be called during {@link #onPrepareSim()}.
	 *
	 * @return the list of {@link AbstractQNetsimEngineRunner}
	 */
	protected abstract List<A> initQSimEngineRunners() ;

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	public final void doSimStep(final double time) {
		run(time);

		this.printSimLog(time);
	}


	@Override
	public final void setInternalInterface( InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private static int wrnCnt = 0;

	public final void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		if (this.vehicles.put(veh.getId(), (QVehicle) veh) != null) {
			if (wrnCnt < 1) {
				wrnCnt++ ;
				log.warn("existing vehicle in mobsim was just overwritten by other vehicle with same ID.  Not clear what this means.  Continuing anyways ...") ;
				log.warn(Gbl.ONLYONCE);
			}
		}
		QLinkI qlink = qNetwork.getNetsimLinks().get(startLinkId );
		if (qlink == null) {
			throw new RuntimeException("requested link with id=" + startLinkId + " does not exist in network. Possible vehicles "
					+ "or activities or facilities are registered to a different network.") ;
		}
		qlink.addParkedVehicle(veh);
	}

	public final int getNumberOfSimulatedLinks() {

		int numLinks = 0;

		for (AbstractQNetsimEngineRunner engine : this.engines) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}

		return numLinks;
	}

	public final int getNumberOfSimulatedNodes() {

		int numNodes = 0;

		for (AbstractQNetsimEngineRunner engine : this.engines) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}

		return numNodes;
	}

	public final NetsimNetwork getNetsimNetwork() {
		return this.qNetwork;
	}

	public final VehicularDepartureHandler getDepartureHandler() {
		return dpHandler;
	}

	public final Map<Id<Vehicle>, QVehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}

	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		Id<Link> linkId = planAgent.getCurrentLinkId();
		if (linkId != null) { // may be bushwacking
			QLinkI qLink = this.qNetwork.getNetsimLink(linkId );
			if ( qLink==null ) {
				throw new RuntimeException("netsim link lookup failed; agentId=" + planAgent.getId() + "; linkId=" + linkId ) ;
			}
			qLink.registerAdditionalAgentOnLink(planAgent);
		}
	}

	public final MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
		if  (linkId == null) { // seems that this can happen in tests; not sure if it can happen in regular code. kai, jun'15
			return null;
		}
		QLinkI qLink = this.qNetwork.getNetsimLink(linkId );
		return qLink.unregisterAdditionalAgentOnLink(agentId);
	}

	public final void printEngineRunTimes() {
		if (!QSim.analyzeRunTimes) return;

		if (printRunTimesPerTimeStep) log.info("detailed QNetsimEngineRunner run times per time step:");
		{
			StringBuffer sb = new StringBuffer();
			sb.append("\t");
			sb.append("time");
			for (int i = 0; i < this.engines.size(); i++) {
				sb.append("\t");
				sb.append("thread_");
				sb.append(Integer.toString(i));
			}
			sb.append("\t");
			sb.append("min");
			sb.append("\t");
			sb.append("max");
			if (printRunTimesPerTimeStep) log.info(sb.toString());
		}
		long sum = 0;
		long sumMin = 0;
		long sumMax = 0;
		for (int i = 0; i < numObservedTimeSteps; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append("\t" + i);
			long min = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			for (AbstractQNetsimEngineRunner runner : this.engines) {
				long runTime = runner.runTimes[i];
				sum += runTime;
				if (runTime < min) min = runTime;
				if (runTime > max) max = runTime;
				sb.append("\t");
				sb.append(Long.toString(runTime));
			}
			sb.append("\t");
			sb.append(Long.toString(min));
			sb.append("\t");
			sb.append(Long.toString(max));
			if (printRunTimesPerTimeStep) log.info(sb.toString());
			sumMin += min;
			sumMax += max;
		}
		log.info("sum min run times: " + sumMin);
		log.info("sum max run times: " + sumMax);
		log.info("sum all run times / num threads: " + sum / this.numOfThreads);
	}

	@Override
	public final NetsimInternalInterface getNetsimInternalInterface() {
		return ii;
	}

	private final void printSimLog(double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			int nofActiveLinks = this.getNumberOfSimulatedLinks();
			int nofActiveNodes = this.getNumberOfSimulatedNodes();
			log.info("SIMULATION (QNetsimEngine) AT " + Time.writeTime(time)
					+ " : #links=" + nofActiveLinks
					+ " #nodes=" + nofActiveNodes);
		}
	}

	private void letVehicleArrive(QVehicle veh) {
		double now = this.qsim.getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		this.qsim.getEventsManager().processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), veh.getId()));
		// reset vehicles driver
		veh.setDriver(null);
		driver.endLegAndComputeNextState(now);
		this.internalInterface.arrangeNextAgentState(driver);
	}

	 /*
	 * Within the MoveThreads Links are only activated when a Vehicle is moved
	 * over a Node which is processed by that Thread. So we can assign each QLink
	 * to the Thread that handles its InNode.
	 */
	private void assignNetElementActivators() {

		// only for statistics
		int nodes[] = new int[this.engines.size()];
		int links[] = new int[this.engines.size()];

		int roundRobin = 0;
		for (QNodeI node : qNetwork.getNetsimNodes().values()) {
			int i = roundRobin % this.engines.size();
			if( node instanceof AbstractQNode){
				((AbstractQNode) node).setNetElementActivationRegistry(this.engines.get(i));
			}
			nodes[i]++;

			// set activator for out links
			for (Link outLink : node.getNode().getOutLinks().values()) {
				AbstractQLink qLink = (AbstractQLink) qNetwork.getNetsimLink(outLink.getId() );
				// (must be of this type to work.  kai, feb'12)

				// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
				qLink.setNetElementActivationRegistry(this.engines.get(i));

				/*
				 * If the QLink contains agents that end their activity in the first time
				 * step, the link should be activated.
				 */
				// this set is always empty...
//				if (linksToActivateInitially.remove(qLink)
//						|| qsim.getScenario().getConfig().qsim().getSimStarttimeInterpretation()==StarttimeInterpretation.onlyUseStarttime) {
//					this.engines.get(i).registerLinkAsActive(qLink);
//				}

				links[i]++;

			}

			roundRobin++;
		}

		// print some statistics
		for (int i = 0; i < this.engines.size(); i++) {
			log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to QSimEngineRunner #" + i);
		}

//		this.linksToActivateInitially.clear();
	}

	private final void arrangeNextAgentState(MobsimAgent pp) {
		internalInterface.arrangeNextAgentState(pp);
	}

	/**
	 * @return the {@link AbstractQNetsimEngineRunner} created by {@link #initQSimEngineRunners()}
	 */
	protected List<A> getQnetsimEngineRunner(){
		return this.engines;
	}
}
