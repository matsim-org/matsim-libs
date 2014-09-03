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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Phaser;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.VehicularDepartureHandler.VehicleBehavior;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.data.v20.LaneDefinitions20;

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

	/*package*/   QNetwork network;

	private final Map<Id, QVehicle> vehicles = new HashMap<Id, QVehicle>();

	private final QSim qsim;

	private final AbstractAgentSnapshotInfoBuilder positionInfoBuilder;

	private final double stucktimeCache;
	private final DepartureHandler dpHandler;

	private double infoTime = 0;

    private final int numOfThreads;

	private LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator();


    private QSimEngineRunner[] engines;

    private Phaser startBarrier;
    private Phaser endBarrier;
    
    private final Set<QLinkInternalI> linksToActivateInitially = new HashSet<QLinkInternalI>();

	/*package*/ InternalInterface internalInterface = null ;
	@Override
	public void setInternalInterface( InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	public QNetsimEngine(final QSim sim) {
		this(sim, null);
	}

	public QNetsimEngine(final QSim sim, NetsimNetworkFactory<QNode, ? extends QLinkInternalI> netsimNetworkFactory ) {
		this.qsim = sim;

		this.stucktimeCache = sim.getScenario().getConfig().qsim().getStuckTime();

		// configuring the car departure hander (including the vehicle behavior)
		QSimConfigGroup qSimConfigGroup = this.qsim.getScenario().getConfig().qsim();
		VehicleBehavior vehicleBehavior;
		if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION)) {
			vehicleBehavior = VehicleBehavior.EXCEPTION;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_TELEPORT)) {
			vehicleBehavior = VehicleBehavior.TELEPORT;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_WAIT)) {
			vehicleBehavior = VehicleBehavior.WAIT_UNTIL_IT_COMES_ALONG;
		} else {
			throw new RuntimeException("Unknown vehicle behavior option.");
		}
		dpHandler = new VehicularDepartureHandler(this, vehicleBehavior);

		// yyyyyy I am quite sceptic if the following should stay since it does not work.  kai, feb'11
		if ( "queue".equals( sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ) {
			QueueWithBuffer.HOLES=false ;
		} else if ( "withHolesExperimental".equals( sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ) {
			QueueWithBuffer.HOLES = true ;
		} else {
			throw new RuntimeException("trafficDynamics defined in config that does not exist: "
					+ sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ;
		}

		// the following is so confused because I can't separate it out, the reason being that ctor calls need to be the 
		// first in ctors calling each other.  kai, feb'12
		if (sim.getScenario().getConfig().scenario().isUseLanes()) {
			if (sim.getScenario().getScenarioElement(LaneDefinitions20.ELEMENT_NAME) == null) {
				throw new IllegalStateException(
						"Lane definitions in v2.0 format have to be set if feature is enabled!");
			}
			log.info("Lanes enabled...");
			if ( netsimNetworkFactory != null ) {
				throw new RuntimeException("both `lanes' and `netsimNetworkFactory' are defined; don't know what that means; aborting") ;
			}
			network =
					new QNetwork(
							sim.getScenario().getNetwork(),
							new QLanesNetworkFactory(
									new DefaultQNetworkFactory(),
									(LaneDefinitions20) sim.getScenario().getScenarioElement(
											LaneDefinitions20.ELEMENT_NAME)));
		} else if ( netsimNetworkFactory != null ){
			network = new QNetwork( sim.getScenario().getNetwork(), netsimNetworkFactory ) ;
		} else if (QSimConfigGroup.LinkDynamics.valueOf(sim.getScenario().getConfig().qsim().getLinkDynamics()) == QSimConfigGroup.LinkDynamics.PassingQ) {
			network = new QNetwork(sim.getScenario().getNetwork(), new NetsimNetworkFactory<QNode, QLinkImpl>() {
				@Override
				public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
					return new QLinkImpl(link, network, toQueueNode, new PassingVehicleQ());
				}
				@Override
				public QNode createNetsimNode(final Node node, QNetwork network) {
					return new QNode(node, network);
				}
			});
		} else {
			network = new QNetwork(sim.getScenario().getNetwork(), new DefaultQNetworkFactory());
		}
		network.getLinkWidthCalculator().setLinkWidth(sim.getScenario().getConfig().qsim().getLinkWidth());
		network.initialize(this);

		this.positionInfoBuilder = this.createAgentSnapshotInfoBuilder( sim.getScenario() );


        this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
	}

	public void addParkedVehicle(MobsimVehicle veh, Id startLinkId) {
		vehicles.put(veh.getId(), (QVehicle) veh);
		QLinkInternalI qlink = network.getNetsimLinks().get(startLinkId);
		if ( qlink==null ) {
			throw new RuntimeException("requested link with id=" + startLinkId + " does not exist in network.  Possible vehicles "
					+ "or activities or facilities are registered to a different network.") ;
		}
		qlink.addParkedVehicle(veh);
	}

	private AbstractAgentSnapshotInfoBuilder createAgentSnapshotInfoBuilder(Scenario scenario){
		String  snapshotStyle = scenario.getConfig().qsim().getSnapshotStyle();
		if ("queue".equalsIgnoreCase(snapshotStyle)){
			return new QueueAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else  if ("equiDist".equalsIgnoreCase(snapshotStyle)){
			return new EquiDistAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else if ("withHolesExperimental".equalsIgnoreCase(snapshotStyle)){
			log.warn("The snapshot style \"withHolesExperimental\" is no longer supported, using \"queue\" instead. ");
//			return new QueueAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
			return new WithHolesAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else {
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
        for (QSimEngineRunner engine : this.engines) {
            engine.afterSim();
        }

		/*
		 * Triggering the startBarrier of the QSimEngineThreads.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
//        try {
//            this.startBarrier.await();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (BrokenBarrierException e) {
//            throw new RuntimeException(e);
//        }
        this.startBarrier.arriveAndAwaitAdvance();

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

//        try {
            // set current Time
            for (QSimEngineRunner engine : this.engines) {
                engine.setTime(time);
            }

//            this.startBarrier.await();
//
//            this.endBarrier.await();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (BrokenBarrierException e) {
//            throw new RuntimeException(e);
//        }
            this.startBarrier.arriveAndAwaitAdvance();
            
            this.endBarrier.arriveAndAwaitAdvance();
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

        for (QSimEngineRunner engine : this.engines) {
            numLinks = numLinks + engine.getNumberOfSimulatedLinks();
        }

        return numLinks;
    }

    public int getNumberOfSimulatedNodes() {

        int numNodes = 0;

        for (QSimEngineRunner engine : this.engines) {
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
		return this.network ;
	}

	/**
	 * convenience method so that stuck time can be cached without caching it in every node separately.  kai, jun'10
	 */
	double getStuckTime() {
		return this.stucktimeCache;
	}

	public DepartureHandler getDepartureHandler() {
		return dpHandler;
	}

	public final Map<Id, QVehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}

	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		Id linkId = planAgent.getCurrentLinkId(); 
		if (linkId != null) { // may be bushwacking
			QLinkInternalI qLink = network.getNetsimLink(linkId);
			qLink.registerAdditionalAgentOnLink(planAgent);
		}
	}

	public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
		QLinkInternalI qLink = network.getNetsimLink(linkId);
		return qLink.unregisterAdditionalAgentOnLink(agentId);
	}

	void letVehicleArrive(QVehicle veh) {
		double now = qsim.getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		qsim.getEventsManager().processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), veh.getId()));
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

        this.engines = new QSimEngineRunner[this.numOfThreads];

        this.startBarrier = new Phaser(this.numOfThreads + 1);
        Phaser separationBarrier = new Phaser(this.numOfThreads);
        this.endBarrier = new Phaser(this.numOfThreads + 1);
       
        // setup threads
        for (int i = 0; i < this.numOfThreads; i++) {
            QSimEngineRunner engine = new QSimEngineRunner(this.startBarrier, separationBarrier, endBarrier);
            Thread thread = new Thread(engine);
            thread.setName("QNetsimEngineRunner_" + i);
            thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
            this.engines[i] = engine;

            thread.start();
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
        int nodes[] = new int[this.engines.length];
        int links[] = new int[this.engines.length];

        int roundRobin = 0;
        for (QNode node : network.getNetsimNodes().values()) {
            int i = roundRobin % this.numOfThreads;
            node.setNetElementActivator(this.engines[i]);
            nodes[i]++;

            // set activator for out links
            for (Link outLink : node.getNode().getOutLinks().values()) {
                AbstractQLink qLink = (AbstractQLink) network.getNetsimLink(outLink.getId());
                // (must be of this type to work.  kai, feb'12)

                // removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
                qLink.setNetElementActivator(this.engines[i]);

				/*
				 * If the QLink contains agents that end their activity in the first time
				 * step, the link should be activated.
				 */
                if (linksToActivateInitially.remove(qLink)) {
                    this.engines[i].activateLink(qLink);
                }

                links[i]++;

            }

            roundRobin++;
        }

        // print some statistics
        for (int i = 0; i < this.engines.length; i++) {
            log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to QSimEngineRunner #" + i);
        }

        this.linksToActivateInitially.clear();
    }

}
