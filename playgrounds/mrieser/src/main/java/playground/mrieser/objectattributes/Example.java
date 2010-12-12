/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.objectattributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows an example usage of {@link ObjectAttributes}.
 *
 * @author mrieser
 */
public class Example {

	public static void main(String[] args) {
		// assume we're working with some links, e.g. in a network
		List<String> linkIds = new ArrayList<String>();
		linkIds.add("1");
		linkIds.add("2");
		linkIds.add("3");
		linkIds.add("4");

		// define some attributes
		ObjectAttributes linkAttributes = new ObjectAttributes();
		// read from file or create somehow else
		linkAttributes.putAttribute("1", "roadtype", "motorway");
		linkAttributes.putAttribute("1", "hasSpeedBumps", Boolean.TRUE);
		linkAttributes.putAttribute("2", "roadtype", "trunk");
		linkAttributes.putAttribute("2", "hasSpeedBumps", Boolean.TRUE);
		// do not define a roadtype for object "3"
		linkAttributes.putAttribute("3", "hasSpeedBumps", Boolean.FALSE);
		// do not define any attributes for object "4"

		// now do something useful, e.g. use the attributes for filtering
		filterByAttributes(linkIds, linkAttributes, "roadtype", "trunk");
		filterByAttributes(linkIds, linkAttributes, "hasSpeedBumps", true);
		filterByAttributes(linkIds, linkAttributes, "hasSpeedBumps", false);
	}

	public static void filterByAttributes(List<String> objectIds, ObjectAttributes objectAttributes, String attributeName, Object attributeValue) {
		System.out.println("filtering for " + attributeName + " = " + attributeValue);
		for (String id : objectIds) {
			Object o = objectAttributes.getAttribute(id, attributeName);
			if (attributeValue.equals(o)) {
				System.out.println("  " + id);
			}
		}
	}
}
