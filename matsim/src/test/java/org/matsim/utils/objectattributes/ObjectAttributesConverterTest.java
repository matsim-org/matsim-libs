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
