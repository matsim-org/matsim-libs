package org.matsim.core.utils.io;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import lombok.extern.java.Log;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Log
public class SupersonicOsmNetworkReaderTest {

	private static final CoordinateTransformation transformation = new IdentityTransformation();
	private static final String MOTORWAY = "motorway";
	private static final String TRUNK = "trunk";
	private static final String TERTIARY = "tertiary";

	private static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

		try (OutputStream outputStream = Files.newOutputStream(file)) {
			var writer = new PbfWriter(outputStream, true);
			for (OsmNode node : nodes) {
				writer.write(node);
			}

			for (OsmWay way : ways) {
				writer.write(way);
			}
			writer.complete();
		} catch (IOException e) {
			log.severe("could not write osm data");
			e.printStackTrace();
		}
	}

	@Test
	@Ignore
	public void test() throws FileNotFoundException, OsmInputException {

		Path file = Paths.get("C:\\Users\\Janekdererste\\germany-latest.osm.pbf");
		Path output = Paths.get("G:\\Users\\Janek\\Desktop\\nordrhein-westfalen-latest.xml.gz");
		Network network = NetworkUtils.createNetwork();

		Instant start = Instant.now();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
		new SupersonicOsmNetworkReader(network, coordinateTransformation, (coord, level) -> level < 7, id -> false).read(file);
		Duration duration = Duration.between(start, Instant.now());
		System.out.println(duration.toString());

		//new NetworkWriter(network).write(output.toString());
	}

	/**
	 * Single link with three nodes
	 * <p>
	 * (0,0), id: 1
	 * \
	 * \
	 * \
	 * (1000,1000), id: 2
	 * /
	 * /
	 * /
	 * (0,2000), id: 3
	 * <p>
	 * nodes 1 and 3 should be kept, node 2 should be removed to simplify link
	 *
	 */
	@Test
	public void singleLink() throws IOException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 100, 100);
		var node3 = new Node(3, 0, 200);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
		var tags = List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, MOTORWAY));
		var way = new Way(1, nodeReference, tags);

		Path testfile = Paths.get("single-link-one-way.pbf");

		writeOsmData(List.of(node1, node2, node3), List.of(way), testfile);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);

		reader.read(testfile);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		// now, test that the link has all the required properties
		Link link = network.getLinks().values().iterator().next(); // get the only link
		double expectedLengthPart1 = CoordUtils.calcEuclideanDistance(new Coord(node1.getLongitude(), node1.getLatitude()), new Coord(node2.getLongitude(), node2.getLatitude()));
		double expectedLengthPart2 = CoordUtils.calcEuclideanDistance(new Coord(node2.getLongitude(), node2.getLatitude()), new Coord(node3.getLongitude(), node3.getLatitude()));
		assertEquals(expectedLengthPart1 + expectedLengthPart2, link.getLength(), 0);

		var linkProperties = SupersonicOsmNetworkReader.LinkProperties.createMotorway();
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
		assertEquals(linkProperties.laneCapacity * linkProperties.lanesPerDirection, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(Set.of(TransportMode.car), link.getAllowedModes());

		//test attributes
		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.ORIGID));
		assertEquals(way.getId(), (long) link.getAttributes().getAttribute(NetworkUtils.ORIGID));

		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.TYPE));
		assertEquals(MOTORWAY, link.getAttributes().getAttribute(NetworkUtils.TYPE));
	}

	/**
	 * Single link with three nodes
	 * <p>
	 * (0,0), id: 1
	 * \
	 * \
	 * \
	 * (10,10), id: 2
	 * /
	 * /
	 * /
	 * (0,10), id: 3
	 * <p>
	 * nodes 1 and 3 should be kept, node 2 should be removed to simplify link
	 */
	@Test
	public void singleLinkPreserveMiddleNode() throws IOException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 100, 100);
		var node3 = new Node(3, 100, 0);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
		var tags = List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, MOTORWAY));
		var way = new Way(1, nodeReference, tags);

		Path testfile = Paths.get("single-link-preserve-node.pbf");

		writeOsmData(List.of(node1, node2, node3), List.of(way), testfile);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation, (coord, level) -> true, id -> id == 2);

		reader.read(testfile);

		assertEquals(2, network.getLinks().size());
		assertEquals(3, network.getNodes().size());

		// now, test that the link has all the required properties
		Link link = network.getLinks().values().iterator().next(); // get the only link

		var linkProperties = SupersonicOsmNetworkReader.LinkProperties.createMotorway();
		assertEquals(linkProperties.freespeed, link.getFreespeed(), 0);
		assertEquals(linkProperties.laneCapacity * linkProperties.lanesPerDirection, link.getCapacity(), 0);
		assertEquals(linkProperties.lanesPerDirection, link.getNumberOfLanes(), 0);
		assertEquals(Set.of(TransportMode.car), link.getAllowedModes());

		//test attributes
		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.ORIGID));
		assertEquals(way.getId(), (long) link.getAttributes().getAttribute(NetworkUtils.ORIGID));

		assertNotNull(link.getAttributes().getAttribute(NetworkUtils.TYPE));
		assertEquals(MOTORWAY, link.getAttributes().getAttribute(NetworkUtils.TYPE));
	}

	@Test
	public void singleLink_withMaxSpeedTag() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var wayWithMaxSpeed = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag(SupersonicOsmNetworkReader.MAXSPEED, "60")));

		Path file = Paths.get("single-link-with-max-speed.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithMaxSpeed), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);

		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(60 / 3.6, link.getFreespeed(), 0);

	}

	@Test
	public void singleLink_withMaxSpeedTag_milesPerHour() throws FileNotFoundException, OsmInputException {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});
		var wayWithMaxSpeedMph = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag(SupersonicOsmNetworkReader.MAXSPEED, "60 mph")));

		Path file = Paths.get("single-link-with-max-speed-in-mph.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithMaxSpeedMph), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(60 * 1.609344 / 3.6, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_withMaxSpeedTag_urbanLink() throws FileNotFoundException, OsmInputException {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var wayWithMaxSpeedUrban = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag(SupersonicOsmNetworkReader.MAXSPEED, "50")));

		Path file = Paths.get("single-link-with-max-speed-urban-link.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithMaxSpeedUrban), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(50 / 3.6 * 0.5, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_withMaxSpeedTag_cantParseMaxSpeed() throws FileNotFoundException, OsmInputException {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var wayWithInvalidMaxSpeed = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, MOTORWAY),
				new Tag(SupersonicOsmNetworkReader.MAXSPEED, "not a number")));

		Path file = Paths.get("single-link-with-unknown-max-speed.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithInvalidMaxSpeed), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(SupersonicOsmNetworkReader.LinkProperties.createMotorway().freespeed, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_noMaxSpeedTag_ruralLink() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 1000, 1000);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var wayWithoutMaxSpeed = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get("single-link-no-max-speed-rural-link.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithoutMaxSpeed), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(SupersonicOsmNetworkReader.LinkProperties.createTertiary().freespeed, link.getFreespeed(), 0);
	}

	@Test
	public void singleLink_noMaxSpeedTag_urbanLink() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var wayWithoutMaxSpeed = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY)
		));

		Path file = Paths.get("single-link-no-max-speed-urban-link.pbf");
		writeOsmData(List.of(node1, node2), List.of(wayWithoutMaxSpeed), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));

		// the freespeed for 'urban' links (links without a speed tag and shorter than 300m) freespeed is reduced depending on the length of the link
		assertTrue(SupersonicOsmNetworkReader.LinkProperties.createTertiary().freespeed > link.getFreespeed());
	}

	@Test
	public void singleLink_noLanesTag() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY)));

		Path file = Paths.get("single-link-with-no-lanes-tag.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(SupersonicOsmNetworkReader.LinkProperties.createTertiary().lanesPerDirection, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_withLanesTag() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag("lanes", "4")));

		Path file = Paths.get("single-link-with-lanes-tag.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(4 / 2, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_lanesTagOneWay() throws FileNotFoundException, OsmInputException {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("oneway", "true")));

		Path file = Paths.get("single-oneway-link-with-lanes-tag.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(1, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));

		assertEquals(4, link.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_lanesForewardAndBackwardTag() throws FileNotFoundException, OsmInputException {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY),
				new Tag("lanes", "4"), new Tag("lanes:forward", "4"), new Tag("lanes:backward", "1")));

		Path file = Paths.get("single-link-with-lanes-forward-and-backward-tag.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var forwardLink = network.getLinks().get(Id.createLinkId(10001));
		var backwardLink = network.getLinks().get(Id.createLinkId(10002));

		assertEquals(4, forwardLink.getNumberOfLanes(), 0);
		assertEquals(1, backwardLink.getNumberOfLanes(), 0);
	}

	@Test
	public void singleLink_capacityLongLink() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 100, 100);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY)));

		Path file = Paths.get("single-link-capacity-for-long-link.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(SupersonicOsmNetworkReader.LinkProperties.createTertiary().laneCapacity, link.getCapacity(), 0);
	}

	@Test
	public void singleLink_capacityShortLink() throws FileNotFoundException, OsmInputException {

		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 10, 10);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId()});

		var way = new Way(1, nodeReference, List.of(new Tag(SupersonicOsmNetworkReader.HIGHWAY, TERTIARY)));

		Path file = Paths.get("single-link-capacity-for-short-link.pbf");
		writeOsmData(List.of(node1, node2), List.of(way), file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);
		reader.read(file);

		assertEquals(2, network.getLinks().size());
		assertEquals(2, network.getNodes().size());

		var link = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(SupersonicOsmNetworkReader.LinkProperties.createTertiary().laneCapacity * 2, link.getCapacity(), 0);
	}

	/**
	 * Two links
	 * <p>
	 * (0,0), id:1   (2,0), id:5
	 * \        /
	 * \      /
	 * \    /
	 * (1,1), id: 2
	 * /    \
	 * /      \
	 * /        \
	 * (0,2), id:4  (2,2), id:3
	 */
	@Test
	public void twoIntersectingLinks() throws FileNotFoundException, OsmInputException {

		final var tags = List.of(new Tag("highway", MOTORWAY));
		final List<OsmNode> nodes = List.of(new Node(1, 0, 0), new Node(2, 1, 1), new Node(3, 2, 2),
				new Node(4, 0, 2), new Node(5, 2, 0));
		final List<OsmWay> ways = List.of(new Way(1, new TLongArrayList(new long[]{1, 2, 3}), tags),
				new Way(2, new TLongArrayList(new long[]{4, 2, 5}), tags));
		final var file = Paths.get("two-intersecting-links.pbf");
		writeOsmData(nodes, ways, file);

		var network = NetworkUtils.createNetwork();
		var reader = new SupersonicOsmNetworkReader(network, transformation);

		reader.read(file);

		assertEquals(5, network.getNodes().size());
		assertEquals(4, network.getLinks().size());

		// check whether the links were correctly split
		var link1 = network.getLinks().get(Id.createLinkId(10001));
		assertEquals(Id.createNodeId(1), link1.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link1.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link1.getFromNode().getCoord(), link1.getToNode().getCoord()), link1.getLength(), 0);

		var link2 = network.getLinks().get(Id.createLinkId(10003));
		assertEquals(Id.createNodeId(2), link2.getFromNode().getId());
		assertEquals(Id.createNodeId(3), link2.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord()), link2.getLength(), 0);

		var link3 = network.getLinks().get(Id.createLinkId(20001));
		assertEquals(Id.createNodeId(4), link3.getFromNode().getId());
		assertEquals(Id.createNodeId(2), link3.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link3.getFromNode().getCoord(), link3.getToNode().getCoord()), link3.getLength(), 0);

		var link4 = network.getLinks().get(Id.createLinkId(20003));
		assertEquals(Id.createNodeId(2), link4.getFromNode().getId());
		assertEquals(Id.createNodeId(5), link4.getToNode().getId());
		assertEquals(CoordUtils.calcEuclideanDistance(link4.getFromNode().getCoord(), link4.getToNode().getCoord()), link4.getLength(), 0);
	}
}
