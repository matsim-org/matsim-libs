/* *********************************************************************** *
 * project: org.matsim.*
 * RunIndivSimulation.java
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
package playground.thibautd.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.analysis.listeners.ModeAnalysis;

/**
 * loads a config, sets the scoring function and some listeners, and runs the
 * simulation with the default controler.
 *
 * @author thibautd
 */
public class RunIndivSimulation {
	public static void main(final String[] args) {
		String configFile = args[0];

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		//controler.addControlerListener(new CliqueScoreStats(
		//			"scoresStats",
		//			true));
		controler.addControlerListener(new ModeAnalysis());
		//controler.setOverwriteFiles(true);
		//controler.addControlerListener(new JointReplanningControlerListener());

		setScoringFunction(controler);
		controler.run();
	}

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

