package org.matsim.utils.objectattributes.attributeconverters;

import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StringCollectionConverterTest {

	@Test
	public void test() {

		var expectedString = "[\"a\",\"b\"]";
		var converter = new StringCollectionConverter();
		Collection<String> convert = converter.convert(expectedString);
		var serializedString = converter.convertToString(convert);
		assertEquals(expectedString, serializedString);

	}
}