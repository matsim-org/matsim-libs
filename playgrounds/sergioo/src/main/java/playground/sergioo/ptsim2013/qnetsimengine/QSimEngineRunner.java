/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineRunner.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;

/**
 * These are the "threads" of the {@link ParallelQNetsimEngine}.  The "run()" method is implicitly called by starting the thread.  
 * 
 * @author (of this documentation) nagel
 *
 */
public class QSimEngineRunner extends NetElementActivator implements Runnable {

	private double time = 0.0;
	private boolean simulateAllNodes = false;
	private boolean simulateAllLinks = false;
	private boolean useNodeArray = PTQNetsimEngine.useNodeArray;

	private volatile boolean simulationRunning = true;

	private final CyclicBarrier startBarrier;
	private final CyclicBarrier separationBarrier;
	private final CyclicBarrier endBarrier;

	private QNode[] nodesArray = null;
	private List<QNode> nodesList = null;
	private List<PTQLink> linksList = new ArrayList<PTQLink>();

	/** 
	 * This is the collection of nodes that have to be activated in the current time step.
	 * This needs to be thread-safe since it is not guaranteed that each incoming link is handled
	 * by the same thread as a node itself.
	 * A node could be activated multiple times concurrently from different incoming links within 
	 * a time step. To avoid this,
	 * a) 	the activateNode() method in the QNode class could be synchronized or 
	 * b) 	a map could be used instead of a list. By doing so, no multiple entries are possible.
	 * 		However, still multiple "put" operations will be performed for the same node.
	 */
	private final Map<Id<Node>, QNode> nodesToActivate = new ConcurrentHashMap<Id<Node>, QNode>();
	
	/** This is the collection of links that have to be activated in the current time step */
	private final ArrayList<PTQLink> linksToActivate = new ArrayList<PTQLink>();
	
	/*package*/ QSimEngineRunner(boolean simulateAllNodes, boolean simulateAllLinks, CyclicBarrier startBarrier, CyclicBarrier separationBarrier, CyclicBarrier endBarrier) {
		this.simulateAllNodes = simulateAllNodes;
		this.simulateAllLinks = simulateAllLinks;
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
	}

	/*package*/ void setQNodeArray(QNode[] nodes) {
		this.nodesArray = nodes;
	}

	/*package*/ void setQNodeList(List<QNode> nodes) {
		this.nodesList = nodes;
	}

	/*package*/ void setLinks(List<PTQLink> links) {
		this.linksList = links;
	}

	/*package*/ void setTime(final double t) {
		time = t;
	}

	public void afterSim() {
		this.simulationRunning = false;
	}

	@Override
	public void run() {
		/*
		 * The method is ended when the simulationRunning Flag is
		 * set to false.
		 */
		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are
				 * triggered in the next TimeStep by the run() method in
				 * the ParallelQNetsimEngine.
				 */
				startBarrier.await();

				/*
				 * Check if Simulation is still running.
				 * Otherwise print CPU usage and end Thread.
				 */
				if (!simulationRunning) {
					Gbl.printCurrentThreadCpuTime();
					return;
				}

				/*
				 * Move Nodes
				 */
				if (useNodeArray) {
					for (QNode node : nodesArray) {
						if (node.isActive() /*|| node.isSignalized()*/ || simulateAllNodes) {
							node.doSimStep(time);
						}
					}
				} else {
					ListIterator<QNode> simNodes = this.nodesList.listIterator();
					QNode node;

					while (simNodes.hasNext()) {
						node = simNodes.next();
						node.doSimStep(time);
						if (!node.isActive()) simNodes.remove();
					}
				}

				/*
				 * After moving the Nodes all we use a CyclicBarrier to synchronize
				 * the Threads. By using a Runnable within the Barrier we activate
				 * some Links.
				 */
				this.separationBarrier.await();

				/*
				 * Move Links
				 */
				ListIterator<PTQLink> simLinks = this.linksList.listIterator();
				PTQLink link;
				boolean isActive;

				while (simLinks.hasNext()) {
					link = simLinks.next();

					isActive = link.doSimStep(time);

					if (!isActive && !simulateAllLinks) {
						simLinks.remove();
					}
				}

				/*
				 * The End of the Moving is synchronized with
				 * the endBarrier. If all Threads reach this Barrier
				 * the main Thread can go on.
				 */
				endBarrier.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (BrokenBarrierException e) {
            	throw new RuntimeException(e);
            }
		}
	}	// run()

	@Override
	protected void activateLink(NetsimLink link) {
		if (!simulateAllLinks) {
			linksToActivate.add((PTQLink)link);
		}
	}

	/*package*/ void activateLinks() {
		this.linksList.addAll(this.linksToActivate);
		this.linksToActivate.clear();
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.linksList.size();
	}

	@Override
	protected void activateNode(QNode node) {
		if (!useNodeArray && !simulateAllNodes) {
			this.nodesToActivate.put(node.getNode().getId(), node);
		}
	}

	/*package*/ void activateNodes() {
		if (!useNodeArray && !simulateAllNodes) {
			this.nodesList.addAll(this.nodesToActivate.values());
			this.nodesToActivate.clear();
		}
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		if (useNodeArray) return nodesArray.length;
		else return nodesList.size();
	}

	public NetsimNetworkFactory<QNode,PTQLink> getNetsimNetworkFactory() {
		return new PTQNetworkFactory() ;
	}

}
