package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class OsmBicycleReaderTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void test_singleLinkWithAttributes() {

		final String surface = "surface-value";
		final String smoothness = "smoothness-value";
		final String cycleway = "cycleway-value";
		final String restrictions = "restrictions-value";

		List<OsmTag> tags = Arrays.asList(
				new Tag(OsmTags.SURFACE, surface),
				new Tag(OsmTags.SMOOTHNESS, smoothness),
				new Tag(OsmTags.CYCLEWAY, cycleway),
				new Tag(OsmTags.BICYCLE, restrictions),
				new Tag(OsmTags.HIGHWAY, OsmTags.RESIDENTIAL));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		// check for all the tags
		Link link = network.getLinks().get(Id.createLinkId("10002f"));
		assertPresentAndEqual(link, OsmTags.SURFACE, surface);
		assertPresentAndEqual(link, OsmTags.SMOOTHNESS, smoothness);
		assertPresentAndEqual(link, OsmTags.CYCLEWAY, cycleway);
		assertPresentAndEqual(link, TransportMode.bike, restrictions);
	}

	@Test
	void test_singleLinkPrimaryWithSurfaceAsphalt() {

		List<OsmTag> tags = Collections.singletonList(
				new Tag(OsmTags.HIGHWAY, OsmTags.PRIMARY));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		// check for all the tags
		Link link = network.getLinks().get(Id.createLinkId("10002f"));
		assertPresentAndEqual(link, OsmTags.SURFACE, "asphalt");
	}

	@Test
	void test_singleLinkWithBicycleNotAllowed() {

		List<OsmTag> tags = Collections.singletonList(
				new Tag(OsmTags.HIGHWAY, OsmTags.MOTORWAY));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		// check for all the tags
		Link link = network.getLinks().get(Id.createLinkId("10002f"));
		assertFalse(link.getAllowedModes().contains(TransportMode.bike));
		assertTrue(link.getAllowedModes().contains(TransportMode.car));
	}

	@Test
	void test_singleLinkWithOnlyBicycleAllowed() {

		List<OsmTag> tags = Collections.singletonList(
				new Tag(OsmTags.HIGHWAY, OsmTags.PEDESTRIAN));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		// check for all the tags
		Link link = network.getLinks().get(Id.createLinkId("10002f"));
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
		assertFalse(link.getAllowedModes().contains(TransportMode.car));
	}

	@Test
	void test_singleOnewayLinkOneWayBikeNo() {

		final String surface = "surface-value";

		List<OsmTag> tags = Arrays.asList(
				new Tag(OsmTags.HIGHWAY, OsmTags.SECONDARY),
				new Tag(OsmTags.ONEWAYBICYCLE, "no"),
				new Tag(OsmTags.SURFACE, surface),
				new Tag(OsmTags.ONEWAY, "yes"));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link forward = network.getLinks().get(Id.createLinkId("10002f"));
		assertTrue(forward.getAllowedModes().contains(TransportMode.car));
		assertTrue(forward.getAllowedModes().contains(TransportMode.bike));

		Link reverse = network.getLinks().get(Id.createLinkId("10002f_bike-reverse"));
		assertFalse(reverse.getAllowedModes().contains(TransportMode.car));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));
		assertPresentAndEqual(reverse, OsmTags.SURFACE, surface);
	}

	@Test
	void test_singleOnewayLinkOppositeBike() {

		final String surface = "surface-value";

		List<OsmTag> tags = Arrays.asList(
				new Tag(OsmTags.HIGHWAY, OsmTags.SECONDARY),
				new Tag(OsmTags.ONEWAY, "yes"),
				new Tag(OsmTags.SURFACE, surface),
				new Tag(OsmTags.CYCLEWAY, "opposite"));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link forward = network.getLinks().get(Id.createLinkId("10002f"));
		assertTrue(forward.getAllowedModes().contains(TransportMode.car));
		assertTrue(forward.getAllowedModes().contains(TransportMode.bike));

		Link reverse = network.getLinks().get(Id.createLinkId("10002f_bike-reverse"));
		assertFalse(reverse.getAllowedModes().contains(TransportMode.car));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));
		assertPresentAndEqual(reverse, OsmTags.SURFACE, surface);
	}

	@Test
	void test_singleReverseOnewayLinkOneWayBikeNo() {

		final String surface = "surface-value";

		List<OsmTag> tags = Arrays.asList(
				new Tag(OsmTags.HIGHWAY, OsmTags.SECONDARY),
				new Tag(OsmTags.ONEWAY, "reverse"),
				new Tag(OsmTags.SURFACE, surface),
				new Tag(OsmTags.CYCLEWAY, "opposite"));
		Utils.OsmData singleLink = Utils.createSingleLink(tags);

		Path file = Paths.get(testUtils.getOutputDirectory()).resolve("single-link.osm.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link forward = network.getLinks().get(Id.createLinkId("10002r_bike-reverse"));
		assertFalse(forward.getAllowedModes().contains(TransportMode.car));
		assertTrue(forward.getAllowedModes().contains(TransportMode.bike));
		assertPresentAndEqual(forward, OsmTags.SURFACE, surface);

		Link reverse = network.getLinks().get(Id.createLinkId("10002r"));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.car));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));
		assertPresentAndEqual(reverse, OsmTags.SURFACE, surface);
	}

	@Test
	void test_builderDoesntOverrideLinkProperties() {

		var osmFile = Paths.get(testUtils.getOutputDirectory()).resolve("osm-data.osm.pbf");

		// this yields a motorway and a tertiary link
		var osmData = Utils.createTwoIntersectingLinksWithDifferentLevels();
		Utils.writeOsmData(osmData, osmFile);

		var network = new OsmBicycleReader.Builder()
				// we only want the motorway
				.setLinkProperties(new ConcurrentHashMap<>(Map.of(OsmTags.MOTORWAY, LinkProperties.createMotorway())))
				.setCoordinateTransformation(Utils.transformation)
				.build()
				.read(osmFile);

		// we expect one linke and it should be only the motorway link
		assertEquals(1, network.getLinks().size());

		for (Link link : network.getLinks().values()) {
			assertPresentAndEqual(link, NetworkUtils.TYPE ,OsmTags.MOTORWAY);
		}
	}

	private void assertPresentAndEqual(Link link, String tag, String expected) {
		assertNotNull(link.getAttributes().getAttribute(tag));
		String attribute = (String) link.getAttributes().getAttribute(tag);
		assertEquals(expected, attribute);
	}
}
