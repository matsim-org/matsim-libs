package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Relation;
import de.topobyte.osm4j.core.model.impl.RelationMember;
import de.topobyte.osm4j.core.model.impl.Tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OsmSignalsParserTest {

	private static final CoordinateTransformation transformation = new IdentityTransformation();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@AfterEach
	public void shutDownExecutor() {
		executor.shutdown();
	}

	@Test
	void parse_singleLinkWithCrossing() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createSingleLink();
		List<OsmTag> tags = new ArrayList<>();
		tags.add(new Tag(OsmTags.HIGHWAY, OsmTags.CROSSING));

		// copy the middle node
		OsmNode crossing = new Node(
				data.getNodes().get(1).getId(),
				data.getNodes().get(1).getLatitude(),
				data.getNodes().get(1).getLongitude(),
				tags
		);

		// replace middle node with crossing
		data.getNodes().set(1, crossing);

		Utils.writeOsmData(data, file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(1, parser.getWays().size());
		assertEquals(4, parser.getNodes().size());
		assertEquals(1, parser.getSignalizedNodes().size());
		assertEquals(0, parser.getNodeRestrictions().size());

		// assert that the right node-id is memorized as crossing
		assertTrue(parser.getSignalizedNodes().containsKey(crossing.getId()));
		assertEquals(OsmTags.CROSSING, parser.getSignalizedNodes().get(crossing.getId()));
	}

	@Test
	void parse_singleLinkWithTrafficSignals() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createSingleLink();
		List<OsmTag> tags = new ArrayList<>();
		tags.add(new Tag(OsmTags.HIGHWAY, OsmTags.TRAFFIC_SINGALS));

		// copy the middle node
		OsmNode crossing = new Node(
				data.getNodes().get(1).getId(),
				data.getNodes().get(1).getLatitude(),
				data.getNodes().get(1).getLongitude(),
				tags
		);

		// replace middle node with crossing
		data.getNodes().set(1, crossing);

		Utils.writeOsmData(data, file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(1, parser.getWays().size());
		assertEquals(4, parser.getNodes().size());
		assertEquals(1, parser.getSignalizedNodes().size());
		assertEquals(0, parser.getNodeRestrictions().size());

		// assert that the right node-id is memorized as crossing
		assertTrue(parser.getSignalizedNodes().containsKey(crossing.getId()));
		assertEquals(OsmTags.TRAFFIC_SINGALS, parser.getSignalizedNodes().get(crossing.getId()));
	}

	@Test
	void parse_singleLinkWithNonHighwayNode() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createSingleLink();
		List<OsmTag> tags = new ArrayList<>();
		tags.add(new Tag(OsmTags.HIGHWAY, "other-than-traffic-ligh-and-crossing"));

		// copy the middle node
		OsmNode crossing = new Node(
				data.getNodes().get(1).getId(),
				data.getNodes().get(1).getLatitude(),
				data.getNodes().get(1).getLongitude(),
				tags
		);

		// replace middle node with crossing
		data.getNodes().set(1, crossing);

		Utils.writeOsmData(data, file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(1, parser.getWays().size());
		assertEquals(4, parser.getNodes().size());
		assertEquals(0, parser.getSignalizedNodes().size());
		assertEquals(0, parser.getNodeRestrictions().size());
	}

	@Test
	void parse_singleLinkWithNodeNotReferencedByWay() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createSingleLink();

		List<OsmNode> nodes = new ArrayList<>(data.getNodes());
		nodes.add(new Node(6, 1, 2));

		Utils.writeOsmData(nodes, data.getWays(), file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(1, parser.getWays().size());
		assertEquals(4, parser.getNodes().size());
		assertEquals(0, parser.getSignalizedNodes().size());
		assertEquals(0, parser.getNodeRestrictions().size());
	}

	@Test
	void parse_intersectingWaysWithProhibitiveRestriction() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createTwoIntersectingLinksWithDifferentLevels();

		List<OsmRelation> relations = new ArrayList<>();
		relations.add(createRestriction(1, data.getWays().get(0), data.getWays().get(1), data.getNodes().get(1), "no_right_turn"));

		Utils.OsmData withRelation = new Utils.OsmData(data.getNodes(), data.getWays(), relations);
		Utils.writeOsmData(withRelation, file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(2, parser.getWays().size());
		assertEquals(17, parser.getNodes().size());
		assertEquals(0, parser.getSignalizedNodes().size());
		assertEquals(1, parser.getNodeRestrictions().size());

		ProcessedRelation restriction = parser.getNodeRestrictions().get(data.getNodes().get(1).getId()).iterator().next();
		assertEquals(data.getWays().get(0).getId(), restriction.getFromWayId());
		assertEquals(data.getWays().get(1).getId(), restriction.getToWayId());
		assertEquals(data.getNodes().get(1).getId(), restriction.getNodeId());
		assertEquals(ProcessedRelation.Type.PROHIBITIVE, restriction.getType());
	}

	@Test
	void parse_intersectingWaysWithImperativeRestriction() {

		Path file = Paths.get(utils.getOutputDirectory()).resolve("tmp.pbf");
		Utils.OsmData data = Utils.createTwoIntersectingLinksWithDifferentLevels();

		List<OsmRelation> relations = new ArrayList<>();
		relations.add(createRestriction(1, data.getWays().get(0), data.getWays().get(1), data.getNodes().get(1), "only_right_turn"));

		Utils.OsmData withRelation = new Utils.OsmData(data.getNodes(), data.getWays(), relations);
		Utils.writeOsmData(withRelation, file);

		OsmSignalsParser parser = createDefaultParser();
		parser.parse(file);

		assertEquals(2, parser.getWays().size());
		assertEquals(17, parser.getNodes().size());
		assertEquals(0, parser.getSignalizedNodes().size());
		assertEquals(1, parser.getNodeRestrictions().size());

		ProcessedRelation restriction = parser.getNodeRestrictions().get(data.getNodes().get(1).getId()).iterator().next();
		assertEquals(data.getWays().get(0).getId(), restriction.getFromWayId());
		assertEquals(data.getWays().get(1).getId(), restriction.getToWayId());
		assertEquals(data.getNodes().get(1).getId(), restriction.getNodeId());
		assertEquals(ProcessedRelation.Type.IMPERATIVE, restriction.getType());
	}

	private OsmSignalsParser createDefaultParser() {
		return new OsmSignalsParser(transformation,
				LinkProperties.createLinkProperties(), (coord, id) -> true, executor);
	}

	private OsmRelation createRestriction(long id, OsmWay from, OsmWay to, OsmNode via, String restriction) {

		List<OsmRelationMember> members = new ArrayList<>();
		members.add(new RelationMember(from.getId(), EntityType.Way, "from"));
		members.add(new RelationMember(to.getId(), EntityType.Way, "to"));
		members.add(new RelationMember(via.getId(), EntityType.Node, "via"));

		List<OsmTag> tags = new ArrayList<>();
		tags.add(new Tag(OsmTags.TYPE, OsmTags.RESTRICTION));
		tags.add(new Tag(OsmTags.RESTRICTION, restriction));

		return new Relation(id, members, tags);
	}
}
