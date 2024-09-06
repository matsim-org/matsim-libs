package org.matsim.contrib.osm.networkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class SupersonicOsmNetworkReaderTest {

	private static final Logger log = LogManager.getLogger(SupersonicOsmNetworkReaderTest.class);
	private static final CoordinateTransformation transformation = new IdentityTransformation();
	private static final String MOTORWAY = "motorway";
	private static final String TERTIARY = "tertiary";

	@RegisterExtension
	private MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	private static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

		try (OutputStream outputStream = Files.newOutputStream(file)) {
			PbfWriter writer = new PbfWriter(outputStream, true);
			for (OsmNode node : nodes) {
				writer.write(node);
			}

			for (OsmWay way : ways) {
				writer.write(way);
			}
			writer.complete();
		} catch (IOException e) {
			log.error("could not write osm data");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void singleLink() {

		Utils.OsmData singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-one-way.pbf");
		writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		// we expect two links one forward, one backwards
		assertEquals(1, network.getLinks().size());
		// we expect two nodes, since the middle node should be removed for simplification
		assertEquals(2, network.getNodes().size());

		// now, test that the link has all the required properties
		OsmNode node1 = singleLink.getNodes().get(0);
		OsmNode node2 = singleLink.getNodes().get(1);
		OsmNode node3 = singleLink.getNodes().get(2);
		OsmNode node4 = singleLink.getNodes().get(3);
		OsmWay way = singleLink.getWays().get(0);

		Link link = network.getLinks().values().iterator().next(); // get the only link
		double expectedLengthPart1 = CoordUtils.calcEuclideanDistance(new Coord(node1.getLongitude(), node1.getLatitude()), new Coord(node2.getLongitude(), node2.getLatitude()));
		double expectedLengthPart2 = CoordUtils.calcEuclideanDistance(new Coord(node2.getLongitude(), node2.getLatitude()), new Coord(node3.getLongitude(), node3.getLatitude()));
		double expectedLengthPart3 = CoordUtils.calcEuclideanDistance(new Coord(node3.getLongitude(), node3.getLatitude()), new Coord(node4.getLongitude(), node4.getLatitude()));
		assertEquals(expectedLengthPart1 + expectedLengthPart2 + expectedLengthPart3, link.getLength(), 0);

		LinkProperties linkProperties = LinkProperties.createMotorway();
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
		assertEquals(linkProperties.laneCapacity * linkProperties.lanesPerDirection, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(Collections.singleton(TransportMode.car), link.getAllowedModes());

		//test attributes
		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.ORIGID));
		assertEquals(way.getId(), (long) link.getAttributes().getAttribute(NetworkUtils.ORIGID));

		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.TYPE));
		assertEquals(MOTORWAY, link.getAttributes().getAttribute(NetworkUtils.TYPE));
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void singleLinkPreserveMiddleNodes() {

		Utils.OsmData singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-preserve-node.pbf");

		writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.setPreserveNodeWithId(id -> true)
				.build()
				.read(file);

		assertEquals(3, network.getLinks().size());
		assertEquals(4, network.getNodes().size());

		// now, test that the link has all the required properties
		Link link = network.getLinks().get(Id.createLinkId("10001f"));

		LinkProperties linkProperties = LinkProperties.createMotorway();
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
		assertEquals(linkProperties.laneCapacity * linkProperties.lanesPerDirection, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(Collections.singleton(TransportMode.car), link.getAllowedModes());

		//test attributes
		OsmWay way = singleLink.getWays().get(0);
		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.ORIGID));
		assertEquals(way.getId(), (long) link.getAttributes().getAttribute(NetworkUtils.ORIGID));

		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.TYPE));
		assertEquals(MOTORWAY, link.getAttributes().getAttribute(NetworkUtils.TYPE));
	}

	@Test
	void singleLink_withMaxSpeedTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithMaxSpeed = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "60")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(60 / 3.6, link.getFreespeed(), 0);
		assertEquals(link.getFreespeed(), (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_withMaxSpeedTag_milesPerHour() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});
		Way wayWithMaxSpeedMph = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "60 mph")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed-in-mph.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeedMph), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(60 * 1.609344 / 3.6, link.getFreespeed(), 0);
		assertEquals(link.getFreespeed(), (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_withMaxSpeedTag_urbanLink() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithMaxSpeedUrban = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "50")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed-urban-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeedUrban), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		// the freespeed of the link should be reduced by the speed factor
		assertEquals(50 / 3.6 * LinkProperties.DEFAULT_FREESPEED_FACTOR, link.getFreespeed(), 0);
		// the original max speed should be stored as is
		assertEquals(50 / 3.6, (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_withMaxSpeedTag_cantParseMaxSpeed() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithInvalidMaxSpeed = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, MOTORWAY),
				new Tag(OsmTags.MAXSPEED, "not a number")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-unknown-max-speed.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithInvalidMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(LinkProperties.createMotorway().freespeed, link.getFreespeed(), 0);
		assertEquals(link.getFreespeed(), (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_noMaxSpeedTag_ruralLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 1000, 1000);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithoutMaxSpeed = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-no-max-speed-rural-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithoutMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(LinkProperties.createTertiary().freespeed, link.getFreespeed(), 0);
		assertEquals(link.getFreespeed(), (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_noMaxSpeedTag_urbanLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithoutMaxSpeed = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-no-max-speed-urban-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithoutMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));

		// the freespeed for 'urban' links (links without a speed tag and shorter than 300m) freespeed is reduced depending on the length of the link
		assertTrue(LinkProperties.createTertiary().freespeed > link.getFreespeed());
		assertEquals(link.getFreespeed(), (double) link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED), 0);
	}

	@Test
	void singleLink_noLanesTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-no-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));

		assertEquals(LinkProperties.createTertiary().lanesPerDirection, link.getNumberOfLanes(), 0);
	}

	@Test
	void singleLink_withLanesTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));

		assertEquals(2, link.getNumberOfLanes(), 0);
	}

	@Test
	void singleLink_lanesTagOneWay() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("oneway", "true")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-oneway-link-with-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));

		assertEquals(4, link.getNumberOfLanes(), 0);
	}

	@Test
	void singleLink_lanesForwardAndBackwardTag() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("lanes:forward", "4"), new Tag("lanes:backward", "1")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-lanes-forward-and-backward-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link forwardLink = network.getLinks().get(Id.createLinkId("10000f"));
		Link backwardLink = network.getLinks().get(Id.createLinkId("10000r"));

		assertEquals(4, forwardLink.getNumberOfLanes(), 0);
		assertEquals(1, backwardLink.getNumberOfLanes(), 0);
	}

	@Test
	void singleLink_capacityLongLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 100, 100);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-capacity-for-long-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(LinkProperties.createTertiary().laneCapacity, link.getCapacity(), 0);
	}

	@Test
	void singleLink_capacityShortLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-capacity-for-short-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(LinkProperties.createTertiary().laneCapacity * 2, link.getCapacity(), 0);
	}

	@Test
	void singleLink_overridingLinkProperties() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 100, 100);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});
		final String linkCategory = "some-category";
		final LinkProperties linkProperties = new LinkProperties(9, 1, 100, 100, false);

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, linkCategory)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-overriding-link-properties.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.addOverridingLinkProperties(linkCategory, linkProperties)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId("10000f"));
		assertEquals(linkProperties.laneCapacity, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
	}

	@Test
	void twoIntersectingLinks() {

		var twoLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();
		var file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links.pbf");
		Utils.writeOsmData(twoLinks, file);

		var network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(5, network.getNodes().size());
		assertEquals(6, network.getLinks().size());

		// check whether the links were correctly split
		Link link1 = network.getLinks().get(Id.createLinkId("10003f"));
		assertEquals(Id.createNodeId(1), link1.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link1.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link1.getFromNode().getCoord(), link1.getToNode().getCoord()), link1.getLength(), 0);

		Link link2 = network.getLinks().get(Id.createLinkId("10007f"));
		assertEquals(Id.createNodeId(5), link2.getFromNode().getId());
		assertEquals(Id.createNodeId(9), link2.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord()), link2.getLength(), 0);

		Link link3 = network.getLinks().get(Id.createLinkId("20003f"));
		assertEquals(Id.createNodeId(10), link3.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link3.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link3.getFromNode().getCoord(), link3.getToNode().getCoord()), link3.getLength(), 0);

		Link link4 = network.getLinks().get(Id.createLinkId("20007f"));
		assertEquals(Id.createNodeId(5), link4.getFromNode().getId());
		assertEquals(Id.createNodeId(17), link4.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link4.getFromNode().getCoord(), link4.getToNode().getCoord()), link4.getLength(), 0);

		Link link5 = network.getLinks().get(Id.createLinkId("20007r"));
		assertEquals(Id.createNodeId(17), link5.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link5.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link5.getFromNode().getCoord(), link5.getToNode().getCoord()), link5.getLength(), 0);

		Link link6 = network.getLinks().get(Id.createLinkId("20003r"));
		assertEquals(Id.createNodeId(5), link6.getFromNode().getId());
		assertEquals(Id.createNodeId(10), link6.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link6.getFromNode().getCoord(), link6.getToNode().getCoord()), link6.getLength(), 0);
	}

	@Test
	void twoIntersectingLinks_withAfterLinkCreatedHook() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links.pbf");
		writeOsmData(nodes, ways, file);

		HashSet<String> allowedModes = new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.airplane));
		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(allowedModes))
				.build()
				.read(file);

		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		// check whether the links were correctly split
		Link link1 = network.getLinks().get(Id.createLinkId("10000f"));
		allowedModes.forEach(mode -> assertTrue(link1.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(1), link1.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link1.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link1.getFromNode().getCoord(), link1.getToNode().getCoord()), link1.getLength(), 0);

		Link link2 = network.getLinks().get(Id.createLinkId("10001f"));
		allowedModes.forEach(mode -> assertTrue(link2.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(2), link2.getFromNode().getId());
		assertEquals(Id.createNodeId(3), link2.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord()), link2.getLength(), 0);

		Link link3 = network.getLinks().get(Id.createLinkId("20000f"));
		allowedModes.forEach(mode -> assertTrue(link3.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(4), link3.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link3.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link3.getFromNode().getCoord(), link3.getToNode().getCoord()), link3.getLength(), 0);

		Link link4 = network.getLinks().get(Id.createLinkId("20001f"));
		allowedModes.forEach(mode -> assertTrue(link4.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(2), link4.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link4.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link4.getFromNode().getCoord(), link4.getToNode().getCoord()), link4.getLength(), 0);
	}

	@Test
	void twoIntersectingLinks_oneShouldBeSimplified() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0),
				new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0),
				new Node(6, 3, 3));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 3, 6}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links.pbf");
		writeOsmData(nodes, ways, file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		// node 3 should be simplified
		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		Link simplifiedLink = network.getLinks().get(Id.createLinkId("10002f"));
		assertEquals(Id.createNodeId(6), simplifiedLink.getToNode().getId());
		assertEquals(Id.createNodeId(2), simplifiedLink.getFromNode().getId());
	}

	@Test
	void linkGrid_oneWayNotInFilter() {

		Utils.OsmData grid = Utils.createGridWithDifferentLevels();
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "grid-with-filter.pbf");
		writeOsmData(grid.getNodes(), grid.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				// we don't want the tertiary link wich is on the 'right' side of the grid
				.setIncludeLinkAtCoordWithHierarchy((coord, level) -> !(level == LinkProperties.LEVEL_TERTIARY && coord.getX() > 100))
				.build()
				.read(file);

		// we want 10 links (4 highways which are oneway, 6 tertiary which are two way
		assertEquals(10, network.getLinks().size());
		// we want 8 nodes, since 4 should be simplified
		assertEquals(8, network.getNodes().size());

		// check that way 4 is not added
		for (Link link : network.getLinks().values()) {
			assertFalse(link.getId().toString().startsWith("4"));
		}

		// check that ids 2, 5, 9, 12 were not added
		assertFalse(network.getNodes().containsKey(Id.createNodeId(2)));
		assertFalse(network.getNodes().containsKey(Id.createNodeId(5)));
		assertFalse(network.getNodes().containsKey(Id.createNodeId(9)));
		assertFalse(network.getNodes().containsKey(Id.createNodeId(12)));
	}

	@Test
	void twoIntersectingLinks_oneWithLoop() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0), new Node(6, 3, 3),
				new Node(7, 4, 3), new Node(8, 4, 2));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 6, 7, 8, 6, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links-with-loop.pbf");
		writeOsmData(nodes, ways, file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(8, network.getNodes().size());
		assertEquals(8, network.getLinks().size());

		// check for node 6 and if it has two incoming and two outgoing links
		org.matsim.api.core.v01.network.Node node6 = network.getNodes().get(Id.createNodeId(6));
		assertEquals(2, node6.getOutLinks().size());
		assertEquals(2, node6.getInLinks().size());

		// check for node7 and 8, and that they have one incoming and on outgoing link
		org.matsim.api.core.v01.network.Node node7 = network.getNodes().get(Id.createNodeId(7));
		assertEquals(1, node7.getInLinks().size());
		assertEquals(1, node7.getOutLinks().size());

		org.matsim.api.core.v01.network.Node node8 = network.getNodes().get(Id.createNodeId(7));
		assertEquals(1, node8.getInLinks().size());
		assertEquals(1, node8.getOutLinks().size());
	}

	@Test
	void simplifiedLinksWithPreservedOriginalGeometry() {

		var file = Paths.get(matsimTestUtils.getOutputDirectory() + "file.osm.bbf");
		var osmData = Utils.createSingleLink(List.of(new Tag(OsmTags.HIGHWAY, OsmTags.LIVING_STREET)));
		Utils.writeOsmData(osmData, file);

		var network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.setStoreOriginalGeometry(true)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertTrue(network.getLinks().containsKey(Id.createLinkId("10002f")));
		assertTrue(network.getLinks().containsKey(Id.createLinkId("10002r")));

		var forwardLink = network.getLinks().get(Id.createLinkId("10002f"));
		assertOriginalGeometryOnLink(forwardLink, osmData.getNodes(), true);

		var reverseLink = network.getLinks().get(Id.createLinkId("10002r"));
		assertOriginalGeometryOnLink(reverseLink, osmData.getNodes(), false);

	}

	private void assertOriginalGeometryOnLink(Link link, List<OsmNode> osmNodes, boolean isForward) {
		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.ORIG_GEOM));
		var originalGeometry = NetworkUtils.getOriginalGeometry(link);

		assertEquals(osmNodes.size(), originalGeometry.size());

		for (var i = 0; i != osmNodes.size() ; i++) {
			var osmNode = osmNodes.get(i);
			// if forward link traverse forward, otherwise the nodes should be reversed compared to original geometry
			var actualNode = isForward ? originalGeometry.get(i) : originalGeometry.get(originalGeometry.size() - 1 - i);

			assertEquals(osmNode.getId(), Long.parseLong(actualNode.getId().toString()));
			assertEquals(osmNode.getLongitude(), actualNode.getCoord().getX(), 0.0000000001);
			assertEquals(osmNode.getLatitude(), actualNode.getCoord().getY(), 0.000000001);
		}
	}
}
