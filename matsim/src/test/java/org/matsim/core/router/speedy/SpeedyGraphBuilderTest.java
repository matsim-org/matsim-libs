package org.matsim.core.router.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mrieser / Simunto
 */
class SpeedyGraphBuilderTest {

	private static final Logger LOG = LogManager.getLogger(SpeedyGraphBuilderTest.class);

	@Test
	public void testSingleDisallowedRightTurn() {
		var f = new Fixture();
		// make some other links more expensive to prevent routing along them
		f.overwriteLinkLength("jf", 10000);
		f.overwriteLinkLength("ji", 10000);

		var graph = runTest(f.network, "J", "G", "jk", "kg");
		Assertions.assertEquals(16, graph.nodeCount);
		Assertions.assertEquals(48, graph.linkCount);

		f.addTurnRestriction("jk", "kg");
		graph = runTest(f.network, "J", "G", "jk", "kl", "lh", "hg");
		Assertions.assertEquals(16 + 1, graph.nodeCount, "only node K should be duplicated: K'");
		Assertions.assertEquals(48 + 3, graph.linkCount, "expected the following new links: k'j, k'o, k'l");
	}

	@Test
	public void testMultipleRestrictionsOnSameLink() {
		var f = new Fixture();
		// make some other links more expensive to prevent routing along them
		f.overwriteLinkLength("jf", 10000);
		f.overwriteLinkLength("ji", 10000);

		var graph = runTest(f.network, "J", "G", "jk", "kg");
		Assertions.assertEquals(16, graph.nodeCount);
		Assertions.assertEquals(48, graph.linkCount);

		// prevent u-turns by higher costs on some of those links
		f.overwriteLinkLength("lk", 10000);
		f.overwriteLinkLength("ko", 600);

		f.addTurnRestriction("jk", "kg");
		f.addTurnRestriction("jk", "kl", "lh");
		graph = runTest(f.network, "J", "G", "jn", "no", "ok", "kg");
		Assertions.assertEquals(16 + 2, graph.nodeCount, "expected 2 duplicated nodes: K', L'");
		Assertions.assertEquals(48 + 5, graph.linkCount, "expected the following duplicated links: k'j, k'o, k'l', l'p, l'k");
	}

	@Test
	public void testMultipleRestrictionsOnMultipleLink() {
		// corresponds to figure 5 in the documentation
		// the "green" turn restriction starting on link kg will be applied first
		var f = new Fixture();
		// make some other links more expensive to prevent routing along them
		f.overwriteLinkLength("jf", 10000);
		f.overwriteLinkLength("ie", 10000);
		f.overwriteLinkLength("cg", 600);
		f.overwriteLinkLength("cd", 600);

		var graph = runTest(f.network, "J", "B", "jk", "kg", "gc", "cb");
		Assertions.assertEquals(16, graph.nodeCount);
		Assertions.assertEquals(48, graph.linkCount);

		f.addTurnRestriction("jk", "kg", "gf");
		f.addTurnRestriction("jk", "kl", "lp");

		f.addTurnRestriction("kg", "gc", "cb");
		f.addTurnRestriction("kg", "gh");

//		SpeedyGraphBuilder.build(f.network, TransportMode.car).printDebug();
		verifyGraph(f.network, "J", new String[][] {
			new String[] {"jk", "kg", "gf"},
			new String[] {"jk", "kl", "lp"},
			new String[] {"kg", "gc", "cb"},
			new String[] {"kg", "gh"},
			new String[] {"jk", "kg", "gh"},
			new String[] {"jk", "kg", "gc", "cb"},
		});

		graph = runTest(f.network, "J", "B", "jk", "kl", "lh", "hd", "dc", "cb");
		Assertions.assertEquals(16 + 5, graph.nodeCount, "expected duplicated nodes: G', C', K', L', G''");
		Assertions.assertEquals(48 + 13, graph.linkCount, "expected duplicated links: g'k, g'f, g'c', c'g, c'd, k'j, k'o, k'g'', g''k, g''c', k'l', l'k, l'h");
	}

