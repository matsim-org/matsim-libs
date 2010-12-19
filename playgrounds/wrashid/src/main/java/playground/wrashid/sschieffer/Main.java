package playground.wrashid.sschieffer;

import java.io.IOException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.MatsimConfigReader;
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
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	public static double penetrationPercent=1.0;
	
	/*/
	 * battery capacity
	 * /Andersson 2010 - 10kWh
	 * Nissan Leaf - 24kWh
	 */
	
	public static double batteryCapacity= 24000*3600; //Wsec = Joules
	
	/*
	 * depth of discharge
	 * Andersson 2010 - 80%
	 * 
	 */
	public static double minCharge=0.1;
	public static double maxCharge=0.9;
	public static double startSOCInWattSeconds=batteryCapacity*(maxCharge-minCharge); // Wsec
	/*
	 * prices.... random up to now, TODO CHANGE
	 */
	public static double priceBase=0.13;
	public static double pricePeak=0.2;
	/*
	 * random assumption up to now TODO
	 * wikipedia national grid UK - average power flow of 11GW
	 */
	public static double peakLoad=Math.pow(10, 6); // adjust max peakLoad in Joule
	
	/*
	 * charging speed
	 * Andersson 2010 - 3.5kW (standard European 230V, 16A) and 15kW
	 * Kempton Tomic 2005 - 10-15kW
	 * William Kurani 2007 - 1.8-17.9kW
	 * Nissan leaf speed charge - 50kW
	 */
	public static double chargingSpeedPerSecond=50000; // Joule/second = Watt
	
	public static double secondsPerMin=60;
	public static double secondsPer15Min=15*60;
	public static double secondsPerDay=24*60*60;
	public static double slotLength=1*60; // choose min slot length and min bookable slot time
	
	public static ParkingTimesPlugin parkingTimesPlugin;
	public static EnergyConsumptionPlugin energyConsumptionPlugin;

	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		Controler controler=new Controler(configPath);
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		//penetrationPercent can be adjusted in EnergyConsumptionInit.java
		
		controler.addControlerListener(new EnergyConsumptionInit());
		
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				DecentralizedChargerV1 decentralizedChargerV1=new DecentralizedChargerV1(event.getControler(),Main.energyConsumptionPlugin,Main.parkingTimesPlugin);
				
				try {
					
					decentralizedChargerV1.performChargingAlgorithm();
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		});
		
		controler.run();		
				
	}
	
}
