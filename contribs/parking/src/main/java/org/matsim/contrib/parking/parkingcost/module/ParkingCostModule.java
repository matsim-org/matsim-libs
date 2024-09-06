/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingcost.module;

import org.matsim.contrib.parking.parkingcost.config.ParkingCostConfigGroup;
import org.matsim.contrib.parking.parkingcost.eventhandling.MainModeParkingCostVehicleTracker;
import org.matsim.contrib.parking.parkingcost.eventhandling.TeleportedModeParkingCostTracker;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

import java.util.Collection;

/**
 * @author jfbischoff (SBB)
 */
public class ParkingCostModule extends AbstractModule {

	@Override
	public void install() {
		ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(getConfig(), ParkingCostConfigGroup.class);
		if (parkingCostConfigGroup.useParkingCost) {
			Collection<String> mainModes = switch (getConfig().controller().getMobsim()) {
				case "qsim" -> getConfig().qsim().getMainModes();
				case "hermes" -> getConfig().hermes().getMainModes();
				default -> throw new RuntimeException("ParkingCosts are currently supported for Qsim and Hermes");
			};
			for (String mode : parkingCostConfigGroup.getModesWithParkingCosts()) {
				if (mainModes.contains(mode)) {
					addEventHandlerBinding().toInstance(new MainModeParkingCostVehicleTracker(mode, parkingCostConfigGroup));
				} else {
					addEventHandlerBinding().toInstance(new TeleportedModeParkingCostTracker(mode, parkingCostConfigGroup));
				}
			}
		}
	}
}
