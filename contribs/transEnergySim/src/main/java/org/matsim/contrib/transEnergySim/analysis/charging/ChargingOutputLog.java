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

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionLogRow;
import org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads.LinkEvent;
import org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads.LinkValueChangeEvent;
import org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads.LinkVisualizationQueue;
import org.matsim.facilities.ActivityFacilities;

public abstract class ChargingOutputLog {
	
	
	protected LinkedList<ChargingLogRow> log;
	
	
	public ChargingOutputLog(){
		reset();
	}
	
	public void reset(){
		log=new LinkedList<ChargingLogRow>();
	}
	
	public void add(ChargingLogRowLinkLevel row){
		log.add(row);
	}
	
	public ChargingLogRow get(int i){
		return log.get(i);
	}
	
	public int getNumberOfEntries(){
		return log.size();
	}
	
	protected abstract String getTitleRowFileOutput();
	
	public abstract void printToConsole();
	
	public abstract void writeToFile(String outputFile);
	
	public int size(){
		return log.size();
	}
	
	public LinkVisualizationQueue getLinkEventsQueue(){
		LinkVisualizationQueue visualizationQueue=new LinkVisualizationQueue();
		DoubleValueHashMap<Id> initValueAtLinks=new DoubleValueHashMap<Id>();
		
		for (ChargingLogRow row:log){
			double power=row.getEnergyChargedInJoule()/row.getChargingDuration();
			Id linkId = row.getLinkId();
			visualizationQueue.addEvent(new LinkValueChangeEvent(row.getStartChargingTime(), power, linkId));
			double endChargingTime = GeneralLib.projectTimeWithin24Hours(row.getStartChargingTime() + row.getChargingDuration());
			visualizationQueue.addEvent(new LinkValueChangeEvent(endChargingTime,-1.0* power, linkId));
		
			if (GeneralLib.isIn24HourInterval(row.getStartChargingTime(), endChargingTime, 0)){
				initValueAtLinks.incrementBy(linkId,power);
			}
			
		}
		
		visualizationQueue.setInitValues(initValueAtLinks);
		
		return visualizationQueue;
	}
	
}
