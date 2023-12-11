package org.matsim.utils.objectattributes.attributeconverters;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

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