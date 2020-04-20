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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.MobsimScopeEventHandler;
import org.matsim.contrib.ev.MobsimScopeEventHandling;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import com.google.inject.Inject;

/**
 * AUX discharging is executed for non-moving vehicles. This is useful for vehicles with idle engines,
 * such as taxis (where heating is on during a stay at a taxi rank), but should not be used with ordinary passenger cars.
 * <p>
 * VehicleProvider is responsible to decide if AUX discharging applies to a given vehicle based on information from
 * ActivityStartEvent.
 */
public class AuxDischargingHandler
		implements MobsimAfterSimStepListener, ActivityStartEventHandler, ActivityEndEventHandler,
		MobsimScopeEventHandler {
	public interface VehicleProvider {
		/**
		 * During activities such as stopping at a bus stop or taxi rank, picking up/dropping off passengers etc.
		 * some energy is consumed for the so-called AUX (on-board devices, cooling/heating...)
		 *
		 * @param event activity start event
		 * @return vehicle being discharged (AUX) as a result of this activity (return null if N/A)
		 */
		ElectricVehicle getVehicle(ActivityStartEvent event);
	}

	private final class VehicleAndLink {
		private final ElectricVehicle vehicle;
		private final Id<Link> linkId;

		private VehicleAndLink(ElectricVehicle vehicle, Id<Link> linkId) {
			this.vehicle = vehicle;
			this.linkId = linkId;
		}
	}

	private final VehicleProvider vehicleProvider;
	private final int auxDischargeTimeStep;

	private final ConcurrentMap<Id<Person>, VehicleAndLink> vehicles = new ConcurrentHashMap<>();

	@Inject
	public AuxDischargingHandler(VehicleProvider vehicleProvider, EvConfigGroup evCfg,
			MobsimScopeEventHandling events) {
		this.vehicleProvider = vehicleProvider;
		this.auxDischargeTimeStep = evCfg.getAuxDischargeTimeStep();
		events.addMobsimScopeHandler(this);
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		if (e.getSimulationTime() % auxDischargeTimeStep == 0) {
			for (VehicleAndLink vehicleAndLink : vehicles.values()) {
				ElectricVehicle ev = vehicleAndLink.vehicle;
				double energy = ev.getAuxEnergyConsumption()
						.calcEnergyConsumption(e.getSimulationTime(), auxDischargeTimeStep, vehicleAndLink.linkId);
				ev.getBattery().changeSoc(-energy);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		ElectricVehicle electricVehicle = vehicleProvider.getVehicle(event);
		if (electricVehicle != null) {
			vehicles.put(event.getPersonId(), new VehicleAndLink(electricVehicle, event.getLinkId()));
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		vehicles.remove(event.getPersonId());
	}
}
