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

import javax.management.loading.PrivateClassLoader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingInfrastructureZH extends ParkingInfrastructure{

	private HashMap<Id,Parking> parkings;
	// activity facility Id, activityType, parking facility id
	private TwoHashMapsConcatenated<Id, String, Id> privateParkingFacilityIdMapping;

	public ParkingInfrastructureZH(Scenario scenario, HashMap<String, HashSet<Id>> parkingTypes,
			ParkingCostCalculator parkingCostCalculator,LinkedList<Parking> parkings) {
		super(scenario, parkingTypes, parkingCostCalculator);
		
		this.parkings=new HashMap<Id, Parking>();
		privateParkingFacilityIdMapping=new TwoHashMapsConcatenated<Id, String, Id>();
		for (Parking parking:parkings){
			this.parkings.put(parking.getId(), parking);
			if (!parking.getType().equalsIgnoreCase("public")){
				PrivateParking privateParking=(PrivateParking) parking;
				ActivityFacility activityFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parking.getId());
				nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), activityFacility);
			
				privateParkingFacilityIdMapping.put(privateParking.getActInfo().getFacilityId(), privateParking.getActInfo().getActType(), parking.getId());
			} else {
				
			}
			
			
		}
	}
	
	public ActivityFacility getFreePrivateParking(Id actFacilityId, String actType){
		Id parkingFacilityId = privateParkingFacilityIdMapping.get(actFacilityId, actType);
		
		ActivityFacility parkingFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parkingFacilityId);
	
		if (parkingFacility==null){
			return null;
		}
		
		if (getFreeCapacity(parkingFacility.getId())>0){
			return parkingFacility;
		} else {
			return null;
		}
	}
	
}
