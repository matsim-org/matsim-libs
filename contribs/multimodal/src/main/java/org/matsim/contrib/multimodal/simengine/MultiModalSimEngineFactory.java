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

package org.matsim.contrib.multimodal.simengine;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.util.TravelTime;

public class MultiModalSimEngineFactory implements MatsimFactory {

	final private static Logger log = Logger.getLogger(MultiModalSimEngineFactory.class);
	
	public MultiModalSimEngine createMultiModalSimEngine(Netsim sim, Map<String, TravelTime> map) {
		
		MultiModalSimEngine simEngine;
		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) sim.getScenario().getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		
		int numOfThreads = multiModalConfigGroup.getNumberOfThreads(); 
		if (numOfThreads > 1) {
			simEngine = new ParallelMultiModalSimEngine(sim, map);
			log.info("Using ParallelMultiModalSimEngine with " + numOfThreads + " threads.");
		}
		else {
			simEngine = new MultiModalSimEngine(sim, map);
		}		
		return simEngine;
	}
}
