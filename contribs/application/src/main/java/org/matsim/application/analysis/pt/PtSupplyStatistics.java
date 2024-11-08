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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.table.TableSliceGroup;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

@CommandLine.Command(
	name = "transit-supply", description = "Analysis of public transit supply scheduled.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"pt_departures_at_stops_per_hour_per_mode.csv","pt_count_unique_ids_per_mode.csv"
	}
)
public class PtSupplyStatistics implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PtSupplyStatistics.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PtSupplyStatistics.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PtSupplyStatistics.class);

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createStatistics(scenario.getTransitSchedule());

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

	private void createStatistics(TransitSchedule transitSchedule) {
		/*
		 * creates a table very similar to PtStop2StopAnalysis but based only on run inputs.
		 * Therefore, it contains all scheduled services even if the simulation ended before the last pt service operated.
		 * Deliberately using transportMode and not gtfsRouteType since the latter might be missing, especially if gtfs is not the source.
		 */
		Table departuresFromStops = Table.create("DeparturesAtStops").addColumns(
			StringColumn.create("transitLine"),
			StringColumn.create("transitRoute"),
			StringColumn.create("departure"),
			StringColumn.create("stop"),
			IntColumn.create("stopSequence"),
			StringColumn.create("stopPrevious"),
			DoubleColumn.create("arrivalTimeScheduled"),
			DoubleColumn.create("departureTimeScheduled"),
			StringColumn.create("transportMode"),
			StringColumn.create("gtfsRouteType"),
			IntColumn.create("departureHour")
			// TODO: could add linkIds, however only as a single String since no list column is available
		);

		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					int stopSequence = 0;
					Id<TransitStopFacility> previousStop = null;
					for (TransitRouteStop transitRouteStop : route.getStops()) {
						Row row = departuresFromStops.appendRow();
						row.setString("transitLine", line.getId().toString());
						row.setString("transitRoute", route.getId().toString());
						row.setString("departure", departure.getId().toString());
						row.setString("stop", transitRouteStop.getStopFacility().getId().toString());
						row.setInt("stopSequence", stopSequence);
						row.setString("stopPrevious", previousStop == null ? "" : previousStop.toString());
						double arrivalTime = departure.getDepartureTime() + transitRouteStop.getArrivalOffset().orElse(0.0);
						row.setDouble("arrivalTimeScheduled", arrivalTime);
						double departureTime = departure.getDepartureTime() + transitRouteStop.getDepartureOffset().orElse(0.0);
						row.setDouble("departureTimeScheduled", departureTime);
						row.setString("transportMode", route.getTransportMode());
						Object attr = line.getAttributes().getAttribute("gtfs_route_type");
						if (attr != null) {
							row.setString("gtfsRouteType", (String) attr);
						} else {
							row.setMissing("gtfsRouteType");
						}
						row.setInt("departureHour", (int) departureTime / 3600);

						stopSequence++;
						previousStop = transitRouteStop.getStopFacility().getId();
					}
				}
			}
		}

		Table countsPerMode = departuresFromStops.
			summarize("transitLine", "transitRoute", "departure", "stop", countUnique).
			by("transportMode").sortOn("transportMode");
		// rename column names to Matsim TransitSchedule class names
		countsPerMode.column("transportMode").setName("TransportMode");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("transitLine", countUnique.functionName())).setName("TransitLines");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("transitRoute", countUnique.functionName())).setName("TransitRoutes");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("departure", countUnique.functionName())).setName("Departures");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("stop", countUnique.functionName())).setName("TransitStopFacilities");

		countsPerMode.reorderColumns("TransportMode","TransitLines","TransitRoutes","Departures","TransitStopFacilities");
		countsPerMode.write().csv(output.getPath("pt_count_unique_ids_per_mode.csv").toString());

		// Count number of departures per hour and mode (summing up over all stops)
		Table departuresPerHourAndMode = departuresFromStops.countBy("departureHour","transportMode").
			sortOn("departureHour","transportMode");
		departuresPerHourAndMode.write().csv(output.getPath("pt_departures_at_stops_per_hour_per_mode.csv").toString());
	}
}
