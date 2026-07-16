package org.matsim.contrib.bicycle.network;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TagCopy}. Generates two Networks and compares them.
 *
 * <p>Coverage:
 * <ul>
 *   <li>Ordinary tags</li>
 *   <li>Blank and Null tags</li>
 * </ul>
 *
 * @author esarikaya
 */
public class TagCopyTest {


	/**
	 * Tests basic tag-copy
	 */
	@Test
	void simpleTagCopyTest() {
		String prefix = "test";
		Map <String, String> tags = new HashMap<>();
		tags.put("tag1","1");
		tags.put("tag2","2");

		TagCopy tagCopy = new TagCopy(tags.keySet().stream().toList(), prefix);

		Network built = NetworkUtils.createNetwork();
		Node fromBuilt = NetworkUtils.createNode(Id.createNodeId("from"), CoordUtils.createCoord(0,0));
		Node toBuilt = NetworkUtils.createNode(Id.createNodeId("to"), CoordUtils.createCoord(1,1));
		Link linkBuilt = NetworkUtils.createLink(
			Id.createLinkId("test"),
			fromBuilt,
			toBuilt,
			built,
			0.0, 0.0, 0.0, 0.0);

		for(String key : tags.keySet()) {
			linkBuilt.getAttributes().putAttribute(prefix + ":" + key, tags.get(key));
		}
		built.addNode(fromBuilt);
		built.addNode(toBuilt);
		built.addLink(linkBuilt);

		Network generated = NetworkUtils.createNetwork();
		Node fromGenerated = NetworkUtils.createNode(Id.createNodeId("from"), CoordUtils.createCoord(0,0));
		Node toGenerated = NetworkUtils.createNode(Id.createNodeId("to"), CoordUtils.createCoord(1,1));
		Link linkGenerated = NetworkUtils.createLink(
			Id.createLinkId("test"),
			fromGenerated,
			toGenerated,
			built,
			0.0, 0.0, 0.0, 0.0);

		tagCopy.copy(linkGenerated, tags);
		generated.addNode(fromGenerated);
		generated.addNode(toGenerated);
		generated.addLink(linkGenerated);

		assertTrue(NetworkUtils.compare(built, generated));
	}

	/**
	 * Tests "blank or null tag" edge case
	 */
	@Test
	void blankAndNullTagCopyTest() {
		String prefix = "test";
		Map <String, String> tags = new HashMap<>();
		tags.put("tag1","");
		tags.put("tag2",null);

		TagCopy tagCopy = new TagCopy(tags.keySet().stream().toList(), prefix);

		Network built = NetworkUtils.createNetwork();
		Node fromBuilt = NetworkUtils.createNode(Id.createNodeId("from"), CoordUtils.createCoord(0,0));
		Node toBuilt = NetworkUtils.createNode(Id.createNodeId("to"), CoordUtils.createCoord(1,1));
		Link linkBuilt = NetworkUtils.createLink(
			Id.createLinkId("test"),
			fromBuilt,
			toBuilt,
			built,
			0.0, 0.0, 0.0, 0.0);

		built.addNode(fromBuilt);
		built.addNode(toBuilt);
		built.addLink(linkBuilt);

		Network generated = NetworkUtils.createNetwork();
		Node fromGenerated = NetworkUtils.createNode(Id.createNodeId("from"), CoordUtils.createCoord(0,0));
		Node toGenerated = NetworkUtils.createNode(Id.createNodeId("to"), CoordUtils.createCoord(1,1));
		Link linkGenerated = NetworkUtils.createLink(
			Id.createLinkId("test"),
			fromGenerated,
			toGenerated,
			built,
			0.0, 0.0, 0.0, 0.0);

		tagCopy.copy(linkGenerated, tags);
		generated.addNode(fromGenerated);
		generated.addNode(toGenerated);
		generated.addLink(linkGenerated);

		assertTrue(NetworkUtils.compare(built, generated));
	}

}
