/* *********************************************************************** *
 * project: org.matsim.*
 * EventsTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelLAV;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class EnergyConsumptionTest extends MatsimTestCase {

	private EnergyConsumptionPlugin runWithModel(EnergyConsumptionModel energyConsumptionModel){
		String eventsFile="test/input/playground/wrashid/PSF2/pluggable/0.events.txt.gz";
		EventsManagerImpl events = new EventsManagerImpl();
		
		LinkedListValueHashMap<Id, Vehicle> vehicles=new LinkedListValueHashMap<Id, Vehicle>();
		vehicles.put(new IdImpl(1), new PlugInHybridElectricVehicle(new IdImpl(1)));
		NetworkImpl network=GeneralLib.readNetwork("test/scenarios/equil/network.xml");
		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,vehicles,network);
		
		events.addHandler(energyConsumptionPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		
		return energyConsumptionPlugin;
	}
	
	public void testLAVModel(){
		
		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelLAV("test/input/playground/wrashid/PSF2/vehicle/energyConsumption/VehicleEnergyConsumptionRegressionTable.txt",140);
		
		EnergyConsumptionPlugin energyConsumptionPlugin = runWithModel(energyConsumptionModel);
		
		assertEquals(2, energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(new IdImpl(1)).size());
	}
	
	
	public void testPSLModel(){

		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelPSL(140);
		
		EnergyConsumptionPlugin energyConsumptionPlugin = runWithModel(energyConsumptionModel);
		
		assertEquals(2, energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(new IdImpl(1)).size());
	}
	
}
