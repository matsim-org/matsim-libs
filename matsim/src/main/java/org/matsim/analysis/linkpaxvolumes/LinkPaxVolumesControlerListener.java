/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPaxVolumesControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.linkpaxvolumes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import jakarta.inject.Inject;

/**
 * @author vsp-gleich
 */
public final class LinkPaxVolumesControlerListener implements IterationEndsListener, IterationStartsListener {

	private LinkPaxVolumesAnalysis linkPaxVolumesAnalysis;
	private OutputDirectoryHierarchy controlerIO;
	private final EventsManager eventsManager;
	private final Scenario scenario;
	private final String sep;

	@Inject
	LinkPaxVolumesControlerListener(Scenario scenario, EventsManager eventsManager, OutputDirectoryHierarchy controlerIO) {
		this.eventsManager = eventsManager;
		this.controlerIO = controlerIO;
		this.scenario = scenario;
		linkPaxVolumesAnalysis = new LinkPaxVolumesAnalysis(scenario.getVehicles(), scenario.getTransitVehicles());
		sep = scenario.getConfig().global().getDefaultDelimiter();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.isLastIteration()) {
			// the above hopefully points to TerminationCriterion, if not working should be fixed in IterationStartsEvent.isLastIteration()
			// registering this handler here hopefully avoids having it running in previous iterations, when we prefer saving computation time over having this analysis output
			eventsManager.addHandler(linkPaxVolumesAnalysis);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.isLastIteration()) {
			LinkPaxVolumesWriter linkPaxVolumesWriter = new LinkPaxVolumesWriter(
					linkPaxVolumesAnalysis, scenario.getNetwork(), sep);

			String outputCsvAll = controlerIO.getOutputFilename("linkPaxVolumesAllPerDay.csv.gz");
			linkPaxVolumesWriter.writeLinkVehicleAndPaxVolumesAllPerDayCsv(outputCsvAll);

			if (linkPaxVolumesAnalysis.observeNetworkModes) {
				String outputPerNetworkModePerHour = controlerIO.getOutputFilename("linkPaxVolumesPerNetworkModePerHour.csv.gz");
				linkPaxVolumesWriter.writeLinkVehicleAndPaxVolumesPerNetworkModePerHourCsv(outputPerNetworkModePerHour);
			}

			if (linkPaxVolumesAnalysis.observePassengerModes) {
				String outputPerPassengerModePerHour = controlerIO.getOutputFilename("linkPaxVolumesPerPassengerModePerHour.csv.gz");
				linkPaxVolumesWriter.writeLinkVehicleAndPaxVolumesPerPassengerModePerHourCsv(outputPerPassengerModePerHour);
			}

			if (linkPaxVolumesAnalysis.observeVehicleTypes) {
				String outputPerVehicleTypePerHour = controlerIO.getOutputFilename("linkPaxVolumesPerVehicleTypePerHour.csv.gz");
				linkPaxVolumesWriter.writeLinkVehicleAndPaxVolumesPerVehicleTypePerHourCsv(outputPerVehicleTypePerHour);
			}

			VehicleStatsPerVehicleType vehicleStatsPerVehicleType = new VehicleStatsPerVehicleType(linkPaxVolumesAnalysis, scenario.getNetwork(), sep);
			String outputVehicleTypeStats = controlerIO.getOutputFilename("vehicleType_stats.csv.gz");
			vehicleStatsPerVehicleType.writeOperatingStatsPerVehicleType(outputVehicleTypeStats);
		}
	}

}
