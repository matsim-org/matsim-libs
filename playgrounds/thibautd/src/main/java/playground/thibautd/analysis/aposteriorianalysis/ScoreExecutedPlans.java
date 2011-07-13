/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreExecutedPlans.java
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
package playground.thibautd.analysis.aposteriorianalysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationWriter;

import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.scoring.JointEventsToScore;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Aims at getting the scores of executed plans for a given iteration for which
 * an event file and a plan file is available.
 *
 * @author thibautd
 */
public class ScoreExecutedPlans {
	public static void main(final String[] args) {
		String configFile = args[0];
		String inputEvents = args[1];
		String outputFile = args[2];

		Controler controler = JointControlerUtils.createControler(configFile);
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(
				new JointEventsToScore(
					(PopulationWithCliques) scenario.getPopulation(),
					controler.getScoringFunctionFactory()));
		(new MatsimEventsReader(eventsManager)).readFile(inputEvents);
		(new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())).write(outputFile);
	}
}

