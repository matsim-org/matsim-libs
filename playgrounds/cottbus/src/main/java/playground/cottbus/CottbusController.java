/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsConsistencyChecker
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
package playground.cottbus;

/**
 * @author 	rschneid-btu
 * start-up for our scenarios
 */

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class CottbusController {
	
	public static void main (String[] args) {
		String scenario = "cottbus";
//		String scenario = "denver";
//		String scenario = "portland";
						
		String config = "./input/"+scenario+"/config.xml";
		// configuration that describes current scenario
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		// effects output-folder
		controler.run();
		
		// @deprecated output via NetVis:
		//String[] visargs = {"output/denver/ITERS/it.0/Snapshot"};
		//NetVis.main(visargs);
		
		// visualization via OTFVis
		Config conf = ConfigUtils.loadConfig(config);
		MatsimRandom.reset(conf.global().getRandomSeed());
		Scenario sc = ScenarioUtils.createScenario(conf);
		ScenarioUtils.loadScenario(sc);
		
		EventsManager e = EventsUtils.createEventsManager();
		QSim otfVisQSim = QSimUtils.createDefaultQSim(sc, e);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, e, otfVisQSim);
		OTFClientLive.run(sc.getConfig(), server);
		otfVisQSim.run();
		
		
	}
}