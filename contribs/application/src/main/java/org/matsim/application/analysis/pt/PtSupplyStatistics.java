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
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.AnalysisUtils;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.*;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.DoubleColumnType;
import tech.tablesaw.table.TableSliceGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

@CommandLine.Command(
	name = "transit-supply", description = "Analysis of public transit supply scheduled.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"pt_departures_at_stops_per_hour_per_mode.csv", "pt_count_unique_ids_per_mode.csv",
		"pt_active_transit_lines_per_hour_per_mode_per_area.csv", "pt_active_transit_stops_per_hour_per_mode_per_area.csv",
		"pt_headway_per_stop_area_pair_and_hour.csv", "pt_headway_per_line_stop_area_pair_and_hour.csv",
		"pt_headway_per_line_and_hour.csv", "pt_headway_per_mode_and_hour.csv", "pt_headway_group_per_mode_and_hour.csv"
	}
)
public class PtSupplyStatistics implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PtSupplyStatistics.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PtSupplyStatistics.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PtSupplyStatistics.class);

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new PtSupplyStatistics().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createStatistics(scenario.getTransitSchedule(), config.global().getCoordinateSystem());

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

	private void createStatistics(TransitSchedule transitSchedule, String coordinateSystem) {
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
			IntColumn.create("departureHour"),
			StringColumn.create("shpFilter"),
			StringColumn.create("stopAreaOrStop") // attempt to aggregate nearby stops or read parent stop id from gtfs
			// TODO: could add linkIds, however only as a single String since no list column is available
		);

		// prepare filtering by shape file, e.g. in Berlin vs. outside Berlin
		Geometry filterGeometry = null;
		if (shp.isDefined()) {
			filterGeometry = shp.getGeometry(coordinateSystem);
		}

		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					int stopSequence = 0;
					Id<TransitStopFacility> previousStop = null;
					for (TransitRouteStop transitRouteStop : route.getStops()) {
						TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

						Row row = departuresFromStops.appendRow();
						row.setString("transitLine", line.getId().toString());
						row.setString("transitRoute", route.getId().toString());
						row.setString("departure", departure.getId().toString());
						row.setString("stop", stopFacility.getId().toString());
						row.setInt("stopSequence", stopSequence);
						row.setString("stopPrevious", previousStop == null ? "" : previousStop.toString());
						double arrivalTime = departure.getDepartureTime() + transitRouteStop.getArrivalOffset().orElse(0.0);
						row.setDouble("arrivalTimeScheduled", arrivalTime);
						double departureTime = departure.getDepartureTime() + transitRouteStop.getDepartureOffset().orElse(0.0);
						row.setDouble("departureTimeScheduled", departureTime);
						row.setString("transportMode", route.getTransportMode());
						Object lineAttr = line.getAttributes().getAttribute("gtfs_route_type");
						if (lineAttr != null) {
							row.setString("gtfsRouteType", (String) lineAttr);
						} else {
							row.setMissing("gtfsRouteType");
						}
						row.setInt("departureHour", (int) departureTime / 3600);

						if (filterGeometry != null) {
							if (filterGeometry.contains(MGC.coord2Point(stopFacility.getCoord()))) {
								row.setString("shpFilter", "in area");
							} else {
								row.setString("shpFilter", "outside area");
							}
						} else {
							row.setString("shpFilter", "no area defined");
						}

						if (stopFacility.getStopAreaId() != null) {
							row.setString("stopAreaOrStop", stopFacility.getStopAreaId().toString());
						} else {
							row.setString("stopAreaOrStop", stopFacility.getId().toString());
						}

						stopSequence++;
						previousStop = transitRouteStop.getStopFacility().getId();
					}
				}
			}
		}

		Table countsAllModes = departuresFromStops
		.summarize(List.of("transitLine", "transitRoute", "departure", "stop", "stopAreaOrStop"), countUnique).apply()
		.addColumns(StringColumn.create("transportMode","allModes"));
		Table countsPerMode = departuresFromStops
			.summarize(List.of("transitLine", "transitRoute", "departure", "stop", "stopAreaOrStop"), countUnique)
			.by("transportMode").sortOn("transportMode");
		countsPerMode = countsAllModes.append(countsPerMode);
		// rename column names to Matsim TransitSchedule class names
		countsPerMode.column("transportMode").setName("TransportMode");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("transitLine", countUnique.functionName())).setName("TransitLines");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("transitRoute", countUnique.functionName())).setName("TransitRoutes");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("departure", countUnique.functionName())).setName("Departures");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("stop", countUnique.functionName())).setName("TransitStopFacilities");
		countsPerMode.column(TableSliceGroup.aggregateColumnName("stopAreaOrStop", countUnique.functionName())).setName("TransitStopAreasAndSingleStops");

		countsPerMode = countsPerMode.reorderColumns("TransportMode", "TransitLines", "TransitRoutes", "Departures", "TransitStopFacilities", "TransitStopAreasAndSingleStops");
		countsPerMode.write().csv(output.getPath("pt_count_unique_ids_per_mode.csv").toString());

		// Count number of departures per hour and mode (summing up over all stops)
		Table departuresPerHourAndMode = departuresFromStops.countBy("departureHour", "transportMode")
			.sortOn("departureHour", "transportMode");
		departuresPerHourAndMode.write().csv(output.getPath("pt_departures_at_stops_per_hour_per_mode.csv").toString());

		// Share of active transit lines (at least one departure) per hour and mode
		/*
		 * We could only look at departures at first stop and thereby ignore departures departed earlier and still on their way.
		 * However, this makes spacial filtering complicated. If a line starts and ends outside but passes through the shape filtering by
		 * stopSequence == 0 will make it appear as if it was not operating inside the area.
		 * where(t -> t.intColumn("stopSequence").isEqualTo(0)).
		 */
		Table activeTransitLinesPerHourAndArea = departuresFromStops
			.summarize("transitLine", countUnique)
			.by("shpFilter", "departureHour", "transportMode")
			.sortOn("shpFilter", "departureHour", "transportMode");

		activeTransitLinesPerHourAndArea
			.column(TableSliceGroup.aggregateColumnName("transitLine", countUnique.functionName()))
			.setName("activeTransitLinesInHourAndArea");

		Table transitLinesPerArea = departuresFromStops
			.summarize("transitLine", countUnique)
			.by("shpFilter", "transportMode")
			.sortOn("shpFilter", "transportMode");
		transitLinesPerArea
			.column(TableSliceGroup.aggregateColumnName("transitLine", countUnique.functionName()))
			.setName("totalNumberOfTransitLinesInArea");

		activeTransitLinesPerHourAndArea = activeTransitLinesPerHourAndArea
			.joinOn("shpFilter", "transportMode")
			.leftOuter(transitLinesPerArea);
		DoubleColumn shareActiveTransitLinesPerHour = activeTransitLinesPerHourAndArea.doubleColumn("activeTransitLinesInHourAndArea")
			.divide(activeTransitLinesPerHourAndArea.doubleColumn("totalNumberOfTransitLinesInArea"))
			.setName("share");

		activeTransitLinesPerHourAndArea.addColumns(shareActiveTransitLinesPerHour);
		// TODO: for the time being there seems to be no way to filter or display by column shpFilter in PublicTransitDashboard, so temporarily filter here
		activeTransitLinesPerHourAndArea = activeTransitLinesPerHourAndArea
			.where(t -> t.stringColumn("shpFilter").isIn("in area", "no area defined"));
		activeTransitLinesPerHourAndArea.write().csv(output.getPath("pt_active_transit_lines_per_hour_per_mode_per_area.csv").toString());

		// Share of active TransitStopAreas (at least one departure) per hour and mode
		Table activeTransitStopsPerHourAndArea = departuresFromStops
			.summarize("stopAreaOrStop", countUnique)
			.by("shpFilter", "departureHour", "transportMode")
			.sortOn("shpFilter", "departureHour", "transportMode");

		activeTransitStopsPerHourAndArea
			.column(TableSliceGroup.aggregateColumnName("stopAreaOrStop", countUnique.functionName()))
			.setName("activeTransitStopsInHourAndArea");

		Table transitStopsPerArea = departuresFromStops
			.summarize("stopAreaOrStop", countUnique)
			.by("shpFilter", "transportMode")
			.sortOn("shpFilter", "transportMode");
		transitStopsPerArea
			.column(TableSliceGroup.aggregateColumnName("stopAreaOrStop", countUnique.functionName()))
			.setName("totalNumberOfTransitStopsInArea");

		activeTransitStopsPerHourAndArea = activeTransitStopsPerHourAndArea
			.joinOn("shpFilter", "transportMode")
			.leftOuter(transitStopsPerArea);
		DoubleColumn shareActiveTransitStopsPerHour = activeTransitStopsPerHourAndArea.doubleColumn("activeTransitStopsInHourAndArea")
			.divide(activeTransitStopsPerHourAndArea.doubleColumn("totalNumberOfTransitStopsInArea"))
			.setName("share");

		activeTransitStopsPerHourAndArea.addColumns(shareActiveTransitStopsPerHour);
		// TODO: for the time being there seems to be no way to filter or display by column shpFilter in PublicTransitDashboard, so temporarily filter here
		activeTransitStopsPerHourAndArea = activeTransitStopsPerHourAndArea
			.where(t -> t.stringColumn("shpFilter").isIn("in area", "no area defined"));
		activeTransitStopsPerHourAndArea.write().csv(output.getPath("pt_active_transit_stops_per_hour_per_mode_per_area.csv").toString());

		// calculate headway/longestGap/frequency per departureHour and stop pair (where direct non-stop service exists) (based on departure time at from stop)
		Table nextStopTmp = departuresFromStops
			.selectColumns("transitLine", "transitRoute", "departure", "stopSequence", "stopAreaOrStop");
		nextStopTmp.replaceColumn("stopSequence", nextStopTmp.intColumn("stopSequence").map(i -> i - 1)); // rename to join on. Would be better if joining on different column names would be possible
		nextStopTmp.column("stopAreaOrStop").setName("stopAreaOrStopNext");

		Table stopAreaToNextStopAreaIgnoringLine = departuresFromStops
			.joinOn("transitLine", "transitRoute", "departure", "stopSequence")
			.inner(nextStopTmp)
			.sortOn("stopAreaOrStop", "stopAreaOrStopNext", "departureTimeScheduled");
		// sorting is essential to find last departure times in same stop pair using lag()
		stopAreaToNextStopAreaIgnoringLine.addColumns(stopAreaToNextStopAreaIgnoringLine
			.doubleColumn("departureTimeScheduled")
			.lag(1)
			.setName("lastDepartureTimeScheduled"));
		// set lastDepartureTimeScheduled missing where stop->stopNext pair changes
		stopAreaToNextStopAreaIgnoringLine.doubleColumn("lastDepartureTimeScheduled")
			.set(stopAreaToNextStopAreaIgnoringLine.stringColumn("stopAreaOrStop").lag(1)
					.isNotEqualTo(stopAreaToNextStopAreaIgnoringLine.stringColumn("stopAreaOrStop"))
					.or(stopAreaToNextStopAreaIgnoringLine.stringColumn("stopAreaOrStopNext").lag(1)
						.isNotEqualTo(stopAreaToNextStopAreaIgnoringLine.stringColumn("stopAreaOrStopNext"))),
				DoubleColumnType.missingValueIndicator());

		stopAreaToNextStopAreaIgnoringLine.write().csv("keks.csv");

		stopAreaToNextStopAreaIgnoringLine.addColumns(stopAreaToNextStopAreaIgnoringLine
			.doubleColumn("departureTimeScheduled")
			.subtract(stopAreaToNextStopAreaIgnoringLine.doubleColumn("lastDepartureTimeScheduled"))
			.setName("timeSinceLastDeparture"));

		Table headwayStopAreaToNextStopAreaIgnoringLine = stopAreaToNextStopAreaIgnoringLine
			.summarize("timeSinceLastDeparture", min, mean, median, max, countWithMissing)
			.by("stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode", "shpFilter")
			.sortOn("stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode");
		headwayStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", min.functionName())).setName("minHeadway");
		headwayStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", mean.functionName())).setName("meanHeadway");
		headwayStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", median.functionName())).setName("medianHeadway");
		headwayStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", max.functionName())).setName("maxHeadway");
		headwayStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", countWithMissing.functionName())).setName("departures");

		headwayStopAreaToNextStopAreaIgnoringLine.write().csv(output.getPath("pt_headway_per_stop_area_pair_and_hour.csv").toString()); // can be used for visualisation on map

		// Similar headway statistics divided per transit line
		Table stopAreaToNextStopAreaPerLine = departuresFromStops
			.joinOn("transitLine", "transitRoute", "departure", "stopSequence")
			.inner(nextStopTmp)
			.sortOn("transitLine", "stopAreaOrStop", "stopAreaOrStopNext", "departureTimeScheduled");
		// sorting is essential to find last departure times in same stop pair using lag()
		stopAreaToNextStopAreaPerLine.addColumns(stopAreaToNextStopAreaPerLine
			.doubleColumn("departureTimeScheduled")
			.lag(1)
			.setName("lastDepartureTimeScheduled"));
		// set lastDepartureTimeScheduled missing where stop->stopNext pair changes
		stopAreaToNextStopAreaPerLine.doubleColumn("lastDepartureTimeScheduled").
			set(
				stopAreaToNextStopAreaPerLine.stringColumn("stopAreaOrStop").lag(1)
					.isNotEqualTo(stopAreaToNextStopAreaPerLine.stringColumn("stopAreaOrStop"))
					.or(stopAreaToNextStopAreaPerLine.stringColumn("stopAreaOrStopNext").lag(1)
						.isNotEqualTo(stopAreaToNextStopAreaPerLine.stringColumn("stopAreaOrStopNext"))),
				DoubleColumnType.missingValueIndicator());

		stopAreaToNextStopAreaPerLine.addColumns(stopAreaToNextStopAreaPerLine
			.doubleColumn("departureTimeScheduled")
			.subtract(stopAreaToNextStopAreaPerLine.doubleColumn("lastDepartureTimeScheduled"))
			.setName("timeSinceLastDeparture"));

		Table headwayStopAreaToNextStopAreaPerLine = stopAreaToNextStopAreaPerLine
			.summarize("timeSinceLastDeparture", min, mean, median, max, countWithMissing)
			.by("transitLine", "stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode", "shpFilter")
			.sortOn("transitLine", "stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode");
		headwayStopAreaToNextStopAreaPerLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", min.functionName())).setName("minHeadway");
		headwayStopAreaToNextStopAreaPerLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", mean.functionName())).setName("meanHeadway");
		headwayStopAreaToNextStopAreaPerLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", median.functionName())).setName("medianHeadway");
		headwayStopAreaToNextStopAreaPerLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", max.functionName())).setName("maxHeadway");
		headwayStopAreaToNextStopAreaPerLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", countWithMissing.functionName())).setName("departures");

		headwayStopAreaToNextStopAreaPerLine.write().csv(output.getPath("pt_headway_per_line_stop_area_pair_and_hour.csv").toString()); // can be used for visualisation on map

		Table headwayPerLineAndHour = headwayStopAreaToNextStopAreaPerLine
			.summarize("medianHeadway", "maxHeadway", "departures", median, max)
			.by("transitLine", "departureHour", "transportMode", "shpFilter");
		headwayPerLineAndHour.column(TableSliceGroup.aggregateColumnName("medianHeadway", median.functionName())).setName("medianPerLineOfMedianHeadwayPerStopPair");
		headwayPerLineAndHour.column(TableSliceGroup.aggregateColumnName("medianHeadway", max.functionName())).setName("maxPerLineOfMedianHeadwayPerStopPair");
		headwayPerLineAndHour.column(TableSliceGroup.aggregateColumnName("maxHeadway", median.functionName())).setName("medianPerLineOfMaxHeadwayPerStopPair");
		headwayPerLineAndHour.column(TableSliceGroup.aggregateColumnName("maxHeadway", max.functionName())).setName("maxPerLineOfMaxHeadwayPerStopPair");
		headwayPerLineAndHour.column(TableSliceGroup.aggregateColumnName("departures", median.functionName())).setName("medianDepartures");
		headwayPerLineAndHour = headwayPerLineAndHour.selectColumns("transitLine", "departureHour", "transportMode", "shpFilter",
			"medianPerLineOfMedianHeadwayPerStopPair", "maxPerLineOfMedianHeadwayPerStopPair", "medianPerLineOfMaxHeadwayPerStopPair",
			"maxPerLineOfMaxHeadwayPerStopPair", "medianDepartures");

		headwayPerLineAndHour
			.where(t -> t.stringColumn("shpFilter").isIn("in area", "no area defined"))
			.write().csv(output.getPath("pt_headway_per_line_and_hour.csv").toString());

		Table headwayPerModeAndHour = headwayPerLineAndHour
			.where(t -> t.stringColumn("shpFilter").isIn("in area", "no area defined"))
			.summarize("medianPerLineOfMedianHeadwayPerStopPair", "medianPerLineOfMaxHeadwayPerStopPair", "medianDepartures", median, max)
			.by("transportMode", "departureHour")
			.sortOn("transportMode", "departureHour");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianPerLineOfMedianHeadwayPerStopPair", median.functionName())).setName("medianPerModeOfMedianPerLineOfMedianHeadwayPerStopPair");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianPerLineOfMedianHeadwayPerStopPair", max.functionName())).setName("maxPerModeOfMedianPerLineOfMedianHeadwayPerStopPair");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianPerLineOfMaxHeadwayPerStopPair", median.functionName())).setName("medianPerModeOfMedianPerLineOfMaxHeadwayPerStopPair");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianPerLineOfMaxHeadwayPerStopPair", max.functionName())).setName("maxPerModeOfMedianPerLineOfMaxHeadwayPerStopPair");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianDepartures", median.functionName())).setName("medianPerModeOfMedianDepartures");
		headwayPerModeAndHour.column(TableSliceGroup.aggregateColumnName("medianDepartures", max.functionName())).setName("maxPerModeOfMedianDepartures");

		headwayPerModeAndHour.write().csv(output.getPath("pt_headway_per_mode_and_hour.csv").toString());

		List<Integer> headwayGroups = List.of(0, 5, 10, 15, 20, 30, 40, 60, Integer.MAX_VALUE); //  min
		List<String> headwayLabels = new ArrayList<>();
		for (int i = 0; i < headwayGroups.size() - 2; i++) {
			headwayLabels.add(headwayGroups.get(i) + "<X<=" + headwayGroups.get(i + 1));
		}
		headwayLabels.add(headwayGroups.get(headwayGroups.size() - 2) + "<X");
		List<Double> finalHeadwayGroups = headwayGroups.stream().map(min -> min * 60.).collect(Collectors.toUnmodifiableList()); // sec

		StringColumn medianHeadwayGroup = headwayPerLineAndHour.doubleColumn("medianPerLineOfMedianHeadwayPerStopPair")
			.map(headway -> AnalysisUtils.cut(headway, finalHeadwayGroups, headwayLabels), ColumnType.STRING::create).setName("headwayGroup");
		headwayPerLineAndHour.addColumns(medianHeadwayGroup);

		Table headwayGroupPerModeAndHour = headwayPerLineAndHour
			.where(t -> t.stringColumn("shpFilter").isIn("in area", "no area defined"))
			.summarize("medianPerLineOfMedianHeadwayPerStopPair", count)
			.by("transportMode", "departureHour", "headwayGroup")
			.sortOn("transportMode", "departureHour", "headwayGroup");
		headwayGroupPerModeAndHour
			.column(TableSliceGroup.aggregateColumnName("medianPerLineOfMedianHeadwayPerStopPair", count.functionName()))
			.setName("count");

		headwayGroupPerModeAndHour.write().csv(output.getPath("pt_headway_group_per_mode_and_hour.csv").toString());
	}
}
