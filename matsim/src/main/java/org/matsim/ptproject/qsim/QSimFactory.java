/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFactory
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
package org.matsim.ptproject.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;


/**The MobsimFactory is necessary so that something can be passed to the controler which instantiates this.
 * Can (presumably) be something much more minimalistic than QSimI.  kai, jun'10
 *
 * @author dgrether
 *
 */
public class QSimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(QSimFactory.class);

	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {
		if (sc.getConfig().getQSimConfigGroup() == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		if (sc.getConfig().getQSimConfigGroup().getNumberOfThreads() > 1) {
			SynchronizedEventsManagerImpl em = new SynchronizedEventsManagerImpl(eventsManager);
		  ParallelQSimulation sim = new ParallelQSimulation(sc, em);

		  // Get number of parallel Threads
		  QSimConfigGroup conf = (QSimConfigGroup) sc.getConfig().getModule(QSimConfigGroup.GROUP_NAME);
		  int numOfThreads = conf.getNumberOfThreads();
		  log.info("Using parallel QSim with " + numOfThreads + " threads.");

		  return sim;
		}

		QSim sim = new QSim(sc, eventsManager);
		if (sc.getConfig().scenario().isUseTransit()) {
			sim.getTransitEngine().setUseUmlaeufe(true);
			sim.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
		}
		return sim;
	}

}
