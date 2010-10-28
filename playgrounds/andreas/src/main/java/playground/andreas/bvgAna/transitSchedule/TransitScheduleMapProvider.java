/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.transitSchedule;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitSchedule;

/**
 * Builds a map containing the line id for each route id
 * 
 * @author aneumann
 *
 */
public class TransitScheduleMapProvider {
	
	private final Logger log = Logger.getLogger(TransitScheduleMapProvider.class);
	private final Level logLevel = Level.DEBUG;

	private TransitSchedule transitSchedule;
	private TreeMap<Id, Id> routeId2lineIdMap;
	
	public TransitScheduleMapProvider(TransitSchedule transitSchedule){
		this.log.setLevel(this.logLevel);
		this.transitSchedule = transitSchedule;
	}
	
	/**
	 * @return Returns a map containing the line id for each route id
	 */
	public TreeMap<Id, Id> getRouteId2lineIdMap(){
		if(this.routeId2lineIdMap == null){
			createRouteId2lineIdMap();
		}
		return this.routeId2lineIdMap;
	}

	private void createRouteId2lineIdMap() {
		this.log.debug("Creating routeId2lineIdMap...");
		
		TreeMap<Id, Id> map = new TreeMap<Id, Id>();
		
		for (Entry<Id, TransitLine> lineEntry : this.transitSchedule.getTransitLines().entrySet()) {
			Id lineId = lineEntry.getKey();
			
			for (Id routeId : lineEntry.getValue().getRoutes().keySet()) {
				map.put(routeId, lineId);
			}
		}
		
		this.log.debug("... finished");
		this.routeId2lineIdMap = map;		
	}
}
