/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineImpl.java
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

package playground.sergioo.ptsim2013.qnetsimengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.ptsim2013.QSim;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTime;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
public class PTQNetsimEngine extends NetElementActivator implements MobsimEngine {

	private static final class NodeIdComparator implements Comparator<QNode>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final QNode o1, final QNode o2) {
			return o1.getNode().getId().compareTo(o2.getNode().getId());
		}
	}


	private static final Logger log = Logger.getLogger(PTQNetsimEngine.class);

	private static final int INFO_PERIOD = 3600;
	
	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	/*package*/ static boolean simulateAllLinks = false;
	/*package*/ static boolean simulateAllNodes = false;

	/*
	 * "Classic" behavior is using an array of nodes. However, in many
	 * cases it is faster, if non-active nodes are de-activated. Note that
	 * enabling this option might (slightly) change the simulation results. 
	 * A node that is de-activated and re-activated later will be at another 
	 * position in the list of nodes which are simulated. As a result, different
	 * random numbers will be used when the node is simulated.  
	 * In the future each node should get its own random number generator - then
	 * there is no difference anymore between the results w/o de-activated nodes.
	 */
	/*package*/ static boolean useNodeArray = false;
	/*package*/   QNetwork network;

	/*package*/  List<PTQLink> allLinks = null;
	/*package*/  List<QNode> allNodes = null;
	/** This is the collection of links that have to be moved in the simulation */
	/*package*/  List<PTQLink> simLinksList = new ArrayList<PTQLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	/*package*/  QNode[] simNodesArray = null;
	/*package*/  List<QNode> simNodesList = null;
	/** This is the collection of links that have to be activated in the current time step */
	/*package*/  ArrayList<PTQLink> simActivateLinks = new ArrayList<PTQLink>();

	/** This is the collection of nodes that have to be activated in the current time step */
	/*package*/  ArrayList<QNode> simActivateNodes = new ArrayList<QNode>();

	private final Map<Id<Vehicle>, QVehicle> vehicles = new HashMap<Id<Vehicle>, QVehicle>();

	private final QSim qsim;

	private final double stucktimeCache;
	private final DepartureHandler dpHandler ;

	private double infoTime = 0;
	
	/*package*/ InternalInterface internalInterface = null ;
	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	public PTQNetsimEngine(final QSim sim ) {
		this( sim, null ) ;
	}

	public PTQNetsimEngine(final QSim sim, NetsimNetworkFactory<QNode, PTQLink> netsimNetworkFactory) {
		this.qsim = sim;

		this.stucktimeCache = sim.getScenario().getConfig().qsim().getStuckTime();

		// configuring the car departure hander (including the vehicle behavior)
		QSimConfigGroup qSimConfigGroup = this.qsim.getScenario().getConfig().qsim();
		org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior vehicleBehavior;
		if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VehicleBehavior.exception)) {
			vehicleBehavior = VehicleBehavior.exception;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VehicleBehavior.teleport)) {
			vehicleBehavior = VehicleBehavior.teleport;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VehicleBehavior.wait )) {
			vehicleBehavior = VehicleBehavior.wait ;
		} else {
			throw new RuntimeException("Unknown vehicle behavior option.");
		}
		dpHandler = new PTVehicularDepartureHandler(this, vehicleBehavior);

		//  I am quite sceptic if the following should stay since it does not work.  kai, feb'11
		if ( "queue".equals( sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ) {
			PTQLink.HOLES=false ;
		} else if ( "withHolesExperimental".equals( sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ) {
			PTQLink.HOLES = true ;
		} else {
			throw new RuntimeException("trafficDynamics defined in config that does not exist: "
					+ sim.getScenario().getConfig().qsim().getTrafficDynamics() ) ;
		}

		// the following is so confused because I can't separate it out, the reason being that ctor calls need to be the 
		// first in ctors calling each other.  kai, feb'12
		if ( netsimNetworkFactory != null ){
			network = new QNetwork( sim.getScenario().getNetwork(), netsimNetworkFactory ) ;
		} else {
			network = new QNetwork(sim.getScenario().getNetwork(), new PTQNetworkFactory());
		}

		//network.getLinkWidthCalculator().setLinkWidth(ConfigUtils.addOrGetModule(sim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).getLinkWidth());
		network.initialize(this);
	}

	public void setStopStopTime(StopStopTime stopStopTime) {
		network.setStopStopTime(stopStopTime);
	}
	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		vehicles.put(veh.getId(), (QVehicle) veh);
		PTQLink qlink = network.getNetsimLinks().get(startLinkId);
		qlink.addParkedVehicle(veh);
	}

	@Override
	public void onPrepareSim() {
		this.allLinks = new ArrayList<PTQLink>(network.getNetsimLinks().values());
		this.allNodes = new ArrayList<QNode>(network.getNetsimNodes().values());
		if (useNodeArray) {
			this.simNodesArray = network.getNetsimNodes().values().toArray(new QNode[network.getNetsimNodes().values().size()]);
			//dg[april08] as the order of nodes has an influence on the simulation
			//results they are sorted to avoid indeterministic simulations
			Arrays.sort(this.simNodesArray, new NodeIdComparator());			
		} else {
			simNodesList = new ArrayList<QNode>();
			Collections.sort(simNodesList, new NodeIdComparator());
		}
		if (simulateAllLinks) {
			this.simLinksList.addAll(this.allLinks);
		}
		this.infoTime = Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already
	}

	@Override
	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (PTQLink link : this.allLinks) {
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
		if (useNodeArray) {
			for (QNode node : this.simNodesArray) {
				if (node.isActive() /*|| node.isSignalized()*/ || simulateAllNodes) {
					/* It is faster to first test if the node is active, and only then call moveNode(),
					 * than calling moveNode() directly and that one returns immediately when it's not
					 * active. Most likely, the getter isActive() can be in-lined by the compiler, while
					 * moveNode() cannot, resulting in fewer method-calls when isActive() is used.
					 * -marcel/20aug2008
					 */
					node.doSimStep(time);
				}
			}			
		} else {
			reactivateNodes();
			ListIterator<QNode> simNodes = this.simNodesList.listIterator();
			QNode node;

			while (simNodes.hasNext()) {
				node = simNodes.next();
				node.doSimStep(time);

				if (!node.isActive()) simNodes.remove();
			}
		}
	}

	private void moveLinks(final double time) {
		reactivateLinks();
		ListIterator<PTQLink> simLinks = this.simLinksList.listIterator();
		PTQLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.doSimStep(time);
			if (!isActive && !simulateAllLinks) {
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
	protected void activateLink(final NetsimLink link) {
		if (!simulateAllLinks) {
			this.simActivateLinks.add((PTQLink)link);
		}
	}

	private void reactivateLinks() {
		if ((!simulateAllLinks) && (!this.simActivateLinks.isEmpty())) {
			this.simLinksList.addAll(this.simActivateLinks);
			this.simActivateLinks.clear();
		}
	}

	@Override
	protected void activateNode(QNode node) {
		if (!useNodeArray && !simulateAllNodes) {
			this.simActivateNodes.add(node);
		}
	}

	private void reactivateNodes() {
		if ((!simulateAllNodes) && (!this.simActivateNodes.isEmpty())) {
			this.simNodesList.addAll(this.simActivateNodes);
			this.simActivateNodes.clear();
		}
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		if (useNodeArray) return this.simNodesArray.length;
		else return this.simNodesList.size();
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.simLinksList.size();
	}

	QSim getMobsim() {
		return this.qsim;
	}

	public NetsimNetwork getNetsimNetwork() {
		return this.network ;
	}

	/**
	 * convenience method so that stuck time can be cached without caching it in every node separately.  kai, jun'10
	 */
	double getStuckTime() {
		return this.stucktimeCache ;
	}

	public DepartureHandler getDepartureHandler() {
		return dpHandler;
	}

	public final Map<Id<Vehicle>, QVehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}

	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		Id<Link> linkId = planAgent.getCurrentLinkId(); 
		if (linkId != null) { // may be bushwacking
			PTQLink qLink = network.getNetsimLink(linkId);
			qLink.registerAdditionalAgentOnLink(planAgent);
		}
	}

	public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
		PTQLink qLink = network.getNetsimLink(linkId);
		return qLink.unregisterAdditionalAgentOnLink(agentId);
	}

	void letVehicleArrive(QVehicle veh) {
		double now = qsim.getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		qsim.getEventsManager().processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), veh.getId()));
		// reset vehicles driver
		veh.setDriver(null);
		driver.endLegAndComputeNextState(now);
		this.internalInterface.arrangeNextAgentState(driver) ;
	}

}
