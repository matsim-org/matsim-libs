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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class LegTimeHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		int time = calcSeconds(attributes, true);
		leg.setAttribute(CommonKeys.LEG_START_TIME, String.valueOf(time));

		time = calcSeconds(attributes, false);
		leg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(time));

	}

	private int calcSeconds(Map<String, String> attributes, boolean mode) {
		String hKey = VariableNames.LEG_END_TIME_HOUR;
		String mKey = VariableNames.LEG_END_TIME_MIN;
		String dKey = VariableNames.END_NEXT_DAY;

		if(mode) {
			hKey = VariableNames.LEG_START_TIME_HOUR;
			mKey = VariableNames.LEG_START_TIME_MIN;
			dKey = VariableNames.START_NEXT_DAY;
		}

		String hour = attributes.get(hKey);
		String min = attributes.get(mKey);
		String nextDay = attributes.get(dKey);

		int time = Integer.parseInt(min) * 60 + Integer.parseInt(hour) * 60 * 60;

		if(nextDay != null && nextDay.equalsIgnoreCase("1")) {
			time += 86400;
		}

		return time;
	}
}
