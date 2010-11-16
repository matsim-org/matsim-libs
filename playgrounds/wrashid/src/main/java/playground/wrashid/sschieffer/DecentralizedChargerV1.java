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

import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DecentralizedChargerV1 {

	
	
	public void performChargingAlgorithm(DecentralizedChargerInfo chargerInfo){
		// TODO: implement method (main starting point of whole programming exercise...)
		
		// STELLA MAIN
		
		
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



	public void performChargingAlgorithm(EnergyConsumptionPlugin energyConsumptionPlugin, ParkingTimesPlugin parkingTimesPlugin, Controler controler) {
		LinkedListValueHashMap<Id, Double> energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfLegs();
		LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals = parkingTimesPlugin.getParkingTimeIntervals();
		

		
		
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

}
