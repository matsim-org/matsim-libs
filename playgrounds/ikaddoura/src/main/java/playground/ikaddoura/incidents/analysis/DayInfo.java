/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.analysis;

import java.util.HashMap;
import java.util.Map;


/**
* @author ikaddoura
*/

public class DayInfo {
	
	private String day;
	private Map<Integer, TimeVariantLinkInfo> timeBin2info = new HashMap<>();
	
	public DayInfo(String day, Map<Integer, TimeVariantLinkInfo> timeBin2info) {
		this.day = day;
		this.timeBin2info = timeBin2info;
	}

	public String getDay() {
		return day;
	}

	public Map<Integer, TimeVariantLinkInfo> getTimeBin2info() {
		return timeBin2info;
	}
		
}

