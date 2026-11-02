package org.matsim.contrib.msp.capability;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.msp.capability.prepare.DiscretionaryActivityFacilityCsv;
import org.matsim.contrib.msp.capability.result.CapabilityWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the capability analysis runs at the end of a MATSim run and writes its output into the output
 * directory of the run.
 */
public class CapabilityModuleTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void writesAnalysisAfterRun() throws IOException {

		URL context = ExamplesUtils.getTestScenarioURL("equil");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"), new CapabilityConfigGroup());

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);
		config.controller().setLastIteration(0);
		config.routing().getOrCreateModeRoutingParams(TransportMode.walk).setTeleportedModeSpeed(1.0);

		// equil calls home 'h' and work 'w'
		CapabilityConfigGroup capabilityConfig = ConfigUtils.addOrGetModule(config, CapabilityConfigGroup.class);
		capabilityConfig.setMandatoryActivities(Set.of("h", "w"));
		capabilityConfig.setHomeActivity("h");

		// equil's persons carry no 'age' attribute, so the age criterion would drop every one of them
		capabilityConfig.setMinAge(0);

		// equil is a toy network of 10 km links, so every car detour is far longer than the 30 minutes the
		// defaults allow. Widen the search here; the realistic defaults are exercised in
		// RunCapabilityAnalysisTest, whose network has street-sized links.
		capabilityConfig.getOrCreateDiscretionaryActivityParams("shop").setMaxExtraTravelTime(3 * 3600);

		// a shop in the middle of the equil network, open all day
		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.createDirectories(places.getParent());
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			shop-1,Equil Shop,shop,Konsum,0,0,06:00:00,22:00:00
			""");

		ActivityFacilities facilities = new DiscretionaryActivityFacilityCsv(capabilityConfig).read(places.toString());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(CapabilityModule.withPlaces(facilities));
		controler.run();

		Path analysis = Path.of(utils.getOutputDirectory(), CapabilityWriter.FACILITIES_FILE);
		assertTrue(Files.exists(analysis), "the capability analysis should have been written");

		List<CSVRecord> rows;
		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(analysis.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
			rows = parser.getRecords();
		}

		assertTrue(!rows.isEmpty(), "equil's agents should be able to reach the shop");

		DiscretionaryActivityParams shopParams = capabilityConfig.getOrCreateDiscretionaryActivityParams("shop");

		for (CSVRecord row : rows) {
			assertEquals("shop", row.get("act_type"));
			assertEquals("Konsum", row.get("facility_sub_type"));
			assertEquals("shop-1", row.get("facility_id"));
			assertTrue(Integer.parseInt(row.get("n_insertions")) >= 1);

			double slack = Double.parseDouble(row.get("best_slack"));
			double detour = Double.parseDouble(row.get("best_t_extra"));
			assertTrue(slack >= 0);
			assertTrue(detour >= 0);
			assertTrue(Double.parseDouble(row.get("sum_slack")) >= slack);
			assertTrue(Double.parseDouble(row.get("c_insertion_min")) >= 0);

			// the row is the utility of its own best insertion, cost included
			assertEquals(shopParams.getBaseUtility()
					+ shopParams.getFlexibilityUtility() * Math.log(1 + slack / shopParams.getReferenceDuration())
					- capabilityConfig.getValueOfTimePerHour() * detour / 3600,
				Double.parseDouble(row.get("u_potential_max")), 1e-3);
		}

		// every analysed person has a row on the person level, reachable shop or not
		Path persons = Path.of(utils.getOutputDirectory(), CapabilityWriter.PERSONS_FILE);
		assertTrue(Files.exists(persons));

		// the analysis is run for every configured mode
		assertEquals(capabilityConfig.getModes(),
			rows.stream().map(row -> row.get("mode")).collect(java.util.stream.Collectors.toSet()));
	}
}
