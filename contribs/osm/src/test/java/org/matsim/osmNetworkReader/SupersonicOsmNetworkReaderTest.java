package org.matsim.osmNetworkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SupersonicOsmNetworkReaderTest {

	private static final Logger log = Logger.getLogger(SupersonicOsmNetworkReaderTest.class);
	private static final CoordinateTransformation transformation = new IdentityTransformation();
	private static final String MOTORWAY = "motorway";
	private static final String TERTIARY = "tertiary";

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

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

    @Test
    @Ignore
	public void test() {

		Path file = Paths.get("C:\\Users\\Janek\\repos\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\osm_data\\nordrhein-westfalen-2019-11-21.osm.pbf");
		//Path file = Paths.get("C:\\Users\\Janek\\Downloads\\bremen-latest.osm(1).pbf");
		Path output = Paths.get("C:\\Users\\Janek\\Desktop\\test-network.xml.gz");
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

		List<Geometry> ruhrShape = ShapeFileReader.getAllFeatures(Paths.get("C:\\Users\\Janek\\repos\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\shapeFiles\\shapeFile_Ruhrgebiet\\ruhrgebiet_boundary.shp").toString()).stream()
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.collect(Collectors.toList());

		Instant start = Instant.now();
		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(coordinateTransformation)
				.includeLinkAtCoordWithHierarchy((coord, level) -> {
					if (level <= LinkProperties.LEVEL_SECONDARY) return true;

					return (level <= LinkProperties.LEVEL_RESIDENTIAL && ruhrShape.stream().anyMatch(g -> g.contains(MGC.coord2Point(coord))));
				})
				.addOverridingLinkProperties("residential", new LinkProperties(9, 1, 30.0 / 3.6, 1500, false))
				.addOverridingLinkProperties("cycleway", new LinkProperties(9, 1, 30.0 / 3.6, 1500, false))
				.addOverridingLinkProperties("service", new LinkProperties(9, 1, 10.0 / 3.6, 1000, false))
				.addOverridingLinkProperties("footway", new LinkProperties(9, 1, 10.0 / 3.6, 600, false))
				.addOverridingLinkProperties("path", new LinkProperties(9, 1, 20.0 / 3.6, 600, false))
				.build()
				.read(file);

		Duration duration = Duration.between(start, Instant.now());
		System.out.println(duration.toString());

		new NetworkWriter(network).write(output.toString());
	}

    @Test
    @Ignore
	public void testOldNetworkReader() {

        Path file = Paths.get("G:\\Users\\Janek\\Downloads\\nordrhein-westfalen-latest.osm\\nordrhein-westfalen-latest.osm");
		Path output = Paths.get("G:\\Users\\Janek\\Desktop\\nordrhein-westfalen-latest-matsim-reader.xml.gz");
		Network network = NetworkUtils.createNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

		Instant start = Instant.now();
		new OsmNetworkReader(network, coordinateTransformation, true, true).parse(file.toString());

		Duration duration = Duration.between(start, Instant.now());
		System.out.println(duration.toString());

		new NetworkWriter(network).write(output.toString());
	}


	@Test
	public void singleLink() {

		Utils.WaysAndLinks singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-one-way.pbf");
		writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
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
		OsmWay way = singleLink.getWays().get(0);

		Link link = network.getLinks().values().iterator().next(); // get the only link
		double expectedLengthPart1 = CoordUtils.calcEuclideanDistance(new Coord(node1.getLongitude(), node1.getLatitude()), new Coord(node2.getLongitude(), node2.getLatitude()));
		double expectedLengthPart2 = CoordUtils.calcEuclideanDistance(new Coord(node2.getLongitude(), node2.getLatitude()), new Coord(node3.getLongitude(), node3.getLatitude()));
		assertEquals(expectedLengthPart1 + expectedLengthPart2, link.getLength(), 0);

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

	@Test
	public void singleLinkPreserveMiddleNode() {

		Utils.WaysAndLinks singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-preserve-node.pbf");

		writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.preserveNodeWithId(id -> id == 2)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(3, network.getNodes().size());

		// now, test that the link has all the required properties
		Link link = network.getLinks().values().iterator().next(); // get the only link

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
	public void singleLink_withMaxSpeedTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithMaxSpeed = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "60")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(60 / 3.6, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_withMaxSpeedTag_milesPerHour() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});
		Way wayWithMaxSpeedMph = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "60 mph")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed-in-mph.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeedMph), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(60 * 1.609344 / 3.6, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_withMaxSpeedTag_urbanLink() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithMaxSpeedUrban = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag(OsmTags.MAXSPEED, "50")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-max-speed-urban-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithMaxSpeedUrban), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(50 / 3.6 * 0.5, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_withMaxSpeedTag_cantParseMaxSpeed() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithInvalidMaxSpeed = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, MOTORWAY),
				new Tag(OsmTags.MAXSPEED, "not a number")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-unknown-max-speed.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithInvalidMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(LinkProperties.createMotorway().freespeed, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_noMaxSpeedTag_ruralLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 1000, 1000);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithoutMaxSpeed = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-no-max-speed-rural-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithoutMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(LinkProperties.createTertiary().freespeed, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_noMaxSpeedTag_urbanLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way wayWithoutMaxSpeed = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-no-max-speed-urban-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(wayWithoutMaxSpeed), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));

		// the freespeed for 'urban' links (links without a speed tag and shorter than 300m) freespeed is reduced depending on the length of the link
		assertTrue(LinkProperties.createTertiary().freespeed > link.getFreespeed());
	}

	@Test
	public void singleLink_noLanesTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-no-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(LinkProperties.createTertiary().lanesPerDirection, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_withLanesTag() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(2, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_lanesTagOneWay() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("oneway", "true")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-oneway-link-with-lanes-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(4, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_lanesForwardAndBackwardTag() {
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Arrays.asList(new Tag(OsmTags.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("lanes:forward", "4"), new Tag("lanes:backward", "1")));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-with-lanes-forward-and-backward-tag.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link forwardLink = network.getLinks().get(Id.createLinkId(10001));
		Link backwardLink = network.getLinks().get(Id.createLinkId(10002));

		assertEquals(4, forwardLink.getNumberOfLanes(), 0);
		assertEquals(1, backwardLink.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_capacityLongLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 100, 100);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-capacity-for-long-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(LinkProperties.createTertiary().laneCapacity, link.getCapacity(), 0);
	}

	@Test
	public void singleLink_capacityShortLink() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 10, 10);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, TERTIARY)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-capacity-for-short-link.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(LinkProperties.createTertiary().laneCapacity * 2, link.getCapacity(), 0);
	}

	@Test
	public void singleLink_overridingLinkProperties() {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 100, 100);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});
		final String linkCategory = "some-category";
		final LinkProperties linkProperties = new LinkProperties(9, 1, 100, 100, false);

		Way way = new Way(1, nodeReference, Collections.singletonList(new Tag(OsmTags.HIGHWAY, linkCategory)));

		Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "single-link-overriding-link-properties.pbf");
		writeOsmData(Arrays.asList(node1, node2), Collections.singletonList(way), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.addOverridingLinkProperties(linkCategory, linkProperties)
				.build()
				.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		Link link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(linkProperties.laneCapacity, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
	}

	@Test
	public void twoIntersectingLinks() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links.pbf");
		writeOsmData(nodes, ways, file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		// check whether the links were correctly split
		Link link1 = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(Id.createNodeId(1), link1.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link1.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link1.getFromNode().getCoord(), link1.getToNode().getCoord()), link1.getLength(), 0);

		Link link2 = network.getLinks().get(Id.createLinkId(10003));
		assertEquals(Id.createNodeId(2), link2.getFromNode().getId());
		assertEquals(Id.createNodeId(3), link2.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord()), link2.getLength(), 0);

		Link link3 = network.getLinks().get(Id.createLinkId(20001));
		assertEquals(Id.createNodeId(4), link3.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link3.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link3.getFromNode().getCoord(), link3.getToNode().getCoord()), link3.getLength(), 0);

		Link link4 = network.getLinks().get(Id.createLinkId(20003));
		assertEquals(Id.createNodeId(2), link4.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link4.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link4.getFromNode().getCoord(), link4.getToNode().getCoord()), link4.getLength(), 0);
	}

	@Test
	public void twoIntersectingLinks_withAfterLinkCreatedHook() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links.pbf");
		writeOsmData(nodes, ways, file);

		HashSet<String> allowedModes = new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.airplane));
		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				.afterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(allowedModes))
				.build()
				.read(file);

		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		// check whether the links were correctly split
		Link link1 = network.getLinks().get(Id.createLinkId(10001));
		allowedModes.forEach(mode -> assertTrue(link1.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(1), link1.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link1.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link1.getFromNode().getCoord(), link1.getToNode().getCoord()), link1.getLength(), 0);

		Link link2 = network.getLinks().get(Id.createLinkId(10003));
		allowedModes.forEach(mode -> assertTrue(link2.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(2), link2.getFromNode().getId());
		assertEquals(Id.createNodeId(3), link2.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord()), link2.getLength(), 0);

		Link link3 = network.getLinks().get(Id.createLinkId(20001));
		allowedModes.forEach(mode -> assertTrue(link3.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(4), link3.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link3.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link3.getFromNode().getCoord(), link3.getToNode().getCoord()), link3.getLength(), 0);

		Link link4 = network.getLinks().get(Id.createLinkId(20003));
		allowedModes.forEach(mode -> assertTrue(link4.getAllowedModes().contains(mode)));
		assertEquals(Id.createNodeId(2), link4.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link4.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link4.getFromNode().getCoord(), link4.getToNode().getCoord()), link4.getLength(), 0);
	}

	@Test
	public void twoIntersectingLinks_oneShouldBeSimplified() {

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
				.coordinateTransformation(transformation)
				.build()
				.read(file);

		// node 5 should be simplified
		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		Link simplifiedLink = network.getLinks().get(Id.createLinkId(10005));
		assertEquals(Id.createNodeId(6), simplifiedLink.getToNode().getId());
		assertEquals(Id.createNodeId(2), simplifiedLink.getFromNode().getId());
	}

	@Test
	public void linkGrid_oneWayNotInFilter() {

		Utils.WaysAndLinks grid = Utils.createGridWithDifferentLevels();
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "grid-with-filter.pbf");
		writeOsmData(grid.getNodes(), grid.getWays(), file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
				// we don't want the tertiary link wich is on the 'right' side of the grid
				.includeLinkAtCoordWithHierarchy((coord, level) -> !(level == LinkProperties.LEVEL_TERTIARY && coord.getX() > 100))
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
	public void twoIntersectingLinks_oneWithLoop() {

		final List<Tag> tags = Collections.singletonList(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = Arrays.asList(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0), new Node(6, 3, 3),
				new Node(7, 4, 3), new Node(8, 4, 2));
		final List<OsmWay> ways = Arrays.asList(new Way(1, new TLongArrayList(new long[]{1, 2, 6, 7, 8, 6, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "two-intersecting-links-with-loop.pbf");
		writeOsmData(nodes, ways, file);

		Network network = new SupersonicOsmNetworkReader.Builder()
				.coordinateTransformation(transformation)
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
}
