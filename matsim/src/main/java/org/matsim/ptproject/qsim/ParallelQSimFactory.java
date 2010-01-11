/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelQSimFactory
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
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimFactory;


/**
 * @author dgrether
 *
 */
public class ParallelQSimFactory implements MobsimFactory {
  
  private static final Logger log = Logger.getLogger(ParallelQSimFactory.class);
  
  @Override
  public IOSimulation createMobsim(Scenario sc, EventsManager eventsManager) {
	  SynchronizedEventsManagerImpl em = new SynchronizedEventsManagerImpl(eventsManager);
	  ParallelQueueSimulation sim = new ParallelQueueSimulation(sc, em);
	  
	  // Get number of parallel Threads
	  QSimConfigGroup conf = (QSimConfigGroup) sc.getConfig().getModule(QSimConfigGroup.GROUP_NAME);
	  int numOfThreads = conf.getNumberOfThreads();
	  log.info("Using parallel QSim with " + numOfThreads + " parallel Threads.");
	  
	  return sim;
  }

}
