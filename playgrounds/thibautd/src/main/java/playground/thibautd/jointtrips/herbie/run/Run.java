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
package playground.thibautd.jointtrips.herbie.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import herbie.running.config.HerbieConfigGroup;

import playground.thibautd.analysis.listeners.CliqueScoreStats;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.jointtrips.population.ScenarioWithCliques;
import playground.thibautd.jointtrips.utils.JointControlerUtils;

/**
 * @author thibautd
 */
public class Run {
	/**
	 * run the simulation.
	 * @param args the config file to use.
	 */
	public static void main(String[] args) {
		String configFile = args[0];

		Config conf = ConfigUtils.createConfig();
		conf.addModule( HerbieConfigGroup.GROUP_NAME , new HerbieConfigGroup() );
		JointControlerUtils.loadConfig( conf , configFile );
		ScenarioWithCliques sc = JointControlerUtils.createScenario(configFile); 
		Controler controler = new JointHerbieControler( sc );
		controler.addControlerListener(new CliqueScoreStats(
					"scoresStats",
					true));
		controler.addControlerListener(new ModeAnalysis( true ));
		//controler.setOverwriteFiles(true);
		//controler.addControlerListener(new JointReplanningControlerListener());

		controler.run();
	}

}

