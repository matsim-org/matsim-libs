package org.matsim.utils.objectattributes.attributable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributesUtilsTest {

	@Test
	public void testCopyToWithPrimitive() {

		var data = 1L;
		var attributeKey = "data-key";
		var from = new Attributes();
		var to = new Attributes();
		from.putAttribute(attributeKey, data);

		AttributesUtils.copyTo(from, to);

		var value = (long) to.getAttribute(attributeKey);
		assertEquals(data, value);
	}
}