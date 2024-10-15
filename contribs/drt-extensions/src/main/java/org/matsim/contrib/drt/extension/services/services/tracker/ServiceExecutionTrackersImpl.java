/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.services.services.tracker;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.application.options.CsvOptions;
import org.matsim.contrib.drt.extension.services.events.DrtServiceEndedEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceScheduledEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceStartedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author steffenaxer
 */
public class ServiceExecutionTrackersImpl implements ServiceExecutionTrackers {
	private final CsvOptions csvOptions;
	private Map<Id<DvrpVehicle>, ServiceExecutionTracker> delegates;
	private final DrtConfigGroup drtConfigGroup;
	private final MatsimServices matsimServices;
	private final FleetSpecification fleetSpecification;

	public ServiceExecutionTrackersImpl(final FleetSpecification fleetSpecification, final DrtConfigGroup drtConfigGroup, final MatsimServices matsimServices) {
		this.delegates = createExecutionsTrackers(fleetSpecification, drtConfigGroup);
		this.drtConfigGroup = drtConfigGroup;
		this.matsimServices = matsimServices;
		this.csvOptions = new CsvOptions(CSVFormat.Predefined.Default, matsimServices.getConfig().global().getDefaultDelimiter().charAt(0), StandardCharsets.UTF_8);
		this.fleetSpecification = fleetSpecification;
	}

	@Override
	public Map<Id<DvrpVehicle>, ServiceExecutionTracker> getTrackers() {
		return delegates;
	}

	Map<Id<DvrpVehicle>, ServiceExecutionTracker> createExecutionsTrackers(FleetSpecification fleetSpecification, DrtConfigGroup drtConfigGroup) {
		return fleetSpecification.getVehicleSpecifications().values().stream()
			.collect(Collectors.toUnmodifiableMap(Identifiable::getId, v -> new ServiceExecutionTracker(v.getId(), drtConfigGroup)));
	}

	@Override
	public void reset(int iteration) {
		// Create new tracker delegates per iteration
		this.delegates = createExecutionsTrackers(fleetSpecification, drtConfigGroup);
	}

	public void writeServiceExecution() throws IOException {

		try (CSVPrinter printer = csvOptions.createPrinter(Path.of(matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(), "drt_service_executions" + "_" + drtConfigGroup.mode + ".csv")))) {
			printer.printRecord("vehicleId", "serviceName", "linkId", "startTime", "endTime", "duration");
			for (ServiceExecutionTracker serviceExecutionTracker : delegates.values()) {
				for (var drtServiceEntry : serviceExecutionTracker.getExecutionRecords()) {
					double startTime = drtServiceEntry.drtServiceStartedEvent().getTime();
					double endTime = drtServiceEntry.drtServiceEndedEvent().getTime();
					printer.printRecord(
						drtServiceEntry.drtServiceEndedEvent().getVehicleId(),
						drtServiceEntry.drtServiceEndedEvent().getServiceType(),
						drtServiceEntry.drtServiceEndedEvent().getLinkId(),
						startTime,
						endTime,
						endTime - startTime);
				}
			}
		}
	}

	@Override
	public void handleEvent(DrtServiceEndedEvent event) {
		delegates.get(event.getVehicleId()).handleEvent(event);
	}


	@Override
	public void handleEvent(DrtServiceStartedEvent event) {
		delegates.get(event.getVehicleId()).handleEvent(event);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writeServiceExecution();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(DrtServiceScheduledEvent event) {
		delegates.get(event.getVehicleId()).handleEvent(event);
	}
}



