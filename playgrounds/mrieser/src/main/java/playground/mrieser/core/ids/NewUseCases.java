/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mrieser.core.ids;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / Senozon AG
 */
public class NewUseCases {

	public static void main(String[] args) {
		testNumericIds();
		testCharacterIds();
		
		System.out.println("done.");
	}
	
	public static void testNumericIds() {
		NewId<Link> linkId1 = NewId.create("1", Link.class);
		NewId<Link> linkId2 = NewId.create("2", Link.class);
		NewId<Link> linkId1again = NewId.create("1", Link.class);

		NewId<Person> personId1 = NewId.create("1", Person.class);
		
		NewId<TransitStopFacility> stopId1 = NewId.create("1", TransitStopFacility.class);
		// check type hierarchy, Identifiable
		
		if (linkId1 == linkId2) {
			System.err.println("linkId1 and linkId2 are the same");
		}
		if (linkId1 != linkId1again) {
			System.err.println("linkId1 and linkId1again are not the same");
		}
//		if (linkId1 == personId1) { // does not compile
//			System.err.println("linkId1 and personId1 are the same");
//		}
		if ((NewId) linkId1 == (NewId) personId1) {
			System.err.println("linkId1 and personId1 are the same");
		}
		
//		if (linkId1.compareTo(personId1) == 0) { // does not compile
//			// ...
//		}
//		if (linkId1.compareTo((NewId<Link>) personId1) == 0) { // does not compile
//			// ...
//		}
		if (linkId1.compareTo((NewId) personId1) == 0) { // does compile, but only with cast
			System.err.println("linkId1 and personId1 are similar");
			// this could be prevented by storing the class object passed at creation into the id object, but is it worth it?
		}
		if (linkId1.equals(personId1)) { // does compile, but returns the correct answer
			System.err.println("linkId1 and personId1 are equal");
		}
	}

	public static void testCharacterIds() {
		NewId<Link> linkId1 = NewId.create("A", Link.class);
		NewId<Link> linkId2 = NewId.create("B", Link.class);
		NewId<Link> linkId1again = NewId.create("A", Link.class);
		
		NewId<Person> personId1 = NewId.create("A", Person.class);
		
		if (linkId1 == linkId2) {
			System.err.println("linkId1 and linkId2 are the same");
		}
		if (linkId1 != linkId1again) {
			System.err.println("linkId1 and linkId1again are not the same");
		}
//		if (linkId1 == personId1) { // does not compile
//			System.err.println("linkId1 and personId1 are the same");
//		}
		if ((NewId) linkId1 == (NewId) personId1) {
			System.err.println("linkId1 and personId1 are the same");
		}		
	}
}
