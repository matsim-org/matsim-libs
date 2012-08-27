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

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.config.Config;

/**
 * @author wrashid
 *
 */
public class InductiveChargingController extends AddHandlerAtStartupControler {

	private InductiveStreetCharger inductiveCharger;
	private ChargingUponArrival chargingUponArrival;
	private EnergyConsumptionTracker energyConsumptionTracker;

	public InductiveChargingController(Config config, HashMap<Id, Vehicle> vehicles) {
		super(config);
		setInductiveCharger(new InductiveStreetCharger(null,vehicles,network,this));
		setChargingUponArrival(new ChargingUponArrival(vehicles, null, this));
		setEnergyConsumptionTracker(new EnergyConsumptionTracker(vehicles, network,this));
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

}
