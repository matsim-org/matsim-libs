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
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.api.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.IC_BEV;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.lib.obj.DoubleValueHashMap;

public class InductiveChargingController extends Controler {

	public InductiveChargingController(Config config) {
		super(config);
	}
	
	public InductiveChargingController(String[] args) {
		super(args);
	}

	public static void main(String[] args) {
		InductiveChargingController eMobInductiveChargingController = new InductiveChargingController(args);
		
		eMobInductiveChargingController.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				//TODO: set this properly for the scenario
				
				EnergyConsumptionModel ecm=new EnergyConsumptionModelGalus();
				Network network = event.getControler().getNetwork();
				HashMap<Id, Vehicle> vehicles=new HashMap<Id, Vehicle>();
				vehicles.put(new IdImpl("1"), new IC_BEV(ecm));
				
				InductiveStreetCharger inductiveCharger = new InductiveStreetCharger(null,vehicles,network);
				inductiveCharger.allStreetsCanChargeWithPower(3000);
				event.getControler().getEvents().addHandler(inductiveCharger);
				
				event.getControler().getEvents().addHandler(new EnergyConsumptionTracker(vehicles, network));
			}
		});
		
		eMobInductiveChargingController.run();
	}
	
}
