/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.controllers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.VehicleUtils;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;

/**
 * Controller for starting a scenario, which includes the options for stationary
 * charging at activity locations and inductive charging along the roads.
 * 
 * @author rashid_waraich
 */
public class InductiveChargingController {

	private final Controler controler;
	private InductiveStreetCharger inductiveCharger;
	private ChargingUponArrival chargingUponArrival;
	private EnergyConsumptionTracker energyConsumptionTracker;
	private HashMap<Id<Vehicle>, Vehicle> vehicles;

	public InductiveChargingController(Config config, HashMap<Id<Vehicle>, Vehicle> vehicles) {
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		this.controler = new Controler( scenario );
		init(vehicles);
	}

	private void init(HashMap<Id<Vehicle>, Vehicle> vehicles) {
		this.vehicles = vehicles;

		final EventHandlerGroup handlerGroup = new EventHandlerGroup();

        setEnergyConsumptionTracker(new EnergyConsumptionTracker(vehicles, controler.getScenario().getNetwork()));
        setInductiveCharger(new InductiveStreetCharger(vehicles, controler.getScenario().getNetwork(), controler));
		setChargingUponArrival(new ChargingUponArrival(vehicles, controler));

		handlerGroup.addHandler(getEnergyConsumptionTracker());
		handlerGroup.addHandler(getInductiveCharger());
		handlerGroup.addHandler(getChargingUponArrival());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(handlerGroup);
			}
		});
	}

	/**
	 * Prints information to console regarding energy consumption, charging (stationary +
	 * inductive along the roads) and EV drivers, who's vehicle ran out of
	 * battery in the last iteration.
	 */
	public void printStatisticsToConsole() {
		System.out.println("energy consumption stats");
		energyConsumptionTracker.getLog().printToConsole();
		System.out.println("===");
		System.out.println("stationary charging at activity locations (charging scheme: charging upon arrival)");
		chargingUponArrival.getLog().printToConsole();
		System.out.println("===");
		System.out.println("inductive charging along roads");
		inductiveCharger.getLog().printToConsole();
		System.out.println("===");
		System.out.println("electric vehicles drivers, who's vehicle ran out of battery");
		VehicleUtils.printToConsoleVehiclesWhichRanOutOfBattery(vehicles);
		System.out.println("===");
	}

	public InductiveStreetCharger getInductiveCharger() {
		return inductiveCharger;
	}

	private void setInductiveCharger(InductiveStreetCharger inductiveCharger) {
		this.inductiveCharger = inductiveCharger;
	}

	public ChargingUponArrival getChargingUponArrival() {
		return chargingUponArrival;
	}

	private void setChargingUponArrival(ChargingUponArrival chargingUponArrival) {
		this.chargingUponArrival = chargingUponArrival;
	}

	public EnergyConsumptionTracker getEnergyConsumptionTracker() {
		return energyConsumptionTracker;
	}

	private void setEnergyConsumptionTracker(EnergyConsumptionTracker energyConsumptionTracker) {
		this.energyConsumptionTracker = energyConsumptionTracker;
	}


	public void run() {
		controler.run();
	}

	public Scenario getScenario() {
		return controler.getScenario();
	}
}
