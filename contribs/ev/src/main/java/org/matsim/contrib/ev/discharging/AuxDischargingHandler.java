/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.discharging;

import com.google.inject.Inject;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

/**
 * This AUX Discharge runs also when vehicles are not in use. This is handy for vehicles with idle engines, such as taxis (where heating is on while the vehicle is idle), but should not be used with ordinary passenger cars.
 */
public class AuxDischargingHandler implements MobsimAfterSimStepListener {
	private final ElectricFleet evFleet;
	private final int auxDischargeTimeStep;

	@Inject
	public AuxDischargingHandler(ElectricFleet evFleet, EvConfigGroup evConfig) {
		this.evFleet = evFleet;
		this.auxDischargeTimeStep = evConfig.getAuxDischargeTimeStep();
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		if ((e.getSimulationTime() + 1) % auxDischargeTimeStep == 0) {
			for (ElectricVehicle ev : evFleet.getElectricVehicles().values()) {
				double energy = ev.getAuxEnergyConsumption().calcEnergyConsumption(auxDischargeTimeStep);
				ev.getBattery().discharge(energy);
			}
		}
	}
}
