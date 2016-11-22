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

import playground.johannes.synpop.data.PlainElement;
import playground.johannes.synpop.source.invermo.InvermoKeys;
import playground.johannes.synpop.source.invermo.generator.AttributeHandler;
import playground.johannes.synpop.source.invermo.generator.VariableNames;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class HouseholdStationHandler implements AttributeHandler<PlainElement> {

	@Override
	public void handleAttribute(PlainElement household, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.STATION_NAME);
		if(VariableNames.validate(val)) {
			household.setAttribute(InvermoKeys.STATION_NAME, val);
		}
		
		val = attributes.get(VariableNames.STATION_DIST);
		if(VariableNames.validate(val)) {
			double dist = Double.parseDouble(val);
			household.setAttribute(InvermoKeys.STATION_DIST, String.valueOf(dist));
		}
		
		val = attributes.get("wohnplz");
		System.out.println(attributes.get("wohnplz") + " " + attributes.get("wohnort"));
		
	}

}
