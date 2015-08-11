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

import playground.johannes.synpop.data.Attributable;

/**
 * @author johannes
 *
 */
public class LegStartLocHandler implements LegAttributeHandler {

	@Override
	public void handle(Attributable leg, int idx, String key, String value) {
		if(key.contains(ColumnKeys.START1_TRIP1)) {
			if(value.equalsIgnoreCase("1")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "home");
			} else if(value.equalsIgnoreCase("2")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "work");
			}
		} else if(key.contains(ColumnKeys.START2_TRIP1)) {
			if(value.equalsIgnoreCase("1")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "secondHome");
			} else if(key.equalsIgnoreCase("2")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "friends");
			} else if(key.equalsIgnoreCase("3")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "buisiness");
			}
			
		} else if(key.contains(ColumnKeys.START1_TRIP2) || key.contains(ColumnKeys.START1_TRIP3)) {
			if(value.equalsIgnoreCase("1")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "prev");
			} else if(value.equalsIgnoreCase("2")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "sameTown");
			}
		} else if(key.contains(ColumnKeys.START2_TRIP2) || key.contains(ColumnKeys.START2_TRIP3)) {
			leg.setAttribute(InvermoKeys.START_LOCATION, value);
		} else if(key.contains(ColumnKeys.START1_TRIP4)) {
			if(value.equals("1")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "prev");
			} else if(value.equals("2")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "other");
			}
		} else if(key.contains("e4start1a")) {
			if(value.equals("1")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "germany");
			} else if(value.equals("2")) {
				leg.setAttribute(InvermoKeys.START_LOCATION, "foreign");
			}
		} else if(key.contains("e4startd1") || key.contains("e4startd2") || key.contains("e4startd3") || key.contains("e4startd4")) {
			String desc = leg.getAttribute(InvermoKeys.START_LOCATION);
			if(desc == null) {
				desc = value;
			} else {
				desc = desc + ", " + value;
			}
			leg.setAttribute(InvermoKeys.START_LOCATION, desc);
		}
	}

}
