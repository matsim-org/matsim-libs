/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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

package org.matsim.application.analysis.pt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

@CommandLine.Command(
	name = "transit", description = "General public transit analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"pt_pax_volumes.csv.gz",
	}
)
public class PublicTransitAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PublicTransitAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PublicTransitAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PublicTransitAnalysis.class);
	@CommandLine.Mixin
	private SampleOptions sample;

	public static void main(String[] args) {
		new AirPollutionAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory()).toString();

		PtStop2StopAnalysis ptStop2StopEventHandler = new PtStop2StopAnalysis(scenario.getTransitVehicles(), sample.getUpscaleFactor());
		eventsManager.addHandler(ptStop2StopEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		ptStop2StopEventHandler.writeStop2StopEntriesByDepartureCsv(output.getPath("pt_pax_volumes.csv.gz"),
			",", ";");

		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());

		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}
}
