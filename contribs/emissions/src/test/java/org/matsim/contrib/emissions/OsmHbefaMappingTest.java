package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.*;

public class OsmHbefaMappingTest {

	@Test
	void testRegularMapping() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("primary", 70 / 3.6);

        var hbefaType = mapping.determineHbefaType(link);

        assertEquals("URB/Trunk-City/70", hbefaType);
    }

	@Test
	void testMergedLinkTypeMapping() {

		var mapping = OsmHbefaMapping.build();
		var link = getTestLink("primary|railway.tram", 70 / 3.6);

		var hbefaType = mapping.determineHbefaType(link);

		assertEquals("URB/Trunk-City/70", hbefaType);
	}

	@Test
	void testUnknownType() {
		assertThrows(RuntimeException.class, () -> {

			var mapping = OsmHbefaMapping.build();
			var link = getTestLink("unknown-tag", 100 / 3.6);

			mapping.determineHbefaType(link);

			fail("Expected Runtime Exception.");
		});
	}

	@Test
	void testFastMotorway() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("motorway", 100 / 3.6);

        var hbefaType = mapping.determineHbefaType(link);

        assertEquals("URB/MW-Nat./100", hbefaType);
    }

	@Test
	void testMotorwayWithNoExactSpeedTag() {

		var mapping = OsmHbefaMapping.build();

		var link = getTestLink("motorway", 100.11 / 3.6);
		var hbefaType = mapping.determineHbefaType(link);
		assertEquals("URB/MW-Nat./100", hbefaType);

		link = getTestLink("motorway", 86.11 / 3.6);
		hbefaType = mapping.determineHbefaType(link);
		assertEquals("URB/MW-Nat./90", hbefaType);

	}

	@Test
	void testFastMotorwayLink() {

		var mapping = OsmHbefaMapping.build();
		var link = getTestLink("motorway_link", 100 / 3.6);

		var hbefaType = mapping.determineHbefaType(link);

		assertEquals("URB/MW-Nat./100", hbefaType);
	}

	@Test
	void testLivingStreet() {
		var mapping = OsmHbefaMapping.build();
		var link = getTestLink("living_street", 50 / 3.6);

		var hbefaType = mapping.determineHbefaType(link);

		assertEquals("URB/Access/50", hbefaType);
	}

	@Test
	void testUnclassified() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("unclassified", 50 / 3.6);

        var hbefaType = mapping.determineHbefaType(link);

        assertEquals("URB/Access/50", hbefaType);
    }

	@Test
	void testNoHighwayType() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink(" ", 60 / 3.6);

        var hbefaType = mapping.determineHbefaType(link);

        assertEquals("URB/Local/60", hbefaType);
    }

	@Test
	void testNoAllowedSpeedTag() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("residential", 40 / 3.6);
        link.getAttributes().removeAttribute(NetworkUtils.ALLOWED_SPEED);

        var hbefaType = mapping.determineHbefaType(link);

        assertEquals("URB/Access/40", hbefaType);
    }

    private static Link getTestLink(String osmRoadType, double allowedSpeed) {

        var network = NetworkUtils.createNetwork();
        var from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
        var to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(0, 1000));
        var link = network.getFactory().createLink(Id.createLinkId("link"), from, to);
        link.setFreespeed(allowedSpeed);
        link.setCapacity(1000);
        link.setNumberOfLanes(1);
        link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);
        link.getAttributes().putAttribute(NetworkUtils.TYPE, osmRoadType);

        return link;
    }
}
