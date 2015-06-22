/* *********************************************************************** *
 * project: org.matsim.*
 * FlightControllerPSRemove
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

import air.pathsize.PathSizeLogitSelector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterModule;

import java.util.HashSet;
import java.util.Set;


/**
 * @author dgrether
 *
 */
public class FlightControllerPSRemove {
	
	private static final Logger log = Logger.getLogger(FlightControllerPSRemove.class);
	
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

		double logitScaleFactor = scenario.getConfig().planCalcScore().getBrainExpBeta() ;
		double pathSizeLogitExponent = scenario.getConfig().planCalcScore().getPathSizeLogitBeta() ; 
		String modes = scenario.getConfig().getModule("changeLegMode").getParams().get("modes");
		String[] modesArray = modes.split(",");
		Set<String> mainModes = new HashSet<String>();
		for (String mainMode : modesArray) {
			mainModes.add(mainMode);
			log.info("using main mode: " + mainMode);
		}
		final PlanSelector planSelector = new PathSizeLogitSelector( pathSizeLogitExponent, logitScaleFactor, mainModes);
//		PlanSelector planSelector = new RandomPlanSelector() ;

		controler.addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getStrategyManager().setPlanSelectorForRemoval(planSelector);
			}
			
		});
		controler.run();
	}
	
	
	public static void main(String[] args) {
//		String configFilePath  = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml";
//		String configFilePath = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/flight_one_line_mode_choice/air_config_mode_choice_ps_remove.xml";
		String configFilePath = args[0];
		Config config = ConfigUtils.loadConfig(configFilePath);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FlightControllerPSRemove ctrl = new FlightControllerPSRemove();
		ctrl.run(scenario);
		
	}
}
