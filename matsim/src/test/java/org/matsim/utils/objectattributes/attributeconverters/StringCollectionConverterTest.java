package org.matsim.utils.objectattributes.attributeconverters;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringCollectionConverterTest {

	@Test
	void test() {

		var expectedString = "[\"a\",\"b\"]";
		var converter = new StringCollectionConverter();
		Collection<String> convert = converter.convert(expectedString);
		var serializedString = converter.convertToString(convert);
		assertEquals(expectedString, serializedString);

	}
}
