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
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.AnalysisUtils;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.DoubleColumnType;
import tech.tablesaw.table.TableSliceGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static tech.tablesaw.aggregate.AggregateFunctions.*;
import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(
	name = "transit", description = "General public transit analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"pt_pax_volumes.csv.gz",
		"pt_pax_per_hour_and_vehicle_type.csv",
		"pt_pax_per_hour_and_vehicle_type_and_agency.csv",
		"pt_departures_at_stops_per_hour_per_mode.csv",
		"pt_count_unique_ids_per_mode.csv",
		"pt_active_transit_lines_per_hour_per_mode_per_area.csv",
		"pt_active_transit_stops_per_hour_per_mode_per_area.csv",
		"pt_headway_and_pax_per_stop_area_pair_and_hour.csv",
		"pt_headway_per_line_stop_area_pair_and_hour.csv",
		"pt_headway_per_line_and_hour.csv",
		"pt_headway_per_mode_and_hour.csv",
		"pt_headway_group_per_mode_and_hour.csv"
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

	@CommandLine.Mixin
	private ShpOptions shp;

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

		// Create passenger volumes file for transit viz
		ptStop2StopEventHandler.writeStop2StopEntriesByDepartureCsv(output.getPath("pt_pax_volumes.csv.gz"),
			",", ";");
		// Create transit supply data from schedule and join with passenger volumes for flow map viz
		createStatisticsFromScheduleAndJoinPaxVolumes(scenario.getTransitSchedule(), config.global().getCoordinateSystem(), ptStop2StopEventHandler.getStop2StopEntriesByDeparture());

		// this data is apparently not used in Simwrapper.
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

	private void createStatisticsFromScheduleAndJoinPaxVolumes(TransitSchedule transitSchedule,
															   String coordinateSystem,
															   List<PtStop2StopAnalysis.Stop2StopEntry> stop2StopEntriesByDeparture) {
		/*
		 * Create a table very similar to PtStop2StopAnalysis but based only on the schedule file.
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

		// read passenger volumes as tablesaw table and join for flow map
		Table paxVolumes = Table.create("PaxVolumes").addColumns(
			StringColumn.create("transitLine"),
			StringColumn.create("transitRoute"),
			StringColumn.create("departure"),
			StringColumn.create("stop"),
			IntColumn.create("stopSequence"),
			StringColumn.create("stopPrevious"),
			DoubleColumn.create("arrivalTimeScheduled"),
			DoubleColumn.create("arrivalDelay"),
			DoubleColumn.create("departureTimeScheduled"),
			DoubleColumn.create("departureDelay"),
			DoubleColumn.create("passengersAtArrival"),
			DoubleColumn.create("totalVehicleCapacity"),
			DoubleColumn.create("passengersAlighting"),
			DoubleColumn.create("passengersBoarding")
			// ignore "linkIdsSincePreviousStop" for now
		);
		for (PtStop2StopAnalysis.Stop2StopEntry entry : stop2StopEntriesByDeparture) {
			Row newRow = paxVolumes.appendRow();
			newRow.setString("transitLine", entry.transitLineId().toString());
			newRow.setString("transitRoute", entry.transitRouteId().toString());
			newRow.setString("departure", entry.departureId().toString());
			newRow.setString("stop", entry.stopId().toString());
			newRow.setInt("stopSequence", entry.stopSequence());
			newRow.setString("stopPrevious", entry.stopPreviousId() == null ? "" : entry.stopPreviousId().toString());
			newRow.setDouble("arrivalTimeScheduled", entry.arrivalTimeScheduled());
			newRow.setDouble("arrivalDelay", entry.arrivalDelay());
			newRow.setDouble("departureTimeScheduled", entry.departureTimeScheduled());
			newRow.setDouble("departureDelay", entry.departureDelay());
			newRow.setDouble("passengersAtArrival", entry.passengersAtArrival());
			newRow.setDouble("totalVehicleCapacity", entry.totalVehicleCapacity());
			newRow.setDouble("passengersAlighting", entry.passengersAlighting());
			newRow.setDouble("passengersBoarding", entry.passengersBoarding());
		}

//		departuresFromStops.write().csv(output.getPath("test_departuresFromStops_before_join.csv").toString());
//		paxVolumes.write().csv(output.getPath("test_paxVolumes_before_join.csv").toString());

		departuresFromStops = departuresFromStops
			.joinOn("transitLine","transitRoute","departure","stop","stopSequence","stopPrevious",
			"arrivalTimeScheduled","departureTimeScheduled")
			.leftOuter(paxVolumes);

//		departuresFromStops.write().csv(output.getPath("test_departuresFromStops_after_join.csv").toString());

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
		stopAreaToNextStopAreaIgnoringLine.addColumns(stopAreaToNextStopAreaIgnoringLine
			.doubleColumn("passengersAtArrival")
			.subtract(stopAreaToNextStopAreaIgnoringLine.doubleColumn("passengersAlighting"))
			.add(stopAreaToNextStopAreaIgnoringLine.doubleColumn("passengersBoarding"))
			.setName("passengers"));

		Table headwayAndPaxStopAreaToNextStopAreaIgnoringLine = stopAreaToNextStopAreaIgnoringLine
			.summarize("timeSinceLastDeparture","passengers", min, mean, median, max, countWithMissing, sum)
			.by("stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode", "shpFilter")
			.sortOn("stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", min.functionName())).setName("minHeadway");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", mean.functionName())).setName("meanHeadway");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", median.functionName())).setName("medianHeadway");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", max.functionName())).setName("maxHeadway");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("timeSinceLastDeparture", countWithMissing.functionName())).setName("departures");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.column(TableSliceGroup.aggregateColumnName("passengers", sum.functionName())).setName("passengersPerHour");

		headwayAndPaxStopAreaToNextStopAreaIgnoringLine = headwayAndPaxStopAreaToNextStopAreaIgnoringLine.selectColumns("stopAreaOrStop", "stopAreaOrStopNext", "departureHour", "transportMode", "shpFilter",
			"minHeadway","meanHeadway","medianHeadway","maxHeadway","departures","passengersPerHour");
		headwayAndPaxStopAreaToNextStopAreaIgnoringLine.write().csv(output.getPath("pt_headway_and_pax_per_stop_area_pair_and_hour.csv").toString()); // can be used for visualisation on map

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
