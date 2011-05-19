/* *********************************************************************** *
 * project: org.matsim.*
 * JointControlerUtils.java
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
package playground.thibautd.jointtripsoptimizer.utils;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.jointtripsoptimizer.population.CliquesXmlReader;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.run.config.CliquesConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.JointControler;

/**
 * Helper class to create a fully configured jointcontroler from a config file
 * @author thibautd
 */
public class JointControlerUtils {
	/**
	 * @return a JointControler instance, ready for running.
	 */
	public static Controler createControler(final String configFile) {
		JointReplanningConfigGroup jointConfigGroup = new JointReplanningConfigGroup();
		CliquesConfigGroup cliquesConfigGroup = new CliquesConfigGroup();

		Config config = ConfigUtils.createConfig();
		ScenarioWithCliques scenario = null;
		Controler controler = null;

		// /////////////////////////////////////////////////////////////////////
		// initialize the config before passing it to the controler
		config.addCoreModules();
		config.addModule(JointReplanningConfigGroup.GROUP_NAME, jointConfigGroup);
		config.addModule(CliquesConfigGroup.GROUP_NAME, cliquesConfigGroup);

		//read the config file
		ConfigUtils.loadConfig(config, configFile);

		scenario = new ScenarioWithCliques(config);

		//(new ScenarioLoaderImpl(scenario)).loadScenario();
		ScenarioUtils.loadScenario(scenario);

		try {
			new CliquesXmlReader(scenario).parse();
		} catch (Exception e) {
			throw new RuntimeException("Problem while importing clique information", e);
		}

		controler = new JointControler(scenario);
		setScoringFunction(controler);

		return controler;
	}

	/**
	 * In case facilities are defined, sets the scoring function factory to
	 * {@link CharyparNagelOpenTimesScoringFunctionFactory}
	 */
	private static void setScoringFunction(final Controler controler) {
		ActivityFacilities facilities = controler.getFacilities();
		int nFacilities = facilities.getFacilities().size();

		if (nFacilities > 0) {
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup = 
				controler.getConfig().planCalcScore();
			ScoringFunctionFactory factory =
				new CharyparNagelOpenTimesScoringFunctionFactory(
						planCalcScoreConfigGroup,
						facilities);
			controler.setScoringFunctionFactory(factory);
		}
	}
}

