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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@CommandLine.Command(
	name = "transit", description = "General public transit analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"pt_pax_volumes.csv.gz",
		"pt_pax_per_hour_and_vehicle_type.csv",
		"pt_pax_per_hour_and_vehicle_type_and_agency.csv"
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
		new PublicTransitAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory()).toString();

		PtStop2StopAnalysis ptStop2StopEventHandler = new PtStop2StopAnalysis(scenario.getTransitVehicles(), sample.getUpscaleFactor());
		PtPassengerCountsEventHandler passengerCountsHandler = new PtPassengerCountsEventHandler(scenario.getTransitSchedule(), scenario.getTransitVehicles());

		eventsManager.addHandler(ptStop2StopEventHandler);
		eventsManager.addHandler(passengerCountsHandler);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		ptStop2StopEventHandler.writeStop2StopEntriesByDepartureCsv(output.getPath("pt_pax_volumes.csv.gz"),
			",", ";");

		writePassengerCounts(passengerCountsHandler);

		return 0;
	}

	private void writePassengerCounts(PtPassengerCountsEventHandler handler) {

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath("pt_pax_per_hour_and_vehicle_type.csv"), StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {

			csv.printRecord("vehicle_type", "hour", "passenger_count");
			for (Int2ObjectMap.Entry<Object2IntMap<Id<VehicleType>>> kv : handler.getCounts().int2ObjectEntrySet()) {
				for (Object2IntMap.Entry<Id<VehicleType>> vc : kv.getValue().object2IntEntrySet()) {
					csv.printRecord(vc.getKey(), kv.getIntKey(), vc.getIntValue() * sample.getUpscaleFactor());
				}
			}

		} catch (IOException e) {
			log.error("Error writing passenger counts.", e);
		}


		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath("pt_pax_per_hour_and_vehicle_type_and_agency.csv"), StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {

			csv.printRecord("vehicle_type", "agency", "hour", "passenger_count");
			for (Int2ObjectMap.Entry<Object2IntMap<PtPassengerCountsEventHandler.AgencyVehicleType>> kv : handler.getAgencyCounts().int2ObjectEntrySet()) {
				for (Object2IntMap.Entry<PtPassengerCountsEventHandler.AgencyVehicleType> vc : kv.getValue().object2IntEntrySet()) {
					csv.printRecord(vc.getKey().vehicleType(), vc.getKey().agency(), kv.getIntKey(), vc.getIntValue() * sample.getUpscaleFactor());
				}
			}

		} catch (IOException e) {
			log.error("Error writing passenger counts.", e);
		}


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
