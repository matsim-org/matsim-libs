/* *********************************************************************** *
 * project: org.matsim.*
 * TaMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.laemmer.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerSignalsModule;


/**
 * @author dgrether
 * @author tthunig
 */
public class LaemmerMain {
  
	private static final Logger log = Logger.getLogger(LaemmerMain.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Running Laemmer main method...");
		if (args == null || args.length == 0){
			log.info("No args given, running local config...");
			args = new String[1];
			args[0] = "../../playgrounds/tthunig/examples/laemmer/config.xml";
		}

		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new LaemmerSignalsModule());
		//controler.addOverridingModule(new CombinedSignalsModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();

	}

}
