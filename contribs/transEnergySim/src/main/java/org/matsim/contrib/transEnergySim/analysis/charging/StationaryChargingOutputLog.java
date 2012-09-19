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

package org.matsim.contrib.transEnergySim.analysis.charging;

// note somewhere, the facilityId can be a parking facility or a normal facilityId, if parking module not used
public class StationaryChargingOutputLog extends ChargingOutputLog {

	public StationaryChargingOutputLog(){
		super();
	}
	
	
	@Override
	public String getTitleRowFileOutput() {
		return "agentId\tfacilityId\tstartChargingTime\tchargingDuration\tenergyChargedInJoule";
	}
	
	
	@Override
	public void printToConsole(){
		System.out.println(getTitleRowFileOutput());
		
		for (ChargingLogRow row:log){
			ChargingLogRowFacilityLevel chargingLog=(ChargingLogRowFacilityLevel) row;
			System.out.println(row.getAgentId() + "\t" + chargingLog.getFacilityId()  + "\t" + row.getStartChargingTime() + "\t" + row.getChargingDuration() + "\t" + row.getEnergyChargedInJoule());
		}
	}
	
	
	
}
