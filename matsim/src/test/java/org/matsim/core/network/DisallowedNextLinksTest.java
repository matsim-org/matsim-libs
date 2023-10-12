package org.matsim.core.network;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

public class DisallowedNextLinksTest {

	@Test
	public void testSerialization() {

		DisallowedNextLinks dns0 = new DisallowedNextLinks();
		dns0.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("3")));
		dns0.addDisallowedLinkSequence("car",
				List.of(Id.createLinkId("0"), Id.createLinkId("1"), Id.createLinkId("2")));
		dns0.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("10"), Id.createLinkId("11")));

		DisallowedNextLinks.DisallowedNextLinksAttributeConverter ac = new DisallowedNextLinks.DisallowedNextLinksAttributeConverter();

		String s = ac.convertToString(dns0);
		Assert.assertEquals("{\"car\":[[\"0\",\"3\"],[\"0\",\"1\",\"2\"]],\"bike\":[[\"10\",\"11\"]]}", s);
		System.out.println(s);

		DisallowedNextLinks dns1 = ac.convert(s);
		Assert.assertEquals(dns0, dns1);
		Assert.assertSame(dns0.getDisallowedLinkSequences("car").get(0).get(0),
				dns1.getDisallowedLinkSequences("car").get(0).get(0));
	}

}
