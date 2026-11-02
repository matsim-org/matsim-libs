package org.matsim.contrib.msp.capability.facilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reading the places for discretionary activities out of a plain MATSim facilities container.
 */
public class DiscretionaryActivityFacilityExtractorTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private final CapabilityConfigGroup config = new CapabilityConfigGroup();

	@Test
	void readsTypeSubTypeAndOpeningHoursFromAnnotatedFacilities() {

		ActivityFacilities facilities = facilities();

		ActivityFacility konsum = addFacility(facilities, "super-7", new Coord(100, 200));
		annotate(konsum, "shop", "Konsum");
		addOption(konsum, "shop", 8 * 3600, 20 * 3600);

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);

		assertEquals(1, extracted.size());
		DiscretionaryActivityFacility shop = extracted.getByType("shop").get(0);

		assertEquals("shop", shop.getType());
		assertEquals(Optional.of("Konsum"), shop.getSubType());
		assertEquals(new Coord(100, 200), shop.getCoord());
		assertEquals(8 * 3600, shop.getOpeningTime());
		assertEquals(20 * 3600, shop.getClosingTime());
	}

	@Test
	void fallsBackToTheActivityOptionsOfAnUnannotatedFacility() {

		ActivityFacilities facilities = facilities();

		// a facility of a scenario that was never annotated for the capability analysis
		ActivityFacility facility = addFacility(facilities, "mall-1", new Coord(0, 0));
		addOption(facility, "shop_daily", 9 * 3600, 20 * 3600);
		addOption(facility, "leisure", 10 * 3600, 23 * 3600);

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);

		// one entry per activity option, each with the opening hours of that option
		assertEquals(2, extracted.size());
		assertEquals(List.of("leisure", "shop_daily"), List.copyOf(extracted.getTypes()));
		assertEquals(20 * 3600, extracted.getByType("shop_daily").get(0).getClosingTime());
		assertEquals(23 * 3600, extracted.getByType("leisure").get(0).getClosingTime());
		assertEquals(Optional.empty(), extracted.getByType("leisure").get(0).getSubType());
	}

	@Test
	void aUserDefinedTypeDoesNotHaveToMatchTheActivityOption() {

		ActivityFacilities facilities = facilities();

		// the scenario calls the activity 'leisure', the capability analysis calls the place a 'bar'
		ActivityFacility bar = addFacility(facilities, "bar-3", new Coord(0, 0));
		annotate(bar, "bar", "cocktail");
		addOption(bar, "leisure", 10 * 3600, 23 * 3600);
		bar.getAttributes().putAttribute(DiscretionaryActivityFacilityExtractor.OPENING_TIME_ATTRIBUTE, "18:00:00");
		// MATSim counts on past midnight, so closing at 03:00 of the next day is 27:00:00
		bar.getAttributes().putAttribute(DiscretionaryActivityFacilityExtractor.CLOSING_TIME_ATTRIBUTE, "27:00:00");

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);

		assertEquals(List.of("bar"), List.copyOf(extracted.getTypes()), "the attribute wins over the activity option");

		DiscretionaryActivityFacility place = extracted.getByType("bar").get(0);
		assertEquals(Optional.of("cocktail"), place.getSubType());

		// the activity option named 'leisure' does not describe the 'bar', so the attributes are used
		assertEquals(18 * 3600, place.getOpeningTime());
		assertEquals(27 * 3600, place.getClosingTime(), "03:00 of the next day");
		assertTrue(place.isOpenDuring(20 * 3600, 22 * 3600));
	}

	@Test
	void oneFacilityCanOfferSeveralTypes() {

		ActivityFacilities facilities = facilities();

		ActivityFacility mall = addFacility(facilities, "mall-1", new Coord(0, 0));
		annotate(mall, "shop,restaurant", "Centrum");
		addOption(mall, "shop", 9 * 3600, 20 * 3600);
		addOption(mall, "restaurant", 11 * 3600, 22 * 3600);

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);

		assertEquals(2, extracted.size());
		assertEquals(List.of("restaurant", "shop"), List.copyOf(extracted.getTypes()));

		// each type keeps the opening hours of its own activity option
		assertEquals(20 * 3600, extracted.getByType("shop").get(0).getClosingTime());
		assertEquals(22 * 3600, extracted.getByType("restaurant").get(0).getClosingTime());

		// and both are reachable through the one facility they share
		assertEquals(2, extracted.get(mall.getId()).size());
	}

	@Test
	void aFacilityWithoutAnyTypeIsSkipped() {

		ActivityFacilities facilities = facilities();
		addFacility(facilities, "nothing-here", new Coord(0, 0));

		assertTrue(new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities).isEmpty());
	}

	@Test
	void aFacilityWithoutOpeningHoursIsAlwaysOpen() {

		ActivityFacilities facilities = facilities();
		ActivityFacility park = addFacility(facilities, "park-1", new Coord(0, 0));
		annotate(park, "leisure", null);

		DiscretionaryActivityFacility place = new DiscretionaryActivityFacilityExtractor(this.config)
			.extract(facilities).getByType("leisure").get(0);

		assertTrue(place.isAlwaysOpen());
		assertTrue(place.isOpenDuring(2 * 3600, 4 * 3600));
	}

	@Test
	void readsWhatMatsimWrote() {

		ActivityFacilities facilities = facilities();

		ActivityFacility konsum = addFacility(facilities, "super-7", new Coord(100, 200));
		annotate(konsum, "shop", "Konsum");
		addOption(konsum, "shop", 8 * 3600, 20 * 3600);

		ActivityFacility gasthaus = addFacility(facilities, "gasthaus-2", new Coord(300, 400));
		annotate(gasthaus, "restaurant", "German");
		ActivityOption option = addOption(gasthaus, "restaurant", 11 * 3600, 14 * 3600);
		option.addOpeningTime(new OpeningTimeImpl(17 * 3600, 23 * 3600));

		// no format of its own: the annotated facilities survive the standard MATSim writer and reader
		Path file = Path.of(utils.getOutputDirectory(), "facilities.xml");
		new FacilitiesWriter(facilities).write(file.toString());

		ActivityFacilities reread = facilities();
		new MatsimFacilitiesReader(null, null, reread).readFile(file.toString());

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(reread);

		assertEquals(2, extracted.size());
		assertEquals(List.of("restaurant", "shop"), List.copyOf(extracted.getTypes()));

		DiscretionaryActivityFacility shop = extracted.getByType("shop").get(0);
		assertEquals(Optional.of("Konsum"), shop.getSubType());
		assertEquals(new Coord(100, 200), shop.getCoord());
		assertEquals(8 * 3600, shop.getOpeningTime());

		DiscretionaryActivityFacility restaurant = extracted.getByType("restaurant").get(0);
		assertEquals(Optional.of("German"), restaurant.getSubType());
		assertEquals(2, restaurant.getOpeningTimes().size(), "both services survive the round trip");
		assertTrue(restaurant.isOpenDuring(12 * 3600, 13 * 3600));
		assertTrue(restaurant.isOpenDuring(19 * 3600, 21 * 3600));
		assertEquals(false, restaurant.isOpenAt(15 * 3600));
	}

	@Test
	void usesTheConfiguredAttributeNames() {

		this.config.setFacilityTypeAttribute("osmType");
		this.config.setFacilitySubTypeAttribute("brand");

		ActivityFacilities facilities = facilities();
		ActivityFacility konsum = addFacility(facilities, "super-7", new Coord(0, 0));
		konsum.getAttributes().putAttribute("osmType", "shop");
		konsum.getAttributes().putAttribute("brand", "Konsum");

		DiscretionaryActivityFacilities extracted = new DiscretionaryActivityFacilityExtractor(this.config).extract(facilities);

		assertEquals(List.of("shop"), List.copyOf(extracted.getTypes()));
		assertEquals(Optional.of("Konsum"), extracted.getByType("shop").get(0).getSubType());
	}

	private void annotate(ActivityFacility facility, String types, String subType) {
		DiscretionaryActivityFacilityExtractor.annotate(facility, this.config, subType, types);
	}

	private static ActivityFacilities facilities() {
		return ScenarioUtils.createScenario(ConfigUtils.createConfig()).getActivityFacilities();
	}

	private static ActivityFacility addFacility(ActivityFacilities facilities, String id, Coord coord) {
		ActivityFacility facility = facilities.getFactory()
			.createActivityFacility(Id.create(id, ActivityFacility.class), coord);
		facilities.addActivityFacility(facility);
		return facility;
	}

	private static ActivityOption addOption(ActivityFacility facility, String type, double opening, double closing) {
		ActivityOption option = facility.getActivityOptions().containsKey(type)
			? facility.getActivityOptions().get(type)
			: newOption(facility, type);
		option.addOpeningTime(new OpeningTimeImpl(opening, closing));
		return option;
	}

	private static ActivityOption newOption(ActivityFacility facility, String type) {
		ActivityOption option = ScenarioUtils.createScenario(ConfigUtils.createConfig())
			.getActivityFacilities().getFactory().createActivityOption(type);
		facility.addActivityOption(option);
		return option;
	}
}
