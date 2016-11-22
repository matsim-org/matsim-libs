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

package playground.johannes.synpop.source.invermo.generator;

import playground.johannes.synpop.data.PlainElement;
import playground.johannes.synpop.source.invermo.InvermoKeys;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class HouseholdLocationHandler implements AttributeHandler<PlainElement> {

	@Override
	public void handleAttribute(PlainElement household, Map<String, String> attributes) {
		String town = attributes.get(VariableNames.HOME_TOWN);
		if(!VariableNames.validate(town))
			town = "";
		
		String zip = attributes.get(VariableNames.HOME_ZIPCODE);
		if(VariableNames.validate(zip)) {
			int code = Integer.parseInt(zip);
			household.setAttribute(InvermoKeys.HOME_LOCATION, String.format("%05d %s", code, town));
		}
		
	}

}
