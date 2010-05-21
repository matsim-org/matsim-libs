/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
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

package org.matsim.pt.qsim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimI;



public class TransitQSimulation extends QSim implements QSimI {
	// this is the one hook that needs to be there into the QSim 
	// (although one could consider moving this class into the QSim package).  kai, may'10

	private static final Logger log = Logger.getLogger(TransitQSimulation.class);
	
	public TransitQSimulation(Scenario scenario, EventsManager events) {
		super(scenario, events);
		if (scenario.getConfig().getQSimConfigGroup() == null){
		  scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		  log.warn("using transit without specifying QSimConfigGroup/Module in config. A default" +
		  		"QSimConfigModule is added to the config, values in simulation config module are ignored!");
		}
		if (!scenario.getConfig().scenario().isUseTransit()){
			installFeatures();
			log.warn("Transit is enabled via TransitQSimulation but without enabling" +
					"transit features in the scenario config group of the Scenario instance" +
					"used to start the QSim. Try to modify your code in a way that" +
					"the isUseTransit flag is set to true in the config before QSim is initialized. The" +
					"Code producing this warning enables transit even if it is not switched on in the config, " +
					"however this functionality will be removed somewhen!");
		}
	}

	private void installFeatures() {
		transitEngine = new TransitQSimEngine(this);
		super.addDepartureHandler(transitEngine);
//		super.addFeature(transitEngine);
	}

	public TransitStopAgentTracker getAgentTracker() {
		return transitEngine.getAgentTracker();
	}
	
	public void setUseUmlaeufe(boolean useUmlaeufe) {
		transitEngine.setUseUmlaeufe(useUmlaeufe);
	}
	
	public void setTransitStopHandlerFactory(TransitStopHandlerFactory stopHandlerFactory) {
		transitEngine.setTransitStopHandlerFactory(stopHandlerFactory);
	}
	
}
