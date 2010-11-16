package playground.wrashid.sschieffer;

import java.io.IOException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
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

//package playground.wrashid.sschieffer;
//
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class Main {

	
	public static ParkingTimesPlugin parkingTimesPlugin;
	public static EnergyConsumptionPlugin energyConsumptionPlugin;

	public static void main(String[] args) {
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		Controler controler=new Controler(configPath);
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		
		controler.addControlerListener(new EnergyConsumptionInit());
		
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				DecentralizedChargerV1 decentralizedChargerV1=new DecentralizedChargerV1();
				try {
					
					
					decentralizedChargerV1.performChargingAlgorithm(Main.energyConsumptionPlugin,Main.parkingTimesPlugin);
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}

		});
		
		
		
		
		controler.run();		
		
		// need to get information 
		// which slots I have access to electricity
		// how many of the time slots I need to charge
		// what is my consumption
		
		//TODO check general feasibility and case specific feasibility
		// TODO call chargeInAllValidSlots
		// TODO call chargeInAllValidSlots and addPeakSlots(int n)
		// TODO call chooseSlots()
		
			
		
		// TODO: physical feasibility test given the time plan and charging plan
		// for case 1 and 2, which route becomes infeasible
		// add in one more charging slot
		// check again...
		
		// for case 3 check if choice of slots within possible slots is valid
		// if not generate a different set 
		// if after (n over k) iterations no suitable solution is found
		// change to case 1 and 2 procedure
		
		
		
	}
	
}
