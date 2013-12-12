/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngineFactory.java
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

package playground.christoph.mobsim.modularqsimfactory;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class QNetsimEngineFactory implements MobsimEngineFactory {

	private final static Logger log = Logger.getLogger(QNetsimEngineFactory.class);
	
	@Override
	public MobsimEngine createMobsimSimEngine(Netsim sim) {
		
		Config config = sim.getScenario().getConfig();
		QSimConfigGroup conf = config.qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		if (numOfThreads > 1) {
			log.info("Using parallel QSim with " + numOfThreads + " threads. +" +
					"Please ensure that a EventsManager is used which can handle parallel ocurring events.");
			// TODO: ParallelQNetsimEngine is package protected
//			return new ParallelQNetsimEngine((QSim) sim);
			return null;
		} else {
			return new QNetsimEngine((QSim) sim);
		}
	}

}
