/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class ValidateDatesTask implements EpisodeTask {

	private final Map<String, String> replacements;
	
	public ValidateDatesTask() {
		replacements = new HashMap<String, String>();
		replacements.put("0", "2000");
		replacements.put("1", "2001");
		replacements.put("2", "2002");
		replacements.put("99", "1999");
		replacements.put("3899", "1999");
		replacements.put("82", "1982");
		replacements.put("98", "1998");
	}

	@Override
	public void apply(Episode plan) {
		for(Attributable leg : plan.getLegs()) {
			String startYear = leg.getAttribute("startTimeYear");
			if(startYear != null)
				leg.setAttribute("startTimeYear", validate(startYear));
			
			String endYear = leg.getAttribute("endTimeYear");
			if(endYear != null)
				leg.setAttribute("endTimeYear", validate(endYear));
		}
	}

	private String validate(String str) {
		String replace = replacements.get(str);
		if(replace == null) {
			return str;
		} else {
			return replace;
		}
	}
}
