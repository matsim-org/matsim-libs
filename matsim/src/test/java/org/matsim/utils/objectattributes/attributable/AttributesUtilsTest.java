package org.matsim.utils.objectattributes.attributable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AttributesUtilsTest {

	@Test
	void testCopyToWithPrimitive() {

		var data = 1L;
		var attributeKey = "data-key";
		var from = new AttributesImpl();
		var to = new AttributesImpl();
		from.putAttribute(attributeKey, data);

		AttributesUtils.copyTo(from, to);

		var value = (long) to.getAttribute(attributeKey);
		assertEquals(data, value);
	}
}