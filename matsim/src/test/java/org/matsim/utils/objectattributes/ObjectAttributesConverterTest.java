
/* *********************************************************************** *
 * project: org.matsim.*
 * ObjectAttributesConverterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.junit.Assert;
import org.junit.Test;

import java.time.Month;

/**
 * @author thibautd
 */
public class ObjectAttributesConverterTest {

	@Test
	public void testEnumToString() {
		final ObjectAttributesConverter converter = new ObjectAttributesConverter();
		// cannot use an enum type defined here, because test classes are not on classpath for classes outside of test...
		// And is thus unavailable to the ObjectAttributesConverter
		// So use something from the standard library, as there is no way this goes away
		String converted = converter.convertToString(Month.JANUARY);
		Assert.assertEquals("unexpected string converted from enum value", "JANUARY", converted);
	}

	@Test
	public void testStringToEnum() {
		final ObjectAttributesConverter converter = new ObjectAttributesConverter();
		Object converted = converter.convert(Month.class.getCanonicalName(), "JANUARY");
		Assert.assertEquals("unexpected enum converted from String value", Month.JANUARY, converted);
	}
}
