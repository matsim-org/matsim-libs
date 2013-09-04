/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalSimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;
import org.matsim.core.utils.misc.Time;

public class FlexibleCellSimEngine implements MobsimEngine, NetworkElementActivator {

	private static Logger log = Logger.getLogger(FlexibleCellSimEngine.class);

	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;
	
	/*package*/ Netsim qSim;
	/*package*/ Collection<FlexibleCellLink> activeLinks;
	/*package*/ Collection<FlexibleCellNode> activeNodes;

	private Map<Id, FlexibleCellNode> nodes = new HashMap<Id, FlexibleCellNode>();
	private Map<Id, FlexibleCellLink> links = new HashMap<Id, FlexibleCellLink>();
	
	/*package*/ InternalInterface internalInterface = null;

	/*package*/ FlexibleCellSimEngine(Netsim qSim) {
		this.qSim = qSim;

		/*
		 * This is the collection of active nodes. This needs to be thread-safe since in the
		 * parallel implementation, multiple threads could activate nodes concurrently.
		 * (Each thread has its own queue. However, it is not guaranteed that all incoming
		 * links of a node are handled by the same thread). 
		 */
		activeNodes = new ConcurrentLinkedQueue<FlexibleCellNode>();

		/*
		 * Here, in theory, no thread-safe data structure is needed since links can only
		 * be activated by their from links and all links are handled by the same thread
		 * as that node is handled (see assignSimEngines() method in ParallelMultiModalSimEngine).
		 * However, since this assignment might be changed, we still use a thread-safe data 
		 * structure.
		 */
		activeLinks = new ConcurrentLinkedQueue<FlexibleCellLink>();
	}

	Netsim getMobsim() {
		return qSim;
	}

	@Override
	public void onPrepareSim() {
		
		double timeStep = this.getMobsim().getSimTimer().getSimTimestepSize();
		
		for (NetsimNode node : qSim.getNetsimNetwork().getNetsimNodes().values()) {
			FlexibleCellNode extension = new FlexibleCellNode(node.getNode(), this);
			nodes.put(node.getNode().getId(), extension);
		}
		
		// so far hard-coded
		double vehicleLength = 7.5;
		double minSpaceCellLength = 0.5;
		for (NetsimLink link : qSim.getNetsimNetwork().getNetsimLinks().values()) {
			Id toNodeId = link.getLink().getToNode().getId();
			FlexibleCellLink extension = new FlexibleCellLink(link.getLink(), this, 
					this.getFlexibleCellNode(toNodeId), vehicleLength, minSpaceCellLength, timeStep);
			links.put(link.getLink().getId(), extension);
		}
		
		// after creating all links, set next links in velocity models where necessary
		// TODO: make this more flexible based on the network structure
		for (NetsimLink link : qSim.getNetsimNetwork().getNetsimLinks().values()) {

			List<Link> nextLinks = new ArrayList<Link>(link.getLink().getToNode().getOutLinks().values());
			if (nextLinks.size() == 1) {
				FlexibleCellLink flexibleCellLink = this.getFlexibleCellLink(link.getLink().getId());
				FlexibleCellLink nextLink = this.getFlexibleCellLink(nextLinks.get(0).getId());
				flexibleCellLink.setNextLink(nextLink);
			}
		}
		
		for (NetsimNode node : qSim.getNetsimNetwork().getNetsimNodes().values()) {
			FlexibleCellNode extension = getFlexibleCellNode(node.getNode().getId());
			extension.init();
		}
		
		/*
		 * InfoTime may be < simStartTime, this ensures to print out the info 
		 * at the very first timestep already
		 */
		this.infoTime = Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD; 
	}

	@Override
	public void doSimStep(double time) {
		moveNodes(time);
		moveLinks(time);
		printSimLog(time);
	}

	/*package*/ void moveNodes(final double time) {

		Iterator<FlexibleCellNode> simNodes = this.activeNodes.iterator();
		FlexibleCellNode node;
		boolean isActive;

		while (simNodes.hasNext()) {
			node = simNodes.next();
			isActive = node.moveNode(time);
			if (!isActive) {
				simNodes.remove();
			}
		}
	}

	/*package*/ void moveLinks(final double time) {

		Iterator<FlexibleCellLink> simLinks = this.activeLinks.iterator();
		FlexibleCellLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
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
			log.info("SIMULATION (MultiModalSimEngine) AT " + Time.writeTime(time) 
					+ " #links=" + nofActiveLinks + " #nodes=" + nofActiveNodes);
		}
	}

	@Override
	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (FlexibleCellLink link : this.links.values()) {
			link.clearVehicles();
		}
	}

	/*
	 * This is now thread-safe since the MultiModalQLinkExtension uses an
	 * AtomicBoolean to store its state. Therefore, it cannot be activated
	 * multiple times.
	 */
	@Override
	public void activateLink(FlexibleCellLink link) {
		this.activeLinks.add(link);
	}

	/*
	 * This is now thread-safe since the MultiModalQNodeExtension uses an
	 * AtomicBoolean to store its state. Therefore, it cannot be activated
	 * multiple times.
	 */
	@Override
	public void activateNode(FlexibleCellNode node) {
		this.activeNodes.add(node);
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return activeLinks.size();
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		return activeNodes.size();
	}

	/*package*/ FlexibleCellNode getFlexibleCellNode(Id nodeId) {
		return nodes.get(nodeId);
	}

	/*package*/ FlexibleCellLink getFlexibleCellLink(Id linkId) {
		return links.get(linkId);
	}
	
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
	
}
