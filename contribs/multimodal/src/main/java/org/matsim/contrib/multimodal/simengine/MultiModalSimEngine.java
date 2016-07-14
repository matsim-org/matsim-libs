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

package org.matsim.contrib.multimodal.simengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Phaser;

class MultiModalSimEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(MultiModalSimEngine.class);

	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;

	/*package*/ Map<String, TravelTime> multiModalTravelTimes;

	private final Map<Id<Node>, MultiModalQNodeExtension> nodes = new HashMap<>();
	private final Map<Id<Link>, MultiModalQLinkExtension> links = new HashMap<>();
	
	/*package*/ InternalInterface internalInterface = null;

	private final int numOfThreads;
	
	private MultiModalSimEngineRunner[] runners;
	private Phaser startBarrier;
    private Phaser endBarrier;
	    
    /*package*/ MultiModalSimEngine(Map<String, TravelTime> multiModalTravelTimes, MultiModalConfigGroup multiModalConfigGroup) {		
    	this.multiModalTravelTimes = multiModalTravelTimes;
    	this.numOfThreads = multiModalConfigGroup.getNumberOfThreads();
    	
    	if (this.numOfThreads > 1) log.info("Using " + multiModalConfigGroup.getNumberOfThreads() + " threads for MultiModalSimEngine.");
    }
    
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	QSim getMobsim() {
		return (QSim) this.internalInterface.getMobsim();
	}

	/*package*/ EventsManager getEventsManager() {
        return ((QSim) this.internalInterface.getMobsim()).getEventsManager();
	}
	
	@Override
	public void onPrepareSim() {
		
		// debug message
		log.info("TravelTime classes used for multi-modal simulation: ");
		for (Entry<String, TravelTime> entry : multiModalTravelTimes.entrySet()) {
			log.info("\t" + entry.getKey() + "\t" + entry.getValue().getClass().toString());
		}
		
		Scenario scenario = ((QSim) this.internalInterface.getMobsim()).getScenario();
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		
		/*
		 * Identify links and nodes that allow one of the simulated modes.
		 */
		Set<Link> simulatedLinks = new LinkedHashSet<>();
		Set<Node> simulatedNodes = new LinkedHashSet<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			// node mode restrictions -> use link
			if (link.getAllowedModes() == null) {
				simulatedLinks.add(link);
				simulatedNodes.add(link.getFromNode());
				simulatedNodes.add(link.getToNode());
			} else {
				for (String mode : link.getAllowedModes()) {
					if (simulatedModes.contains(mode)) {
						simulatedLinks.add(link);
						simulatedNodes.add(link.getFromNode());
						simulatedNodes.add(link.getToNode());
						break;
					}
				}
			}
		}
		
		for (Node node : simulatedNodes) {
			int numInLinks = 0;
			for (Link inLink : node.getInLinks().values()) {
				if (simulatedLinks.contains(inLink)) numInLinks++;
			}
			MultiModalQNodeExtension extension = new MultiModalQNodeExtension(this, numInLinks);
			this.nodes.put(node.getId(), extension);
		}
		
		for (Link link : simulatedLinks) {
			Id<Node> toNodeId = link.getToNode().getId();
			MultiModalQLinkExtension extension = new MultiModalQLinkExtension(link, this, getMultiModalQNodeExtension(toNodeId));
			this.links.put(link.getId(), extension);
		}
		
		for (Node node : simulatedNodes) {
			MultiModalQNodeExtension extension = this.getMultiModalQNodeExtension(node.getId());
			List<MultiModalQLinkExtension> inLinks = new ArrayList<>();
			for (Link inLink : node.getInLinks().values()) {
				if (simulatedLinks.contains(inLink)) inLinks.add(this.getMultiModalQLinkExtension(inLink.getId()));
			}
			extension.init(inLinks);
		}
		
		/*
		 * InfoTime may be < simStartTime, this ensures to print out the info 
		 * at the very first timestep already
		 */
		this.infoTime = Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD;
		
		initMultiModalSimEngineRunners();
	}

	/*
	 * The threads are waiting at the startBarrier. We trigger them by reaching this barrier. Now the threads will start 
	 * moving the nodes and links. We wait until all of them reach the endBarrier to move on. We should not have any 
	 * problems with race conditions since even if the threads would be faster than this thread, means they reach the 
	 * endBarrier before this Method does, it should work anyway.
	 */
	@Override
	public void doSimStep(double time) {
		// set current Time
		for (MultiModalSimEngineRunner runner : this.runners) {
			runner.setTime(time);
		}
		
		/*
		 * Triggering the barrier will cause calls to moveLinks and moveNodes
		 * in the threads.
		 */
		this.startBarrier.arriveAndAwaitAdvance();
		
		this.endBarrier.arriveAndAwaitAdvance();

        this.printSimLog(time);
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
		// Calling the afterSim Method of the MultiModalSimEngineRunners will set their simulationRunning flag to false.
		for (MultiModalSimEngineRunner engine : this.runners) {
			engine.afterSim();
		}

		/*
		 * Triggering the startBarrier of the MultiModalSimEngineRunners. They will check whether the Simulation is 
		 * still running. It is not, so the Threads will stop running.
		 */
		this.startBarrier.arriveAndAwaitAdvance();
		
		/* Reset vehicles on ALL links. We cannot iterate only over the active links (this.simLinksArray), because there 
		 * may be links that have vehicles only in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (MultiModalQLinkExtension link : this.links.values()) {
			link.clearVehicles();
		}
	}

	public int getNumberOfSimulatedLinks() {
		int numLinks = 0;
		for (MultiModalSimEngineRunner engine : this.runners) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}
		return numLinks;
	}

	public int getNumberOfSimulatedNodes() {
		int numNodes = 0;
		for (MultiModalSimEngineRunner engine : this.runners) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}
		return numNodes;
	}
	
	/*package*/ Map<String, TravelTime> getMultiModalTravelTimes() {
		return this.multiModalTravelTimes;
	}

	/*package*/ MultiModalQNodeExtension getMultiModalQNodeExtension(Id<Node> nodeId) {
		return this.nodes.get(nodeId);
	}

	/*package*/ MultiModalQLinkExtension getMultiModalQLinkExtension(Id<Link> linkId) {
		return this.links.get(linkId);
	}
	
	private void initMultiModalSimEngineRunners() {

		this.runners = new MultiModalSimEngineRunner[numOfThreads];

		this.startBarrier = new Phaser(numOfThreads + 1);
        Phaser separationBarrier = new Phaser(numOfThreads); // separates moveNodes and moveLinks
		this.endBarrier = new Phaser(numOfThreads + 1);

		// setup runners
		for (int i = 0; i < numOfThreads; i++) {
			MultiModalSimEngineRunner engine = new MultiModalSimEngineRunner(this.startBarrier, 
					separationBarrier, this.endBarrier);
			
			Thread thread = new Thread(engine);
			thread.setName("MultiModalSimEngineRunner_" + i);

			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			this.runners[i] = engine;

			thread.start();
		}
		
		// assign the Links and Nodes to the SimEngines
		assignSimEngines();
	}

	private void assignSimEngines() {

		// only for statistics
		int nodes[] = new int[this.runners.length];
		int links[] = new int[this.runners.length];
		
		int roundRobin = 0;
		Scenario scenario = ((QSim) this.internalInterface.getMobsim()).getScenario();
		
		for (Node node : scenario.getNetwork().getNodes().values()) {
			MultiModalQNodeExtension multiModalQNodeExtension = this.getMultiModalQNodeExtension(node.getId());
			
			// if the node is simulated by the MultiModalSimulation
			if (multiModalQNodeExtension != null) {
				int i = roundRobin % this.numOfThreads;
				MultiModalSimEngineRunner simEngineRunner = this.runners[i];
				multiModalQNodeExtension.setNetworkElementActivator(simEngineRunner);
				nodes[i]++;
				
				/*
				 * Assign each link to its in-node to ensure that they are processed by the same
				 * thread which should avoid running into some race conditions.
				 */
				for (Link link : node.getOutLinks().values()) {
					MultiModalQLinkExtension multiModalQLinkExtension = this.getMultiModalQLinkExtension(link.getId());
					if (multiModalQLinkExtension != null) {
						multiModalQLinkExtension.setNetworkElementActivator(simEngineRunner);
						links[i]++;
					}
				}
				
				roundRobin++;
			}
		}
		
		// print some statistics
		for (int i = 0; i < this.runners.length; i++) {
			log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to MultiModalSimEngineRunner #" + i);
		}
	}
}