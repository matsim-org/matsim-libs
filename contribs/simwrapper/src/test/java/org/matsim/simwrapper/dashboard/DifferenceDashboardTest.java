package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.*;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

public class DifferenceDashboardTest {


		@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();
//	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
//	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
//	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
//	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc";
//	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
//

	@Test
	void generate() {

		Config config = TestScenario.loadConfig(utils);

		generateDummyCounts(config);

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		simWrapperConfigGroup.setSampleSize(0.01);

		SimWrapperConfigGroup.ContextParams contextParams = simWrapperConfigGroup.defaultParams();
		contextParams.setMapCenter("12,48.95");
		contextParams.setMapZoomLevel(9.0);

		simWrapperConfigGroup.setBasePath("/home/brendan/git/matsim-libs/contribs/simwrapper/test/output/org/matsim/simwrapper/dashboard/DashboardTests/ptCustom/");

		Dashboard TC_a = new TrafficCountsDashboard().withModes("car", Set.of(TransportMode.car))
			.withModes("truck", Set.of(TransportMode.truck, "freight"));
		TC_a.setPathToBaseCase(simWrapperConfigGroup.getBasePath());

		Dashboard TC_b = new TrafficCountsDashboard().withQualityLabels(
			List.of(0.0, 0.3, 1.7, 2.5),
			List.of("way too few", "fewer", "exact", "too much", "way too much")
		);
		TC_b.setPathToBaseCase(simWrapperConfigGroup.getBasePath());

		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(TC_a)
			.addDashboard(TC_b);

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.addOverridingModule(new CountsModule());
		controler.run();

		Path defaultDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic");
		Path carDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-car");
		Path truckDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-truck");

		for (Path dir : List.of(defaultDir, carDir, truckDir)) {
			Assertions.assertThat(dir)
				.isDirectoryContaining("glob:**count_comparison_daily.csv")
				.isDirectoryContaining("glob:**count_comparison_by_hour.csv");
		}
	}

	public void generateDummyCounts(Config config) {

		SplittableRandom random = new SplittableRandom(1234);

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Network network = NetworkUtils.readNetwork(context + config.network().getInputFile());

		List<? extends Link> links = List.copyOf(network.getLinks().values());
		int size = links.size();

		Counts<Link> counts = new Counts<>();

		for (int i = 0; i <= 100; i++) {
			Link link = links.get(random.nextInt(size));

			if (counts.getMeasureLocations().containsKey(link.getId()))
				continue;

			MeasurementLocation<Link> station = counts.createAndAddMeasureLocation(link.getId(), link.getId().toString() + "_count_station");

			Measurable carVolume = station.createVolume(TransportMode.car);
			Measurable freightVolume = station.createVolume(TransportMode.truck);

			for (int hour = 0; hour < 24; hour++) {
				carVolume.setAtHour(hour, random.nextInt(500));
				freightVolume.setAtHour(hour, random.nextInt(100));
			}
		}

		try {
			Files.createDirectories(Path.of(utils.getPackageInputDirectory()));
			String absolutPath = Path.of(utils.getPackageInputDirectory()).normalize().toAbsolutePath() + "/dummy_counts.xml";

			config.counts().setInputFile(absolutPath);
			new CountsWriter(counts).write(absolutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

//	private void runBaseScenario(Boolean baseRun) {
//		Config config = TestScenarioBase.loadConfig(utils);
//		ScenarioSetupInfo(config, baseRun);
//	}
//
//	private void runPolicyScenario(Boolean baseRun) {
//
//	}
//
//	/**
//	 * we set all vehicles to average except for KEXI vehicles, i.e. drt. Drt vehicles are set to electric light commercial vehicles.
//	 *
//	 * @param scenario scenario object for which to prepare vehicle types
//	 */
//	private void prepareVehicleTypes(Scenario scenario) {
//
//		Iterable<VehicleType> allTypes = Iterables.concat(scenario.getVehicles().getVehicleTypes().values(), scenario.getTransitVehicles().getVehicleTypes().values());
//
//		for (VehicleType type : allTypes) {
//			EngineInformation engineInformation = type.getEngineInformation();
//			VehicleUtils.setHbefaTechnology(engineInformation, "average");
//			VehicleUtils.setHbefaSizeClass(engineInformation, "average");
//			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
//				// consider transit vehicles as non-hbefa vehicles, i.e. ignore them
//				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
//			} else if (type.getId().toString().equals("car")) {
//				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
//				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
//			} else if (type.getId().toString().equals("conventional_vehicle") || type.getId().toString().equals("autonomous_vehicle")) {
//				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
//				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
//			} else if (type.getId().toString().equals("freight")) {
//				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
//				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
//			}
//		}
//	}
//
//	/**
//	 * changes/adds link attributes of the network in the given scenario.
//	 *
//	 * @param scenario for which to prepare the network
//	 */
//	private void prepareHbefaNetwork(Scenario scenario) {
//		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
//
//		// Remove "highway." prefix from type attributes
//		for (Link link : scenario.getNetwork().getLinks().values()) {
//			// pt links can be disregarded
//			if (!link.getAllowedModes().contains("pt")) {
//				NetworkUtils.setType(link, NetworkUtils.getType(link).replaceFirst("highway\\.", ""));
//			}
//		}
//
//		// Add HBEFA mappings to all links
//		roadTypeMapping.addHbefaMappings(scenario.getNetwork());
//
//	}
//
//	private void ScenarioSetupInfo(Config config, Boolean isBaseRun) {
////		Assumptions.assumeTrue(System.getenv("MATSIM_DECRYPTION_PASSWORD") != null);
//
//		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
//		simWrapperConfigGroup.setSampleSize(0.01);
//
//		SimWrapperConfigGroup.ContextParams contextParams = simWrapperConfigGroup.defaultParams();
//		simWrapperConfigGroup.setBasePath("/home/brendan/git/matsim-libs/contribs/simwrapper/test/output/org/matsim/simwrapper/dashboard/DashboardTests/ptCustom");
//
//		contextParams.setMapCenter("12,48.95");
//		contextParams.setMapZoomLevel(9.0);
//
////		var emissionsConfig = new EmissionsConfigGroup();
////		if (!isBaseRun) {
////			simWrapperConfigGroup.setBasePath("/home/brendan/git/matsim-libs/contribs/simwrapper/test/output/org/matsim/simwrapper/dashboard/output_base/");
////		}
//
////		emissionsConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
////		emissionsConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
////		emissionsConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
////		emissionsConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
////		emissionsConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
////		emissionsConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
//
//		SimWrapper sw = SimWrapper.create(config);
//
//		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
//
////		prepareVehicleTypes(controler.getScenario());
////		prepareHbefaNetwork(controler.getScenario());
////		config.addModule(emissionsConfig);
//
//		controler.run();
//
//		Path defaultDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic");
//		Path carDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-car");
//		Path truckDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-truck");
//	}

//}
