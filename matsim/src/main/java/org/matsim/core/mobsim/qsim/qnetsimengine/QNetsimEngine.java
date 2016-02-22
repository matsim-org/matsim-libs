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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
public class QNetsimEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(QNetsimEngine.class);

	private static final int INFO_PERIOD = 3600;

	private QNetwork network;

	private final Map<Id<Vehicle>, QVehicle> vehicles = new HashMap<>();

	private final QSim qsim;

	private final AbstractAgentSnapshotInfoBuilder positionInfoBuilder;

	private final double stucktimeCache;
	private final VehicularDepartureHandler dpHandler;

	private double infoTime = 0;

	private final int numOfThreads;

	private LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator();

	private List<QNetsimEngineRunner> engines;

	private Phaser startBarrier;
	private Phaser endBarrier;

	private final Set<QLinkInternalI> linksToActivateInitially = new HashSet<>();

	/*package*/ InternalInterface internalInterface = null;

	private int numOfRunners;

	private ExecutorService pool;

	private final boolean usingThreadpool;
	
	// for detailed run time analysis - used in combination with QSim.analyzeRunTimes
	public static int numObservedTimeSteps = 24*3600;
	public static boolean printRunTimesPerTimeStep = false;
	
	@Override
	public void setInternalInterface( InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	//	/**
	//	 * A design thought to provide a configurable default factory that also allows to make the direct constructors non-public.
	//	 * 
	//	 * @author nagel
	//	 */
	//	public static final class Builder implements QNetsimEngineFactory {
	//		private NetsimNetworkFactory<QNode, ? extends QLinkInternalI> netsimNetworkFactory = null ;
	//		public void setNetworkFactory( NetsimNetworkFactory<QNode, ? extends QLinkInternalI> factory ) {
	//			this.netsimNetworkFactory = factory ;
	//		}
	//		@Override
	//		public QNetsimEngine createQSimEngine(Netsim sim) {
	//			return new QNetsimEngine( (QSim) sim, netsimNetworkFactory ) ;
	//		}
	//	}

	@Inject
	public QNetsimEngine(final QSim sim) {
		this(sim, null);
	}

	public QNetsimEngine(final QSim sim, NetsimNetworkFactory netsimNetworkFactory) {
		this.qsim = sim;

		final Config config = sim.getScenario().getConfig();
		final QSimConfigGroup qsimConfigGroup = config.qsim();
		this.stucktimeCache = qsimConfigGroup.getStuckTime();
		this.usingThreadpool = qsimConfigGroup.isUsingThreadpool();


		// configuring the car departure hander (including the vehicle behavior)
		QSimConfigGroup qSimConfigGroup = this.qsim.getScenario().getConfig().qsim();

		VehicleBehavior vehicleBehavior = qSimConfigGroup.getVehicleBehavior();
		switch(vehicleBehavior) {
		case exception:
		case teleport:
		case wait:
			break;
		default:
			throw new RuntimeException("Unknown vehicle behavior option.");			
		}
		dpHandler = new VehicularDepartureHandler(this, vehicleBehavior);
		
		switch(qsimConfigGroup.getTrafficDynamics()) {
		case queue:
			QueueWithBuffer.HOLES = false;
			break;
		case withHoles:
			QueueWithBuffer.HOLES = true;
			break;
		default:
			throw new RuntimeException("trafficDynamics defined in config that does not exist: "
					+ qsimConfigGroup.getTrafficDynamics() ) ;
		}

		if(QueueWithBuffer.HOLES && config.getModule("WITH_HOLE")!=null){
			QueueWithBuffer.hole_speed = Double.valueOf(config.getParam("WITH_HOLE", "HOLE_SPEED"));
			if (QueueWithBuffer.hole_speed!=15) {
				log.warn("Hole speed is set to "+QueueWithBuffer.hole_speed+". Default hardcoded value is 15.");
			}
		}
		
		QueueWithBuffer.fastCapacityUpdate = qSimConfigGroup.isUsingFastCapacityUpdate() ;
		
		if(qSimConfigGroup.getLinkDynamics().equals(LinkDynamics.SeepageQ)) {
			QueueWithBuffer.isSeepageAllowed = true;
			QueueWithBuffer.seepMode = qSimConfigGroup.getSeepMode();
			QueueWithBuffer.isSeepModeStorageFree = qSimConfigGroup.isSeepModeStorageFree();
			log.info("Seepage is allowed. Seep mode is " + QueueWithBuffer.seepMode + ".");
			if(QueueWithBuffer.isSeepModeStorageFree) log.warn("Seep mode " + QueueWithBuffer.seepMode + " do not take storage space thus only considered for flow capacities.");
			QueueWithBuffer.isRestrictingSeepage = qSimConfigGroup.isRestrictingSeepage();
		}
		
		if (QSimConfigGroup.SnapshotStyle.withHoles.equals( qsimConfigGroup.getSnapshotStyle())) {
			QueueWithBuffer.VIS_HOLES = true;
		}

		// the following is so confused because I can't separate it out, the reason being that ctor calls need to be the 
		// first in ctors calling each other.  kai, feb'12
		if (config.qsim().isUseLanes()) {
			log.info("Lanes enabled...");
			if (netsimNetworkFactory != null) {
				throw new RuntimeException("both `lanes' and `netsimNetworkFactory' are defined; don't know what that means; aborting") ;
			}
			network = new QNetwork(
							sim.getScenario().getNetwork(),
							new QLanesNetworkFactory(new DefaultQNetworkFactory(), sim.getScenario().getLanes()));
		} else if (netsimNetworkFactory != null){
			network = new QNetwork( sim.getScenario().getNetwork(), netsimNetworkFactory ) ;
		} else if (qsimConfigGroup.getLinkDynamics() == QSimConfigGroup.LinkDynamics.PassingQ || qsimConfigGroup.getLinkDynamics() == QSimConfigGroup.LinkDynamics.SeepageQ) {
			network = new QNetwork(sim.getScenario().getNetwork(), new NetsimNetworkFactory() {
				@Override
				public QLinkImpl createNetsimLink(final Link link, final QNetwork network2, final QNode toQueueNode) {
					return new QLinkImpl(link, network2, toQueueNode, new PassingVehicleQ());
				}
				@Override
				public QNode createNetsimNode(final Node node, QNetwork network2) {
					return new QNode(node, network2);
				}
			});
		} else {
			network = new QNetwork(sim.getScenario().getNetwork(), new DefaultQNetworkFactory());
		}
		network.getLinkWidthCalculatorForVis().setLinkWidthForVis(qsimConfigGroup.getLinkWidthForVis());
		network.initialize(this);

		this.positionInfoBuilder = this.createAgentSnapshotInfoBuilder( sim.getScenario() );

		this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
	}

	private static int wrnCnt = 0;
	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		if (this.vehicles.put(veh.getId(), (QVehicle) veh) != null) {
			if (wrnCnt < 1) {
				wrnCnt++ ;
				log.warn("existing vehicle in mobsim was just overwritten by other vehicle with same ID.  Not clear what this means.  Continuing anyways ...") ;
				log.warn(Gbl.ONLYONCE);
			}
		}
		QLinkInternalI qlink = network.getNetsimLinks().get(startLinkId);
		if (qlink == null) {
			throw new RuntimeException("requested link with id=" + startLinkId + " does not exist in network. Possible vehicles "
					+ "or activities or facilities are registered to a different network.") ;
		}
		qlink.addParkedVehicle(veh);
	}

	private AbstractAgentSnapshotInfoBuilder createAgentSnapshotInfoBuilder(Scenario scenario) {
		final SnapshotStyle snapshotStyle = scenario.getConfig().qsim().getSnapshotStyle();
		switch(snapshotStyle) {
		case queue:
			return new QueueAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		case withHoles:
			// the difference is not in the spacing, thus cannot be differentiated by using different classes.  kai, sep'14
			// ??? kai, nov'15
			return new WithHolesAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		case equiDist:
			return new EquiDistAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		default:
			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported. Using equiDist");
			return new EquiDistAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
	}

	@Override
	public void onPrepareSim() {
		this.infoTime = 
				Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD; 
		/*
		 * infoTime may be < simStartTime, this ensures to print out the
		 * info at the very first timestep already 
		 */

		initQSimEngineThreads();
	}

	@Override
	public void afterSim() {

		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (QNetsimEngineRunner engine : this.engines) {
			engine.afterSim();
		}

		if (this.usingThreadpool) {
			this.pool.shutdown();
		} else {
			/*
			 * Triggering the startBarrier of the QSimEngineThreads.
			 * They will check whether the Simulation is still running.
			 * It is not, so the Threads will stop running.
			 */
			this.startBarrier.arriveAndAwaitAdvance();
		}

		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QLinkInternalI link : network.getNetsimLinks().values()) {
			link.clearVehicles();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	public void doSimStep(final double time) {
		run(time);

		this.printSimLog(time);
	}

	/*
	 * The Threads are waiting at the startBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes and Links. We wait
	 * until all of them reach the endBarrier to move
	 * on. We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the endBarrier before
	 * this Method does, it should work anyway.
	 */
	private void run(double time) {
		// yy Acceleration options to try out (kai, jan'15):

		// (a) Try to do without barriers.  With our 
		// message-based experiments a decade ago, it was better to let each runner decide locally when to proceed.  For intuition, imagine that
		// one runner is slowest on the links, and some other runner slowest on the nodes.  With the barriers, this cannot overlap.
		// With message passing, this was achieved by waiting for all necessary messages.  Here, it could (for example) be achieved with runner-local
		// clocks:
		// for ( all runners that own incoming links to my nodes ) { // (*)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalNodes() ;
		// mytime += 0.5 ;
		// for ( all runners that own toNodes of my links ) { // (**)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalLinks() ;
		// myTime += 0.5 ;

		// (b) Do deliberate domain decomposition rather than round robin (fewer runners to wait for at (*) and (**)).

		// (c) One thread that is much faster than all others is much more efficient than one thread that is much slower than all others. 
		// So make sure that no thread sticks out in terms of slowness.  Difficult to achieve, though.  A decade back, we used a "typical" run
		// as input for the domain decomposition under (b).

		// set current Time
		for (QNetsimEngineRunner engine : this.engines) {
			engine.setTime(time);
		}

		if (this.usingThreadpool) {
			try {
				for (QNetsimEngineRunner engine : this.engines) {
					engine.setMovingNodes(true);
				}
				for (Future<Boolean> future : pool.invokeAll(this.engines)) {
					future.get();
				}
				for (QNetsimEngineRunner engine : this.engines) {
					engine.setMovingNodes(false);
				}
				for (Future<Boolean> future : pool.invokeAll(this.engines)) {
					future.get();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e) ;
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
		} else {
			this.startBarrier.arriveAndAwaitAdvance();
			this.endBarrier.arriveAndAwaitAdvance();
		}
	}


	/*package*/ void printSimLog(double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			int nofActiveLinks = this.getNumberOfSimulatedLinks();
			int nofActiveNodes = this.getNumberOfSimulatedNodes();
			log.info("SIMULATION (QNetsimEngine) AT " + Time.writeTime(time)
					+ " : #links=" + nofActiveLinks
					+ " #nodes=" + nofActiveNodes);
		}
	}

	public int getNumberOfSimulatedLinks() {

		int numLinks = 0;

		for (QNetsimEngineRunner engine : this.engines) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}

		return numLinks;
	}

	public int getNumberOfSimulatedNodes() {

		int numNodes = 0;

		for (QNetsimEngineRunner engine : this.engines) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}

		return numNodes;
	}

	QSim getMobsim() {
		return this.qsim;
	}

	AbstractAgentSnapshotInfoBuilder getAgentSnapshotInfoBuilder(){
		return this.positionInfoBuilder;
	}

	public NetsimNetwork getNetsimNetwork() {
		return this.network;
	}

	/**
	 * convenience method so that stuck time can be cached without caching it in every node separately.  kai, jun'10
	 */
	double getStuckTime() {
		return this.stucktimeCache;
	}

	public VehicularDepartureHandler getDepartureHandler() {
		return dpHandler;
	}

	public final Map<Id<Vehicle>, QVehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}

	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		Id<Link> linkId = planAgent.getCurrentLinkId(); 
		if (linkId != null) { // may be bushwacking
			QLinkInternalI qLink = this.network.getNetsimLink(linkId);
			qLink.registerAdditionalAgentOnLink(planAgent);
		}
	}

	public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
		if  (linkId == null) { // seems that this can happen in tests; not sure if it can happen in regular code. kai, jun'15
			return null;
		}
		QLinkInternalI qLink = this.network.getNetsimLink(linkId);
		return qLink.unregisterAdditionalAgentOnLink(agentId);
	}

	void letVehicleArrive(QVehicle veh) {
		double now = this.qsim.getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		this.qsim.getEventsManager().processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), veh.getId()));
		// reset vehicles driver
		veh.setDriver(null);
		driver.endLegAndComputeNextState(now);
		this.internalInterface.arrangeNextAgentState(driver);
	}

	public void setLinkSpeedCalculator(LinkSpeedCalculator linkSpeedCalculator) {
		this.linkSpeedCalculator = linkSpeedCalculator;
	}

	public LinkSpeedCalculator getLinkSpeedCalculator() {
		return this.linkSpeedCalculator;
	}

	private void initQSimEngineThreads() {

		this.engines = new ArrayList<>();

		this.startBarrier = new Phaser(this.numOfThreads + 1);
		Phaser separationBarrier = new Phaser(this.numOfThreads);
		this.endBarrier = new Phaser(this.numOfThreads + 1);

		numOfRunners = this.numOfThreads;
		if (this.usingThreadpool) {
			// The number of runners should be larger than the number of threads, yes,
			// but see MATSIM-404 - Simulation result still depends on the number of runners.
//			numOfRunners *= 10 ;
			this.pool = Executors.newFixedThreadPool(
					this.numOfThreads,
					new NamedThreadFactory());
		}

		// setup threads
		for (int i = 0; i < numOfRunners; i++) {
			QNetsimEngineRunner engine ;
			if (this.usingThreadpool) {
				engine = new QNetsimEngineRunner();
			} else {
				engine = new QNetsimEngineRunner(this.startBarrier, separationBarrier, endBarrier);
				Thread thread = new Thread(engine);
				thread.setName("QNetsimEngineRunner_" + i);
				thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
				thread.start();
			}
			this.engines.add(engine);
		}

		/*
		 *  Assign every Link and Node to an Activator. By doing so, the
		 *  activateNode(...) and activateLink(...) methods in this class
		 *  should become obsolete.
		 */
		assignNetElementActivators();
	}

	/*
	 * Within the MoveThreads Links are only activated when a Vehicle is moved
	 * over a Node which is processed by that Thread. So we can assign each QLink
	 * to the Thread that handles its InNode.
	 */
	private void assignNetElementActivators() {

		// only for statistics
		int nodes[] = new int[numOfRunners];
		int links[] = new int[numOfRunners];

		int roundRobin = 0;
		for (QNode node : network.getNetsimNodes().values()) {
			int i = roundRobin % this.numOfRunners;
			node.setNetElementActivator(this.engines.get(i));
			nodes[i]++;

			// set activator for out links
			for (Link outLink : node.getNode().getOutLinks().values()) {
				AbstractQLink qLink = (AbstractQLink) network.getNetsimLink(outLink.getId());
				// (must be of this type to work.  kai, feb'12)

				// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
				qLink.setNetElementActivator(this.engines.get(i));

				/*
				 * If the QLink contains agents that end their activity in the first time
				 * step, the link should be activated.
				 */
				if (linksToActivateInitially.remove(qLink)) {
					this.engines.get(i).activateLink(qLink);
				}

				links[i]++;

			}

			roundRobin++;
		}

		// print some statistics
		for (int i = 0; i < this.engines.size(); i++) {
			log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to QSimEngineRunner #" + i);
		}

		this.linksToActivateInitially.clear();
	}

	public void printEngineRunTimes() {
		if (!QSim.analyzeRunTimes) return;
		
		if (printRunTimesPerTimeStep) log.info("detailed QNetsimEngineRunner run times per time step:");
		{
			StringBuffer sb = new StringBuffer();
			sb.append("\t" + "time");
			for (int i = 0; i < this.engines.size(); i++) {
				sb.append("\t" + "thread_" + i); 
			}
			sb.append("\t" + "min");
			sb.append("\t" + "max");
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
			for (QNetsimEngineRunner runner : this.engines) {
				long runTime = runner.runTimes[i];
				sum += runTime;
				if (runTime < min) min = runTime;
				if (runTime > max) max = runTime;
				sb.append("\t" + runTime);
			}
			sb.append("\t" + min);
			sb.append("\t" + max);
			if (printRunTimesPerTimeStep) log.info(sb.toString());
			sumMin += min;
			sumMax += max;
		}
		log.info("sum min run times: " + sumMin);
		log.info("sum max run times: " + sumMax);
		log.info("sum all run times / num threads: " + sum / this.numOfThreads);
	}
	
	private static class NamedThreadFactory implements ThreadFactory {
		private int count = 0;

		@Override
		public Thread newThread(Runnable r) {
			return new Thread( r , "QNetsimEngine_PooledThread_" + count++);
		}
	}
}