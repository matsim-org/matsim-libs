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

package org.matsim.utils.objectattributes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser
 */
public class ObjectAttributesTest {

	@Test
	void testPutGet() {
		ObjectAttributes linkAttributes = new ObjectAttributes();
		Assertions.assertNull(linkAttributes.getAttribute("1", "osm:roadtype"));
		Assertions.assertNull(linkAttributes.putAttribute("1", "osm:roadtype", "trunk"));
		Assertions.assertEquals("trunk", linkAttributes.getAttribute("1", "osm:roadtype"));
		Assertions.assertEquals("trunk", linkAttributes.putAttribute("1", "osm:roadtype", "motorway"));
		Assertions.assertEquals("motorway", linkAttributes.getAttribute("1", "osm:roadtype"));
	}
}
