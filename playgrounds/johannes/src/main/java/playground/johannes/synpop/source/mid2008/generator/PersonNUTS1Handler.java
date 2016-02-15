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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonNUTS1Handler implements PersonAttributeHandler {

	private static Map<String, String> labels = new HashMap<>();

	static {
		labels.put("1", "Schleswig-Holstein");
		labels.put("2", "Hamburg");
		labels.put("3", "Niedersachsen");
		labels.put("4", "Bremen");
		labels.put("5", "Nordrhein-Westfalen");
		labels.put("6", "Hessen");
		labels.put("7", "Rheinland-Pfalz");
		labels.put("8", "Baden-Württemberg");
		labels.put("9", "Bayern");
		labels.put("10", "Saarland");
		labels.put("11", "Berlin");
		labels.put("12", "Brandenburg");
		labels.put("13", "Mecklenburg-Vorpommern");
		labels.put("14", "Sachsen");
		labels.put("15", "Sachsen-Anhalt");
		labels.put("16", "Thüringen");
	}

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.PERSON_STATE);
		if(val != null) {
			person.setAttribute(MiDKeys.PERSON_NUTS1, labels.get(val));
		}

	}

}
