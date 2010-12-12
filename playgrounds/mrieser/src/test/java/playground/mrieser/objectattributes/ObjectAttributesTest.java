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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser
 */
public class ObjectAttributesTest {

	@Test
	public void testPutGet() {
		ObjectAttributes linkAttributes = new ObjectAttributes();
		Assert.assertNull(linkAttributes.getAttribute("1", "osm:roadtype"));
		Assert.assertNull(linkAttributes.putAttribute("1", "osm:roadtype", "trunk"));
		Assert.assertEquals("trunk", linkAttributes.getAttribute("1", "osm:roadtype"));
		Assert.assertEquals("trunk", linkAttributes.putAttribute("1", "osm:roadtype", "motorway"));
		Assert.assertEquals("motorway", linkAttributes.getAttribute("1", "osm:roadtype"));
	}
}
