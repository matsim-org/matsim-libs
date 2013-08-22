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

package playground.christoph.mobsim.ca2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;
import org.matsim.core.utils.misc.Time;

public class CASimEngine implements MobsimEngine, NetworkElementActivator {

	private static Logger log = Logger.getLogger(CASimEngine.class);

	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;
	
	/*package*/ Netsim qSim;
	/*package*/ Collection<CALink> activeLinks;
	/*package*/ Collection<CANode> activeNodes;

	private Map<Id, CANode> nodes = new HashMap<Id, CANode>();
	private Map<Id, CALink> links = new HashMap<Id, CALink>();
	
	/*package*/ InternalInterface internalInterface = null;

	private final double spatialResolution;

	/*package*/ CASimEngine(Netsim qSim, double spatialResolution) {
		this.qSim = qSim;

		/*
		 * This is the collection of active nodes. This needs to be thread-safe since in the
		 * parallel implementation, multiple threads could activate nodes concurrently.
		 * (Each thread has its own queue. However, it is not guaranteed that all incoming
		 * links of a node are handled by the same thread). 
		 */
		activeNodes = new ConcurrentLinkedQueue<CANode>();

		/*
		 * Here, in theory, no thread-safe data structure is needed since links can only
		 * be activated by their from links and all links are handled by the same thread
		 * as that node is handled (see assignSimEngines() method in ParallelMultiModalSimEngine).
		 * However, since this assignment might be changed, we still use a thread-safe data 
		 * structure.
		 */
		activeLinks = new ConcurrentLinkedQueue<CALink>();
		
		this.spatialResolution = spatialResolution;
	}

	/*package*/ double getSpatialResoluation() {
		return this.spatialResolution;
	}
	
	Netsim getMobsim() {
		return qSim;
	}

	@Override
	public void onPrepareSim() {
		
		double timeStep = this.getMobsim().getSimTimer().getSimTimestepSize();
		
		for (NetsimNode node : qSim.getNetsimNetwork().getNetsimNodes().values()) {
			CANode extension = new CANode(node.getNode(), this);
			nodes.put(node.getNode().getId(), extension);
		}
		
		for (NetsimLink link : qSim.getNetsimNetwork().getNetsimLinks().values()) {
			Id toNodeId = link.getLink().getToNode().getId();
			CALink extension = new CALink(link.getLink(), this, getCANode(toNodeId),
					this.spatialResolution, timeStep);
			links.put(link.getLink().getId(), extension);
		}
		
		for (NetsimNode node : qSim.getNetsimNetwork().getNetsimNodes().values()) {
			CANode extension = getCANode(node.getNode().getId());
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

		Iterator<CANode> simNodes = this.activeNodes.iterator();
		CANode node;
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

		Iterator<CALink> simLinks = this.activeLinks.iterator();
		CALink link;
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
		for (CALink link : this.links.values()) {
			link.clearVehicles();
		}
	}

	/*
	 * This is now thread-safe since the MultiModalQLinkExtension uses an
	 * AtomicBoolean to store its state. Therefore, it cannot be activated
	 * multiple times.
	 */
	@Override
	public void activateLink(CALink link) {
		this.activeLinks.add(link);
	}

	/*
	 * This is now thread-safe since the MultiModalQNodeExtension uses an
	 * AtomicBoolean to store its state. Therefore, it cannot be activated
	 * multiple times.
	 */
	@Override
	public void activateNode(CANode node) {
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

	/*package*/ CANode getCANode(Id nodeId) {
		return nodes.get(nodeId);
	}

	/*package*/ CALink getCALink(Id linkId) {
		return links.get(linkId);
	}
	
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
	
}
