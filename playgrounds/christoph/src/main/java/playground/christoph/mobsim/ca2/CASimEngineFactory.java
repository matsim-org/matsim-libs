/* *********************************************************************** *
 * project: org.matsim.*
 * CASimEngineFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

public class CASimEngineFactory implements MatsimFactory {

	final private static Logger log = Logger.getLogger(CASimEngineFactory.class);
	
	public CASimEngine createCASimEngine(Netsim sim, double spatialResolution) {
		
		CASimEngine simEngine;
		
		int numOfThreads = sim.getScenario().getConfig().qsim().getNumberOfThreads(); 
		if (numOfThreads > 1) {
			simEngine = new ParallelCASimEngine(sim, spatialResolution);
			log.info("Using ParallelCASimEngine with " + numOfThreads + " threads.");
		}
		else {
			simEngine = new CASimEngine(sim, spatialResolution);
		}		
		return simEngine;
	}
}
