/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example script that shows how to use railsim included in this contrib.
 */
public class RunRailsimExample {

	// TODO: Vehicle
	// vehicle should start on link end and go directly to next
	// vehicle drives the last link completely

	// TODO: Zwischenebene mit Segmenten

	// TODO: blockId attribute für links

	// TODO: Kreuzungsweiche, Knoten mit Belegungslogik

	// TODO: Alle x Sekunden links vorreservieren

	// 1 extrem fall Reservation für ganze strecke im vorraus

	// bei train departure wird zukünftiger fahrweg mit übergeben

	// Disposition interface

	// TODO: tail link umstellen auf entfernung from node
	// head position = 0, link länge - zug länge

	// TODO: erste link darf auch negativ sein
	// run railsim example

	public static void main(String[] args) {

		if (args.length == 0) {
			System.err.println("Path to config is required as first argument.");
			System.exit(2);
		}

		String configFilename = args[0];
		Config config = ConfigUtils.loadConfig(configFilename);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> {
			new RailsimQSimModule().configure(components);
			// if you have other extensions that provide QSim components, call their configure-method here
		});

		controler.run();
	}

}
