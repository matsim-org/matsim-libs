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

package vwExamples.utils.customEV;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import java.util.HashSet;
import java.util.Set;

/**
 * This AUX Discharge runs also when vehicles are not in use. This is handy for
 * vehicles with idle engines, such as taxis (where heating is on while the
 * vehicle is idle), but should not be used with ordinary passenger cars.
 */
public class CustomAuxDischargingHandler
		implements MobsimAfterSimStepListener, IterationStartsListener, LinkLeaveEventHandler, LinkEnterEventHandler {
	private final ElectricFleet evFleet;
	private final int auxDischargeTimeStep;
	private final ChargingInfrastructure chargers;
	private final Set<Id<Link>> chargerLinkIdSet = new HashSet<>();
	Set<String> isVehicleInDepotSet = new HashSet<>();

	@Inject
	public CustomAuxDischargingHandler(ElectricFleet evFleet, ChargingInfrastructure chargers, EvConfigGroup evConfig) {
		this.evFleet = evFleet;
		this.auxDischargeTimeStep = evConfig.getAuxDischargeTimeStep();
		this.chargers = chargers;
	}
	
	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		if ((e.getSimulationTime() + 1) % auxDischargeTimeStep == 0) {

			for (ElectricVehicle ev : evFleet.getElectricVehicles().values()) {

				if (isVehicleInDepotSet.contains(ev.getId().toString())) {
					 double energy = 0.0;
					 ev.getBattery().discharge(energy);
					 //System.out.println("Do not discharge " + ev.getId());

				} else {
					double energy = ev.getAuxEnergyConsumption().calcEnergyConsumption(auxDischargeTimeStep, e.getSimulationTime());
					ev.getBattery().discharge(energy);
				}

			}
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		for (Charger charger : chargers.getChargers().values()) {
			chargerLinkIdSet.add(charger.getLink().getId());
		}
		
		//Add all vehicles at start of the iteration into the mode into isVehicleInDepotSet
		for (ElectricVehicle ev : evFleet.getElectricVehicles().values()) {
			isVehicleInDepotSet.add(ev.getId().toString());
			System.out.println(ev.getId().toString() + " registered at charger");
		}
		
		

	}


	@Override
	public void handleEvent(LinkEnterEvent event) {

		if (evFleet.getElectricVehicles().keySet().contains(event.getVehicleId())) {

			// System.out.println(event.getVehicleId() + "enters link");

			if (chargerLinkIdSet.contains(event.getLinkId())) {
				//System.out.println(event.getVehicleId() + " enters charger");
				isVehicleInDepotSet.add(event.getVehicleId().toString());
			}
		}

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (evFleet.getElectricVehicles().keySet().contains(event.getVehicleId())) {

			// System.out.println(event.getVehicleId() + " leaves link");

			if (chargerLinkIdSet.contains(event.getLinkId())) {
				//System.out.println(event.getVehicleId() + "leaves charger");
				isVehicleInDepotSet.remove(event.getVehicleId().toString());
			}
		}

	}

}
