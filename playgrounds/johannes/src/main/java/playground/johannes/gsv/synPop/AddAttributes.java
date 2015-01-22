/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import playground.johannes.gsv.synPop.io.XMLParser;

/**
 * @author johannes
 *
 */
public class AddAttributes {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		Set<ProxyPerson> persons = parser.getPersons();
		
		parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[1]);
		
		Map<String, ProxyPerson> templates = new HashMap<>();
		for(ProxyPerson person : parser.getPersons()) {
			templates.put(person.getId(), person);
		}
		
		for(ProxyPerson person : persons) {
			
		}
		
	}

}
