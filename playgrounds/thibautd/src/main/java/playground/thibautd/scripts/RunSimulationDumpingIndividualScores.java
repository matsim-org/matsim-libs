/* *********************************************************************** *
 * project: org.matsim.*
 * RunSimulationDumpingIndividualScores.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.analysis.listeners.IndividualExecutedScoreDumper;

/**
 * @author thibautd
 */
public class RunSimulationDumpingIndividualScores {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Config config = ConfigUtils.loadConfig( configFile );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( scenario );
		controler.addControlerListener(
				new IndividualExecutedScoreDumper(
					scenario.getPopulation().getPersons().values(),
					config.controler().getOutputDirectory()+"/scores.dat" ) );
		controler.run();
	}
}

