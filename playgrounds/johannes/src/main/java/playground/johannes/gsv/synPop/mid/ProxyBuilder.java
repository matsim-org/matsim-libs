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

import playground.johannes.gsv.synPop.ProxyLeg;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class ProxyBuilder {

	public ProxyPerson buildPerson(Map<String, String> attributes) {
		ProxyPerson person = new ProxyPerson(personIdBuilder(attributes));
		
		return person;
	}
	
	public ProxyLeg addLeg(Map<String, String> attributes, Map<String, ProxyPerson> persons) {
		String personId = personIdBuilder(attributes);
		ProxyPerson person = persons.get(personId); 
		ProxyPlan plan = person.getPlan();
		
		ProxyLeg leg = new ProxyLeg();
		plan.addLeg(leg);
		
		return leg;
	}
	
	public String personIdBuilder(Map<String, String> attributes) {
		StringBuilder builder = new StringBuilder(20);
		builder.append(attributes.get(MIDKeys.HOUSEHOLD_ID));
		builder.append(".");
		builder.append(attributes.get(MIDKeys.PERSON_ID));
		
		return builder.toString();
	}
}
