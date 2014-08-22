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

import playground.johannes.gsv.synPop.ProxyObject;

/**
 * @author johannes
 *
 */
public class LegStartLocHandler implements LegAttributeHandler {

	@Override
	public void handle(ProxyObject leg, int idx, String key, String value) {
		if(key.contains("e1start1")) {
			if(value.equalsIgnoreCase("1")) {
				leg.setAttribute("startLoc", "home");
			} else if(value.equalsIgnoreCase("2")) {
				leg.setAttribute("startLoc", "work");
			}
		} else if(key.contains("e1start2")) {
			leg.setAttribute("e1start2", value);
		} else if(key.contains("e2start1")) {
			if(value.equalsIgnoreCase("1")) {
				leg.setAttribute("startLoc", "prev");
			} else if(value.equalsIgnoreCase("2")) {
//				String loc = 
			}
		}
	}

}
