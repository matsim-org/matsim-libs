
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

	@Test
	public void testHashMap() {

		var expectedString = "{\"a\":\"value-a\",\"b\":\"value-b\"}";
		final var converter = new ObjectAttributesConverter();

		Map<String, String> parsed = (Map<String, String>) converter.convert("java.util.Map", expectedString);
		var serialized = converter.convertToString(parsed);

		assertEquals(expectedString, serialized);
	}

	@Test
	public void testEmptyHashMap() {

		var expectedString = "{}";
		final var converter = new ObjectAttributesConverter();

		Map<String, String> parsed = new HashMap<>();
		var serialized = converter.convertToString(parsed);

		assertEquals(expectedString, serialized);
	}

	@Test
	public void testCollection() {

		var expectedString = "[\"a\",\"b\"]";
		final var converter = new ObjectAttributesConverter();

		Collection<String> parsed = (Collection<String>) converter.convert("java.util.Collection", expectedString);
		var serialized = converter.convertToString(parsed);

		assertEquals(expectedString, serialized);
	}

	@Test
	public void testEmptyCollection() {

		var expectedString = "[]";
		final var converter = new ObjectAttributesConverter();

		Collection<String> parsed = Arrays.asList();
		var serialized = converter.convertToString(parsed);

		assertEquals(expectedString, serialized);
	}

	@Test
	public void testUnsupported() {

		final var converter = new ObjectAttributesConverter();
		var serialized = converter.convertToString(new UnsupportedType());
		var parsed = converter.convert(UnsupportedType.class.getName(), "some-value");

		assertNull(serialized);
		assertNull(parsed);
	}

	private static class UnsupportedType {
	}
}
