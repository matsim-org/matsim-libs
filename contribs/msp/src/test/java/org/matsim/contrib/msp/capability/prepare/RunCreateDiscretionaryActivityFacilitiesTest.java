package org.matsim.contrib.msp.capability.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the command that turns a csv of places into a MATSim facilities file.
 */
public class RunCreateDiscretionaryActivityFacilitiesTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void writesFacilitiesTheAnalysisCanRead() throws IOException {

		Path input = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(input, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			super-7,Konsum Striesen,shop,Konsum,4621000,5820000,08:00:00,20:00:00
			super-8,Edeka Blasewitz,shop,Edeka,4622000,5821000,07:00:00,22:00:00
			gasthaus-2,Gasthaus Am Markt,restaurant,German,4622500,5821500,11:00:00,23:00:00
			park-1,Großer Garten,leisure,,4623000,5822000,,
			""");

		Path output = Path.of(utils.getOutputDirectory(), "discretionary_activity_facilities.xml.gz");
		Path summary = Path.of(utils.getOutputDirectory(), "places-summary.csv");

		new RunCreateDiscretionaryActivityFacilities().execute(
			"--input", input.toString(),
			"--output", output.toString(),
			"--crs", "EPSG:25833",
			"--summary", summary.toString()
		);

		assertTrue(Files.exists(output), "the facilities file should have been written");
		assertTrue(Files.exists(summary), "the summary should have been written");

		// the output is a plain MATSim facilities file
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		new MatsimFacilitiesReader(null, null, facilities).readFile(output.toString());

		assertEquals(4, facilities.getFacilities().size());
		assertEquals("EPSG:25833", ProjectionUtils.getCRS(facilities));

		// and the analysis reads its places back out of it
		CapabilityConfigGroup config = ConfigUtils.addOrGetModule(
			ConfigUtils.createConfig(new CapabilityConfigGroup()), CapabilityConfigGroup.class);
		DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(config).extract(facilities);

		assertEquals(4, places.size());
		assertEquals(List.of("leisure", "restaurant", "shop"), List.copyOf(places.getTypes()));

		// two supermarket brands to choose from, which is what the variety analysis needs
		assertEquals(2, places.getByType("shop").size());
		assertEquals(List.of("Konsum", "Edeka"), List.copyOf(places.getSubTypes("shop")));

		DiscretionaryActivityFacility konsum = places.getByType("shop").stream()
			.filter(place -> place.getSubType().equals(Optional.of("Konsum")))
			.findFirst().orElseThrow();

		assertEquals(Optional.of("Konsum Striesen"), konsum.getName());
		assertEquals(new Coord(4621000, 5820000), konsum.getCoord());
		assertTrue(konsum.isOpenDuring(17 * 3600, 18 * 3600));
		assertTrue(places.getByType("leisure").get(0).isAlwaysOpen());
	}

	@Test
	void failsOnABrokenCsv() throws IOException {

		Path input = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(input, """
			id,name,x,y
			super-7,Konsum Striesen,4621000,5820000
			""");

		// MATSimAppCommand.execute rethrows what the command threw
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> new RunCreateDiscretionaryActivityFacilities().execute(
				"--input", input.toString(),
				"--output", Path.of(utils.getOutputDirectory(), "out.xml.gz").toString()));

		assertTrue(e.getMessage().contains("type"), e.getMessage());
	}
}
