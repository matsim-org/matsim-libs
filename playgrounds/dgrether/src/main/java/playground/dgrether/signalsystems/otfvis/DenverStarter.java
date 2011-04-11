/* *********************************************************************** *
 * project: org.matsim.*
 * DenverStarter
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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DenverStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile = DgPaths.STUDIESDG + "denver/dgConfig.xml";
		
		ScenarioLoaderImpl scl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile);
		Scenario sc = scl.loadScenario();
		EventsManager e = (EventsManager) EventsUtils.createEventsManager();
		QSim otfVisQSim = new QSim(sc, e);
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(otfVisQSim);
		otfVisQSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(sc.getConfig().otfVis().isShowTeleportedAgents());
		
		QSim sim = otfVisQSim;
		sim.run();
		
		
	}

}
