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

import java.util.Map;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.generator.LegAttributeHandler;

/**
 * @author johannes
 *
 */
public class LegStartTimeHandler implements LegAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.generator.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String hour = attributes.get(MIDKeys.LEG_START_TIME_HOUR);
		String min = attributes.get(MIDKeys.LEG_START_TIME_MIN);
		String nextDay = attributes.get(MIDKeys.START_NEXT_DAY);
		
		if(hour.equalsIgnoreCase("301") || min.equalsIgnoreCase("301"))
			return;
		
		int time = Integer.parseInt(min) * 60 + Integer.parseInt(hour) * 60 * 60;

		if(nextDay != null && nextDay.equalsIgnoreCase("Folgetag")) {
			time += 86400;
		}
		
		leg.setAttribute(CommonKeys.LEG_START_TIME, String.valueOf(time));
	}

}
