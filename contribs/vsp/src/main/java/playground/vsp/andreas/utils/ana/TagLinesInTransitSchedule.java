/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.utils.ana;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Tags all routes of lines starting with a given identifier.
 * 
 * @author aneumann
 *
 */
public class TagLinesInTransitSchedule {
	
	private final static Logger log = Logger.getLogger(TagLinesInTransitSchedule.class);
	
	public static TransitSchedule tagLinesInTransitSchedule(TransitSchedule transitSchedule, String identifier){
		int routesTagged = 0;
		int linesTagged = 0;

		for (Entry<Id<TransitLine>, TransitLine> transitLineEntry : transitSchedule.getTransitLines().entrySet()) {
			if (transitLineEntry.getKey().toString().startsWith(identifier)) {
				linesTagged++;
				for (TransitRoute transitRoute : transitLineEntry.getValue().getRoutes().values()) {
					transitRoute.setTransportMode(identifier);
					routesTagged++;
				}
			}
		}
		
		log.info("Tagged " + linesTagged + " lines and " + routesTagged + " routes.");
		return transitSchedule;
	}
	
	public static void tagLinesInTransitSchedule(TransitSchedule transitSchedule, HashMap<Id<TransitLine>, String> lineId2ptModeMap) {
		int routesTagged = 0;
		int linesTagged = 0;
		
		for (Entry<Id<TransitLine>, String> lineId2ptModeEntry : lineId2ptModeMap.entrySet()) {
			TransitLine transitLine = transitSchedule.getTransitLines().get(lineId2ptModeEntry.getKey());
			linesTagged++;
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				transitRoute.setTransportMode(lineId2ptModeEntry.getValue());
				routesTagged++;
			}
		}

		log.info("Tagged " + linesTagged + " lines and " + routesTagged + " routes.");
	}
}
