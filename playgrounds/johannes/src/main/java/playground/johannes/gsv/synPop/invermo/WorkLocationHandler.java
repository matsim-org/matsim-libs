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

import playground.johannes.gsv.synPop.mid.PersonAttributeHandler;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Map;

/**
 * @author johannes
 * 
 */
public class WorkLocationHandler implements PersonAttributeHandler {

	@Override
	public void handle(PlainPerson person, Map<String, String> attributes) {
		String foreign = attributes.get("arbland");
		if (ColumnKeys.validate(foreign)) {
			if (foreign.equals("1")) {
				String zip = attributes.get("arbplzd");
				if(!ColumnKeys.validate(zip)) {
					zip = "";
				}
				
				String town = attributes.get("arbstadtd");
				if(!ColumnKeys.validate(town)) {
					town = "";
				}
				
				person.setAttribute("workLoc", String.format("%s %s", zip, town));
				
			} else if (foreign.equals("2")) {
				String town = attributes.get("arbstadta");
				if(!ColumnKeys.validate(town)) {
					town = "";
				}
				
				String country = attributes.get("arblandname");
				if(!ColumnKeys.validate(country)) {
					country = "";
				}
				
				person.setAttribute("workLoc", String.format("%s %s", town, country));
			}
		}
		
	}
}
