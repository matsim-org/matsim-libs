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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.synpop.data.Episode;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyDaysHandler implements PlanAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.PlanAttributeHandler#hanle(playground.johannes.synpop.data.PlainEpisode, java.util.Map)
	 */
	@Override
	public void hanle(Episode plan, Map<String, String> attributes) {
		int nights = Integer.parseInt(attributes.get("p1014"));
		
		if(nights < 995)
			plan.setAttribute(MIDKeys.JOURNEY_DAYS, String.valueOf(nights + 1));
	}

}
