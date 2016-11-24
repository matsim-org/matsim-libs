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

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;


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

	@Override
	public void writeToFile(String outputFile) {
		ArrayList<String> list=new ArrayList<String>();
		
		StringBuffer sb=null;
		
		list.add(getTitleRowFileOutput());
		
		for (ChargingLogRow row:log){
			sb=new StringBuffer();
			sb.append(row.getAgentId());
			sb.append("\t");
			sb.append(row.getLinkId());
			sb.append("\t");
			sb.append(row.getStartChargingTime());
			sb.append("\t");
			sb.append(row.getChargingDuration());
			sb.append("\t");
			sb.append(row.getEnergyChargedInJoule());
			list.add(sb.toString());
		}
		
		GeneralLib.writeList(list, outputFile);
	}
}
