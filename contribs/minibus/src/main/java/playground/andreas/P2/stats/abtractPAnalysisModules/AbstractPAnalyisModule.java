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

package playground.andreas.P2.stats.abtractPAnalysisModules;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicles;

/**
 * 
 * Some common methods for paratransit related analysis modules.
 * 
 * @author aneumann
 *
 */
abstract class AbstractPAnalyisModule implements PAnalysisModule{
	
	private final String name;
	protected LinkedList<String> ptModes = null;
	protected HashMap<Id<TransitLine>, String> lineIds2ptModeMap;
	protected Set<Id<Person>> ptDriverIds;
	
	/**
	 * 
	 * @param name The name of the module.
	 * @param ptDriverPrefix The prefix identifying a driver of driving a public transit vehicles.
	 */
	public AbstractPAnalyisModule(String name){
		this.name = name;
		this.ptDriverIds = new TreeSet<>();
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setLineId2ptModeMap(HashMap<Id<TransitLine>, String> lineIds2ptModeMap) {
		this.lineIds2ptModeMap = lineIds2ptModeMap;
		
		if (this.ptModes == null) {
			Set<String> ptModesSet = new TreeSet<String>();
			for (String ptMode : this.lineIds2ptModeMap.values()) {
				ptModesSet.add(ptMode);
			}
			this.ptModes = new LinkedList<String>();
			
			for (String ptMode : ptModesSet) {
				this.ptModes.add(ptMode);
			}
		}
	}
	
	@Override
	public String getHeader() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + ptMode);
		}
		return strB.toString();
	}
	
	@Override
	public abstract String getResult();
	
	@Override
	public void updateVehicles(Vehicles vehicles) {
				
	}
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIds = new TreeSet<>();
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.ptDriverIds.add(event.getDriverId());
	}
}
