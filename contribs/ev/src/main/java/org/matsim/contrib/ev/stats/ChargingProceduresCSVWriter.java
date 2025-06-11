/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.stats;

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEventSequenceCollector;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

public final class ChargingProceduresCSVWriter implements IterationEndsListener {

	@Inject
	ChargingEventSequenceCollector chargingEventSequenceCollector;
	@Inject
	private ChargingInfrastructureSpecification chargingInfrastructureSpecification;

	@Inject ChargingProceduresCSVWriter(){}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		try (CSVPrinter csvPrinter = new CSVPrinter(
			Files.newBufferedWriter(Paths.get(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "chargingStats.csv"))),
			CSVFormat.DEFAULT.withDelimiter(';')
				.withHeader("chargerId", "vehicleId", "linkId",
					"waitStartTime", "waitEndTime", "waitDuration",
					"chargeStartTime", "chargeEndTime", "chargingDuration",
					"energyTransmitted_kWh"))) {

			proccessChargingEventSequences(csvPrinter, chargingEventSequenceCollector.getCompletedSequences());
			proccessChargingEventSequences(csvPrinter, chargingEventSequenceCollector.getOnGoingSequences());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void proccessChargingEventSequences(CSVPrinter csvPrinter, Collection<ChargingEventSequenceCollector.ChargingSequence> chargingSequences) throws IOException {
		for (ChargingEventSequenceCollector.ChargingSequence sequence : chargingSequences) {
			Id<Charger> chargerId = sequence.getQueuedAtCharger().isPresent() ?
				sequence.getQueuedAtCharger().get().getChargerId() : sequence.getChargingStart().get().getChargerId();

			Id<Link> linkId = chargingInfrastructureSpecification.getChargerSpecifications().get(chargerId).getLinkId();

			Id<Vehicle> vehicleId = sequence.getQueuedAtCharger().isPresent() ?
				sequence.getQueuedAtCharger().get().getVehicleId() : sequence.getChargingStart().get().getVehicleId();

			double waitStartTime = Double.NaN;
			double waitEndTime = Double.NaN;

			if (sequence.getQueuedAtCharger().isPresent()) {
				waitStartTime = sequence.getQueuedAtCharger().get().getTime();

				if (sequence.getQuitQueueAtChargerEvent().isPresent()) {
					waitEndTime = sequence.getQuitQueueAtChargerEvent().get().getTime();
				} else if (sequence.getChargingStart().isPresent()) {
					waitEndTime = sequence.getChargingStart().get().getTime();
				}
			}

			double startEnergy = Double.NaN;
			double startTime = Double.NaN;
			if (sequence.getChargingStart().isPresent()) {
				startEnergy = sequence.getChargingStart().get().getCharge();
				startTime = sequence.getChargingStart().get().getTime();
			}
			double endEnergy = Double.NaN;
			double endTime = Double.NaN;
			if (sequence.getChargingEnd().isPresent()) {
				endEnergy = sequence.getChargingEnd().get().getCharge();
				endTime = sequence.getChargingEnd().get().getTime();
			}
			double energyTransmitted = endEnergy - startEnergy;

			double energyKWh = Math.round(EvUnits.J_to_kWh(energyTransmitted) * 10.) / 10.;

			csvPrinter.printRecord(chargerId, vehicleId, linkId,
				Time.writeTime(waitStartTime), Time.writeTime(waitEndTime), Time.writeTime(waitEndTime - waitStartTime),
				Time.writeTime(startTime), Time.writeTime(endTime), Time.writeTime(endTime - startTime),
				energyKWh);
		}
	}
}
