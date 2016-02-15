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

package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZone;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HighStreetTariffZonesZHCity;




	public class ParkingCostCalculatorZH implements ParkingCostCalculator {

		private final CityZones zones;
		private HighStreetTariffZonesZHCity highTariffParkingZone;
		private HashSet<Id> paidStreetParking;
		private final LinkedList<PParking> parkings;
		private HashMap<Id, PParking> parkingHM;
		

		public ParkingCostCalculatorZH(CityZones zones, LinkedList<PParking> parkings) {
			this.zones = zones;
			this.parkings = parkings;
			this.highTariffParkingZone=new HighStreetTariffZonesZHCity();
			this.paidStreetParking=new HashSet<Id>();
			this.parkingHM=new HashMap<Id,PParking>();
			
			// define for steet parking, if it is paid parking or not.
			for (PParking parking:parkings){
				parkingHM.put(parking.getId(), parking);
				
				if (parking.getId().toString().contains("stp")){
					//TODO: to be more precise, I should weight the parking according to their capacity
					// but this could also work fine, because there are quite a lot of street parking facilities.
					CityZone closestZone = zones.getClosestZone(parking.getCoord());
					
					if (MatsimRandom.getRandom().nextInt(100)<closestZone.getPctNonFreeParking()){
						paidStreetParking.add(parking.getId());
					}
				}
			}
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		@Override
		public Double getParkingCost(Id parkingId, double arrivalTime, double parkingDurationInSeconds) {
			if (parkingId.toString().contains("gp") || parkingId.toString().contains("stp")){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			
			PParking parking = parkingHM.get(parkingId);
			if (parkingId.toString().contains("private") || parkingId.toString().contains("OutsideCity")){
				return 0.0;
			} else if(parkingId.toString().contains("gp")){
				// TODO: make this more detailed also for garage parking
				return zones.getClosestZone(parking.getCoord()).getParkingGarageFee2h()/2*parkingDurationInSeconds/3600;
			} else if(parkingId.toString().contains("stp")){
				if (paidStreetParking.contains(parkingId)){
					if (highTariffParkingZone.isInHighTariffZone(parking.getCoord())){
						if (parkingDurationInSeconds<30*60){
							return 0.50;
						} else {
							return 0.5 + Math.ceil((parkingDurationInSeconds-(30*60))/(30*60))*1.5;
						}
					} else {
						return Math.ceil(parkingDurationInSeconds/(60*60))*0.5;
					}
				} else {
					return 0.0;
				}
			}else if(parkingId.toString().contains("illegal") || parkingId.toString().contains("backupParking")){
				return 0.0;
			}
			
			DebugLib.stopSystemAndReportInconsistency("parking id:" + parkingId);
			
			return null;
		}

		public LinkedList<PParking> getParkings() {
			return parkings;
		}

	

}
