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

import playground.mrieser.core.ids.AlternativeId.LinkId;
import playground.mrieser.core.ids.AlternativeId.PersonId;

/**
 * @author mrieser / Senozon AG
 */
public class AlternativeUseCases {

	public static void main(String[] args) {
		LinkId linkId1 = AlternativeId.createLinkId("1");
		LinkId linkId2 = AlternativeId.createLinkId("2");
		LinkId linkId1again = AlternativeId.createLinkId("1");

		PersonId personId1 = AlternativeId.createPersonId("1");
		
		if (linkId1 == linkId2) {
			System.err.println("linkId1 and linkId2 are the same");
		}
		if (linkId1 != linkId1again) {
			System.err.println("linkId1 and linkId1again are not the same");
		}
//		if (linkId1 == personId1) { // does not compile
//			System.err.println("linkId1 and personId1 are the same");
//		}
		if ((AlternativeId) linkId1 == (AlternativeId) personId1) {
			System.err.println("linkId1 and personId1 are the same");
		}
		
		if (linkId1.compareTo(personId1) == 0) { // compiles!!!! no type safety at compile time
			System.err.println("linkId1 and personId1 are comparably the same");
		}
		if (linkId1.equals(personId1)) { // does compile
			System.err.println("linkId1 and personId1 are equal");
		}
		
		System.out.println("done.");

	}
	
}
