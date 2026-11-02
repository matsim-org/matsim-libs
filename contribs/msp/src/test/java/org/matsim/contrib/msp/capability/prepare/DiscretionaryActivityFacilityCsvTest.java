package org.matsim.contrib.msp.capability.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the csv interchange format of the places for discretionary activities.
 */
public class DiscretionaryActivityFacilityCsvTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private final CapabilityConfigGroup config = new CapabilityConfigGroup();

	@Test
	void readsAllColumns() {

		DiscretionaryActivityFacilities places = read("""
			id,name,type,sub_type,x,y,opening_time,closing_time
			super-7,Konsum Striesen,shop,Konsum,4621000,5820000,08:00:00,20:00:00
			gasthaus-2,Gasthaus Am Markt,restaurant,German,4622000,5821000,11:00:00,23:00:00
			""");

		assertEquals(2, places.size());
		assertEquals(List.of("restaurant", "shop"), List.copyOf(places.getTypes()));

		DiscretionaryActivityFacility shop = places.getByType("shop").get(0);
		assertEquals(Id.create("super-7", org.matsim.facilities.ActivityFacility.class), shop.getId());
		assertEquals(Optional.of("Konsum Striesen"), shop.getName());
		assertEquals("shop", shop.getType());
		assertEquals(Optional.of("Konsum"), shop.getSubType());
		assertEquals(new Coord(4621000, 5820000), shop.getCoord());
		assertEquals(8 * 3600, shop.getOpeningTime());
		assertEquals(20 * 3600, shop.getClosingTime());
		assertTrue(shop.isOpenDuring(17 * 3600, 18 * 3600));
	}

	@Test
	void anEmptyCellMeansNotKnown() {

		DiscretionaryActivityFacilities places = read("""
			id,name,type,sub_type,x,y,opening_time,closing_time
			park-1,,leisure,,4623000,5822000,,
			late-1,Spätshop,shop,,4624000,5823000,18:00:00,
			""");

		DiscretionaryActivityFacility park = places.getByType("leisure").get(0);
		assertEquals(Optional.empty(), park.getName());
		assertEquals(Optional.empty(), park.getSubType());
		assertTrue(park.isAlwaysOpen(), "two empty time cells mean open around the clock");

		DiscretionaryActivityFacility late = places.getByType("shop").get(0);
		assertFalse(late.isAlwaysOpen());
		assertEquals(18 * 3600, late.getOpeningTime());
		assertEquals(Double.POSITIVE_INFINITY, late.getClosingTime(), "an empty closing time is open ended");
		assertTrue(late.isOpenAt(23 * 3600));
		assertFalse(late.isOpenAt(12 * 3600));
	}

	@Test
	void acceptsTimesPastMidnightAndSeconds() {

		DiscretionaryActivityFacilities places = read("""
			id,type,x,y,opening_time,closing_time
			bar-3,bar,0,0,18:00:00,27:00:00
			shop-9,shop,0,0,28800,72000
			""");

		DiscretionaryActivityFacility bar = places.getByType("bar").get(0);
		assertEquals(18 * 3600, bar.getOpeningTime());
		assertEquals(27 * 3600, bar.getClosingTime(), "03:00 of the next day");
		assertTrue(bar.isOpenDuring(23 * 3600, 26 * 3600));

		DiscretionaryActivityFacility shop = places.getByType("shop").get(0);
		assertEquals(8 * 3600, shop.getOpeningTime(), "a plain number is a number of seconds");
		assertEquals(20 * 3600, shop.getClosingTime());
	}

	@Test
	void columnOrderDoesNotMatterAndExtraColumnsBecomeAttributes() {

		ActivityFacilities facilities = csv().read(write("""
			type,osm_id,y,id,x
			shop,node/12345,5820000,super-7,4621000
			"""));

		assertEquals("node/12345",
			facilities.getFacilities().get(Id.create("super-7", org.matsim.facilities.ActivityFacility.class))
				.getAttributes().getAttribute("osm_id"));

		DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);
		assertEquals(List.of("shop"), List.copyOf(places.getTypes()));
	}

	@Test
	void severalRowsOfOneIdGiveItSeveralTypes() {

		DiscretionaryActivityFacilities places = read("""
			id,name,type,sub_type,x,y,opening_time,closing_time
			mall-1,Centrum Galerie,shop,Centrum,4621000,5820000,09:00:00,20:00:00
			mall-1,Centrum Galerie,restaurant,Centrum,4621000,5820000,11:00:00,22:00:00
			""");

		assertEquals(2, places.size(), "one entry per type");
		assertEquals(List.of("restaurant", "shop"), List.copyOf(places.getTypes()));

		// each type keeps its own opening hours
		assertEquals(20 * 3600, places.getByType("shop").get(0).getClosingTime());
		assertEquals(22 * 3600, places.getByType("restaurant").get(0).getClosingTime());

		// and both belong to the one facility
		var mall = Id.create("mall-1", org.matsim.facilities.ActivityFacility.class);
		assertEquals(2, places.get(mall).size());
	}

	@Test
	void linkIdIsAnAlternativeToCoordinates() {

		DiscretionaryActivityFacilities places = read("""
			id,type,link_id
			kiosk-1,shop,123
			""");

		DiscretionaryActivityFacility kiosk = places.getByType("shop").get(0);
		assertEquals(Id.createLinkId("123"), kiosk.getLinkId());
	}

	@Test
	void roundTripsThroughCsvAndFacilities() {

		String written = Path.of(utils.getOutputDirectory(), "places-out.csv").toString();

		DiscretionaryActivityFacilities places = read("""
			id,name,type,sub_type,x,y,opening_time,closing_time
			super-7,Konsum Striesen,shop,Konsum,4621000.0,5820000.0,08:00:00,20:00:00
			park-1,Großer Garten,leisure,,4623000.0,5822000.0,,
			""");

		csv().write(places, written);

		// reading what was written gives the same places back
		DiscretionaryActivityFacilities reread = new DiscretionaryActivityFacilityExtractor(this.config).extract(csv().read(written));

		assertEquals(places.size(), reread.size());
		assertEquals(places.getTypes(), reread.getTypes());

		DiscretionaryActivityFacility shop = reread.getByType("shop").get(0);
		assertEquals(Optional.of("Konsum Striesen"), shop.getName());
		assertEquals(Optional.of("Konsum"), shop.getSubType());
		assertEquals(new Coord(4621000, 5820000), shop.getCoord());
		assertEquals(8 * 3600, shop.getOpeningTime());
		assertEquals(20 * 3600, shop.getClosingTime());

		assertTrue(reread.getByType("leisure").get(0).isAlwaysOpen());
	}

	@Test
	void writesTheColumnsItReads() throws IOException {

		String written = Path.of(utils.getOutputDirectory(), "places-out.csv").toString();
		csv().write(read("""
			id,name,type,sub_type,x,y,opening_time,closing_time
			super-7,Konsum Striesen,shop,Konsum,4621000.0,5820000.0,08:00:00,20:00:00
			"""), written);

		assertEquals(List.of(
			"id,name,type,sub_type,x,y,link_id,opening_time,closing_time",
			"super-7,Konsum Striesen,shop,Konsum,4621000.0,5820000.0,,08:00:00,20:00:00"
		), Files.readAllLines(Path.of(written)));
	}

	@Test
	void complainsAboutAMissingColumn() {

		// no type column
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> read("""
			id,x,y
			super-7,0,0
			"""));
		assertTrue(e.getMessage().contains("type"), e.getMessage());

		// no location at all
		assertThrows(IllegalArgumentException.class, () -> read("""
			id,type
			super-7,shop
			"""));
	}

	@Test
	void complainsAboutAnUnreadableCellAndNamesTheRow() {

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> read("""
			id,type,x,y
			super-7,shop,0,0
			super-8,shop,here,0
			"""));
		assertTrue(e.getMessage().contains("Row 2"), e.getMessage());
		assertTrue(e.getMessage().contains("'x'"), e.getMessage());

		IllegalArgumentException times = assertThrows(IllegalArgumentException.class, () -> read("""
			id,type,x,y,opening_time,closing_time
			bar-3,bar,0,0,18:00:00,03:00:00
			"""));
		assertTrue(times.getMessage().contains("27:00:00"), "the message should hint at the convention");
	}

	private DiscretionaryActivityFacilities read(String content) {
		return new DiscretionaryActivityFacilityExtractor(this.config).extract(csv().read(write(content)));
	}

	private DiscretionaryActivityFacilityCsv csv() {
		return new DiscretionaryActivityFacilityCsv(this.config);
	}

	/**
	 * @return the path of a csv file holding the given content
	 */
	private String write(String content) {
		try {
			Path file = Files.createTempFile(Path.of(utils.getOutputDirectory()), "places", ".csv");
			Files.writeString(file, content);
			return file.toString();
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}
}
