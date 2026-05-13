/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.simwrapper.DbViewer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.sql.SQLException;

/**
 * This class contains a main method to call the
 * example event handler for Select Link Analysis.
 *
 * @author brendan-lawton
 */
public class RunEventsHandlingExample {
	public static void main(String[] args) throws SQLException {

		final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(2);
		config.controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists // ← add this
		);

		config.controller().setOutputDirectory("/home/brendan/git/matsim-libs/contribs/simwrapper/test/output/org/matsim/simwrapper/dashboard/SelectLinkAnalysis/");
		String inputFile = "output_events.xml.zst";

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new DbEventsModule(config.controller().getOutputDirectory()));
			}
		});

		controler.run();
//
//		//create the reader and read the file
//		EventsManager events = EventsUtils.createEventsManager();
//		events.initProcessing();
//		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile(config.controller().getOutputDirectory() + inputFile);
//		events.finishProcessing();
//
//		System.out.println("Events file read!");
	}

}
