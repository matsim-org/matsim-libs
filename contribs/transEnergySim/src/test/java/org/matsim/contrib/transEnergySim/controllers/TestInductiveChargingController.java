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
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowFacilityLevel;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.InductivelyChargableBatteryElectricVehicle;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author wrashid
 *
 */
public class TestInductiveChargingController extends MatsimTestCase {

	public void testBasic(){
		Config config= loadConfig(getClassInputDirectory()+"config.xml");
		
		EnergyConsumptionModel ecm=new EnergyConsumptionModelGalus();
		HashMap<Id<Vehicle>, Vehicle> vehicles=new HashMap<>();
		int batteryCapacityInJoules = 10*1000*3600;
		
		Id<Vehicle> vehicleId = Id.create("1", Vehicle.class);
		vehicles.put(vehicleId, new InductivelyChargableBatteryElectricVehicle(ecm,batteryCapacityInJoules));
		
		InductiveChargingController controller = new InductiveChargingController(config,vehicles);

		EnergyConsumptionTracker energyConsumptionTracker = controller.getEnergyConsumptionTracker();

		InductiveStreetCharger inductiveCharger = controller.getInductiveCharger();
		inductiveCharger.setSamePowerAtAllStreets(3000);
		
		ChargingUponArrival chargingUponArrival= controller.getChargingUponArrival();
		chargingUponArrival.setPowerForNonInitializedActivityTypes(controller.getScenario().getActivityFacilities(), 3500);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("h", 7000.0);
		
		
		controller.run();
		controller.printStatisticsToConsole();
		
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
		
		isBatteryFullyChargedAtEndOfSimulation(vehicles, batteryCapacityInJoules, vehicleId);
		
		ChargingLogRowFacilityLevel chargingLogRow = (ChargingLogRowFacilityLevel) chargingUponArrival.getLog().get(0);
		chargingLogRow.getFacilityId().equals("2");
		chargingLogRow.getLinkId().equals("21");
		chargingLogRow = (ChargingLogRowFacilityLevel) chargingUponArrival.getLog().get(1);
		chargingLogRow.getFacilityId().equals("1");
	}
	

	private void isBatteryFullyChargedAtEndOfSimulation(HashMap<Id<Vehicle>, Vehicle> vehicles, int batteryCapacityInJoules, Id<Vehicle> agentId) {
		assertEquals(batteryCapacityInJoules, ((VehicleWithBattery) vehicles.get(agentId)).getSocInJoules(),1.0);
	}
}
