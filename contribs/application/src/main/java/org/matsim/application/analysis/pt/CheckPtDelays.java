/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author vsp-gleich
 */
@CommandLine.Command(
		name = "pt-delays",
		description = "Calculate the pt delays for all vehicles and stops"
)
public class CheckPtDelays implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(CheckPtDelays.class);

	@CommandLine.Option(names = "--events", description = "Path to event file", required = true)
	private Path eventsFile;

	@CommandLine.Option(names = "--schedule", description = "Path to schedule file", required = true)
	private Path scheduleFile;

	@CommandLine.Option(names = "--output", description = "Path to output csv", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	private final List<DelayRecord> allDelays = new ArrayList<>();
	private final Map<Id<Vehicle>, DelayRecord> veh2minDelay = new HashMap<>();
	private final Map<Id<Vehicle>, DelayRecord> veh2maxDelay = new HashMap<>();
	private final Map<Id<TransitLine>, DelayRecord> line2minDelay = new TreeMap<>();
	private final Map<Id<TransitLine>, DelayRecord> line2maxDelay = new TreeMap<>();
	private final Map<Id<Vehicle>, Id<TransitRoute>> vehId2RouteId = new HashMap<>();
	private final Map<Id<TransitRoute>, Id<TransitLine>> transitRouteId2TransitLineId = new HashMap<>();

	public static void main(String[] args) {
		new CheckPtDelays().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		// read TransitSchedule in order to sum up results by TransitLine
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleReader tsReader = new TransitScheduleReader(scenario);
		tsReader.readFile(scheduleFile.toString());

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {

			for (TransitRoute route : line.getRoutes().values()) {
				transitRouteId2TransitLineId.put(route.getId(), line.getId());

				for (Departure dep : route.getDepartures().values()) {
					vehId2RouteId.put(dep.getVehicleId(), route.getId());
				}
			}
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		VehicleDelayEventHandler delayHandler = new VehicleDelayEventHandler();
		eventsManager.addHandler(delayHandler);
		eventsManager.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile.toString());
		eventsManager.finishProcessing();
		aggregatePerTransitLine();


		try (CSVPrinter printer = csv.createPrinter(output)) {

			printer.printRecord("lineId", "routeId", "vehId", "stopId", "minDelay", "maxDelay");

			for (Id<TransitLine> k : line2minDelay.keySet()) {

				DelayRecord min = line2minDelay.get(k);
				DelayRecord max = line2maxDelay.get(k);


				printer.printRecord(min.lineId, min.routeId, min.vehId, min.stopId, min.delay, max.delay);
			}
		}

		log.info("Result written to {}", output);

		return 0;
	}

	// it seems that VehicleArrivesAtFacility and VehicleDepartsAtFacility evnt types are only used for transit vehicles,
	// because both have TransitStopFacility
	private class VehicleDelayEventHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			handleDelay(event.getVehicleId(), event.getFacilityId(), event.getDelay(), veh2minDelay, veh2maxDelay);
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
			handleDelay(event.getVehicleId(), event.getFacilityId(), event.getDelay(), veh2maxDelay, veh2minDelay);
		}

		private void handleDelay(Id<Vehicle> vehicleId, Id<TransitStopFacility> facilityId, double delayTime,
		                         Map<Id<Vehicle>, DelayRecord> veh2maxDelay, Map<Id<Vehicle>, DelayRecord> veh2minDelay) {
			Id<TransitRoute> routeId = vehId2RouteId.get(vehicleId);
			Id<TransitLine> lineId = transitRouteId2TransitLineId.get(routeId);
			DelayRecord delay = new DelayRecord(lineId, routeId, vehicleId, facilityId, delayTime);

			if (Double.isFinite(delay.delay)) {
				allDelays.add(delay);
				if (!veh2maxDelay.containsKey(vehicleId)) veh2maxDelay.put(vehicleId, delay);
				if (!veh2minDelay.containsKey(vehicleId)) veh2minDelay.put(vehicleId, delay);
				if (delay.delay > 0) {
					if (delay.delay > veh2maxDelay.get(vehicleId).delay) {
						veh2maxDelay.put(vehicleId, delay);
					}
				} else {
					if (delay.delay < veh2minDelay.get(vehicleId).delay) {
						veh2minDelay.put(vehicleId, delay);
					}
				}
			}
		}
	}

	/**
	 * This assumes that each TransitVehicle is used on exactly one TransitLine, never on multiple lines
	 */
	private void aggregatePerTransitLine() {
		for (Entry<Id<Vehicle>, DelayRecord> entry : veh2maxDelay.entrySet()) {
			Id<TransitLine> lineId = entry.getValue().lineId;
			if (!line2maxDelay.containsKey(lineId)) line2maxDelay.put(lineId, entry.getValue());
			if (entry.getValue().delay > line2maxDelay.get(lineId).delay) {
				line2maxDelay.put(lineId, entry.getValue());
			}
		}
		for (Entry<Id<Vehicle>, DelayRecord> entry : veh2minDelay.entrySet()) {
			Id<TransitLine> lineId = entry.getValue().lineId;
			if (!line2minDelay.containsKey(lineId)) line2minDelay.put(lineId, entry.getValue());
			if (entry.getValue().delay < line2minDelay.get(lineId).delay) {
				line2minDelay.put(lineId, entry.getValue());
			}
		}
	}


	private static final class DelayRecord {
		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<Vehicle> vehId;
		private final Id<TransitStopFacility> stopId;
		private final double delay;

		DelayRecord(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<Vehicle> vehId,
		            Id<TransitStopFacility> stopId, double delay) {
			this.lineId = lineId;
			this.routeId = routeId;
			this.vehId = vehId;
			this.stopId = stopId;
			this.delay = delay;
		}

		@Override
		public String toString() {
			return "lineId " + lineId + "; routeId " + routeId + "; vehId " + vehId + "; stopId " + stopId + "; delay " + delay;
		}

	}

}
