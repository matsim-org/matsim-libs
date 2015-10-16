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
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

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
		
		Config config = ConfigUtils.loadConfig(configFile);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		EventsManager e = EventsUtils.createEventsManager();
		QSim otfVisQSim = QSimUtils.createDefaultQSim(scenario, e);
			
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, e, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
		
	}

}
