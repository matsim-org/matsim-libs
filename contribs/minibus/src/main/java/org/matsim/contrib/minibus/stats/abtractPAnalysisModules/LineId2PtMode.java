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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.HashMap;

/**
 * Some common methods for setting public transit modes for each public transit line.
 * This information should come from the mode set in the schedule itself.
 * As of now August 2012, it is not clear what the transport mode in the schedule is ment to be.
 * 
 * @author aneumann
 *
 */
public interface LineId2PtMode {

	/**
	 * 
	 * @param transitSchedule Transit schedule with the lines
	 * @param pIdentifier A String identifying paratransit lines
	 */
	void setPtModesForEachLine(TransitSchedule transitSchedule, String pIdentifier);
	
	/**
	 * 
	 * @return Returns one public transit mode for each line id
	 */
	HashMap<Id<TransitLine>, String> getLineId2ptModeMap();
	
}
