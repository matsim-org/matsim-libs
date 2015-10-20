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
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Segment;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyModeHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String mode = attributes.get(VariableNames.JOURNEY_MODE);
		
		if(mode.equalsIgnoreCase("1")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);
		} else if(mode.equalsIgnoreCase("2")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("3")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("4")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("5")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_BIKE);
		} else if(mode.equalsIgnoreCase("6")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_PT);
		} else {
			leg.setAttribute(CommonKeys.LEG_MODE, "undefined");
		}
	}

}
