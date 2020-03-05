package org.matsim.utils.objectattributes.attributable;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

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

	@Test
	public void testCopyToWithMap() {

		var data = Map.of("some-key", "some-value");
		var attributeKey = "map-key";
		var from = new Attributes();
		var to = new Attributes();
		from.putAttribute(attributeKey, data);

		AttributesUtils.copyTo(from, to);

		var map = (Map) to.getAttribute(attributeKey);
		for (var entry : data.entrySet()) {
			assertTrue(map.containsKey(entry.getKey()));
			assertTrue(map.containsValue(entry.getValue()));
		}
	}

	@Test(expected = Exception.class)
	public void testCopyToWithUnsupported() {

		var data = new Object();
		var attributeKey = "key";
		var from = new Attributes();
		var to = new Attributes();
		from.putAttribute(attributeKey, data);

		AttributesUtils.copyTo(from, to);

		fail(); // we are expecting an exception. Therefore we should fail if this line is reached
	}
}