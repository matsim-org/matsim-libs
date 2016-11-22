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

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.invermo.InvermoKeys;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;

import java.util.Map;

/**
 * @author johannes
 * 
 */
public class WorkLocationHandler implements PersonAttributeHandler {

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String foreign = attributes.get(VariableNames.WORK_COUNTRY);
		if (VariableNames.validate(foreign)) {
			if (foreign.equals("1")) {
				String zip = attributes.get(VariableNames.WORK_ZIP);
				if(!VariableNames.validate(zip)) {
					zip = "";
				}
				
				String town = attributes.get(VariableNames.WORK_TOWN);
				if(!VariableNames.validate(town)) {
					town = "";
				}
				
				person.setAttribute(InvermoKeys.WORK_LOCATION, String.format("%s %s", zip, town));
				
			} else if (foreign.equals("2")) {
				String town = attributes.get("arbstadta");
				if(!VariableNames.validate(town)) {
					town = "";
				}
				
				String country = attributes.get("arblandname");
				if(!VariableNames.validate(country)) {
					country = "";
				}
				
				person.setAttribute(InvermoKeys.WORK_LOCATION, String.format("%s %s", town, country));
			}
		}
		
	}
}