	@Test
	public void testMultipleRestrictionsOnMultipleLink_rotated() {
		// corresponds to figure 5 in the documentation, but rotate by 90deg clockwise
		// the "red" turn restriction starting on link fj (instead of jk) will be applied first
		var f = new Fixture();
		// make some other links more expensive to prevent routing along them
		f.overwriteLinkLength("fg", 10000);
		f.overwriteLinkLength("bc", 10000);
		f.overwriteLinkLength("lk", 600);
		f.overwriteLinkLength("lp", 600);
		f.overwriteLinkLength("gh", 550);
		f.overwriteLinkLength("fe", 550);
		f.overwriteLinkLength("ij", 600);
		f.overwriteLinkLength("kj", 600);
		f.overwriteLinkLength("ok", 600);
		f.overwriteLinkLength("nj", 600);

		var graph = runTest(f.network, "F", "H", "fj", "jk", "kl", "lh");
		Assertions.assertEquals(16, graph.nodeCount);
		Assertions.assertEquals(48, graph.linkCount);

		f.addTurnRestriction("fj", "jk", "kg");
		f.addTurnRestriction("fj", "jn", "nm");

		f.addTurnRestriction("jk", "kl", "lh");
		f.addTurnRestriction("jk", "ko");

		verifyGraph(f.network, "F", new String[][] {
			new String[] {"fj", "jk", "kg"},
			new String[] {"fj", "jn", "nm"},
			new String[] {"jk", "kl", "lh"},
			new String[] {"jk", "ko"},
			new String[] {"fj", "jk", "ko"},
			new String[] {"fj", "jk", "kl", "lh"},
		});

		graph = runTest(f.network, "F", "H", "fj", "jn", "no", "op", "pl", "lh");
		Assertions.assertEquals(16 + 7, graph.nodeCount, "expected duplicated nodes: J', N', K', K'', L', K''', L''");
		Assertions.assertEquals(48 + 18, graph.linkCount, "expected duplicated links: j'i, j'f, j'n', n'j, n'o, k'j, k'o, k'l  -  k''j, k''g, k''l', l'k, l'p,  - jk''', k'''j, k'''l'', l''k, l''p");
	}

	@Test
	public void testShortRestrictionsForReuse() {
		// corresponds to figure 8 in the documentation
		// the "red" turn restriction starting on link kg will be applied first
		var f = new Fixture();
		// make some other links more expensive to prevent routing along them
		f.overwriteLinkLength("jf", 10000);
		f.overwriteLinkLength("ie", 10000);
		// make link kg same as link lh
		f.overwriteLinkLength("gh", 600);
		f.overwriteLinkLength("cd", 600);
		f.overwriteLinkLength("nj", 10000);
		f.overwriteLinkLength("lp", 600);
		// make link no cheaper than link jk
		f.overwriteLinkLength("no", 500);

		var graph = runTest(f.network, "J", "D", "jk", "kl", "lh", "hd");
		Assertions.assertEquals(16, graph.nodeCount);
		Assertions.assertEquals(48, graph.linkCount);

		f.addTurnRestriction("jk", "kg");

		f.addTurnRestriction("kl", "lh");

//		SpeedyGraphBuilder.build(f.network, TransportMode.car).printDebug();
		verifyGraph(f.network, "J", new String[][] {
			new String[] {"jk", "kg"},
			new String[] {"kl", "lh"},
			new String[] {"jk", "kl", "lh"},
		});

		graph = runTest(f.network, "J", "D", "jn", "no", "op", "pl", "lh", "hd");
		Assertions.assertEquals(16 + 2, graph.nodeCount, "expected duplicated nodes: K', L'");
		Assertions.assertEquals(48 + 5, graph.linkCount, "expected duplicated links: k'j, k'o, k'l', l'k, l'p");
	}

	/** Checks that none of the provided paths exist in the graph based on the provided network */
	private static void verifyGraph(Network network, String fromNode, String[][] forbiddenPaths) {
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		Node realFromNode = network.getNodes().get(Id.create(fromNode, Node.class));

		for (String[] path : forbiddenPaths) {
			if (findPath(graph, realFromNode.getId().index(), path)) {
				Assertions.fail("Found path that should not exist in graph");
			}
		}

		SpeedyGraph.LinkIterator outLinkIterator = graph.getOutLinkIterator();
		outLinkIterator.reset(realFromNode.getId().index());
	}

	private static boolean findPath(SpeedyGraph graph, int nodeIndex, String[] path) {
		Id<Link> nextLinkId = Id.create(path[0], Link.class);
		SpeedyGraph.LinkIterator outLinkIterator = graph.getOutLinkIterator();
		outLinkIterator.reset(nodeIndex);
		while (outLinkIterator.next()) {
			int linkIndex = outLinkIterator.getLinkIndex();
			Link link = graph.getLink(linkIndex);
			if (link.getId() == nextLinkId) {
				if (path.length == 1) {
					return true;
				}
				return findPath(graph, outLinkIterator.getToNodeIndex(), Arrays.copyOfRange(path, 1, path.length));
			}
		}
		return false;
	}

	private static SpeedyGraph runTest(Network network, String fromNode, String toNode, String... expectedPath) {
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyDijkstra dijkstra = new SpeedyDijkstra(graph, freespeed, freespeed);

		Node realFromNode = network.getNodes().get(Id.create(fromNode, Node.class));
		Node realToNode = network.getNodes().get(Id.create(toNode, Node.class));
		LeastCostPathCalculator.Path path = dijkstra.calcLeastCostPath(realFromNode, realToNode, 7*3600, null, null);

		assertPath(path, expectedPath);
		return graph;
	}


	private static void assertPath(LeastCostPathCalculator.Path path, String... linkIds) {
		var links = path.links;
		LOG.info("expected path: " + Arrays.toString(linkIds) + "\nactual path: " + path.links.stream().map(link -> link.getId().toString()).collect(Collectors.joining(",")));
		Assertions.assertEquals(linkIds.length, links.size());
		for (int i = 0; i < linkIds.length; i++) {
			Assertions.assertEquals(linkIds[i], links.get(i).getId().toString());
		}
	}

