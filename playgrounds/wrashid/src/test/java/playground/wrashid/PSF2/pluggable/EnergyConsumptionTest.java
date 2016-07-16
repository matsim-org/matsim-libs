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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleType;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelLAV;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

public class EnergyConsumptionTest extends MatsimTestCase {

	private EnergyConsumptionPlugin runWithModel(EnergyConsumptionModel energyConsumptionModel){
		String eventsFile="test/input/playground/wrashid/PSF2/pluggable/0.events.xml";
		EventsManager events = EventsUtils.createEventsManager();
		
		LinkedListValueHashMap<Id<Vehicle>, Vehicle> vehicles=new LinkedListValueHashMap<>();
		vehicles.put(Id.create(1, Vehicle.class), new PlugInHybridElectricVehicle(Id.create(1, VehicleType.class)));
		Network network=GeneralLib.readNetwork("test/scenarios/equil/network.xml");
		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,vehicles,network);
		
		events.addHandler(energyConsumptionPlugin);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(eventsFile);
		
		
		return energyConsumptionPlugin;
	}
	
	public void testLAVModel(){
		
		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelLAV("test/input/playground/wrashid/PSF2/vehicle/energyConsumption/VehicleEnergyConsumptionRegressionTable.txt",140);
		
		EnergyConsumptionPlugin energyConsumptionPlugin = runWithModel(energyConsumptionModel);
		
		assertEquals(2, energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(Id.create(1, Person.class)).size());
	}
	
	
	public void testPSLModel(){

		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelPSL(140);
		
		EnergyConsumptionPlugin energyConsumptionPlugin = runWithModel(energyConsumptionModel);
		
		assertEquals(2, energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(Id.create(1, Person.class)).size());
		assertEquals(1.8786265875308476E7, energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(Id.create(1, Person.class)).get(0));
	}
	
}
