/* *********************************************************************** *
 * project: org.matsim.*
 * run.java
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
package playground.thibautd.jointtripsoptimizer.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.thibautd.jointtripsoptimizer.population.CliquesXmlReader;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;

import playground.thibautd.jointtripsoptimizer.run.config.CliquesConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Class responsible of running the custom controler for joint planning
 * @author thibautd
 */
public class run {
	/**
	 * run the simulation.
	 * @param args the config file to use.
	 */
	public static void main(String[] args) {
		String configFile = args[0];
		JointReplanningConfigGroup jointConfigGroup = new JointReplanningConfigGroup();
		CliquesConfigGroup cliquesConfigGroup = new CliquesConfigGroup();

		Config config = new Config();
		JointControler controler = null;
		ScenarioWithCliques scenario = null;

		// /////////////////////////////////////////////////////////////////////
		// initialize the config before passing it to the controler
		config.addCoreModules();
		config.addModule(JointReplanningConfigGroup.GROUP_NAME, jointConfigGroup);
		config.addModule(CliquesConfigGroup.GROUP_NAME, cliquesConfigGroup);

		//read the config file
		//(new MatsimConfigReader(controler.getConfig())).readFile(args[0]);
		(new MatsimConfigReader(config)).readFile(configFile);

		scenario = new ScenarioWithCliques(config);

		(new ScenarioLoaderImpl(scenario)).loadScenario();
		try {
			new CliquesXmlReader(scenario).parse();
		} catch (Exception e) {
			throw new RuntimeException("Problem while importing clique information:"
					+" "+e.getMessage());
		}

		controler = new JointControler(scenario);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new JointReplanningControlerListener());
		controler.run();
	}
}
