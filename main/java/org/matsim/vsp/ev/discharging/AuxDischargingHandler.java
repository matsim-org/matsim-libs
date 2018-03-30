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

package org.matsim.vsp.ev.discharging;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.EvFleet;

import com.google.inject.Inject;

public class AuxDischargingHandler implements MobsimAfterSimStepListener {
	private final Iterable<? extends ElectricVehicle> eVehicles;
	private final int auxDischargeTimeStep;

	@Inject
	public AuxDischargingHandler(EvFleet evFleet, EvConfigGroup evConfig) {
		this.eVehicles = evFleet.getElectricVehicles().values();
		this.auxDischargeTimeStep = evConfig.getAuxDischargeTimeStep();
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		if ((e.getSimulationTime() + 1) % auxDischargeTimeStep == 0) {
			for (ElectricVehicle ev : eVehicles) {
				double energy = ev.getAuxEnergyConsumption().calcEnergyConsumption(auxDischargeTimeStep);
				ev.getBattery().discharge(energy);
			}
		}
	}
}
