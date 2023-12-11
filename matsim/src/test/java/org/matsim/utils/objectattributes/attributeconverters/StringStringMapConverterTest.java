package org.matsim.utils.objectattributes.attributeconverters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringStringMapConverterTest {

	@Test
	void test() {

		var expectedString = "{\"a\":\"value-a\",\"b\":\"value-b\"}";
		var converter = new StringStringMapConverter();

		var serializedString = converter.convertToString(converter.convert(expectedString));

		assertEquals(expectedString, serializedString);
	}
}