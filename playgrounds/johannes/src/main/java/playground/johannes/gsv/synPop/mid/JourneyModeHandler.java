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
import playground.johannes.synpop.data.PlainElement;

/**
 * @author johannes
 *
 */
public class JourneyModeHandler implements LegAttributeHandler {

	@Override
	public void handle(PlainElement leg, Map<String, String> attributes) {
		String mode = attributes.get("hvm_r");
		
		if(mode.equalsIgnoreCase("Auto")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_CAR);
		} else if(mode.equalsIgnoreCase("Bahn")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("Reisebus")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("Flugzeug")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("Schiff")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_PT);
		} else if(mode.equalsIgnoreCase("Schiff")) {
			leg.setAttribute(CommonKeys.LEG_MODE, CommonKeys.LEG_MODE_PT);
		} else {
			leg.setAttribute(CommonKeys.LEG_MODE, "undefined");
		}
	}

}
