package org.matsim.utils.objectattributes.attributeconverters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringDoubleMapConverterTest {

	@Test
	void test() {
		var expectedString = "{\"a\":0.1,\"b\":0.2}";
		var converter = new StringDoubleMapConverter();
		var serializedString = converter.convertToString(converter.convert(expectedString));
		assertEquals(expectedString, serializedString);
	}
}