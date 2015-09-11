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

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.generator.LegAttributeHandler;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyPurposeHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String purpose = attributes.get("p101");
		
		if(purpose.equalsIgnoreCase("Ausflug, Urlaub, Kurzreise zu touristischen Zielen")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "vacations");
		} else if(purpose.equalsIgnoreCase("Besuche von Freunden oder Bekannten")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "vacations");
		} else if(purpose.equalsIgnoreCase("andere Privatreise")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "vacations");
		} else if(purpose.equalsIgnoreCase("Dienst- oder Geschäftsreise")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.BUSINESS);
		} else if(purpose.equalsIgnoreCase("Fahrt als Berufspendler | Wochenendpendler")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "wecommuter");
		} else {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityTypes.MISC);
		}
	}

}
