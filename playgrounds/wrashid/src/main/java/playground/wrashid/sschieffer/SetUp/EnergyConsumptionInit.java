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

package playground.wrashid.sschieffer.SetUp;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;


import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * initializes energyConsumptionPlugin and 
 * vehicles for all agents according to desired percentages of EV/PHEV/normal car
 * 
 * @author Stella
 *
 */
public class EnergyConsumptionInit implements StartupListener {
	
	
	private LinkedListValueHashMap<Id, Vehicle> vehicles=new LinkedListValueHashMap<Id, Vehicle>();
	private LinkedListValueHashMap<Id, Vehicle> electricVehicles=new LinkedListValueHashMap<Id, Vehicle>();
	private ParkingTimesPlugin parkingTimesPlugin;
	private EnergyConsumptionPlugin energyConsumptionPlugin;
	
	private double electrification;
	private double ev;
	
	public EnergyConsumptionInit(			
			double electrification,double ev){
				
		this.electrification=electrification;
		this.ev=ev;
	}
	
	
	
	public LinkedListValueHashMap<Id, Vehicle> getElectricVehicles(){
		return electricVehicles;
	}
	
	
	public EnergyConsumptionPlugin getEnergyConsumptionPlugin(){
		return energyConsumptionPlugin;
	}
	
	
	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		
		int totalPpl= (int) Math.round(controler.getPopulation().getPersons().size()*electrification);
		
		int count = 0;
		for (Id personId: controler.getPopulation().getPersons().keySet()){
			
				if (count< totalPpl*ev){
					ElectricVehicle ev= new ElectricVehicle(null, new IdImpl(1));
					vehicles.put(personId, ev);
					electricVehicles.put(personId, ev);
				}else{
					if(count< totalPpl){
						PlugInHybridElectricVehicle phev= new PlugInHybridElectricVehicle(new IdImpl(1));
						vehicles.put(personId,phev );
						electricVehicles.put(personId, phev);
					}else{
						vehicles.put(personId,new ConventionalVehicle(null, new IdImpl(2)));
					}
				}
				count++;
		}
		
		EnergyConsumptionModelPSL energyConsumptionModel = new EnergyConsumptionModelPSL(140);
		
		electricVehicles.put(Vehicle.getPlaceholderForUnmappedPersonIds(),new ConventionalVehicle(null, new IdImpl(2)));
		
		energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel, electricVehicles, controler.getNetwork());
		
		controler.getEvents().addHandler(energyConsumptionPlugin);
		
		
				
	}

}
