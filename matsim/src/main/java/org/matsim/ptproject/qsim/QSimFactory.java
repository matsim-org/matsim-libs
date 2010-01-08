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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.IOSimulation;
import org.matsim.core.mobsim.MobsimFactory;


/**
 * @author dgrether
 *
 */
public class QSimFactory implements MobsimFactory {

  private static final Logger log = Logger.getLogger(QSimFactory.class);
  
  @Override
  public IOSimulation createMobsim(Scenario sc, EventsManager eventsManager) {
    QueueSimulation sim = new QueueSimulation(sc, eventsManager);
    Config config = sc.getConfig();
    if (config.scenario().isUseLanes()) {
      if (((ScenarioImpl)sc).getLaneDefinitions() == null) {
        throw new IllegalStateException("Lane definition have to be set if feature is enabled!");
      }
      sim.setLaneDefinitions(((ScenarioImpl)sc).getLaneDefinitions());
    }
    if (config.scenario().isUseSignalSystems()) {
      if ((((ScenarioImpl)sc).getSignalSystems() == null)
          || (((ScenarioImpl)sc).getSignalSystemConfigurations() == null)) {
        throw new IllegalStateException(
            "Signal systems and signal system configurations have to be set if feature is enabled!");
      }
      sim.setSignalSystems(((ScenarioImpl)sc).getSignalSystems(), ((ScenarioImpl)sc).getSignalSystemConfigurations());
    }
    log.info("using QSim...");
    return sim;
  }

}
