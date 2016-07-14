/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.transEnergySim.example.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.pt.ElectricPtSimModule;
import org.matsim.contrib.transEnergySim.pt.PtVehicleEnergyControl;
import org.matsim.contrib.transEnergySim.pt.PtVehicleEnergyState;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ConstantEnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

/*
 * This uses the input files from the following tutorial:
 * http://matsim.org/docs/tutorials/transit
 */
public class ExampleElectricPt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		HashMap<Id<Vehicle>, PtVehicleEnergyState> ptEnergyMangementModels = new HashMap<>();
		EnergyConsumptionModel ecm=new ConstantEnergyConsumptionModel(600);
		double batterySize=24*3600000;
		ptEnergyMangementModels.put(Id.create("tr_1", Vehicle.class), new PtVehicleEnergyState(batterySize, ecm, ecm));
		ptEnergyMangementModels.put(Id.create("tr_2", Vehicle.class), new PtVehicleEnergyState(batterySize, ecm, ecm));
		
		DoubleValueHashMap<Id<TransitStopFacility>> chargingPowerAtStops=new DoubleValueHashMap<>();
		chargingPowerAtStops.put(Id.create("1", TransitStopFacility.class), 1.0);
		chargingPowerAtStops.put(Id.create("2a", TransitStopFacility.class), 10.0);
		chargingPowerAtStops.put(Id.create("2b", TransitStopFacility.class), 5.0);
		chargingPowerAtStops.put(Id.create("3", TransitStopFacility.class), 1.0);
        PtVehicleEnergyControlImpl ptVehicleEnergyControl = new PtVehicleEnergyControlImpl(controler.getScenario().getNetwork(),chargingPowerAtStops);
		
		new ElectricPtSimModule(controler, ptEnergyMangementModels,ptVehicleEnergyControl);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}
	
	private static class PtVehicleEnergyControlImpl implements PtVehicleEnergyControl{

		private Network network;
		private DoubleValueHashMap<Id<TransitStopFacility>> chargingPowerAtStops;

		public PtVehicleEnergyControlImpl(Network network, DoubleValueHashMap<Id<TransitStopFacility>> chargingPowerAtStops){
			this.network = network;
			this.chargingPowerAtStops = chargingPowerAtStops;
		}
		
		@Override
		public void handleLinkTravelled(PtVehicleEnergyState ptVehicleEnergyState, Id<Link> linkId, double travelTime) {
			DebugLib.emptyFunctionForSettingBreakPoint();
			Link link = network.getLinks().get(linkId);
			ptVehicleEnergyState.useBattery(link.getLength(), link.getFreespeed(), link.getLength()/travelTime);
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		@Override
		public void handleChargingOpportunityAtStation(PtVehicleEnergyState ptVehicleEnergyState,
				double durationOfStayAtStationInSeconds, Id<TransitStopFacility> stationId) {
			DebugLib.emptyFunctionForSettingBreakPoint();
			if (ptVehicleEnergyState.getSocInJoules()<ptVehicleEnergyState.getUsableBatterySize()){
				double charge = durationOfStayAtStationInSeconds*chargingPowerAtStops.get(stationId);
				if (ptVehicleEnergyState.getSocInJoules()+charge <ptVehicleEnergyState.getUsableBatterySize()){
					ptVehicleEnergyState.chargeVehicle(charge);
				} else {
					ptVehicleEnergyState.chargeVehicle(ptVehicleEnergyState.getUsableBatterySize()-ptVehicleEnergyState.getSocInJoules());
				}
			}
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
	}

}

