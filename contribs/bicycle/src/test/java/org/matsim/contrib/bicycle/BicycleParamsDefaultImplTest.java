package org.matsim.contrib.bicycle;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.core.network.NetworkUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BicycleParamsDefaultImplTest {

	BicycleParamsDefaultImpl params = new BicycleParamsDefaultImpl();

//	################################## Test Gradients ##################################################

	@Test
	void getGradientPctNoFromZ() {
		Link link = createLink(new Coord(0, 0), new Coord(100, 0, 100));
		assertEquals(0., params.getGradient_pct(link ), 0.00001 );
	}

	@Test
	void getGradientPctNoToZ() {
		Link link = createLink(new Coord(0, 0, 100), new Coord(100, 0));
		assertEquals(0., params.getGradient_pct(link ), 0.00001 );
	}

	@Test
	void getGradientPctFlat() {
		Link link = createLink(new Coord(0, 0, 100), new Coord(100, 0, 100));
		assertEquals(0., params.getGradient_pct(link ), 0.00001 );
	}

	@Test
	void getGradientPctUphill() {
		Link link = createLink(new Coord(0, 0, 0), new Coord(100, 0, 100));
		assertEquals(100., params.getGradient_pct(link ), 0.00001 );
	}

	@Test
	void getGradientPctDownhill() {
		Link link = createLink(new Coord(0, 0, 100), new Coord(100, 0, 0));
		assertEquals(0., params.getGradient_pct(link ), 0.00001 );
		// yyyy The method returns 0 when the gradient is downhill in order to set the corresponding scoring to zero.  I don't think it is
		// good to change the "physics" in order to have a scoring consequence; the gradient should just be the gradient.  kai, jul'25
	}

//		################################## Test comfort factors ##################################################

	@Test
	void testComfortFactors() {
		List<ObjectDoublePair<String>> surfaces = List.of(ObjectDoublePair.of("paved", 1.0), ObjectDoublePair.of("asphalt", 1.0),
			ObjectDoublePair.of("concrete:lanes", .95), ObjectDoublePair.of("concrete_plates", .9), ObjectDoublePair.of("concrete:plates", .9),
			ObjectDoublePair.of("fine_gravel", .9), ObjectDoublePair.of("paving_stones", .8), ObjectDoublePair.of("paving_stones:35", .8),
			ObjectDoublePair.of("paving_stones:30", .8), ObjectDoublePair.of("compacted", .7), ObjectDoublePair.of("unpaved", .6),
			ObjectDoublePair.of("asphalt;paving_stones:35", .6), ObjectDoublePair.of("bricks", .6), ObjectDoublePair.of("gravel", .6),
			ObjectDoublePair.of("ground", .6), ObjectDoublePair.of("sett", .5), ObjectDoublePair.of("cobblestone;flattened", .5),
			ObjectDoublePair.of("cobblestone:flattened", .5), ObjectDoublePair.of("cobblestone", .4), ObjectDoublePair.of("stone", .4),
			ObjectDoublePair.of("grass", .4), ObjectDoublePair.of("compressed", .4), ObjectDoublePair.of("paving_stones:3", .4),
			ObjectDoublePair.of("cobblestone (bad)", .3), ObjectDoublePair.of("dirt", .3), ObjectDoublePair.of("earth", .3),
			ObjectDoublePair.of("wood", .3), ObjectDoublePair.of("pebblestone", .3), ObjectDoublePair.of("sand", .3), ObjectDoublePair.of("concrete", .1),
			ObjectDoublePair.of(null, 1.), ObjectDoublePair.of("test:default", .85));

		Link link = createLink(new Coord(0, 0), new Coord(100, 0));

		for (ObjectDoublePair<String> pair : surfaces) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, pair.left());
			assertEquals(pair.rightDouble(), params.getComfortFactor((String) link.getAttributes().getAttribute(OsmTags.SURFACE)), 0.00001);
		}
	}

