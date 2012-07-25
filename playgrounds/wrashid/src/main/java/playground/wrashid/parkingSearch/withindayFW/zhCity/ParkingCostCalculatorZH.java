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

package playground.wrashid.parkingSearch.withindayFW.zhCity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;




	public class ParkingCostCalculatorZH implements ParkingCostCalculator {

		private final CityZones zones;
		private final ScenarioImpl scenario;
		private HighStreetTariffZonesZHCity highTariffParkingZone;
		private HashSet<Id> paidStreetParking;

		public ParkingCostCalculatorZH(CityZones zones, ScenarioImpl scenario, LinkedList<Parking> parkings) {
			this.zones = zones;
			this.scenario = scenario;
			this.highTariffParkingZone=new HighStreetTariffZonesZHCity();
			
			this.paidStreetParking=new HashSet<Id>();
			
			// define for steet parking, if it is paid parking or not.
			for (Parking parking:parkings){
				if (parking.getId().toString().contains("sp")){
					CityZone closestZone = zones.getClosestZone(parking.getCoord());
					
					if (MatsimRandom.getRandom().nextInt(100)<closestZone.getPctNonFreeParking()){
						paidStreetParking.add(parking.getId());
					}
				}
			}
		}
		
		@Override
		public Double getParkingCost(Id parkingFacilityId, double arrivalTime, double parkingDurationInSeconds) {
			if (parkingFacilityId.toString().contains("gp") || parkingFacilityId.toString().contains("stp")){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			ActivityFacility parkingFacility = scenario.getActivityFacilities().getFacilities().get(parkingFacilityId);
			
			if (parkingFacilityId.toString().contains("private") || parkingFacilityId.toString().contains("OutsideCity")){
				return 0.0;
			} else if(parkingFacilityId.toString().contains("gp")){
				// TODO: make this more detailed also for garage parking
				return zones.getClosestZone(parkingFacility.getCoord()).getParkingGarageFee2h()/2*parkingDurationInSeconds/3600;
			} else if(parkingFacilityId.toString().contains("stp")){
				if (paidStreetParking.contains(parkingFacilityId)){
					if (highTariffParkingZone.isInHighTariffZone(parkingFacility.getCoord())){
						if (parkingDurationInSeconds<30*60){
							return 0.50;
						} else {
							return 0.5 + Math.ceil(parkingDurationInSeconds/(30*60))*1.5;
						}
					} else {
						return Math.ceil(parkingDurationInSeconds/(60*60))*0.5;
					}
				} else {
					return 0.0;
				}
			}
			
			DebugLib.stopSystemAndReportInconsistency("parking id:" + parkingFacilityId);
			
			return null;
		}

	

}
