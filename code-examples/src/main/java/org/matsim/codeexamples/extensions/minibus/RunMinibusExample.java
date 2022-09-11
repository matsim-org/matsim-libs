/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.extensions.minibus;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.RunMinibus;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunMinibusExample {
	private final static Logger log = LogManager.getLogger(RunMinibus.class);

	public static void main(final String[] args) {

		if(args.length == 0){
			log.info("Arg 1: config.xml is missing.");
			log.info("Check https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/config.xml for an example.");
			System.exit(1);
		}

		Config config = ConfigUtils.loadConfig( args[0], new PConfigGroup() ) ;
		
//		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.GROUP_NAME, PConfigGroup.class ) ;
//		
//		pConfig.getCostPerKilometer() ;

		Scenario scenario = ScenarioUtils.loadScenario(config);


		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setCreateGraphs(false);
		
		controler.addOverridingModule(new PModule()) ;

		controler.run();
	}		
}
