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
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.api.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.IC_BEV;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.obj.DoubleValueHashMap;
/**
 * @author wrashid
 *
 */
public class TestInductiveChargingController extends MatsimTestCase {

	public void testBasic8(){
		Config config= loadConfig(getClassInputDirectory()+"config.xml");
		
		EnergyConsumptionModel ecm=new EnergyConsumptionModelGalus();
		HashMap<Id, Vehicle> vehicles=new HashMap<Id, Vehicle>();
		int batteryCapacityInJoules = 10*1000*3600;
		IdImpl agentId = new IdImpl("1");
		vehicles.put(agentId, new IC_BEV(ecm,batteryCapacityInJoules));
		
		InductiveChargingController controller = new InductiveChargingController(config,vehicles);

		EnergyConsumptionTracker energyConsumptionTracker = controller.getEnergyConsumptionTracker();

		InductiveStreetCharger inductiveCharger = controller.getInductiveCharger();
		inductiveCharger.setSamePowerAtAllStreets(3000);
		
		ChargingUponArrival chargingUponArrival= controller.getChargingUponArrival();
		chargingUponArrival.setPowerForNonInitializedActivityTypes(controller.getFacilities(), 3500);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("h", 7000.0);
		
		//TODO: remove this line soon!
		controller.setOverwriteFiles(true);
		
		controller.run();
		
		assertEquals(7, energyConsumptionTracker.getLog().size());
		assertEquals(7503001, energyConsumptionTracker.getLog().get(0).getEnergyConsumedInJoules(),1.0);
		
		assertEquals(7, inductiveCharger.getLog().size());
		assertEquals(21600.0, inductiveCharger.getLog().get(0).getStartChargingTime());
		assertEquals(359, inductiveCharger.getLog().get(0).getChargingDuration(),1.0);
		assertEquals(1079913, inductiveCharger.getLog().get(0).getEnergyChargedInJoule(),1.0);
		
		assertEquals(2, chargingUponArrival.getLog().size());
		assertEquals(23219, chargingUponArrival.getLog().get(0).getStartChargingTime(),1.0);
		assertEquals(8258, chargingUponArrival.getLog().get(0).getChargingDuration(),1.0);
		assertEquals(28903897, chargingUponArrival.getLog().get(0).getEnergyChargedInJoule(),1.0);
		assertEquals(4129, chargingUponArrival.getLog().get(1).getChargingDuration(),1.0);
		
		isBatteryFullyChargedAtEndOfSimulation(vehicles, batteryCapacityInJoules, agentId);
	}

	private void isBatteryFullyChargedAtEndOfSimulation(HashMap<Id, Vehicle> vehicles, int batteryCapacityInJoules, IdImpl agentId) {
		assertEquals(batteryCapacityInJoules, ((VehicleWithBattery) vehicles.get(agentId)).getSocInJoules(),1.0);
	}
}
