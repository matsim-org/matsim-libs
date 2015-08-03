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

import java.util.Map;

import playground.johannes.synpop.data.PlainElement;

/**
 * @author johannes
 *
 */
public class HouseholdLocationHandler implements AttributeHandler<PlainElement> {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.invermo.AttributeHandler#handleAttribute(java.lang.Object, java.util.Map)
	 */
	@Override
	public void handleAttribute(PlainElement household, Map<String, String> attributes) {
		String town = attributes.get(ColumnKeys.HOME_TOWN);
		if(!ColumnKeys.validate(town))
			town = "";
		
		String zip = attributes.get(ColumnKeys.HOME_ZIPCODE);
		if(ColumnKeys.validate(zip)) {
			int code = Integer.parseInt(zip);
			household.setAttribute(InvermoKeys.HOME_LOCATION, String.format("%05d %s", code, town));
		}
		
	}

}
