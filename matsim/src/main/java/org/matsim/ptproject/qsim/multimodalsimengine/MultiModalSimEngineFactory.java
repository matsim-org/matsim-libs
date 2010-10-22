/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalSimEngineFactory.java
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

package org.matsim.ptproject.qsim.multimodalsimengine;

import org.apache.log4j.Logger;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.interfaces.NetsimNode;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.netsimengine.QNode;

public class MultiModalSimEngineFactory {

	final private static Logger log = Logger.getLogger(MultiModalSimEngineFactory.class);
	
	public MultiModalSimEngine createMultiModalSimEngine(Mobsim sim) {
		
		MultiModalSimEngine simEngine;
		
		int numOfThreads = sim.getScenario().getConfig().getQSimConfigGroup().getNumberOfThreads(); 
		if (numOfThreads > 1) {
			simEngine = new ParallelMultiModalSimEngine(sim);
			log.info("Using ParallelMultiModalSimEngine with " + numOfThreads + " threads.");
		}
		else {
			simEngine = new MultiModalSimEngine(sim);			
		}
		
		addMultiModalToQNetwork(sim.getNetsimNetwork(), simEngine);
		
		return simEngine;
	}
	
	private void addMultiModalToQNetwork(NetsimNetwork network, MultiModalSimEngine simEngine) {
		for (NetsimNode node : network.getNetsimNodes().values()) {
			MultiModalQNodeExtension extension = new MultiModalQNodeExtension(node.getNode(), simEngine);
			node.getCustomAttributes().put(MultiModalQNodeExtension.class.getName(), extension);
		}
		
		for (NetsimLink link : network.getNetsimLinks().values()) {
			QNode toNode = link.getToQueueNode();
			MultiModalQLinkExtension extension = new MultiModalQLinkExtension(link, simEngine, toNode);
			link.getCustomAttributes().put(MultiModalQLinkExtension.class.getName(), extension);
		}
		
		for (NetsimNode node : network.getNetsimNodes().values()) {
			MultiModalQNodeExtension extension = simEngine.getMultiModalQNodeExtension(node);
			extension.init();
		}

	}
}
