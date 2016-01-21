/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.wrashid.ABMT.rentParking;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.core.config.Config;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingCostCalculatorZH;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;

public class ParkingCostModelZH implements ParkingCostModel {

	private ParkingCostCalculatorZH parkingCostCalculator;

	public ParkingCostModelZH(Config config, LinkedList<PParking> parkings){
		String cityZonesFilePath = config.getParam("parkingChoice.ZH", "cityZonesFile");
		
		parkingCostCalculator = new ParkingCostCalculatorZH(new CityZones(cityZonesFilePath), parkings);
	}
	
	@Override
	public double calcParkingCost(double arrivalTimeInSeconds, double durationInSeconds, Id personId, Id parkingFacilityId) {
		//return 0;
		return parkingCostCalculator.getParkingCost(parkingFacilityId, arrivalTimeInSeconds, durationInSeconds);
	}


}
