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

import playground.johannes.synpop.data.PlainPerson;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonMunicipalityClassHandler implements PersonAttributeHandler {

	@Override
	public void handle(PlainPerson person, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.PERSON_MUNICIPALITY);

		if(val.equalsIgnoreCase("unter 5.000 Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 0);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 5000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "0");
		} else if(val.equalsIgnoreCase("5.000 bis unter 20.000 Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 5000);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 20000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "1");
		} else if(val.equalsIgnoreCase("20.000 bis unter 50.000 Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 20000);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 50000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "2");
		} else if(val.equalsIgnoreCase("50.000 bis unter 100.000 Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 50000);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 100000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "3");
		} else if(val.equalsIgnoreCase("100.000 bis unter 500.000 Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 100000);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 500000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "4");
		} else if(val.equalsIgnoreCase("500.000 und mehr Einw.")) {
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 500000);
//			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, Integer.MAX_VALUE);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS, "5");
		}
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
