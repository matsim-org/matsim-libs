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

import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.data.Person;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonMunicipalityClassHandler implements PersonAttributeHandler {

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.PERSON_LAU2_CLASS);

		int cat = Integer.parseInt(val);
		cat = cat - 1; //TODO: Adapt simulation

		person.setAttribute(MiDKeys.PERSON_LAU2_CLASS, String.valueOf(cat));
	}

	private static int[][] categories;
	
	private static void initCategories() {
		categories = new int[6][2];
		categories[0][0] = 0;
		categories[0][1] = 5000;
		categories[1][0] = categories[0][1];
		categories[1][1] = 20000;
		categories[2][0] = categories[1][1];
		categories[2][1] = 50000;
		categories[3][0] = categories[2][1];
		categories[3][1] = 100000;
		categories[4][0] = categories[3][1];
		categories[4][1] = 500000;
		categories[5][0] = categories[4][1];
		categories[5][1] = Integer.MAX_VALUE;
	}
	
	public static int getLowerBound(int cat) {
		if(categories == null)
			initCategories();
		return categories[cat][0];
	}
	
	public static int getUpperBound(int cat) {
		if(categories == null)
			initCategories();
		return categories[cat][1];
	}
	
	public static int getCategory(int inhabs) {
		if(categories == null)
			initCategories();
		
		for(int i = 0; i < categories.length; i++) {
			if(categories[i][1] > inhabs) {
				return i;
			}
		}
		
		throw new RuntimeException("upps");
	}
}
