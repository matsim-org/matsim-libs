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

package org.matsim.contrib.transEnergySim.analysis.energyConsumption;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRow;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowFacilityLevel;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowLinkLevel;

/**
 * 
 * 
 * 
 * @author User
 *
 */
public class EnergyConsumptionOutputLog {

	private LinkedList<EnergyConsumptionLogRow> log;

	public EnergyConsumptionOutputLog() {
		reset();
	}

	public void reset() {
		log = new LinkedList<EnergyConsumptionLogRow>();
	}

	public void add(EnergyConsumptionLogRow row) {
		log.add(row);
	}

	public EnergyConsumptionLogRow get(int i) {
		return log.get(i);
	}

	public int getNumberOfEntries() {
		return log.size();
	}

	public String getTitleRowFileOutput() {
		return "agentId\tlinkId\tenergyConsumedInJoules";
	}

	public void printToConsole() {
		System.out.println(getTitleRowFileOutput());
		
		for (EnergyConsumptionLogRow row:log){
			System.out.println(row.getAgentId() + "\t" + row.getLinkId() + "\t" + row.getEnergyConsumedInJoules());
		}
	}

	public void writeToFile(String outputFile) {
		ArrayList<String> list=new ArrayList<String>();
		
		StringBuffer sb=null;
		
		list.add(getTitleRowFileOutput());
		
		for (EnergyConsumptionLogRow row:log){
			sb=new StringBuffer();
			sb.append(row.getAgentId());
			sb.append("\t");
			sb.append(row.getLinkId());
			sb.append("\t");
			sb.append(row.getEnergyConsumedInJoules());
			list.add(sb.toString());
		}
		
		GeneralLib.writeList(list, outputFile);
	}
	
	public int size(){
		return log.size();
	}

}
