package org.matsim.contrib.msp.capability.facilities;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the view on a MATSim facility that the capability analysis works with, and in particular the
 * opening hours question the insertion step asks of it.
 */
public class DiscretionaryActivityFacilityTest {

	@Test
	void keepsTypeSubTypeAndLocation() {

		ActivityFacility facility = facility("super-7", new Coord(100, 200), "123");
		DiscretionaryActivityFacility shop = new DiscretionaryActivityFacility(facility, "shop", "Konsum",
			List.of(new OpeningTimeImpl(8 * 3600, 20 * 3600)));

		assertEquals(Id.create("super-7", ActivityFacility.class), shop.getId());
		assertEquals("shop", shop.getType());
		assertEquals(Optional.of("Konsum"), shop.getSubType());

		// the location comes from the MATSim facility, so the view can be routed to directly
		assertEquals(new Coord(100, 200), shop.getCoord());
		assertEquals(Id.createLinkId("123"), shop.getLinkId());
		assertEquals(facility, shop.getFacility());

		assertEquals(8 * 3600, shop.getOpeningTime());
		assertEquals(20 * 3600, shop.getClosingTime());
		assertFalse(shop.isAlwaysOpen());
	}

	@Test
	void hasNoSubTypeIfNoneIsGiven() {
		DiscretionaryActivityFacility leisure = DiscretionaryActivityFacility.create(facility("park-1"), "leisure");
		assertEquals(Optional.empty(), leisure.getSubType());
	}

	@Test
	void withoutOpeningTimesItIsAlwaysOpen() {

		DiscretionaryActivityFacility park = DiscretionaryActivityFacility.create(facility("park-1"), "leisure");

		assertTrue(park.isAlwaysOpen());
		assertEquals(Double.NEGATIVE_INFINITY, park.getOpeningTime());
		assertEquals(Double.POSITIVE_INFINITY, park.getClosingTime());
		assertTrue(park.isOpenAt(3 * 3600));
		assertTrue(park.isOpenDuring(0, 24 * 3600));
	}

	@Test
	void answersWhetherItIsOpen() {

		DiscretionaryActivityFacility shop = new DiscretionaryActivityFacility(facility("super-7"), "shop", null,
			List.of(new OpeningTimeImpl(8 * 3600, 20 * 3600)));

		assertFalse(shop.isOpenAt(7 * 3600));
		assertTrue(shop.isOpenAt(8 * 3600), "the boundary counts as open");
		assertTrue(shop.isOpenAt(12 * 3600));
		assertTrue(shop.isOpenAt(20 * 3600), "the boundary counts as open");
		assertFalse(shop.isOpenAt(21 * 3600));
	}

	@Test
	void answersWhetherAnActivityFitsIntoTheOpeningHours() {

		DiscretionaryActivityFacility shop = new DiscretionaryActivityFacility(facility("super-7"), "shop", null,
			List.of(new OpeningTimeImpl(8 * 3600, 20 * 3600)));

		// this is the question the insertion step asks of a gap in the fundamental plan
		assertTrue(shop.isOpenDuring(17 * 3600, 18 * 3600));
		assertTrue(shop.isOpenDuring(8 * 3600, 20 * 3600), "exactly the opening hours fit");
		assertFalse(shop.isOpenDuring(7 * 3600, 9 * 3600), "it is still closed at the start");
		assertFalse(shop.isOpenDuring(19 * 3600, 21 * 3600), "it closes before the end");
		assertFalse(shop.isOpenDuring(22 * 3600, 23 * 3600));
	}

	@Test
	void anActivityHasToFitIntoOneSingleWindow() {

		// a restaurant with a lunch and a dinner service
		DiscretionaryActivityFacility restaurant = new DiscretionaryActivityFacility(facility("gasthaus-2"), "restaurant", "German",
			List.of(new OpeningTimeImpl(11 * 3600, 14 * 3600), new OpeningTimeImpl(17 * 3600, 23 * 3600)));

		assertEquals(11 * 3600, restaurant.getOpeningTime());
		assertEquals(23 * 3600, restaurant.getClosingTime());

		assertTrue(restaurant.isOpenDuring(12 * 3600, 13 * 3600), "lunch");
		assertTrue(restaurant.isOpenDuring(19 * 3600, 21 * 3600), "dinner");
		assertFalse(restaurant.isOpenAt(15 * 3600), "closed in the afternoon");

		// the agent cannot sit there across the afternoon closure, even though it is open on both sides
		assertFalse(restaurant.isOpenDuring(13 * 3600, 18 * 3600));
	}

	@Test
	void rejectsAnIntervalThatEndsBeforeItStarts() {
		DiscretionaryActivityFacility park = DiscretionaryActivityFacility.create(facility("park-1"), "leisure");
		assertThrows(IllegalArgumentException.class, () -> park.isOpenDuring(10 * 3600, 9 * 3600));
	}

	@Test
	void groupsPlacesByTypeAndSubType() {

		ActivityFacility konsum = facility("super-7");
		ActivityFacility edeka = facility("super-8");
		ActivityFacility mall = facility("mall-1");

		DiscretionaryActivityFacilities facilities = new DiscretionaryActivityFacilities(List.of(
			new DiscretionaryActivityFacility(konsum, "shop", "Konsum", List.of()),
			new DiscretionaryActivityFacility(edeka, "shop", "Edeka", List.of()),
			// one facility offering two types shows up once per type
			new DiscretionaryActivityFacility(mall, "shop", "Konsum", List.of()),
			new DiscretionaryActivityFacility(mall, "restaurant", "Italian", List.of())
		));

		assertEquals(4, facilities.size());
		assertEquals(List.of("restaurant", "shop"), List.copyOf(facilities.getTypes()));
		assertEquals(3, facilities.getByType("shop").size());
		assertEquals(1, facilities.getByType("restaurant").size());
		assertTrue(facilities.getByType("bar").isEmpty(), "nothing of that type is known");

		// the variety of a type: two distinct supermarket brands, each counted once
		assertEquals(List.of("Konsum", "Edeka"), List.copyOf(facilities.getSubTypes("shop")));
		assertEquals(List.of("Italian"), List.copyOf(facilities.getSubTypes("restaurant")));

		// the mall is reachable by its facility id and offers both types
		assertEquals(List.of("shop", "restaurant"),
			facilities.get(mall.getId()).stream().map(DiscretionaryActivityFacility::getType).toList());
	}

	private static ActivityFacility facility(String id) {
		return facility(id, new Coord(0, 0), null);
	}

	private static ActivityFacility facility(String id, Coord coord, String linkId) {
		ActivityFacilities facilities = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getActivityFacilities();
		ActivityFacility facility = facilities.getFactory()
			.createActivityFacility(Id.create(id, ActivityFacility.class), coord,
				linkId == null ? null : Id.create(linkId, Link.class));
		facilities.addActivityFacility(facility);
		return facility;
	}
}
