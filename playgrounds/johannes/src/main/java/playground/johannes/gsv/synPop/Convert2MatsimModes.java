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

package playground.johannes.gsv.synPop;


import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class Convert2MatsimModes implements ProxyPlanTask {

	@Override
	public void apply(Episode plan) {
		for(Element leg : plan.getLegs()) {
			String mode = leg.getAttribute(CommonKeys.LEG_MODE);
			
			if(mode == null) {
				leg.setAttribute(CommonKeys.LEG_MODE, "undefined");
			} else if(mode.equalsIgnoreCase("rail")) {
				leg.setAttribute(CommonKeys.LEG_MODE, "pt");
			} else if(mode.equalsIgnoreCase("plane")) {
				leg.setAttribute(CommonKeys.LEG_MODE, "undefined");
			}
		}
	}

}
