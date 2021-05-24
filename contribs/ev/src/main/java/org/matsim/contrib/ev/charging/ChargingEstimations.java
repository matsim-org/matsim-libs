/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import java.util.stream.Stream;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * TODO Does not include future energy demand (e.g. AUX for plugged/queued vehs)
 *
 * @author michalm
 */
public class ChargingEstimations {
	// TODO overestimates for short queues!!! (which is often the case for more advanced dispatch strategies)
	public static double estimateMaxWaitTimeForNextVehicle(Charger charger) {
		if (charger.getLogic().getPluggedVehicles().size() < charger.getPlugCount()) {
			return 0;
		}
		return ChargingEstimations.estimateTotalTimeToCharge(charger.getLogic()) / charger.getPlugCount();
	}

	public static double estimateTotalTimeToCharge(ChargingLogic logic) {
		return estimateTotalTimeToCharge(logic.getChargingStrategy(),
				Stream.concat(logic.getPluggedVehicles().stream(), logic.getQueuedVehicles().stream()));
	}

	public static double estimateTotalEnergyToCharge(ChargingLogic logic) {
		return estimateTotalEnergyToCharge(logic.getChargingStrategy(),
				Stream.concat(logic.getPluggedVehicles().stream(), logic.getQueuedVehicles().stream()));
	}

	public static double estimateTotalTimeToCharge(ChargingStrategy strategy, Stream<ElectricVehicle> vehicles) {
		return vehicles.mapToDouble(strategy::calcRemainingTimeToCharge).sum();
	}

	public static double estimateTotalEnergyToCharge(ChargingStrategy strategy, Stream<ElectricVehicle> vehicles) {
		return vehicles.mapToDouble(strategy::calcRemainingEnergyToCharge).sum();
	}
}