// ############################################# Test infrastructure factors ##################################################

	@Test
	void testInfrastructureFactors() {
		List<ObjectDoublePair<String>> noCycleWay = List.of(
			ObjectDoublePair.of("trunk", 0.05),
			ObjectDoublePair.of("primary", 0.1),
			ObjectDoublePair.of("primary_link", 0.1),
			ObjectDoublePair.of("secondary", 0.3),
			ObjectDoublePair.of("secondary_link", 0.3),
			ObjectDoublePair.of("tertiary", 0.4),
			ObjectDoublePair.of("tertiary_link", 0.4),
			ObjectDoublePair.of("unclassified", 0.9),
			ObjectDoublePair.of("default", 0.95)
		);

		List<ObjectDoublePair<String>> cycleWay = List.of(
			ObjectDoublePair.of("cycleway", 1.0),
			ObjectDoublePair.of("path", 1.0),
			ObjectDoublePair.of("steps", 0.1),
			ObjectDoublePair.of("default", 0.95)
		);

		Map<String, List<ObjectDoublePair<String>>> cycleWays = Map.of("noCycleWay", noCycleWay,"cycleWay", cycleWay,
			"noTypeAttr", List.of(ObjectDoublePair.of(null, 0.85)));

		Link link = createLink(new Coord(0, 0), new Coord(100, 0));

		for (Map.Entry<String, List<ObjectDoublePair<String>>> entry : cycleWays.entrySet()) {
			if (entry.getKey().equals("cycleWay")) {
				link.getAttributes().putAttribute(BicycleUtils.CYCLEWAY, entry.getKey());
			} else if (entry.getKey().equals("noTypeAttr")) {
				link.getAttributes().removeAttribute("type");
			} else {
				//				do not set this attr for "noCycleWay"
				link.getAttributes().removeAttribute(BicycleUtils.CYCLEWAY);
			}

			for (ObjectDoublePair<String> pair : entry.getValue()) {
				NetworkUtils.setType(link, pair.left());

				String linkType = NetworkUtils.getType(link);
				String cycleWayType = BicycleUtils.getCyclewaytype(link);

				double expected = pair.rightDouble();
				double actual = params.getInfrastructureFactor(linkType, cycleWayType);

				assertEquals(expected, actual, 0.00001);
			}
		}
	}

	@Test
	void testComputeSurfaceFactor() {
		List<ObjectDoublePair<String>> surfaces = List.of(ObjectDoublePair.of("paved", 1.0), ObjectDoublePair.of("asphalt", 1.0),
			ObjectDoublePair.of("concrete:lanes", .8), ObjectDoublePair.of("concrete_plates", .8), ObjectDoublePair.of("concrete:plates", .8),
			ObjectDoublePair.of("fine_gravel", .7), ObjectDoublePair.of("paving_stones", .7), ObjectDoublePair.of("paving_stones:35", .7),
			ObjectDoublePair.of("paving_stones:30", .7), ObjectDoublePair.of("compacted", .9),
			ObjectDoublePair.of("asphalt;paving_stones:35", .9), ObjectDoublePair.of("bricks", .7), ObjectDoublePair.of("gravel", .7),
			ObjectDoublePair.of("ground", .7), ObjectDoublePair.of("sett", .6), ObjectDoublePair.of("cobblestone;flattened", .6),
			ObjectDoublePair.of("cobblestone:flattened", .6), ObjectDoublePair.of("stone", .7),
			ObjectDoublePair.of("grass", .4), ObjectDoublePair.of("compressed", .7), ObjectDoublePair.of("paving_stones:3", .8),
			ObjectDoublePair.of("cobblestone (bad)", .4), ObjectDoublePair.of("earth", .6), ObjectDoublePair.of("pebblestone", .7),
			ObjectDoublePair.of("sand", .2), ObjectDoublePair.of("concrete", .9),
			ObjectDoublePair.of(null, 1.), ObjectDoublePair.of("test:default", .5));

		Link link = createLink(new Coord(0, 0), new Coord(100, 0));
		link.getAttributes().putAttribute(BicycleUtils.WAY_TYPE, "type");

		for (ObjectDoublePair<String> pair : surfaces) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, pair.left());
			assertEquals(pair.rightDouble(), params.computeSurfaceFactor(link), 0.00001);
		}

	}

	private static Link createLink(Coord from, Coord to) {

		Network net = NetworkUtils.createNetwork();
		Node fromNode = net.getFactory().createNode(Id.createNodeId("from"), from);
		Node toNode = net.getFactory().createNode(Id.createNodeId("to"), to);
		Link link = net.getFactory().createLink(Id.createLinkId("link"), fromNode, toNode);
		link.setLength(100);
		return link;
	}
}
