/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedChargerV1.java
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

package playground.wrashid.sschieffer;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicles;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DecentralizedChargerV1 {

	private LinkedListValueHashMap<Id, Double> energyConsumptionOfLegs;
	private LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals;
	//TODO add method to return vehicles in energyConsumptionPlugin
	//LinkedListValueHashMap<Id, Vehicle> vehicles=energyConsumptionPlugin.
	
	private double priceBase=0.13;
	private double pricePeak=0.2;
	private double peakLoad=10000;
	
	
	private double populationTotal;
	private double penetration;
	private double averagePHEVConsumption;
	final Controler controler;
	
	/**
	 * Public constructor
	 */
	public DecentralizedChargerV1(Controler controler, EnergyConsumptionPlugin energyConsumptionPlugin, ParkingTimesPlugin parkingTimesPlugin){
		
		this.controler=controler;
		energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfLegs();
		parkingTimeIntervals = parkingTimesPlugin.getParkingTimeIntervals();
		
	}
	
	/**
	 * Output call of the Decentralized Charger!
	 * @param startChargingTime
	 * @param endChargingTime
	 * @param agentId
	 * @param linkId
	 */
	public void getElectricityFromGrid(double startChargingTime, double endChargingTime, Id agentId, Id linkId){
		System.out.println();
	}
	
	
	public void performChargingAlgorithm() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException {
		
		populationTotal=controler.getPopulation().getPersons().size();
		penetration = populationTotal*Main.penetrationPercent;
		averagePHEVConsumption= getAveragePHEVConsumption(penetration);
		DecentralizedChargerInfo myChargerInfo = new DecentralizedChargerInfo(peakLoad, penetration, averagePHEVConsumption, priceBase, pricePeak); 
		
		//
		
		
		
		
		for (Id personId: controler.getPopulation().getPersons().keySet()){
			LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
			
			System.out.println("energyConsumption of personId " + personId);
			
			for (int i=0;i<legEnergyConsumptionList.size();i++) {
				Double legEnergyConsumption = legEnergyConsumptionList.get(i);
				System.out.println("leg:" + i + " => " + legEnergyConsumption);
			}
			
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			
			System.out.println("parking intervals of personId " + personId);
			
			for (int i=0;i<parkingIntervals.size();i++) {
				ParkingIntervalInfo parkingIntervalInfo = parkingIntervals.get(i);
				System.out.println("parking dep. time:" + i + " => " + parkingIntervalInfo.getDepartureTime());
			}
		}
	}
		
	/**
	 * adds up the Consumptions of all PHEV owners and divides results by Penetration
	 * @return
	 */
		public double getAveragePHEVConsumption(double penetration){
			double sumOfAllConsumptions=0;
		
			for (Id personId: controler.getPopulation().getPersons().keySet()){
				LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
				
				// person has PHEV then add his total consumption to sumOfAllConsumptions
				PlugInHybridElectricVehicle dummyPHEV= new PlugInHybridElectricVehicle(new IdImpl(1));
				//only works if every person has only one car?
				if (Main.vehicles.get(personId).getClass().equals(dummyPHEV.getClass())){
					for (int i=0;i<legEnergyConsumptionList.size();i++) {
						sumOfAllConsumptions = sumOfAllConsumptions + legEnergyConsumptionList.get(i);
					}	
				}
			}
			return (sumOfAllConsumptions/penetration);
		}
		
		
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

