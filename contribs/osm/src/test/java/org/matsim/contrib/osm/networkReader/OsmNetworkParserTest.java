package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.impl.Relation;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class OsmNetworkParserTest {

	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	@Rule
	public MatsimTestUtils matsimUtils = new MatsimTestUtils();

	@Test
	public void parse_singleLink() {

		Utils.OsmData singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser-single_link.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		AtomicInteger nodesCounter = new AtomicInteger();
		AtomicInteger waysCounter = new AtomicInteger();
		new PbfParser.Builder()
				.setNodeHandler(node -> {
					nodesCounter.getAndIncrement();
				})
				.setWaysHandler(way -> {
					waysCounter.getAndIncrement();
				})
				.setExecutor(executor)
				.build()
				.parse(file);

		assertEquals(singleLink.getWays().size(), waysCounter.get());
		assertEquals(singleLink.getNodes().size(), nodesCounter.get());
	}

	@Test
	public void parse_onlyRelations() {

		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser-only_relations.pbf");

		Utils.OsmData data = Utils.createTwoIntersectingLinksWithDifferentLevels();
		List<OsmRelation> relations = new ArrayList<>();
		relations.add(new Relation(1, new ArrayList<>()));
		Utils.writeOsmData(new Utils.OsmData(data.getNodes(), data.getWays(), relations), file);

		AtomicInteger relationsCounter = new AtomicInteger();
		new PbfParser.Builder()
				.setExecutor(executor)
				.setRelationHandler(relation -> {
					relationsCounter.getAndIncrement();
				})
				.build()
				.parse(file);

		assertEquals(1, relationsCounter.get());
	}
}
