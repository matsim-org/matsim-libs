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
package playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.analysis.ParkingGroupOccupancies;
import org.matsim.core.controler.MatsimServices;

public class ParkingGroupOccupanciesZH extends ParkingGroupOccupancies {
	
	private MatsimServices controler;

	public ParkingGroupOccupanciesZH(){
		reset(0);
	}
	
	public ParkingGroupOccupanciesZH(MatsimServices controler){
		this.controler = controler;
		reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		//if (iteration>0){
		//	savePlot(controler.getControlerIO().getIterationFilename(iteration-1, "parkingGroupOccupancy.png"));
		//}
		super.reset(iteration);
	}
	
	
	@Override
	public String getGroupName(Id parkingId) {
		return getGroup(parkingId);
	}
	
	public static String getGroup(Id parkingId){
		if (parkingId.toString().contains("stp")) {
			return "streetParking";
		} else if (parkingId.toString().contains("gp")) {
			return "garageParking";
		} else if (parkingId.toString().contains("publicPOutsideCity")) {
			return "publicPOutsideCity";
		} else {
			return "privateParking";
		}
	}

}