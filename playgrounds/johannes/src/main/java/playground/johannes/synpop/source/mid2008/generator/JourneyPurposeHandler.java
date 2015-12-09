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

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyPurposeHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String purpose = attributes.get(VariableNames.JOURNEY_PURPOSE);
		
		if(purpose.equalsIgnoreCase("1")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.LEISURE);
		} else if(purpose.equalsIgnoreCase("2")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.LEISURE);
		} else if(purpose.equalsIgnoreCase("3")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.LEISURE);
		} else if(purpose.equalsIgnoreCase("4")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.BUSINESS);
		} else if(purpose.equalsIgnoreCase("5")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.WECOMMUTER);
		} else {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.MISC);
		}
	}

}
