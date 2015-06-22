/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightController2013
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
package air.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterModule;

/**
 * Runs the MATSim Controler with some additions for the flight model.
 * @author dgrether
 * 
 */
public class FlightController {
	
	private static final Logger log = Logger.getLogger(FlightController.class);
	
	public void run(Scenario scenario) {
		Controler controler = new Controler(scenario);
		FlightConfigModule flightConfig = new FlightConfigModule(controler.getConfig());
		controler.addOverridingModule(new OTFVisModule());
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		ControlerListener lis = new SfFlightTimeControlerListener();
		controler.addControlerListener(lis);
		if (flightConfig.doRandomizedTTAndDisutilityRouting()) {
			controler.addOverridingModule(new RandomizedTransitRouterModule());
			log.info("Enabled RandomizedTravelTimeAndDisutilityRouting...");
		}
		if (flightConfig.doRerouteStuckedPersons()){
			controler.addControlerListener(new FlightStuckedReplanning());
			log.info("Switched on flight stucked replanning...");
		}
		controler.run();
	}
	
	
	public static void main(String[] args) {
//		String configFilePath  = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml";
//		String configFilePath = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/flight_one_line_mode_choice/air_config_mode_choice_type.xml";
		String configFilePath = args[0];
		Config config = ConfigUtils.loadConfig(configFilePath);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new FlightController().run(scenario);
	}

}