	private static class Fixture {
		private final Network network;

		public Fixture() {
			this.network = NetworkUtils.createNetwork();

			Node a = createAndAddNode("A", 1000, 2500);
			Node b = createAndAddNode("B", 1500, 2500);
			Node c = createAndAddNode("C", 2000, 2500);
			Node d = createAndAddNode("D", 2500, 2500);
			Node e = createAndAddNode("E", 1000, 2000);
			Node f = createAndAddNode("F", 1500, 2000);
			Node g = createAndAddNode("G", 2000, 2000);
			Node h = createAndAddNode("H", 2500, 2000);
			Node i = createAndAddNode("I", 1000, 1500);
			Node j = createAndAddNode("J", 1500, 1500);
			Node k = createAndAddNode("K", 2000, 1500);
			Node l = createAndAddNode("L", 2500, 1500);
			Node m = createAndAddNode("M", 1000, 1000);
			Node n = createAndAddNode("N", 1500, 1000);
			Node o = createAndAddNode("O", 2000, 1000);
			Node p = createAndAddNode("P", 2500, 1000);

			createAndAddLink("ab", a, b, 500);
			createAndAddLink("ba", b, a, 501);
			createAndAddLink("bc", b, c, 502);
			createAndAddLink("cb", c, b, 503);
			createAndAddLink("cd", c, d, 504);
			createAndAddLink("dc", d, c, 505);

			createAndAddLink("ae", a, e, 506);
			createAndAddLink("ea", e, a, 507);
			createAndAddLink("bf", b, f, 508);
			createAndAddLink("fb", f, b, 509);
			createAndAddLink("cg", c, g, 510);
			createAndAddLink("gc", g, c, 511);
			createAndAddLink("dh", d, h, 512);
			createAndAddLink("hd", h, d, 513);

			createAndAddLink("ef", e, f, 514);
			createAndAddLink("fe", f, e, 515);
			createAndAddLink("fg", f, g, 516);
			createAndAddLink("gf", g, f, 517);
			createAndAddLink("gh", g, h, 518);
			createAndAddLink("hg", h, g, 519);

			createAndAddLink("ei", e, i, 520);
			createAndAddLink("ie", i, e, 521);
			createAndAddLink("fj", f, j, 522);
			createAndAddLink("jf", j, f, 523);
			createAndAddLink("gk", g, k, 524);
			createAndAddLink("kg", k, g, 525);
			createAndAddLink("hl", h, l, 526);
			createAndAddLink("lh", l, h, 527);

			createAndAddLink("ij", i, j, 528);
			createAndAddLink("ji", j, i, 529);
			createAndAddLink("jk", j, k, 530);
			createAndAddLink("kj", k, j, 531);
			createAndAddLink("kl", k, l, 532);
			createAndAddLink("lk", l, k, 533);

			createAndAddLink("im", i, m, 534);
			createAndAddLink("mi", m, i, 535);
			createAndAddLink("jn", j, n, 536);
			createAndAddLink("nj", n, j, 537);
			createAndAddLink("ko", k, o, 538);
			createAndAddLink("ok", o, k, 539);
			createAndAddLink("lp", l, p, 540);
			createAndAddLink("pl", p, l, 541);

			createAndAddLink("mn", m, n, 542);
			createAndAddLink("nm", n, m, 543);
			createAndAddLink("no", n, o, 544);
			createAndAddLink("on", o, n, 545);
			createAndAddLink("op", o, p, 546);
			createAndAddLink("po", p, o, 547);
		}

		private Node createAndAddNode(String id, double x, double y) {
			Node node = this.network.getFactory().createNode(Id.create(id, Node.class), new Coord(x, y));
			this.network.addNode(node);
			return node;
		}

		private void createAndAddLink(String id, Node fromNode, Node toNode, double length) {
			Link link = this.network.getFactory().createLink(Id.create(id, Link.class), fromNode, toNode);
			link.setLength(length);
			link.setFreespeed(20.0);
			link.setCapacity(2000);
			link.setNumberOfLanes(1);
			link.setAllowedModes(Set.of(TransportMode.car));
			this.network.addLink(link);
		}

		private void addTurnRestriction(String linkId, String... disallowedLinks) {
			Id<Link> realLinkId = Id.create(linkId, Link.class);
			Link link = this.network.getLinks().get(realLinkId);
			var restrictions = NetworkUtils.getDisallowedNextLinks(link);
			if (restrictions == null) {
				restrictions = new DisallowedNextLinks();
				NetworkUtils.setDisallowedNextLinks(link, restrictions);
			}

			List<Id<Link>> disallowedLinkIds = Arrays.stream(disallowedLinks).map(id -> Id.create(id, Link.class)).toList();
			restrictions.addDisallowedLinkSequence(TransportMode.car, disallowedLinkIds);
		}

		private void overwriteLinkLength(String linkId, double length) {
			this.network.getLinks().get(Id.create(linkId, Link.class)).setLength(length);
		}
	}

}
