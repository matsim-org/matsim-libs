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
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QNetworkI;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.MultiModalTravelTime;
import org.matsim.ptproject.qsim.netsimengine.QNode;

public class MultiModalSimEngineFactory {

	final private static Logger log = Logger.getLogger(MultiModalSimEngineFactory.class);
	
	public MultiModalSimEngine createMultiModalSimEngine(QSimI sim, MultiModalTravelTime multiModalTravelTime) {
		
		MultiModalSimEngine simEngine;
		
		int numOfThreads = sim.getScenario().getConfig().getQSimConfigGroup().getNumberOfThreads(); 
		if (numOfThreads > 1) {
			simEngine = new ParallelMultiModalSimEngine(sim);
			log.info("Using ParallelMultiModalSimEngine with " + numOfThreads + " threads.");
		}
		else {
			simEngine = new MultiModalSimEngine(sim);			
		}
		
		addMultiModalToQNetwork(sim.getQNetwork(), simEngine, multiModalTravelTime);
		
		return simEngine;
	}
	
	private void addMultiModalToQNetwork(QNetworkI network, MultiModalSimEngine simEngine, MultiModalTravelTime multiModalTravelTime) {
		for (QNode node : network.getNodes().values()) {
			MultiModalQNodeExtension extension = new MultiModalQNodeExtension(node.getNode(), simEngine);
			node.getCustomAttributes().put(MultiModalQNodeExtension.class.getName(), extension);
		}
		
		for (QLink link : network.getLinks().values()) {
			QNode toNode = link.getToQueueNode();
			MultiModalQLinkExtension extension = new MultiModalQLinkExtension(link, simEngine, toNode, multiModalTravelTime);
			link.getCustomAttributes().put(MultiModalQLinkExtension.class.getName(), extension);
		}
		
		for (QNode node : network.getNodes().values()) {
			MultiModalQNodeExtension extension = simEngine.getMultiModalQNodeExtension(node);
			extension.init();
		}

	}
}
