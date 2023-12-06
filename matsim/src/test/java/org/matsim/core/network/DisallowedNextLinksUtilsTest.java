package org.matsim.core.network;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class DisallowedNextLinksUtilsTest {

	Network n = DisallowedNextLinksTest.createNetwork();

	@Test
	public void testNoDisallowedNextLinks() {
		Assert.assertTrue(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	public void testIsNotValid1() {
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l1.getId(), l3.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(l1.getId()));

		Assert.assertFalse(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	public void testIsNotValid2() {
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l1.getId(), l3.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("a")));

		Assert.assertFalse(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	public void testIsValid() {
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));
		Link l5 = links.get(Id.createLinkId("5"));

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l3.getId(), l5.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(l3.getId()));

		Assert.assertTrue(DisallowedNextLinksUtils.isValid(n));
	}
}
