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

import org.matsim.api.core.v01.Id;


public class InductiveChargingAtRoadOutputLog extends ChargingOutputLog {

	public InductiveChargingAtRoadOutputLog(){
		super();
		
		
	}

	@Override
	public String getTitleRowFileOutput() {
		return "agentId\tlinkId\tstartChargingTime\tchargingDuration\tenergyChargedInJoule";
	}
	
	@Override
	public void printToConsole(){
		System.out.println(getTitleRowFileOutput());
		
		for (ChargingLogRow row:log){
			System.out.println(row.getAgentId() + "\t" + row.getLinkId() + "\t" + row.getStartChargingTime() + "\t" + row.getChargingDuration() + "\t" + row.getEnergyChargedInJoule() );
		}
	}
	
	public void readFromFile(String fileName){
		//TODO:
	}
}
