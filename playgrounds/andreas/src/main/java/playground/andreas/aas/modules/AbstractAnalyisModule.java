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

package playground.andreas.aas.modules;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

/**
 * 
 * Some common methods for analysis modules.
 * 
 * @author aneumann
 *
 */
public abstract class AbstractAnalyisModule {
	
	private final String name;
	protected final String ptDriverPrefix;
	protected LinkedList<String> ptModes = null;
	protected HashMap<Id,String> lineIds2ptModeMap;
	
	/**
	 * 
	 * @param name The name of the module.
	 * @param ptDriverPrefix The prefix identifying a driver of driving a public transit vehicles.
	 */
	public AbstractAnalyisModule(String name, String ptDriverPrefix){
		this.name = name;
		this.ptDriverPrefix = ptDriverPrefix;
	}
	
	/**
	 * 
	 * @return The name of that module
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * 
	 * @param lineIds2ptModeMap Is called at the beginning of each iteration. Contains on public transport mode for each line in the schedule. 
	 */
	public void setLineId2ptModeMap(HashMap<Id, String> lineIds2ptModeMap) {
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
	
	/**
	 * 
	 * @return A list of all the event handler of the module, if necessary, otherwise an empty List.
	 */
	public abstract List<EventHandler> getEventHandler();
	
	/**
	 * Hook called before the events stream is processed.
	 */
	public abstract void preProcessData();
	
	/**
	 * Hook after the events stream is processed.
	 */
	public abstract void postProcessData();
	
	/**
	 * 
	 * @return Write the results collected by the module to the given output folder.
	 */
	public abstract void writeResults(String outputFolder);
	
}
