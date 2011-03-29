/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionInit.java
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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF2.chargingSchemes.dumbCharging.ARTEMISEnergyStateMaintainer_StartChargingUponArrival;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class EnergyConsumptionInit implements StartupListener {
	
	EnergyConsumptionModel energyConsumptionModel;
	EnergyConsumptionPlugin energyConsumptionPlugin;
	LinkedListValueHashMap<Id, Vehicle> vehicles;
	
	double phev=1.0;
	double ev=0.0;
	double combustion=0.0;
	
	public EnergyConsumptionInit(double phev, double ev, double combustion){
		this.phev=phev;
		this.ev=ev;
		this.combustion=combustion;
	}
	

	public LinkedListValueHashMap<Id, Vehicle> getVehicles(){
		return vehicles;
	}
	
	public EnergyConsumptionPlugin getEnergyConsumptionPlugin(){
		return energyConsumptionPlugin;
	}
	
	
	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		
		vehicles=new LinkedListValueHashMap<Id, Vehicle>();
		
		for (Id personId: controler.getPopulation().getPersons().keySet()){
			if (Math.random()<phev){
				vehicles.put(personId, new PlugInHybridElectricVehicle(new IdImpl(1)));
			} else if (Math.random()<phev+ev){
				
				vehicles.put(personId, new ElectricVehicle(null, new IdImpl(1)));
			} else{
				vehicles.put(personId, new ConventionalVehicle(null, new IdImpl(2)));
			}
		}
		
		energyConsumptionModel = new EnergyConsumptionModelPSL(140);
		energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,vehicles,controler.getNetwork());
		
		controler.getEvents().addHandler(energyConsumptionPlugin);
		
		
	}

}
