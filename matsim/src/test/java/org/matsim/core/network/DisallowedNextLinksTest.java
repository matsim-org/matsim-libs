package org.matsim.core.network;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class DisallowedNextLinksTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

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

	@Test
	public void testNetworkWriting() throws IOException {

		Network n = NetworkUtils.createNetwork();
		Node n0 = NetworkUtils.createAndAddNode(n, Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createAndAddNode(n, Id.createNodeId("1"), new Coord(1, 0));
		Node n2 = NetworkUtils.createAndAddNode(n, Id.createNodeId("2"), new Coord(2, -1));
		Node n3 = NetworkUtils.createAndAddNode(n, Id.createNodeId("3"), new Coord(2, 1));
		Node n4 = NetworkUtils.createAndAddNode(n, Id.createNodeId("4"), new Coord(3, 0));
		Link l0 = NetworkUtils.createAndAddLink(n, Id.createLinkId("0"), n0, n1, Math.sqrt(2), 30 / 3.6, 900, 1);
		Link l1 = NetworkUtils.createAndAddLink(n, Id.createLinkId("1"), n1, n2, Math.sqrt(2), 30 / 3.6, 900, 1);
		Link l2 = NetworkUtils.createAndAddLink(n, Id.createLinkId("2"), n1, n3, Math.sqrt(2), 30 / 3.6, 900, 1);
		Link l3 = NetworkUtils.createAndAddLink(n, Id.createLinkId("3"), n2, n4, Math.sqrt(2), 30 / 3.6, 900, 1);
		Link l4 = NetworkUtils.createAndAddLink(n, Id.createLinkId("4"), n3, n4, Math.sqrt(2), 30 / 3.6, 900, 1);

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l1.getId()));

		File tempFile = tempFolder.newFile("network.xml");
		new NetworkWriter(n).write(tempFile.toString());
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(tempFile.toString());

		Assert.assertEquals(dnl, NetworkUtils.getDisallowedNextLinks(network.getLinks().get(l1.getId())));
	}

}
