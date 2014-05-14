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
import playground.johannes.gsv.synPop.ProxyLeg;

/**
 * @author johannes
 *
 */
public class LegOriginHandler implements LegAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(ProxyLeg leg, Map<String, String> attributes) {
		String origin = attributes.get(MIDKeys.LEG_ORIGIN);
		if(origin.equalsIgnoreCase("1"))
			leg.setAttribute(CommonKeys.LEG_ORIGIN, "home");
		else if(origin.equalsIgnoreCase("2"))
			leg.setAttribute(CommonKeys.LEG_ORIGIN, "work");
		else
			leg.setAttribute(CommonKeys.LEG_ORIGIN, "misc");
	}

}
