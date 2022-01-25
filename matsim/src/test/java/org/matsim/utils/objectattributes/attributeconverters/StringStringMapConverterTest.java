package org.matsim.utils.objectattributes.attributeconverters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringStringMapConverterTest {

	@Test
	public void test() {

		var expectedString = "{\"a\":\"value-a\",\"b\":\"value-b\"}";
		var converter = new StringStringMapConverter();

		var serializedString = converter.convertToString(converter.convert(expectedString));

		assertEquals(expectedString, serializedString);
	}
}