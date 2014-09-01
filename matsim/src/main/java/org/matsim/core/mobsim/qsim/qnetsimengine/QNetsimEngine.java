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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
public class QNetsimEngine extends NetElementActivator implements MobsimEngine {

	private static final Logger log = Logger.getLogger(QNetsimEngine.class);

	private static final int INFO_PERIOD = 3600;

	/*package*/   QNetwork network;

	/** This is the collection of links that have to be moved in the simulation */
	/*package*/  List<QLinkInternalI> simLinksList = new ArrayList<QLinkInternalI>();

	/** This is the collection of nodes that have to be moved in the simulation */
	/*package*/  List<QNode> simNodesList = null;

	/** This is the collection of links that have to be activated in the current time step */
	/*package*/  ArrayList<QLinkInternalI> simActivateLinks = new ArrayList<QLinkInternalI>();

	/** This is the collection of nodes that have to be activated in the current time step */
	/*package*/  ArrayList<QNode> simActivateNodes = new ArrayList<QNode>();

	private final Map<Id, QVehicle> vehicles = new HashMap<Id, QVehicle>();

	private final QSim qsim;

	private final AbstractAgentSnapshotInfoBuilder positionInfoBuilder;

	private final double stucktimeCache;
	private final DepartureHandler dpHandler;

	private double infoTime = 0;

	private LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator();

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
		simNodesList = new ArrayList<QNode>();
		this.infoTime = 
				Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD; 
		/*
		 * infoTime may be < simStartTime, this ensures to print out the
		 * info at the very first timestep already 
		 */
	}

	@Override
	public void afterSim() {
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
		moveNodes(time);
		moveLinks(time);
		printSimLog(time);
	}

	private void moveNodes(final double time) {
		reactivateNodes();
		ListIterator<QNode> simNodes = this.simNodesList.listIterator();
		QNode node;

		while (simNodes.hasNext()) {
			node = simNodes.next();
			node.doSimStep(time);

			if (!node.isActive()) simNodes.remove();
		}
	}

	private void moveLinks(final double time) {
		reactivateLinks();
		ListIterator<QLinkInternalI> simLinks = this.simLinksList.listIterator();
		QLinkInternalI link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.doSimStep(time);
			if (!isActive) {
				simLinks.remove();
			}
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

	@Override
	protected void activateLink(final QLinkInternalI link) {
		this.simActivateLinks.add(link);
	}

	private void reactivateLinks() {
		if (!this.simActivateLinks.isEmpty()) {
			this.simLinksList.addAll(this.simActivateLinks);
			this.simActivateLinks.clear();
		}
	}

	@Override
	protected void activateNode(QNode node) {
		this.simActivateNodes.add(node);
	}

	private void reactivateNodes() {
		if (!this.simActivateNodes.isEmpty()) {
			this.simNodesList.addAll(this.simActivateNodes);
			this.simActivateNodes.clear();
		}
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		return this.simNodesList.size();
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.simLinksList.size();
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

}
