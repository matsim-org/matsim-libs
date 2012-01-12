/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * @author thibautd
 */
public class Run {
	public static void main(final String[] args) {
		String configFile = args[0];

		Config config = ConfigUtils.createConfig();
		config.addModule( Planomat2ConfigGroup.GROUP_NAME , new Planomat2ConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addControlerListener(new ModeAnalysis( true ));

		controler.run();
	}
}

