package org.matsim.contrib.accessibility.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class DrtAccessibilityTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDrtStopbasedExample() {
		Id.resetCaches();

		// C O N F I G
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
			"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		// drt
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup drtConfig : multiModeDrtConfig.getModalElements()) {
			drtConfig.maxWalkDistance = 100_000;
		}

		// accessibility
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setTileSize_m(100);
		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(-15181.2948);
		acg.setBoundingBoxTop(4967.218);
		acg.setBoundingBoxLeft(-4934.1583);
		acg.setBoundingBoxRight(12641.5889);
		acg.setUseParallelization(false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);

		// misc
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.routing().setRoutingRandomness(0);

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		// S C E N A R I O
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(3150.8321, -2587.9409));
		opportunities.addActivityFacility(facility1);
//		ActivityFacility facility2 = opportunities.getFactory().createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(200, 200));
//		opportunities.addActivityFacility(facility2);
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);


		// C O N T R O L E R

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		final AccessibilityModule module = new AccessibilityModule();

//		final TinyDRTAccessibilityTest.ResultsComparator resultsComparator = new TinyDRTAccessibilityTest.ResultsComparator();
//
//		resultsComparator.setConfig(config);
//		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);

		controler.run();

//		var expectedStats = Stats.newBuilder()
//			.rejectionRate(0.05)
//			.rejections(17)
//			.waitAverage(260.41)
//			.inVehicleTravelTimeMean(374.87)
//			.totalTravelTimeMean(635.28)
//			.build();

//		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}
}
