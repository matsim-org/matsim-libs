/* *********************************************************************** *
 * project: org.matsim.*
 * PtController
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
package org.matsim.ptproject.controller;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationListener;
import org.matsim.ptproject.qsim.QueueSimulation;


/**
 * @author dgrether
 *
 */
public class PtController extends Controler {

	public PtController(Config config) {
		super(config);
	}
	public PtController(final String[] args) {
		super(args);
	}

	public PtController(final String configFileName) {
		super(configFileName);
	}

	public PtController(final ScenarioImpl scenario) {
		super(scenario);
	}
	
	@Override
	protected void runMobSim() {
		QueueSimulation sim = new QueueSimulation(this.scenarioData, this.events);
		
		for (QueueSimulationListener l : this.getQueueSimulationListener()) {
			sim.addQueueSimulationListeners(l);
		}
		sim.setUseActivityDurations(this.getConfig().vspExperimental().isUseActivityDurations());
		if (this.config.scenario().isUseLanes()) {
			if (this.scenarioData.getLaneDefinitions() == null) {
				throw new IllegalStateException("Lane definition have to be set if feature is enabled!");
			}
			sim.setLaneDefinitions(this.scenarioData.getLaneDefinitions());
		}
		if (this.config.scenario().isUseSignalSystems()) {
			if ((this.scenarioData.getSignalSystems() == null)
					|| (this.scenarioData.getSignalSystemConfigurations() == null)) {
				throw new IllegalStateException(
						"Signal systems and signal system configurations have to be set if feature is enabled!");
			}
			sim.setSignalSystems(this.scenarioData.getSignalSystems(), this.scenarioData.getSignalSystemConfigurations());
		}
		sim.run();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final PtController controler = new PtController(args);
			controler.run();
		}
		System.exit(0);
	}

}
