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

import playground.johannes.synpop.data.Element;

/**
 * @author johannes
 *
 */
public class LegDestinationLocHandler implements LegAttributeHandler {

	@Override
	public void handle(Element leg, int idx, String key, String value) {
		if(key.endsWith("ziel0")) {
			if(value.equals("1")) {
				leg.setAttribute(InvermoKeys.DESTINATION_LOCATION, "home");
			} else if(value.equals("2")){
				leg.setAttribute(InvermoKeys.DESTINATION_LOCATION, "work");
			}
		} else if(key.endsWith("zielland") || key.endsWith("zieldort") || key.endsWith("ziela3")) {
//			if(value.equalsIgnoreCase("La Gomera/Valle Gran Rey")) {
//				System.err.println();
//			}
			String desc = leg.getAttribute(InvermoKeys.DESTINATION_LOCATION);
			if(desc == null) {
				desc = value;
			} else {
				desc = desc + ", " + value;
			}
			leg.setAttribute(InvermoKeys.DESTINATION_LOCATION, desc);
		}

	}

}
